package ru.ifmo.rain.dolzhanskii.crawler;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * Implementation of {@link Crawler} using breadth-first search (BFS). This file is a public shell of implementation.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 0.9
 * @see BfsWebCrawler
 */
public class WebCrawler implements Crawler {
    private static final int AWAIT_TERMINATION = 1;

    private final Downloader downloader;
    private final ExecutorService extractorsExecutorService;
    private final ExecutorService downloadersExecutorService;
    private final int perHost;

    /**
     * Instance constructor which allows defining critical variables.
     *
     * @param downloader Implementation of {@link Downloader} interface to use for page downloading
     * @param downloaders Maximum number of concurrent download operations
     * @param extractors Maximum number of concurrent links extraction operations
     * @param perHost Maximum number of concurrent files downloads from unique host
     */
    @SuppressWarnings("WeakerAccess")
    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        this.extractorsExecutorService = Executors.newFixedThreadPool(extractors);
        this.downloadersExecutorService = Executors.newFixedThreadPool(downloaders);
        this.perHost = perHost;
    }

    @Override
    public Result download(final String initialLink, final int depth) {
        return new BfsWebCrawler(initialLink, depth, perHost, downloader,
                extractorsExecutorService, downloadersExecutorService).collectResult();
    }

    @Override
    public void close() {
        extractorsExecutorService.shutdown();
        downloadersExecutorService.shutdown();
        try {
            extractorsExecutorService.awaitTermination(AWAIT_TERMINATION, TimeUnit.SECONDS);
            downloadersExecutorService.awaitTermination(AWAIT_TERMINATION, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            // Ignored
        }
    }

    /**
     * Main method to provide console interface.
     *
     * Usage format: {@code WebCrawler url [depth [downloads [extractors [perHost]]]]}
     *
     * Instantiates {@link WebCrawler} provided with given arguments from the command line. All optional arguments
     * default value is set to {@code 1}. {@link CachingDownloader} implementation of {@link Downloader} is used.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        if (args == null || args.length == 0 || args.length > 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.out.println("Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        try {
            final String initialLink = args[0];
            final int depth = getIntArgOrDefault(args, 1);
            final int downloaders = getIntArgOrDefault(args, 2);
            final int extractors = getIntArgOrDefault(args, 3);
            final int perHost = getIntArgOrDefault(args, 4);

            try (final Crawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
                final Result result = crawler.download(initialLink, depth);

                System.out.println("Successfully visited pages:");
                result.getDownloaded().forEach(System.out::println);

                if (!result.getErrors().isEmpty()) {
                    System.out.println("Failed to visit pages:");
                    result.getErrors().keySet().forEach(System.out::println);
                }
            } catch (final IOException e) {
                System.err.println("Failed to initialize downloader: " + e.getMessage());
            }
        } catch (final NumberFormatException e) {
            System.err.println("Invalid argument format: Integer argument expected");
        }
    }

    private static int getIntArgOrDefault(final String[] args, final int index) {
        return index >= args.length ? 1 : Integer.parseInt(args[index]);
    }
}
