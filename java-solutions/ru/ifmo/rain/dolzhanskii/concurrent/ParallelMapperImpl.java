package ru.ifmo.rain.dolzhanskii.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of {@link ParallelMapper}. Creates given number of threads upon creation and tasks queue.
 * Threads are waiting for new tasks to appear and greedily performs them. Tasks are expected to be mapping queries.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 0.9
 */
@SuppressWarnings("unused")
public class ParallelMapperImpl implements ParallelMapper {
    private List<Thread> workers;
    private BlockingTaskQueue taskQueue;

    /**
     * Basic constructor. Creates given number of threads and initializes tasks queue.
     *
     * @param threads Number of threads to distribute tasks among
     */
    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads cannot be negative");
        }

        taskQueue = new BlockingTaskQueue();

        final Runnable workerRoutine = () -> {
            try {
                while (!Thread.interrupted()) {
                    taskQueue.getNext().run();
                }
            } catch (InterruptedException e) {
                // Ignored
            } finally {
                Thread.currentThread().interrupt();
            }
        };

        workers = IntStream.range(0, threads).mapToObj(i -> new Thread(workerRoutine))
                .collect(Collectors.toCollection(ArrayList::new));
        workers.forEach(Thread::start);
    }

    /**
     * Performs mapping query.
     *
     * @param function Map function to perform
     * @param list List of elements to map
     * @param <T> Map input type
     * @param <R> Map output type
     * @return List of mapped elements
     * @throws InterruptedException Transparent exception received from threads
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        CancellableTask<T, R> newTask;

        synchronized (this) {
            taskQueue.addTask(newTask = new CancellableTask<>(function, list));
        }

        return newTask.process();
    }

    /**
     * Terminates all generated threads.
     */
    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        synchronized (this) {
            taskQueue.forEach(CancellableTask::cancelExecution);
        }
        workers.forEach(thread -> {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException e) {
                    // Ignored
                }
            }
        });
    }

    private class BlockingTaskQueue {
        private Queue<CancellableTask> queue = new ArrayDeque<>();

        synchronized void addTask(CancellableTask task) {
            queue.add(task);
            notifyAll();
        }

        public synchronized void remove() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            queue.remove();
        }

        public synchronized Runnable getNext() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            Runnable value = queue.element().getNextMappingTask();
            if (queue.element().isFullyStarted()) {
                queue.remove();
            }
            return value;
        }

        public synchronized void forEach(Consumer<CancellableTask> consumer) {
            queue.forEach(consumer);
        }
    }

    private class CancellableTask<T, R> {
        private final Queue<Runnable> mappingTasks = new ArrayDeque<>();
        private final List<R> results;
        private final List<RuntimeException> errors = new ArrayList<>();

        private boolean isCancelled = false;
        private int awaitingStart;
        private int awaitingCompletion;

        CancellableTask(Function<? super T, ? extends R> mappingFunction, List<? extends T> input) {
            this.results = new ArrayList<>(Collections.nCopies(input.size(), null));
            this.awaitingStart = this.awaitingCompletion = input.size();

            int index = 0;
            for (T key : input) {
                int finalIndex = index;
                mappingTasks.add(() -> {
                    try {
                        setResult(finalIndex, mappingFunction.apply(key));
                    } catch (RuntimeException e) {
                        addError(e);
                    }
                });
                index++;
            }
        }

        public synchronized List<R> process() throws InterruptedException {
            while (!isCancelled) {
                wait();
            }

            if (errors.isEmpty()) {
                return results;
            } else {
                throw errors.stream().reduce((RuntimeException basic, RuntimeException other) -> {
                    basic.addSuppressed(other);
                    return basic;
                }).get();
            }
        }

        synchronized void cancelExecution() {
            this.isCancelled = true;
            notifyAll();
        }

        synchronized Runnable getNextMappingTask() {
            awaitingStart--;
            return mappingTasks.poll();
        }

        boolean isFullyStarted() {
            return awaitingStart == 0;
        }

        private synchronized void setResult(int index, R value) {
            results.set(index, value);
            registerCompletion();
        }

        private synchronized void addError(RuntimeException e) {
            errors.add(e);
            registerCompletion();
        }

        private synchronized void registerCompletion() {
            if (--awaitingCompletion == 0) {
                cancelExecution();
            }
        }
    }
