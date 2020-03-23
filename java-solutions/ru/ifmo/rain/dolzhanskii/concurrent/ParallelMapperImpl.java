package ru.ifmo.rain.dolzhanskii.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

import static ru.ifmo.rain.dolzhanskii.concurrent.IterativeParallelism.joinAll;

@SuppressWarnings("unused")
public class ParallelMapperImpl implements ParallelMapper {
    private final int THREADS;

    private List<Thread> workers;
    private final Queue<Runnable> tasks;

    private void addTask(final Runnable newTask) throws InterruptedException {
        synchronized (tasks) {
            final int MAX_TASKS = 10000;
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

    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads cannot be negative");
        }
        this.THREADS = threads;

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

        for (int i = 0; i < THREADS; i++) {
            workers.add(new Thread(WORKER_ROUTINE));
        }
        workers.forEach(Thread::start);
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        SynchronizedList<R> results = new SynchronizedList<>(list.size());
        List<RuntimeException> taskExceptions = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            addTask(() -> {
                R result = null;
                try {
                    result = function.apply(list.get(index));
                } catch (RuntimeException e) {
                    synchronized (taskExceptions) {
                        taskExceptions.add(e);
                    }
                }
                results.set(index, result);
            });
        }

        if (!taskExceptions.isEmpty()) {
            RuntimeException exception = new RuntimeException("Errors occurred during mapping process");
            taskExceptions.forEach(exception::addSuppressed);
            throw exception;
        }

        return results.asList();
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        try {
            joinAll(THREADS, workers);
        } catch (InterruptedException e) {
            // Ignored
        }
    }
}
