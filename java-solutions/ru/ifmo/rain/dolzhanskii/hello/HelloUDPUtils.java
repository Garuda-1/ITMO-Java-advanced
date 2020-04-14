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

    static boolean validate(String s, int threadId, int requestId) {
        return s.matches("[\\D]*" + threadId + "[\\D]*" + requestId + "[\\D]*");
    }

    static void log(logType type, String message) {
        System.out.format("%s: \t%s%n", type, message);
    }

    static void log(logType type, int threadId, String message) {
        System.out.format("%s: \tThread %d \t%s%n", type, threadId, message);
    }
}
