package ru.ifmo.rain.dolzhanskii.crawler;

import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Breadth-first search (BFS) over web implementation.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 0.9
 */
class BfsWebCrawler {
    private final Set<String> downloaded = ConcurrentHashMap.newKeySet();
    private final Map<String, IOException> errors = new ConcurrentHashMap<>();

    private final Set<String> visitedLinks = ConcurrentHashMap.newKeySet();
    private final Queue<String> layerLinksQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, HostProcessor> hostProcessorMap = new ConcurrentHashMap<>();

    private final int perHost;
    private final Downloader downloader;
    private final ExecutorService extractorsExecutorService;
    private final ExecutorService downloadersExecutorService;
    private final int depth;

    /**
     * Instance constructor. Commences search immediately upon instantiation.
     *
     * @param initialLink URL link to start crawling from
     * @param depth Search depth
     * @param perHost Maximum number of concurrent downloads from unique host
     * @param downloader Implementation of {@link Downloader} interface to use for page downloading
     * @param extractorsExecutorService Pool of streams designed to perform links extraction operations
     * @param downloadersExecutorService Pool of streams designed to perform files downloading operations
     */
    BfsWebCrawler(final String initialLink, final int depth, final int perHost, final Downloader downloader,
                  final ExecutorService extractorsExecutorService, final ExecutorService downloadersExecutorService) {
        this.perHost = perHost;
        this.downloader = downloader;
        this.extractorsExecutorService = extractorsExecutorService;
        this.downloadersExecutorService = downloadersExecutorService;
        this.depth = depth;

        layerLinksQueue.add(initialLink);
    }

    /**
     * Returns search result, containing list of successfully visited URLs and map of erroneous URLs with
     * error messages.
     *
     * @return Structure containing mentioned above data
     */
    Result collectResult() {
        bfs(depth);
        return new Result(new ArrayList<>(downloaded), errors);
    }

    private void bfs(final int depth) {
        for (int currentDepth = 0; currentDepth < depth; currentDepth++) {
            final Phaser layerPhaser = new Phaser(1);
            final boolean keepCrawling = currentDepth < depth - 1;

            final Queue<String> previousLayerLinksQueue = new ArrayDeque<>(layerLinksQueue);
            layerLinksQueue.clear();
            previousLayerLinksQueue
                    .stream()
                    .filter(visitedLinks::add)
                    .forEach(link -> processLink(link, keepCrawling, layerPhaser));

            layerPhaser.arriveAndAwaitAdvance();
        }
    }

    private void processLink(final String link, final boolean keepCrawling, final Phaser layerPhaser) {
        final String host;
        try {
            host = URLUtils.getHost(link);
        } catch (final MalformedURLException e) {
            errors.put(link, e);
            return;
        }

        layerPhaser.register();
        hostProcessorMap.computeIfAbsent(host, s -> new HostProcessor()).submit(() -> {
            try {
                final Document document = downloader.download(link);
                downloaded.add(link);

                if (keepCrawling) {
                    extract(layerPhaser, document);
                }
            } catch (final IOException e) {
                errors.put(link, e);
            } finally {
                layerPhaser.arriveAndDeregister();
            }
        });
    }

    private void extract(final Phaser layerPhaser, final Document document) {
        layerPhaser.register();

        extractorsExecutorService.submit(() -> {
            try {
                layerLinksQueue.addAll(document.extractLinks());
            } catch (final IOException e) {
                // Ignored
            } finally {
                layerPhaser.arriveAndDeregister();
            }
        });
    }

    private class HostProcessor {
        private final Queue<Runnable> tasksQueue = new ArrayDeque<>();
        private int running;

        synchronized void submit(final Runnable task) {
            tasksQueue.add(task);
            submitNextTask(false);
        }

        synchronized private void submitNextTask(final boolean finishedPrevious) {
            if (finishedPrevious) {
                running--;
            }
            if (running < perHost) {
                final Runnable task = tasksQueue.poll();
                if (task != null) {
                    running++;
                    downloadersExecutorService.submit(() -> {
                        try {
                            task.run();
                        } finally {
                            submitNextTask(true);
                        }
                    });
                }
            }
        }
    }
}
