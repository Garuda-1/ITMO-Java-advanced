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
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {
            SocketAddress hostSocket = new InetSocketAddress(InetAddress.getByName(host), port);
            ExecutorService clientService = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; i++) {
                int threadId = i;
                clientService.submit(() -> spamRequests(hostSocket, prefix, threadId, requests));
            }
            clientService.shutdown();
            clientService.awaitTermination(5 * threads * requests, TimeUnit.SECONDS);
        } catch (UnknownHostException e) {
            log(HelloUDPUtils.logType.ERROR, "Failed to resolve host");
        } catch (InterruptedException e) {
            log(HelloUDPUtils.logType.ERROR, "Method had been interrupted");
        }
    }

    private void spamRequests(SocketAddress hostSocket, String prefix, int threadId, int requests) {
        try (DatagramSocket socket = new DatagramSocket()) {
            int bufferSizeRx = socket.getReceiveBufferSize();

            for (int request = 0; request < requests; request++) {
                String message = prefix + threadId + '_' + request;
                byte[] payload = message.getBytes(StandardCharsets.UTF_8);

                int attempt = 0;

                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    log(HelloUDPUtils.logType.INFO, threadId,
                            String.format("Sending message (attempt %d): '%s'", ++attempt, message));
                    String response;

                    try {
                        DatagramPacket packetTx = new DatagramPacket(payload, payload.length, hostSocket);
                        socket.send(packetTx);
                    } catch (IOException e) {
                        log(HelloUDPUtils.logType.ERROR, threadId, "Error occurred during attempt to send");
                        continue;
                    }
                    try {
                        DatagramPacket packetRx = new DatagramPacket(new byte[bufferSizeRx], bufferSizeRx, hostSocket);
                        socket.receive(packetRx);
                        response = new String(packetRx.getData(), packetRx.getOffset(), packetRx.getLength(),
                                StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        log(HelloUDPUtils.logType.ERROR, threadId, "Error occurred during attempt to receive");
                        continue;
                    }

                    log(HelloUDPUtils.logType.INFO, threadId, String.format("Received message: '%s'", response));
                    break;
                }
            }
        } catch (SocketException e) {
            log(HelloUDPUtils.logType.ERROR, "Socket failure, connection lost");
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.out.println("Usage: HelloUDPClient [URL|IP] port request_prefix threads_count requests_count");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Null arguments are not allowed");
            return;
        }

        try {
            int port = Integer.parseInt(args[1]);
            int threads = Integer.parseInt(args[3]);
            int requests = Integer.parseInt(args[4]);

            new HelloUDPClient().run(args[0], port, args[2], threads, requests);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse expected numeric argument: " + e.getMessage());
        }
    }
}
