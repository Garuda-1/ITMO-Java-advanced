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
    private final List<Thread> threads;
    private final BlockingTaskQueue taskQueue;
    private boolean shutdown = false;

    /**
     * Basic constructor. Creates given number of threads and initializes tasks queue.
     *
     * @param threads Number of threads to distribute tasks among
     */
    public ParallelMapperImpl(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads cannot be negative");
        }

        taskQueue = new BlockingTaskQueue();

        final Runnable workerRoutine = () -> {
            try {
                while (!Thread.interrupted()) {
                    taskQueue.getNextTask().run();
                }
            } catch (final InterruptedException e) {
                // Ignored
            } finally {
                Thread.currentThread().interrupt();
            }
        };

        this.threads = IntStream.range(0, threads).mapToObj(i -> new Thread(workerRoutine))
                .collect(Collectors.toCollection(ArrayList::new));
        this.threads.forEach(Thread::start);
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
    public <T, R> List<R> map(final Function<? super T, ? extends R> function, final List<? extends T> list)
            throws InterruptedException {
        final MappingTaskBatch<T, R> newBatch;

        synchronized (this) {
            if (shutdown) {
                throw new IllegalStateException("Attempted to enqueue task to shut down mapper");
            }
            newBatch = new MappingTaskBatch<>(function, list);
            taskQueue.addTask(newBatch);
        }

        return newBatch.getResult();
    }

    /**
     * Terminates all generated threads.
     */
    @Override
    public void close() {
        synchronized (this) {
            shutdown = true;
            threads.forEach(Thread::interrupt);
            taskQueue.forEach(MappingTaskBatch::cancel);
        }
        threads.forEach(thread -> {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (final InterruptedException e) {
                    // Ignored
                }
            }
        });
    }

    private class BlockingTaskQueue {
        private final Queue<MappingTaskBatch<?, ?>> queue = new ArrayDeque<>();

        synchronized void addTask(final MappingTaskBatch<?, ?> task) {
            queue.add(task);
            notify();
        }

        synchronized Runnable getNextTask() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }

            final MappingTaskBatch<?, ?> head = queue.element();
            final Runnable task = head.getNextMappingTask();
            if (head.isFullyStarted()) {
                queue.remove();
            }

            return task;
        }

        synchronized void forEach(final Consumer<MappingTaskBatch<?, ?>> consumer) {
            queue.forEach(consumer);
        }
    }

    private class MappingTaskBatch<T, R> {
        private final Queue<Runnable> mappingTasks = new ArrayDeque<>();
        private final List<R> results;
        private final List<RuntimeException> errors = new ArrayList<>();

        private boolean doneOrCancelled = false;
        private int awaitingStart;
        private int awaitingCompletion;

        MappingTaskBatch(final Function<? super T, ? extends R> mappingFunction, final List<? extends T> input) {
            this.results = new ArrayList<>(Collections.nCopies(input.size(), null));
            this.awaitingStart = this.awaitingCompletion = input.size();

            int index = 0;
            for (final T key : input) {
                final int finalIndex = index;
                mappingTasks.add(() -> {
                    try {
                        setResult(finalIndex, mappingFunction.apply(key));
                    } catch (final RuntimeException e) {
                        addError(e);
                    }
                });
                index++;
            }
        }

        public synchronized List<R> getResult() throws InterruptedException {
            while (!doneOrCancelled) {
                wait();
            }

            if (errors.isEmpty()) {
                return results;
            } else {
                final RuntimeException first = errors.get(0);
                errors.stream().skip(1).forEach(first::addSuppressed);
                throw first;
            }
        }

        void cancel() {
            this.doneOrCancelled = true;
            notifyAll();
        }

        synchronized Runnable getNextMappingTask() {
            awaitingStart--;
            return mappingTasks.poll();
        }

        synchronized boolean isFullyStarted() {
            return awaitingStart <= 0;
        }

        private synchronized void setResult(final int index, final R value) {
            results.set(index, value);
            registerCompletion();
        }

        private synchronized void addError(final RuntimeException e) {
            errors.add(e);
            registerCompletion();
        }

        private synchronized void registerCompletion() {
            if (--awaitingCompletion == 0) {
                cancel();
            }
        }
    }
}
