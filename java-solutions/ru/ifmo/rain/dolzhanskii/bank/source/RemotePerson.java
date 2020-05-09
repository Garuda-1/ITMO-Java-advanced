package ru.ifmo.rain.dolzhanskii.bank.source;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class RemotePerson extends AbstractPerson {
    private final Bank bank;

    RemotePerson(final String firstName, final String lastName, final String passport) throws RemoteException {
        super(firstName, lastName, passport, new ConcurrentHashMap<>());
        try {
            bank = (Bank) Naming.lookup(RemoteCredentials.getBankUrl());
        } catch (final NotBoundException e) {
            throw new RemoteException("Bank not found", e);
        } catch (final MalformedURLException e) {
            throw new RemoteException("Malformed URL", e);
        }
    }

    @Override
    public Account createLinkedAccount(final String subId) throws RemoteException {
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
