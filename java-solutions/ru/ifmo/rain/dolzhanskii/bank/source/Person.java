package ru.ifmo.rain.dolzhanskii.bank.source;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {
    String getFirstName() throws RemoteException;

    String getLastName() throws RemoteException;

    String getPassport() throws RemoteException;

    Account createLinkedAccount(String subId) throws RemoteException;

    Account getLinkedAccount(String subId) throws RemoteException;
}
