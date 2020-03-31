package ru.ifmo.rain.dolzhanskii.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.ifmo.rain.dolzhanskii.concurrent.IterativeParallelism.joinAll;

/**
 * Implementation of {@link ParallelMapper}. Creates given number of threads upon creation and tasks queue.
 * Threads are waiting for new tasks to appear and greedily performs them. Tasks are expected to be mapping queries.
 */
@SuppressWarnings("unused")
public class ParallelMapperImpl implements ParallelMapper {
    private List<Thread> workers;
    private final Queue<Runnable> tasks;

    /**
     * Basic constructor. Creates given number of threads and initializes tasks queue.
     *
     * @param threads Number of threads to distribute tasks among
     */
    public ParallelMapperImpl(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads cannot be negative");
        }

        tasks = new ArrayDeque<>();

        final Runnable workerRoutine = () -> {
            try {
                while (!Thread.interrupted()) {
                    runTask();
                }
            } catch (final InterruptedException e) {
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
    public <T, R> List<R> map(final Function<? super T, ? extends R> function, final List<? extends T> list)
            throws InterruptedException {
        final SynchronizedList<R> results = new SynchronizedList<>(list.size());
        final SynchronizedList<RuntimeException> taskExceptions = new SynchronizedList<>(list.size());

        int index = 0;

        for (final T target : list) {
            final int finalIndex = index++;
            addTask(() -> {
                R result = null;
                RuntimeException exception = null;

                try {
                    result = function.apply(target);
                } catch (final RuntimeException e) {
                    exception = e;
                }

                results.set(finalIndex, result);
                taskExceptions.set(finalIndex, exception);
            });
        }

        taskExceptions.waitUntilComplete();

        final RuntimeException exception = new RuntimeException("Errors occurred during mapping process");
        taskExceptions.forEach(e -> {
            if (e != null) {
                exception.addSuppressed(e);
            }
        });
        if (exception.getSuppressed().length != 0) {
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
        } catch (final InterruptedException e) {
            // Ignored
        }
    }

    private void addTask(final Runnable newTask) {
        synchronized (tasks) {
            tasks.add(newTask);
            tasks.notifyAll();
        }
    }

    private void runTask() throws InterruptedException {
        final Runnable currentTask;
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
        private final List<T> list;
        private int remaining;

        SynchronizedList() {
            list = new ArrayList<>();
            remaining = 0;
        }

        SynchronizedList(final int size) {
            list = new ArrayList<>(Collections.nCopies(size, null));
            remaining = list.size();
        }

        synchronized void set(final int index, final T value) {
            list.set(index, value);
            remaining--;
            if (remaining == 0) {
                notify();
            }
        }

        synchronized void add(final T value) {
            list.add(value);
        }

        synchronized void waitUntilComplete() throws InterruptedException {
            while (remaining > 0) {
                wait();
            }
        }

        synchronized List<T> asList() throws InterruptedException {
            waitUntilComplete();
            return list;
        }

        synchronized boolean isEmpty() {
            return list.isEmpty();
        }

        synchronized void forEach(Consumer<T> action) {
            list.forEach(action);
        }
    }
}
