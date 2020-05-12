package ru.ifmo.rain.dolzhanskii.bank.demos;

import ru.ifmo.rain.dolzhanskii.bank.source.Bank;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteBank;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteCredentials;

import java.rmi.*;
import java.rmi.server.*;
import java.net.*;

/**
 * Server
 */
public class Server {
    public static void main(final String... args) {
        final Bank bank = new RemoteBank(RemoteCredentials.getBankPort());

        try {
            UnicastRemoteObject.exportObject(bank, RemoteCredentials.getBankPort());
            Naming.rebind(RemoteCredentials.getBankUrl(), bank);
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
        } catch (final MalformedURLException e) {
            System.out.println("Malformed URL");
        }

        System.out.println("Server started");
    }
}
