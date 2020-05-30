package ru.ifmo.rain.dolzhanskii.bank.source;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it does not already exist
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String id) throws RemoteException;

    /**
     * Returns remote account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist
     */
    Account getRemoteAccount(String id) throws RemoteException;

    /**
     * Returns local account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist
     */
    Account getLocalAccount(String id) throws RemoteException;

    /**
     * Creates a new person with specified name and passport number as a key or returns an existing person with the same
     * passport number.
     * @param firstName Person first name
     * @param lastName Person last name
     * @param passport Person passport number
     * @return created or existing person
     */
    Person createPerson(String firstName, String lastName, String passport) throws RemoteException;

    /**
     * Returns serialized snapshot of a specified person and all linked accounts.
     * @param passport Passport number of a person to lookup
     * @return a person with specified passport number or {@code null} if such person does not exist
     */
    Person getLocalPerson(String passport) throws RemoteException;

    /**
     * Returns remote reference to a specified person
     * @param passport Passport number of a person to lookup
     * @return a person with specified passport number or {@code null} if such person does not exist
     */
    Person getRemotePerson(String passport) throws RemoteException;
}
