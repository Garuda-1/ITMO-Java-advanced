package ru.ifmo.rain.dolzhanskii.bank.source;

import java.rmi.RemoteException;

public class LocalAccount extends AbstractAccount {
    public LocalAccount(Account remoteAccount) throws RemoteException {
        super(remoteAccount.getId(), remoteAccount.getAmount());
    }
}
