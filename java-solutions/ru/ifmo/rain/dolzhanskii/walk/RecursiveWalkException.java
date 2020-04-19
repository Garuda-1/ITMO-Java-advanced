package ru.ifmo.rain.dolzhanskii.walk;

class RecursiveWalkException extends Exception {
    RecursiveWalkException(String message) {
        super(message);
    }

    RecursiveWalkException(String message, Exception e) {
        super((e == null) ? message : message + String.format("%n") + e.getMessage());
    }
}