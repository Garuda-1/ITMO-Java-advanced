package ru.ifmo.rain.dolzhanskii.hello;

class HelloUDPUtils {
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
