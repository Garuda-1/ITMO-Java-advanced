package ru.ifmo.rain.dolzhanskii.bank.source;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {
    /** Gets first name of the person */
    String getFirstName() throws RemoteException;

    /** Gets last name of the person */
    String getLastName() throws RemoteException;

    /** Gets passport number of the person */
    String getPassport() throws RemoteException;

    /** Creates linked account with {@code id = passport:subId} or bounds existing account with such id */
    Account createLinkedAccount(String subId) throws RemoteException;

    /** Gets linked account with {@code id = passport:subId} */
    Account getLinkedAccount(String subId) throws RemoteException;


}
