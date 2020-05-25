package ru.ifmo.rain.dolzhanskii.bank.demos;

public class BankDemoException extends Exception {
    public BankDemoException(String message, Throwable cause) {
        super(message, cause);
    }

    BankDemoException(String message) {
        super(message);
    }

    public BankDemoException() {
        super();
    }
}
