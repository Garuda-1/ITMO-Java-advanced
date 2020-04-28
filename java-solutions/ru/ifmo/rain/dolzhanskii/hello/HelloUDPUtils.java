package ru.ifmo.rain.dolzhanskii.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

class HelloUDPUtils {
    static void stringToPacket(final DatagramPacket packet, final String s, final SocketAddress destination) {
        final byte[] payload = s.getBytes(StandardCharsets.UTF_8);
        packet.setData(payload, 0, payload.length);
        packet.setSocketAddress(destination);
    }

    static void emptyPacket(final DatagramPacket packet, final int bufferSizeRx) {
        packet.setData(new byte[bufferSizeRx], 0, bufferSizeRx);
    }

    static void emptyPacket(final DatagramPacket packet, final int bufferSizeRx, final SocketAddress destination) {
        emptyPacket(packet, bufferSizeRx);
        packet.setSocketAddress(destination);
    }

    static DatagramPacket createEmptyPacket(final int bufferSizeRx) {
        return new DatagramPacket(new byte[bufferSizeRx], bufferSizeRx);
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
        final String threadIdString = Integer.toString(threadId);
        final String requestIdString = Integer.toString(requestId);

        final int p0 = skip(0, s, false);
        final int p1 = skip(p0, s, true);
        final int p2 = skip(p1, s, false);
        final int p3 = skip(p2, s, true);

        return compareSubstring(threadIdString, s, p0, p1) && compareSubstring(requestIdString, s, p2, p3);
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
