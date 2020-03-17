package ru.ifmo.rain.dolzhanskii.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link ListIP} and {@link ScalarIP} interfaces using iterative parallelism.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 0.9
 */
@SuppressWarnings("unused")
public class IterativeParallelism implements AdvancedIP {
    private <T> List<List<T>> getListPerThreadDistribution(int threads, List<T> list) {
        List<List<T>> distribution = new ArrayList<>();
        int batchSize = list.size() / threads;
        int remainder = list.size() % threads;

        int index = 0;
        for (int thread = 0; thread < threads; thread++) {
            int currentBatchSize = batchSize + ((thread < remainder) ? 1 : 0);
            if (currentBatchSize > 0) {
                distribution.add(list.subList(index, index + currentBatchSize));
            }
            index += currentBatchSize;
        }

        return distribution;
    }

    private <T, B, R> R runIP(int threads, List<T> list, Function<Stream<T>, B> batchJob,
                              Function<Stream<B>, R> reduceFunction) throws InterruptedException {
        List<List<T>> batches = getListPerThreadDistribution(threads, list);
        threads = batches.size();
        List<B> batchResults = new ArrayList<>(Collections.nCopies(threads, null));
        List<InterruptedException> batchExceptions = new ArrayList<>();
        List<Thread> workers = new ArrayList<>();
        InterruptedException exception = new InterruptedException("Some threads were interrupted");

        for (int index = 0; index < threads; index++) {
            final int threadIndex = index;
            Thread thread = new Thread(
                    () -> batchResults.set(threadIndex, batchJob.apply(batches.get(threadIndex).stream())));
            workers.add(thread);
            thread.start();
        }

        workers.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                exception.addSuppressed(e);
            }
        });

        if (exception.getSuppressed().length != 0) {
            throw exception;
        }

        return reduceFunction.apply(batchResults.stream());
    }

    private <T> List<T> flatCollect(Stream<? extends Stream<? extends T>> streams) {
        return streams.flatMap(Function.identity()).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return mapReduce(threads, values, Function.<T>identity(), monoid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.filter(predicate),
                this::flatCollect);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.map(f),
                this::flatCollect);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.max(comparator).orElseThrow(),
                stream -> stream.max(comparator).orElseThrow());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.min(comparator).orElseThrow(),
                stream -> stream.min(comparator).orElseThrow());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue));
    }
}
