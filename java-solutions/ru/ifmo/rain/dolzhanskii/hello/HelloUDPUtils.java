package ru.ifmo.rain.dolzhanskii.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

class HelloUDPUtils {
    static final int SOCKET_TIMEOUT_MS = 200;

    static DatagramPacket initPacket(final int bufferSizeRx) {
        return new DatagramPacket(new byte[bufferSizeRx], bufferSizeRx);
    }

    static boolean receive(final DatagramPacket packet, final DatagramSocket socket) {
        try {
            socket.receive(packet);
        } catch (final IOException e) {
            if (!socket.isClosed()) {
                HelloUDPUtils.logError("Error occurred during receiving packet from socket", e);
            }
            return true;
        }
        return false;
    }

    static boolean send(final DatagramPacket packet, final DatagramSocket socket) {
        try {
            socket.send(packet);
        } catch (final IOException e) {
            if (!socket.isClosed()) {
                HelloUDPUtils.logError("Error occurred in attempt to send " + new String(packet.getData()), e);
            }
            return true;
        }
        return false;
    }

    static void stringToPacket(final DatagramPacket packet, final String s, final SocketAddress destination) {
        final byte[] payload = s.getBytes(StandardCharsets.UTF_8);
        packet.setData(payload, 0, payload.length);
        packet.setSocketAddress(destination);
    }

    private static int skip(int pos, final String s, final boolean skipCount) {
        for (; pos < s.length(); pos++) {
            if (skipCount ^ Character.isDigit(s.charAt(pos))) {
                break;
            }
        }
        return pos;
    }

    private static boolean compareSubstring(final String s, final String t, final int l, final int r) {
        if (r - l != s.length()) {
            return false;
        }
        for (int i = l; i < r; i++) {
            if (s.charAt(i - l) != t.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    static boolean validate(final String response, final int threadId, final int requestId) {
        final int p0 = skip(0, response, false);
        final int p1 = skip(p0, response, true);
        final int p2 = skip(p1, response, false);
        final int p3 = skip(p2, response, true);

        boolean result = compareSubstring(Integer.toString(threadId), response, p0, p1) &&
                compareSubstring(Integer.toString(requestId), response, p2, p3);

        if (result) {
            logInfo(threadId, String.format("Received valid message: '%s'", response));
        } else {
            logInfo(threadId, String.format("Received invalid message: '%s'", response));
        }

        return result;
    }

    static String buildClientRequest(final String prefix, final int threadId, final int requestId) {
        return prefix + threadId + '_' + requestId;
    }

    static String buildServerResponse(final String request) {
        return "Hello, " + request;
    }

    static void logError(final String message, final Exception exception) {
        System.out.format("ERROR: \t%s: %s%n", message, exception.getMessage());
    }

    static void logInfo(final int threadId, final String message) {
        System.out.format("INFO: \tThread %d \t%s%n", threadId, message);
    }

    static DatagramChannel configureChannel(final InetSocketAddress address, boolean server) throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        if (server) {
            channel.bind(address);
        } else {
            channel.connect(address);
        }
        return channel;
    }

    static void clientMain(final String[] args, final Class<? extends HelloClient> clazz) {
        if (args == null || args.length != 5) {
            System.out.println("Usage: HelloUDP[Nonblocking]Client [URL|IP] port request_prefix threads_count requests_count");
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

            clazz.getDeclaredConstructor().newInstance().run(args[0], port, args[2], threads, requests);
        } catch (final NumberFormatException e) {
            System.err.println("Failed to parse expected numeric argument: " + e.getMessage());
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            System.err.println("Reflection error occurred: " + e.getMessage());
        }
    }

    static void serverMain(final String[] args, final Class<? extends HelloServer> clazz) {
        if (args == null || args.length != 2) {
            System.out.println("Usage: HelloUDP[Nonblocking]Server port threads_count");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Null arguments are not allowed");
            return;
        }

        final int port, threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (final NumberFormatException e) {
            System.err.println("Failed to parse expected numeric argument: " + e.getMessage());
            return;
        }
        try (final HelloServer server = clazz.getDeclaredConstructor().newInstance()) {
            server.start(port, threads);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Server has been started. Press any key to terminate");
            reader.readLine();
        } catch (final IOException e) {
            System.err.println("IO error occurred: " + e.getMessage());
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            System.err.println("Reflection error occurred: " + e.getMessage());
        }
    }
}
