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
    BfsWebCrawler(String initialLink, int depth, int perHost, Downloader downloader,
                  ExecutorService extractorsExecutorService, ExecutorService downloadersExecutorService) {
        this.perHost = perHost;
        this.downloader = downloader;
        this.extractorsExecutorService = extractorsExecutorService;
        this.downloadersExecutorService = downloadersExecutorService;

        layerLinksQueue.add(initialLink);
        bfs(depth);
    }

    /**
     * Returns search result, containing list of successfully visited URLs and map of erroneous URLs with erro messages.
     *
     * @return Structure containing mentioned above data
     */
    Result collectResult() {
        return new Result(new ArrayList<>(downloaded), errors);
    }

    private class HostProcessor {
        private final Queue<Runnable> tasksQueue;
        private int running;

        HostProcessor() {
            this.tasksQueue = new ArrayDeque<>();
            this.running = 0;
        }

        synchronized void addTask(Runnable task) {
            tasksQueue.add(task);
            submitNextTask(false);
        }

        synchronized private void submitNextTask(boolean finishedPrevious) {
            if (finishedPrevious) {
                running--;
            }
            if (running < perHost) {
                Runnable task = tasksQueue.poll();
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

    private void bfs(int depth) {
        for (int currentDepth = 0; currentDepth < depth; currentDepth++) {
            final Phaser layerPhaser = new Phaser(1);
            boolean keepCrawling = currentDepth < depth - 1;

            List<String> layerLinks = new ArrayList<>(layerLinksQueue);

            layerLinksQueue.clear();

            layerLinks
                    .stream().filter(visitedLinks::add)
                    .forEach(link -> enqueueDownload(link, keepCrawling, layerPhaser));

            layerPhaser.arriveAndAwaitAdvance();
        }
    }

    private void enqueueDownload(String link, boolean keepCrawling, Phaser layerPhaser) {
        String host;
        try {
            host = URLUtils.getHost(link);
        } catch (MalformedURLException e) {
            errors.put(link, e);
            return;
        }

        HostProcessor hostProcessor = hostProcessorMap.computeIfAbsent(host, s -> new HostProcessor());
        layerPhaser.register();
        hostProcessor.addTask(() -> {
            try {
                Document document = downloader.download(link);
                downloaded.add(link);
                if (keepCrawling) {
                    enqueueExtraction(document, layerPhaser);
                }
            } catch (IOException e) {
                errors.put(link, e);
            } finally {
                layerPhaser.arriveAndDeregister();
            }
        });
    }

    private void enqueueExtraction(Document document, Phaser layerPhaser) {
        layerPhaser.register();
        extractorsExecutorService.submit(() -> {
            try {
                layerLinksQueue.addAll(document.extractLinks());
            } catch (IOException e) {
                // Ignored
            } finally {
                layerPhaser.arriveAndDeregister();
            }
        });
    }
}
