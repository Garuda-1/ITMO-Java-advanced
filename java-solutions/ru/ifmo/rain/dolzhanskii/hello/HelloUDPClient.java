package ru.ifmo.rain.dolzhanskii.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ru.ifmo.rain.dolzhanskii.hello.HelloUDPUtils.*;

public class HelloUDPClient implements HelloClient {
    private static final int TERMINATION_AWAIT = 3;

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
            logError("Failed to resolve host", e);
        } catch (final InterruptedException e) {
            logError("Method had been interrupted", e);
        }
    }

    private void spamRequests(final SocketAddress hostSocket, final String prefix, final int threadId,
                              final int requests) {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);

            final int bufferSizeRx = socket.getReceiveBufferSize();
            final DatagramPacket packet = initPacket(bufferSizeRx);
            final byte[] bufferRx = packet.getData();

            for (int requestId = 0; requestId < requests; requestId++) {
                final String request = buildClientRequest(prefix, threadId, requestId);

                int attempt = 0;

                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    logInfo(threadId, String.format("Sending message (attempt %d): '%s'", ++attempt, request));
                    final String response;

                    stringToPacket(packet, request, hostSocket);
                    if (send(packet, socket)) {
                        continue;
                    }

                    packet.setData(bufferRx, 0, bufferSizeRx);
                    if (receive(packet, socket)) {
                        continue;
                    }
                    response = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                            StandardCharsets.UTF_8);

                    if (validate(response, threadId, requestId)) {
                        break;
                    }
                }
            }
        } catch (final SocketException e) {
            logError("Socket failure, connection lost", e);
        }
    }

    public static void main(final String[] args) {
        clientMain(args, HelloUDPClient.class);
    }
}
