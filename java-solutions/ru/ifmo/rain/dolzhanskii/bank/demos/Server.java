package ru.ifmo.rain.dolzhanskii.bank.demos;

import ru.ifmo.rain.dolzhanskii.bank.source.Bank;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteBank;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteCredentials;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Server demo application
 */
public class Server {
    public static void main(final String... args) {
        final Bank bank = new RemoteBank(RemoteCredentials.getBankPort());

        try {
            UnicastRemoteObject.exportObject(bank, RemoteCredentials.getBankPort());
            Naming.rebind(RemoteCredentials.getBankUrl(), bank);
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Malformed URL");
            return;
        }

        System.out.println("Server started");
    }
}
