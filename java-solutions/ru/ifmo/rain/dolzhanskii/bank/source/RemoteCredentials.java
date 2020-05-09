package ru.ifmo.rain.dolzhanskii.bank.source;

public class RemoteCredentials {
    private static final String BANK_HOST = "localhost";
    private static final String BANK_PORT = "8888";
    private static final String BANK_PATH = "bank";

    public static String getBankUrl() {
        return String.format("//%s:%s/%s", BANK_HOST, BANK_PORT, BANK_PATH);
    }
}
