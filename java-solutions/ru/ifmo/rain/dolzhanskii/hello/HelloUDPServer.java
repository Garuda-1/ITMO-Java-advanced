package ru.ifmo.rain.dolzhanskii.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    static int TERMINATION_AWAIT = 1;

    private ExecutorService listener;
    private ExecutorService responders;
    private DatagramSocket socket;

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            responders = Executors.newFixedThreadPool(threads);
            int bufferSizeRx = socket.getReceiveBufferSize();
            listener = Executors.newSingleThreadExecutor();
            listener.submit(() -> listen(socket, bufferSizeRx));
        } catch (SocketException e) {
            HelloUDPUtils.log(HelloUDPUtils.logType.ERROR, "Failed to start socket");
        }
    }

    @Override
    public void close() {
        listener.shutdown();
        responders.shutdown();
        try {
            listener.awaitTermination(TERMINATION_AWAIT, TimeUnit.SECONDS);
            responders.awaitTermination(TERMINATION_AWAIT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignored
        } finally {
            socket.close();
        }
    }

    private void listen(DatagramSocket socket, int bufferSizeRx) {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            final DatagramPacket packetRx = HelloUDPUtils.emptyPacket(bufferSizeRx);

            try {
                socket.receive(packetRx);
            } catch (IOException e) {
                HelloUDPUtils.log(HelloUDPUtils.logType.ERROR,
                        "Error occurred during receiving packet from socket: " + e.getMessage());
                continue;
            }

            responders.submit(() -> respond(socket, packetRx));
        }
    }

    private void respond(DatagramSocket socket, DatagramPacket packetRx) {
        String request = new String(packetRx.getData(), packetRx.getOffset(), packetRx.getLength(),
                StandardCharsets.UTF_8);

        HelloUDPUtils.log(HelloUDPUtils.logType.INFO, "Received message: " + request);

        String response = "Hello, " + request;
        DatagramPacket packetTx = HelloUDPUtils.stringToPacket(response, packetRx.getSocketAddress());

        HelloUDPUtils.log(HelloUDPUtils.logType.INFO, "Sending message: " + response);

        try {
            socket.send(packetTx);
        } catch (IOException e) {
            if (!socket.isClosed()) {
                HelloUDPUtils.log(HelloUDPUtils.logType.ERROR, "Error occurred in attempt to send response: " +
                        response);
            }
        }

        HelloUDPUtils.log(HelloUDPUtils.logType.INFO, "Message sent");
    }
}
