package ru.ifmo.rain.dolzhanskii.hello;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
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

    static DatagramPacket createEmptyPacket(int bufferSizeRx, SocketAddress destination) {
        return new DatagramPacket(new byte[bufferSizeRx], bufferSizeRx, destination);
    }

    static DatagramPacket createEmptyPacket(int bufferSizeRx) {
        return new DatagramPacket(new byte[bufferSizeRx], bufferSizeRx);
    }

    enum logType {
        INFO,
        ERROR
    }

    private static int skipCharacters(int pos, String s, boolean skipDigits) {
        for (; pos < s.length(); pos++) {
            if (skipDigits ^ Character.isDigit(s.charAt(pos))) {
                break;
            }
        }
        return pos;
    }

    static boolean validate(String s, int threadId, int requestId) {
        String threadIdString = Integer.toString(threadId);
        String requestIdString = Integer.toString(requestId);

        int l1 = skipCharacters(0, s, false);
        int r1 = skipCharacters(l1, s, true);
        int l2 = skipCharacters(r1, s, false);
        int r2 = skipCharacters(l2, s, true);

        try {
            return threadIdString.equals(s.substring(l1, r1)) && requestIdString.equals(s.substring(l2, r2));
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    static void log(logType type, String message) {
        System.out.format("%s: \t%s%n", type, message);
    }

    static void log(logType type, int threadId, String message) {
        System.out.format("%s: \tThread %d \t%s%n", type, threadId, message);
    }
}
