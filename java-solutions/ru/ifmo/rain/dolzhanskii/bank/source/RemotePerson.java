package ru.ifmo.rain.dolzhanskii.bank.source;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class RemotePerson extends AbstractPerson {
    public RemotePerson(String firstName, String lastName, String passport) {
        super(firstName, lastName, passport, new ConcurrentHashMap<>());
    }

    @Override
    public Account createLinkedAccount(String subId) throws RemoteException {
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost:8888/bank");
        } catch (final NotBoundException e) {
            throw new RemoteException("Bank not found", e);
        } catch (final MalformedURLException e) {
            throw new RemoteException("Malformed URL", e);
        }

        final String id = getAccountId(subId);
        final Account account = bank.createAccount(id);
        System.out.println("Creating linked account for " + getLastName() + " " + getFirstName() +
                " (id = " + id + ", remote)");
        if (linkedAccounts.putIfAbsent(id, account) == null) {
            return account;
        } else {
            return getLinkedAccount(subId);
        }
    }
}