//    private List<Thread> workers;
//    private final Queue<Runnable> tasks;
//
//    /**
//     * Basic constructor. Creates given number of threads and initializes tasks queue.
//     *
//     * @param threads Number of threads to distribute tasks among
//     */
//    public ParallelMapperImpl(final int threads) {
//        if (threads <= 0) {
//            throw new IllegalArgumentException("Number of threads cannot be negative");
//        }
//
//        tasks = new ArrayDeque<>();
//
//        final Runnable workerRoutine = () -> {
//            try {
//                while (!Thread.interrupted()) {
//                    runTask();
//                }
//            } catch (final InterruptedException e) {
//                // Ignored
//            } finally {
//                Thread.currentThread().interrupt();
//            }
//        };
//
//        workers = IntStream.range(0, threads).mapToObj(i -> new Thread(workerRoutine))
//                .collect(Collectors.toCollection(ArrayList::new));
//        workers.forEach(Thread::start);
//    }
//
//    /**
//     * Performs mapping query.
//     *
//     * @param function Map function to perform
//     * @param list List of elements to map
//     * @param <T> Map input type
//     * @param <R> Map output type
//     * @return List of mapped elements
//     * @throws InterruptedException Transparent exception received from threads
//     */
//    @Override
//    public <T, R> List<R> map(final Function<? super T, ? extends R> function, final List<? extends T> list)
//            throws InterruptedException {
//        final SynchronizedList<R> results = new SynchronizedList<>(list.size());
//        final SynchronizedList<RuntimeException> taskExceptions = new SynchronizedList<>(list.size());
//
//        int index = 0;
//
//        for (final T target : list) {
//            final int finalIndex = index++;
//            addTask(() -> {
//                R result = null;
//                RuntimeException exception = null;
//
//                try {
//                    result = function.apply(target);
//                } catch (final RuntimeException e) {
//                    exception = e;
//                }
//
//                results.set(finalIndex, result);
//                taskExceptions.set(finalIndex, exception);
//            });
//        }
//
//        taskExceptions.waitUntilComplete();
//
//        final RuntimeException exception = new RuntimeException("Errors occurred during mapping process");
//        taskExceptions.forEach(e -> {
//            if (e != null) {
//                exception.addSuppressed(e);
//            }
//        });
//        if (exception.getSuppressed().length != 0) {
//            throw exception;
//        }
//
//        return results.asList();
//    }
//
//    /**
//     * Terminates all generated threads.
//     */
//    @Override
//    public void close() {
//        workers.forEach(Thread::interrupt);
//        try {
//            joinAll(workers, true);
//        } catch (final InterruptedException e) {
//            // Ignored
//        }
//    }
//
//    private void addTask(final Runnable newTask) {
//        synchronized (tasks) {
//            tasks.add(newTask);
//            tasks.notifyAll();
//        }
//    }
//
//    private void runTask() throws InterruptedException {
//        final Runnable currentTask;
//        synchronized (tasks) {
//            while (tasks.isEmpty()) {
//                tasks.wait();
//            }
//            currentTask = tasks.poll();
//            tasks.notifyAll();
//        }
//        currentTask.run();
//    }
//
//    private class SynchronizedList<T> {
//        private final List<T> list;
//        private int remaining;
//
//        SynchronizedList() {
//            list = new ArrayList<>();
//            remaining = 0;
//        }
//
//        SynchronizedList(final int size) {
//            list = new ArrayList<>(Collections.nCopies(size, null));
//            remaining = list.size();
//        }
//
//        synchronized void set(final int index, final T value) {
//            list.set(index, value);
//            remaining--;
//            if (remaining == 0) {
//                notify();
//            }
//        }
//
//        synchronized void add(final T value) {
//            list.add(value);
//        }
//
//        synchronized void waitUntilComplete() throws InterruptedException {
//            while (remaining > 0) {
//                wait();
//            }
//        }
//
//        synchronized List<T> asList() throws InterruptedException {
//            waitUntilComplete();
//            return list;
//        }
//
//        synchronized boolean isEmpty() {
//            return list.isEmpty();
//        }
//
//        synchronized void forEach(Consumer<T> action) {
//            list.forEach(action);
//        }
//    }
}
