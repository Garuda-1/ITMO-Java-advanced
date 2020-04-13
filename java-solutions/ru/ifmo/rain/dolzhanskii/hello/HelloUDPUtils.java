package ru.ifmo.rain.dolzhanskii.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

class HelloUDPUtils {
    static DatagramPacket stringToPacket(String s, SocketAddress destination) {
        byte[] payload = s.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(payload, payload.length, destination);
    }

    static DatagramPacket emptyPacket(int bufferSizeRx, SocketAddress destination) {
        return new DatagramPacket(new byte[bufferSizeRx], bufferSizeRx, destination);
    }

    static DatagramPacket emptyPacket(int bufferSizeRx) {
        return new DatagramPacket(new byte[bufferSizeRx], bufferSizeRx);
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
