package ru.ifmo.rain.dolzhanskii.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

class HelloUDPUtils {
    static DatagramPacket initPacket(final int bufferSizeRx) {
        return new DatagramPacket(new byte[bufferSizeRx], bufferSizeRx);
    }

    static boolean receive(final DatagramPacket packet, final DatagramSocket socket) {
        try {
            socket.receive(packet);
        } catch (final IOException e) {
            if (!socket.isClosed()) {
                HelloUDPUtils.log(HelloUDPUtils.logType.ERROR,
                        "Error occurred during receiving packet from socket: " + e.getMessage());
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
                HelloUDPUtils.log(HelloUDPUtils.logType.ERROR,
                        "Error occurred in attempt to send: " + new String(packet.getData()));
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

    static boolean validate(final String s, final int threadId, final int requestId) {
        final int p0 = skip(0, s, false);
        final int p1 = skip(p0, s, true);
        final int p2 = skip(p1, s, false);
        final int p3 = skip(p2, s, true);

        return compareSubstring(Integer.toString(threadId), s, p0, p1) &&
                compareSubstring(Integer.toString(requestId), s, p2, p3);
    }

    enum logType {
        INFO,
        ERROR
    }

    static void log(final logType type, final String message) {
        System.out.format("%s: \t%s%n", type, message);
    }

    static void log(final logType type, final int threadId, final String message) {
        System.out.format("%s: \tThread %d \t%s%n", type, threadId, message);
    }
}
