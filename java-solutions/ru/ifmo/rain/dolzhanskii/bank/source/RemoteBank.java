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
        System.out.println("Creating account " + id);
        final Account account = new RemoteAccount(id);
        if (accounts.putIfAbsent(id, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(id);
        }
    }

    public Account getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }

    public Person createPerson(String firstName, String lastName, String passport) throws RemoteException {
        System.out.println("Creating person " + lastName + " " + firstName + " " + passport);
        final Person person = new RemotePerson(firstName, lastName, passport);
        if (persons.putIfAbsent(passport, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return getRemotePerson(passport);
        }
    }

    public Person getLocalPerson(String passport) throws RemoteException {
        System.out.println("Retrieving local person by passport " + passport);
        Person person = persons.get(passport);
        if (person == null) {
            return null;
        } else {
            return new LocalPerson((RemotePerson) person);
        }
    }

    public Person getRemotePerson(String passport) {
        System.out.println("Retrieving remote person by passport " + passport);
        return persons.get(passport);
    }
}
