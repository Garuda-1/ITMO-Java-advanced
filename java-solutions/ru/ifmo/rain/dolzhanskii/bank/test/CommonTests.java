package ru.ifmo.rain.dolzhanskii.bank.test;

import org.junit.Assert;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteCredentials;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;

abstract class CommonTests extends Assert {
    static int PORT = RemoteCredentials.getBankPort();

    static void safeCreateRegistry() throws RemoteException {
        try {
            LocateRegistry.createRegistry(RemoteCredentials.getBankPort());
        } catch (final ExportException e) {
            // Ignored
        }
    }
}
