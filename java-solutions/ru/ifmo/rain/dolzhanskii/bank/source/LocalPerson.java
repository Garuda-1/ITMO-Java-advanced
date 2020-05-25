package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.Serializable;
import java.io.UncheckedIOException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class LocalPerson extends AbstractPerson implements Serializable {
    LocalPerson(final RemotePerson remotePerson, ConcurrentMap<String, Account> map) {
        super(remotePerson.getFirstName(), remotePerson.getLastName(), remotePerson.getPassport(), map);
    }

    static ConcurrentHashMap<String, Account> exportAccounts(final RemotePerson remotePerson)
            throws RemoteException {
        final ConcurrentHashMap<String, Account> linkedAccounts = new ConcurrentHashMap<>(remotePerson.linkedAccounts);

        try {
            remotePerson.linkedAccounts.forEach((key, account) -> {
                try {
                    linkedAccounts.put(key, new LocalAccount(account.getId(), account.getAmount()));
                } catch (final RemoteException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final UncheckedIOException e) {
            throw new RemoteException("Failed to export linked account", e.getCause());
        }

        return linkedAccounts;
    }

    @Override
    public synchronized Account createLinkedAccount(final String subId) {
        final String id = getAccountId(subId);
        System.out.println("Creating linked account for " + getLastName() + " " + getFirstName() +
                " (id = " + id + ", local)");
        return linkedAccounts.computeIfAbsent(id, LocalAccount::new);
    }
}
