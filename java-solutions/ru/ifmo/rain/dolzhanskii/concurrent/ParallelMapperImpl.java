package ru.ifmo.rain.dolzhanskii.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

import static ru.ifmo.rain.dolzhanskii.concurrent.IterativeParallelism.joinAll;

/**
 * Implementation of {@link ParallelMapper}. Creates given number of threads upon creation and tasks queue.
 * Threads are waiting for new tasks to appear and greedily performs them. Tasks are expected to be mapping queries.
 */
@SuppressWarnings("unused")
public class ParallelMapperImpl implements ParallelMapper {
    private List<Thread> workers;
    private final Queue<Runnable> tasks;

    private void addTask(final Runnable newTask) throws InterruptedException {
        synchronized (tasks) {
            final int MAX_TASKS = 100;

            while (tasks.size() >= MAX_TASKS) {
                tasks.wait();
            }
            tasks.add(newTask);
            tasks.notifyAll();
        }
    }

    private void runTask() throws InterruptedException {
        Runnable currentTask;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            currentTask = tasks.poll();
            tasks.notifyAll();
        }
        currentTask.run();
    }

    private class SynchronizedList<T> {
        private List<T> list;
        private int countOfReady = 0;

        private boolean complete() {
            return countOfReady == list.size();
        }

        SynchronizedList(int size) {
            list = new ArrayList<>(Collections.nCopies(size, null));
        }

        synchronized void set(final int index, T value) {
            list.set(index, value);
            countOfReady++;
            if (complete()) {
                notify();
            }
        }

        synchronized List<T> asList() throws InterruptedException {
            while (!complete()) {
                wait();
            }
            return list;
        }
    }

    /**
     * Basic constructor. Creates given number of threads and initializes tasks queue.
     *
     * @param threads Number of threads to distribute tasks among
     */
    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads cannot be negative");
        }

        workers = new ArrayList<>();
        tasks = new ArrayDeque<>();

        final Runnable WORKER_ROUTINE = () -> {
            try {
                while (!Thread.interrupted()) {
                    runTask();
                }
            } catch (InterruptedException e) {
                // Ignored
            } finally {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < threads; i++) {
            workers.add(new Thread(WORKER_ROUTINE));
        }
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
        SynchronizedList<R> results = new SynchronizedList<>(list.size());
        List<RuntimeException> taskExceptions = new ArrayList<>();

        int index = 0;

        for (T target : list) {
            final T finalTarget = target;
            final int finalIndex = index++;
            addTask(() -> {
                R result = null;
                try {
                    result = function.apply(finalTarget);
                } catch (RuntimeException e) {
                    synchronized (taskExceptions) {
                        taskExceptions.add(e);
                    }
                }
                results.set(finalIndex, result);
            });
        }

        if (!taskExceptions.isEmpty()) {
            RuntimeException exception = new RuntimeException("Errors occurred during mapping process");
            taskExceptions.forEach(exception::addSuppressed);
            throw exception;
        }

        return results.asList();
    }

    /**
     * Terminates all generated threads.
     */
    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        try {
            joinAll(workers, true);
        } catch (InterruptedException e) {
            // Ignored
        }
    }
}
