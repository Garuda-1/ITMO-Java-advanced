package ru.ifmo.rain.dolzhanskii.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link ListIP} and {@link ScalarIP} interfaces using iterative parallelism.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 1.0
 * @see ParallelMapper
 */
@SuppressWarnings("unused")
public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;

    /**
     * Default constructor. When class is instantiated via it, separate threads are created for each task.
     */
    public IterativeParallelism() {
        parallelMapper = null;
    }

    /**
     * Default constructor. When class is instantiated via it, {@link ParallelMapper} is used to distribute tasks.
     *
     * @param parallelMapper Mapper to connect to
     */
    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Joins to threads and handles thrown exceptions.
     *
     * @param workers List of threads to join to
     * @param suppressExceptions Flag to define whether generate exception including suppressed ones from threads or not
     * @throws InterruptedException Exception concluding all throws exceptions by threads
     */
    static void joinAll(final List<Thread> workers, final boolean suppressExceptions) throws InterruptedException {
        final int threads = workers.size();
        for (int i = 0; i < threads; i++) {
            try {
                workers.get(i).join();
            } catch (final InterruptedException e) {
                final InterruptedException exception = new InterruptedException("Some threads were interrupted");
                exception.addSuppressed(e);
                for (int j = i; j < threads; j++) {
                    workers.get(j).interrupt();
                }
                for (int j = i; j < threads; j++) {
                    try {
                        workers.get(j).join();
                    } catch (final InterruptedException e1) {
                        exception.addSuppressed(e1);
                        j--;
                    }
                }
                if (!suppressExceptions) {
                    throw exception;
                }
            }
        }
    }

    private <T> List<List<T>> getListPerThreadDistribution(final int threads, final List<T> list) {
        if (threads < 0) {
            throw new IllegalArgumentException("Error: Threads count cannot be negative");
        }
        final List<List<T>> distribution = new ArrayList<>();
        final int batchSize = list.size() / threads;
        final int remainder = list.size() % threads;

        int index = 0;
        for (int thread = 0; thread < threads; thread++) {
            final int currentBatchSize = batchSize + ((thread < remainder) ? 1 : 0);
            if (currentBatchSize > 0) {
                distribution.add(list.subList(index, index + currentBatchSize));
            }
            index += currentBatchSize;
        }

        return distribution;
    }

    private <T> T runIP(final int threads, final List<T> list, final Function<Stream<T>, T> batchAndReduceJob)
            throws InterruptedException {
        return runIP(threads, list, batchAndReduceJob, batchAndReduceJob);
    }

    private <T, B, R> List<B> runIPWithoutMapper(int threads, final List<List<T>> batches,
                                                 final Function<Stream<T>, B> batchJob) throws InterruptedException {
        List<B> batchResults = new ArrayList<>(Collections.nCopies(threads, null));
        final List<Thread> workers = new ArrayList<>();

        for (int index = 0; index < threads; index++) {
            final int threadIndex = index;
            final Thread thread = new Thread(
                    () -> batchResults.set(threadIndex, batchJob.apply(batches.get(threadIndex).stream())));
            workers.add(thread);
            thread.start();
        }

        joinAll(workers, false);

        return batchResults;
    }

    private <T, B, R> R runIP(int threads, final List<T> list, final Function<Stream<T>, B> batchJob,
                                     final Function<Stream<B>, R> reduceFunction) throws InterruptedException {
        final List<List<T>> batches = getListPerThreadDistribution(threads, list);
        threads = batches.size();
        final List<B> batchResults;

        if (parallelMapper == null) {
            batchResults = runIPWithoutMapper(threads, batches, batchJob);
        } else {
            batchResults = parallelMapper.map(batchJob,
                    batches.stream().map(List::stream).collect(Collectors.toList()));
        }

        return reduceFunction.apply(batchResults.stream());
    }

    private static <T> List<T> flatCollect(final Stream<? extends Stream<? extends T>> streams) {
        return streams.flatMap(Function.identity()).collect(Collectors.toList());
    }

    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift,
                              final Monoid<R> monoid)
            throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator()));
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    private <T, U> List<U> flatQuery(final int threads, final List<? extends T> values,
                                     final Function<Stream<? extends T>, Stream<? extends U>> function)
            throws InterruptedException {
        return runIP(threads, values, stream -> function.apply(stream).collect(Collectors.toList()).stream(),
                IterativeParallelism::flatCollect);
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return flatQuery(threads, values, stream -> stream.filter(predicate));
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values,
                              final Function<? super T, ? extends U> f) throws InterruptedException {
        return flatQuery(threads, values, stream -> stream.map(f));
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.max(comparator).orElseThrow());
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return runIP(threads, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }
}
