package ru.ifmo.rain.dolzhanskii.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

class HelloUDPUtils {
    static void stringToPacket(DatagramPacket packet, String s, SocketAddress destination) {
        byte[] payload = s.getBytes(StandardCharsets.UTF_8);
        packet.setData(payload, 0, payload.length);
        packet.setSocketAddress(destination);
    }

    static void emptyPacket(DatagramPacket packet, int bufferSizeRx, SocketAddress destination) {
        packet.setData(new byte[bufferSizeRx], 0, bufferSizeRx);
        packet.setSocketAddress(destination);
    }

    static DatagramPacket createEmptyPacket(int bufferSizeRx) {
        return new DatagramPacket(new byte[bufferSizeRx], bufferSizeRx);
    }

    private static int skipCharacters(int pos, String s, boolean skipDigits) {
        for (; pos < s.length(); pos++) {
            if (skipDigits ^ Character.isDigit(s.charAt(pos))) {
                break;
            }
        }
        return pos;
    }

    private static boolean compareSubstring(String s, String t, int l, int r) {
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

    static boolean validate(String s, int threadId, int requestId) {
        String threadIdString = Integer.toString(threadId);
        String requestIdString = Integer.toString(requestId);

        int l1 = skipCharacters(0, s, false);
        int r1 = skipCharacters(l1, s, true);
        int l2 = skipCharacters(r1, s, false);
        int r2 = skipCharacters(l2, s, true);

        return compareSubstring(threadIdString, s, l1, r1) && compareSubstring(requestIdString, s, l2, r2);
    }

    enum logType {
        INFO,
        ERROR
    }

    static void log(logType type, String message) {
        System.out.format("%s: \t%s%n", type, message);
    }

    static void log(logType type, int threadId, String message) {
        System.out.format("%s: \tThread %d \t%s%n", type, threadId, message);
    }
}
