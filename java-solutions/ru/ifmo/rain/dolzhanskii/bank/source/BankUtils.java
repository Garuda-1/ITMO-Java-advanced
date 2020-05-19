package ru.ifmo.rain.dolzhanskii.bank.source;

public class BankUtils {
    public static <E extends Exception> void checkException(final E exception) throws E {
        if (exception.getSuppressed().length != 0) {
            throw exception;
        }
    }
}
