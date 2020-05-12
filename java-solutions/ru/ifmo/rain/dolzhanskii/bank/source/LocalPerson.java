package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalPerson extends AbstractPerson implements Serializable {
    LocalPerson(final RemotePerson remotePerson) throws RemoteException {
        super(remotePerson.getFirstName(), remotePerson.getLastName(), remotePerson.getPassport(),
                exportAccounts(remotePerson));
    }

    private static ConcurrentHashMap<String, Account> exportAccounts(final RemotePerson remotePerson)
            throws RemoteException {
        final ConcurrentHashMap<String, Account> linkedAccounts = new ConcurrentHashMap<>();
        final RemoteException exception = new RemoteException("Failed to export linked account");

        remotePerson.linkedAccounts.forEach((key, account) -> {
            try {
                linkedAccounts.put(key, new LocalAccount(account));
            } catch (final RemoteException e) {
                exception.addSuppressed(e);
            }
        });

        if (exception.getSuppressed().length != 0) {
            throw exception;
        }
        return linkedAccounts;
    }

    @Override
    public synchronized Account createLinkedAccount(final String subId) {
        final String id = getAccountId(subId);
        final Account account = new RemoteAccount(id);
        System.out.println("Creating linked account for " + getLastName() + " " + getFirstName() +
                " (id = " + id + ", local)");
        if (linkedAccounts.putIfAbsent(id, account) == null) {
            return account;
        } else {
            return getLinkedAccount(subId);
        }
    }
}
