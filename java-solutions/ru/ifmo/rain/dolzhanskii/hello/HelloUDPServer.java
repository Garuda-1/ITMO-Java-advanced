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
import java.util.stream.IntStream;

public class HelloUDPServer implements HelloServer {
    private static final int TERMINATION_AWAIT = 1;

    private ExecutorService listeners;
    private DatagramSocket socket;

    @Override
    public void start(final int port, final int threads) {
        try {
            socket = new DatagramSocket(port);
            final int bufferSizeRx = socket.getReceiveBufferSize();

            listeners = Executors.newFixedThreadPool(threads);

            IntStream.range(0, threads).forEach(i -> listeners.submit(() -> listen(socket, bufferSizeRx)));
        } catch (final SocketException e) {
            HelloUDPUtils.log(HelloUDPUtils.logType.ERROR, "Failed to start socket: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        listeners.shutdown();
        socket.close();
        try {
            listeners.awaitTermination(TERMINATION_AWAIT, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            // Ignored
        }
    }

    private static void listen(final DatagramSocket socket, final int bufferSizeRx) {
        final byte[] bufferRx = new byte[bufferSizeRx];
        final DatagramPacket packet = new DatagramPacket(bufferRx, bufferSizeRx);

        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            packet.setData(bufferRx, 0, bufferSizeRx);

            try {
                socket.receive(packet);
            } catch (final IOException e) {
                if (!socket.isClosed()) {
                    HelloUDPUtils.log(HelloUDPUtils.logType.ERROR,
                            "Error occurred during receiving packet from socket: " + e.getMessage());
                }
                continue;
            }

            final String request = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                    StandardCharsets.UTF_8);
            final String response = "Hello, " + request;
            HelloUDPUtils.stringToPacket(packet, response, packet.getSocketAddress());

            try {
                socket.send(packet);
            } catch (final IOException e) {
                if (!socket.isClosed()) {
                    HelloUDPUtils.log(HelloUDPUtils.logType.ERROR,
                            "Error occurred in attempt to send response: " + new String(packet.getData()));
                }
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

        try (HelloUDPServer server = new HelloUDPServer()) {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);

            server.start(port, threads);

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Server has been started. Press any key to terminate");
            reader.readLine();
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse expected numeric argument: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO error occurred: " + e.getMessage());
        }
    }
}
