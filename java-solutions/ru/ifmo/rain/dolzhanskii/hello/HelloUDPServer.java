package ru.ifmo.rain.dolzhanskii.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private static final int TERMINATION_AWAIT = 1;

    private ExecutorService listener;
    private ExecutorService responders;
    private DatagramSocket socket;

    @Override
    public void start(final int port, final int threads) {
        try {
            socket = new DatagramSocket(port);
            final int bufferSizeRx = socket.getReceiveBufferSize();

            responders = Executors.newFixedThreadPool(threads);
            listener = Executors.newSingleThreadExecutor();
            listener.submit(() -> listen(socket, bufferSizeRx));
        } catch (final SocketException e) {
            HelloUDPUtils.log(HelloUDPUtils.logType.ERROR, "Failed to start socket");
        }
    }

    @Override
    public void close() {
        responders.shutdown();
        listener.shutdown();
        socket.close();
        try {
            listener.awaitTermination(TERMINATION_AWAIT, TimeUnit.SECONDS);
            responders.awaitTermination(TERMINATION_AWAIT, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            // Ignored
        }
    }

    private void listen(final DatagramSocket socket, final int bufferSizeRx) {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            final DatagramPacket packet = HelloUDPUtils.createEmptyPacket(bufferSizeRx);

            try {
                socket.receive(packet);
            } catch (final IOException e) {
                HelloUDPUtils.log(HelloUDPUtils.logType.ERROR,
                        "Error occurred during receiving packet from socket: " + e.getMessage());
                continue;
            }

            responders.submit(() -> respond(socket, packet));
        }
    }

    private void respond(final DatagramSocket socket, final DatagramPacket packet) {
        final String request = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                StandardCharsets.UTF_8);

        final String response = "Hello, " + request;
        HelloUDPUtils.stringToPacket(packet, response, packet.getSocketAddress());

        try {
            socket.send(packet);
        } catch (final IOException e) {
            if (!socket.isClosed()) {
                HelloUDPUtils.log(HelloUDPUtils.logType.ERROR, "Error occurred in attempt to send response: " +
                        response);
            }
        }
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Usage: HelloUDPServer port threads_count");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Null arguments are not allowed");
            return;
        }

        try (final HelloUDPServer server = new HelloUDPServer()) {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);

            server.start(port, threads);

            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Server has been started. Press any key to terminate");
            reader.readLine();
        } catch (final NumberFormatException e) {
            System.err.println("Failed to parse expected numeric argument: " + e.getMessage());
        } catch (final IOException e) {
            System.err.println("IO error occurred: " + e.getMessage());
        }
    }
}
