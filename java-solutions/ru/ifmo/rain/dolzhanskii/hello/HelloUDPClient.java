package ru.ifmo.rain.dolzhanskii.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ru.ifmo.rain.dolzhanskii.hello.HelloUDPUtils.log;

public class HelloUDPClient implements HelloClient {
    private static final int TERMINATION_AWAIT = 3;
    private static final int SOCKET_TIMEOUT_MS = 200;

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        try {
            final SocketAddress hostSocket = new InetSocketAddress(InetAddress.getByName(host), port);
            final ExecutorService clientService = Executors.newFixedThreadPool(threads);

            for (int i = 0; i < threads; i++) {
                final int threadId = i;
                clientService.submit(() -> spamRequests(hostSocket, prefix, threadId, requests));
            }

            clientService.shutdown();
            clientService.awaitTermination(TERMINATION_AWAIT * threads * requests, TimeUnit.SECONDS);
        } catch (final UnknownHostException e) {
            log(HelloUDPUtils.logType.ERROR, "Failed to resolve host");
        } catch (final InterruptedException e) {
            log(HelloUDPUtils.logType.ERROR, "Method had been interrupted");
        }
    }

    private void spamRequests(final SocketAddress hostSocket, final String prefix, final int threadId,
                              final int requests) {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);

            final int bufferSizeRx = socket.getReceiveBufferSize();
            final DatagramPacket packet = HelloUDPUtils.initPacket(bufferSizeRx);
            final byte[] bufferRx = packet.getData();

            for (int requestId = 0; requestId < requests; requestId++) {
                final String request = prefix + threadId + '_' + requestId;

                int attempt = 0;

                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    log(HelloUDPUtils.logType.INFO, threadId,
                            String.format("Sending message (attempt %d): '%s'", ++attempt, request));
                    final String response;

                    HelloUDPUtils.stringToPacket(packet, request, hostSocket);
                    if (HelloUDPUtils.send(packet, socket)) {
                        continue;
                    }

                    packet.setData(bufferRx, 0, bufferSizeRx);
                    if (HelloUDPUtils.receive(packet, socket)) {
                        continue;
                    }
                    response = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);

                    if (HelloUDPUtils.validate(response, threadId, requestId)) {
                        log(HelloUDPUtils.logType.INFO, threadId,
                                String.format("Received valid message: '%s'", response));
                        break;
                    } else {
                        log(HelloUDPUtils.logType.INFO, threadId,
                                String.format("Received invalid message: '%s'", response));
                    }
                }
            }
        } catch (final SocketException e) {
            log(HelloUDPUtils.logType.ERROR, "Socket failure, connection lost");
        }
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 5) {
            System.out.println("Usage: HelloUDPClient [URL|IP] port request_prefix threads_count requests_count");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Null arguments are not allowed");
            return;
        }

        try {
            final int port = Integer.parseInt(args[1]);
            final int threads = Integer.parseInt(args[3]);
            final int requests = Integer.parseInt(args[4]);

            new HelloUDPClient().run(args[0], port, args[2], threads, requests);
        } catch (final NumberFormatException e) {
            System.err.println("Failed to parse expected numeric argument: " + e.getMessage());
        }
    }
}
