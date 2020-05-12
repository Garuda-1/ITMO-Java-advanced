package ru.ifmo.rain.dolzhanskii.bank.source;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    public Account createAccount(final String id) throws RemoteException {
        final RemoteException exception = new RemoteException("Failed to export account");

        final Account account = accounts.computeIfAbsent(id, accountId -> {
            try {
                System.out.println("Creating account " + id);
                final Account tmpAccount = new RemoteAccount(accountId);
                UnicastRemoteObject.exportObject(tmpAccount, port);
                return tmpAccount;
            } catch (RemoteException e) {
                exception.addSuppressed(e);
            }
            return null;
        });

        if (account == null) {
            throw exception;
        }

        return account;
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
            return new LocalAccount(account);
        }
    }

    public Person createPerson(final String firstName, final String lastName, final String passport)
            throws RemoteException {
        final RemoteException exception = new RemoteException("Failed to export person");

        final Person person = persons.computeIfAbsent(passport, accountId -> {
            try {
                System.out.println("Creating person " + lastName + " " + firstName + " " + passport);
                final Person tmpPerson = new RemotePerson(firstName, lastName, passport);
                UnicastRemoteObject.exportObject(tmpPerson, port);
                return tmpPerson;
            } catch (RemoteException e) {
                exception.addSuppressed(e);
            }
            return null;
        });

        if (person == null) {
            throw exception;
        }

        return person;
    }

    public Person getLocalPerson(final String passport) throws RemoteException {
        System.out.println("Retrieving local person by passport " + passport);
        Person person = persons.get(passport);
        if (person == null) {
            return null;
        } else {
            return new LocalPerson((RemotePerson) person);
        }
    }

    public Person getRemotePerson(final String passport) {
        System.out.println("Retrieving remote person by passport " + passport);
        return persons.get(passport);
    }
}
