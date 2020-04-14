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
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.extractorsExecutorService = Executors.newFixedThreadPool(downloaders);
        this.downloadersExecutorService = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    @Override
    public Result download(String initialLink, int depth) {
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
        } catch (InterruptedException e) {
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
    public static void main(String[] args) {
        if (args == null || args.length == 0 || args.length > 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.out.println("Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        try {
            String initialLink = args[0];
            int depth = getIntArgOrDefault(args, 1);
            int downloaders = getIntArgOrDefault(args, 2);
            int extractors = getIntArgOrDefault(args, 3);
            int perHost = getIntArgOrDefault(args, 4);

            try (Crawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
                Result result = crawler.download(initialLink, depth);

                System.out.println("Successfully visited pages:");
                result.getDownloaded().forEach(System.out::println);

                if (!result.getErrors().isEmpty()) {
                    System.out.println("Failed to visit pages:");
                    result.getErrors().keySet().forEach(System.out::println);
                }
            } catch (IOException e) {
                System.err.println("Failed to initialize downloader: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid argument format: Integer argument expected");
        }
    }

    private static int getIntArgOrDefault(String[] args, int index) {
        return index >= args.length ? 1 : Integer.parseInt(args[index]);
    }
}
