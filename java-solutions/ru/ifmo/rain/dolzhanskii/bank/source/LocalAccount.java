package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.Serializable;
import java.rmi.RemoteException;

class LocalAccount extends AbstractAccount implements Serializable {
    LocalAccount(Account remoteAccount) throws RemoteException {
        super(remoteAccount.getId(), remoteAccount.getAmount());
    }
}
