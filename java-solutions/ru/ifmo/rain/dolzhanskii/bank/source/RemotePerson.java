package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.UncheckedIOException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

class RemotePerson extends AbstractPerson<RemoteAccount> {
    private final Bank bank;

    RemotePerson(final String firstName, final String lastName, final String passport, final Bank bank) {
        super(firstName, lastName, passport, new ConcurrentHashMap<>());
        this.bank = bank;
    }

    @Override
    public Account createLinkedAccount(final String subId) throws RemoteException {
        final String id = getAccountId(subId);
        try {
            return linkedAccounts.computeIfAbsent(id, accountId -> {
                try {
                    System.out.println("Creating linked account for " + getLastName() + " " + getFirstName() +
                            " (id = " + id + ", remote)");
                    return bank.createAccount(accountId);
                } catch (final RemoteException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final UncheckedIOException e) {
            throw new RemoteException("Failed to create linked account", e.getCause());
        }
    }
}
