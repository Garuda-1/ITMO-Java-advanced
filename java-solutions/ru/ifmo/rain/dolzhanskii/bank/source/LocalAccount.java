package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.Serializable;
import java.rmi.RemoteException;

class LocalAccount extends AbstractAccount implements Serializable {
    LocalAccount(final String id, final int amount) {
        super(id, amount);
    }

    LocalAccount(final String id) {
        super(id);
    }
}
