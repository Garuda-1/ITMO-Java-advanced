package ru.ifmo.rain.dolzhanskii.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static ru.ifmo.rain.dolzhanskii.hello.HelloUDPUtils.*;

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
            logError("Failed to start socket", e);
        }
    }

    @Override
    public void close() {
        if (socket != null) {
            socket.close();
        }
        if (listeners != null) {
            listeners.shutdown();
            try {
                listeners.awaitTermination(TERMINATION_AWAIT, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                // Ignored
            }
        }
    }

    private static void listen(final DatagramSocket socket, final int bufferSizeRx) {
        final DatagramPacket packet = HelloUDPUtils.initPacket(bufferSizeRx);
        final byte[] bufferRx = packet.getData();

        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            packet.setData(bufferRx, 0, bufferSizeRx);

            if (HelloUDPUtils.receive(packet, socket)) {
                continue;
            }

            final String request = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                    StandardCharsets.UTF_8);
            final String response = buildServerResponse(request);
            HelloUDPUtils.stringToPacket(packet, response, packet.getSocketAddress());

            HelloUDPUtils.send(packet, socket);
        }
    }

    public static void main(final String[] args) {
        serverMain(args, HelloUDPServer.class);
    }
}
