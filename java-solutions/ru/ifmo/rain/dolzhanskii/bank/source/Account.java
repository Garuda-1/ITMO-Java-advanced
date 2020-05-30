package ru.ifmo.rain.dolzhanskii.bank.source;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {
    /** Returns account identifier. */
    String getId() throws RemoteException;

    /** Returns amount of money at the account. */
    int getAmount() throws RemoteException;

    /** Sets amount of money at the account. */
    void setAmount(int amount) throws RemoteException;

    /** Increases amount of money at the accounts. */
    void addAmount(int delta) throws RemoteException;
}