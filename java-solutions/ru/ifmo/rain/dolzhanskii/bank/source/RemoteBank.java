package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.UncheckedIOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    private <T extends Remote> T addInstanceById(final String id, final Function<String, T> factory,
                                               final ConcurrentMap<String, T> map) throws RemoteException {
        try {
            return map.computeIfAbsent(id, instanceId -> {
                try {
                    System.out.println("Creating instance " + id);
                    final T tmpInstance = factory.apply(instanceId);
                    UnicastRemoteObject.exportObject(tmpInstance, port);
                    return tmpInstance;
                } catch (final RemoteException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final UncheckedIOException e) {
            throw new RemoteException("Failed to export instance", e);
        }
    }

    public Account createAccount(final String id) throws RemoteException {
        return addInstanceById(id, RemoteAccount::new, accounts);
    }

    public Account getRemoteAccount(final String id) {
        System.out.println("Retrieving remote account " + id);
        return accounts.get(id);
    }

    public Account getLocalAccount(final String id) throws RemoteException {
        System.out.println("Retrieving local account " + id);
        Account account = accounts.get(id);
        if (account == null) {
            return null;
        } else {
            return new LocalAccount(account.getId(), account.getAmount());
        }
    }

    public Person createPerson(final String firstName, final String lastName, final String passport)
            throws RemoteException {
        return addInstanceById(passport, p -> new RemotePerson(firstName, lastName, p, this), persons);
    }

    public Person getLocalPerson(final String passport) {
        System.out.println("Retrieving local person by passport " + passport);
        Person person = persons.get(passport);
        if (person == null) {
            return null;
        } else {
            return new LocalPerson((RemotePerson) person, LocalPerson.exportAccounts((RemotePerson) person));
        }
    }

    public Person getRemotePerson(final String passport) {
        System.out.println("Retrieving remote person by passport " + passport);
        return persons.get(passport);
    }
}
