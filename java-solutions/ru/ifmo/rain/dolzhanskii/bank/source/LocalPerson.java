package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalPerson extends AbstractPerson implements Serializable {
    public LocalPerson(RemotePerson remotePerson) throws RemoteException {
        super(remotePerson.getFirstName(), remotePerson.getLastName(), remotePerson.getPassport(), localizeAccounts(remotePerson));
    }

    private static ConcurrentHashMap<String, Account> localizeAccounts(RemotePerson remotePerson) throws RemoteException {
        ConcurrentHashMap<String, Account> linkedAccounts = new ConcurrentHashMap<>();
        for (Map.Entry<String, Account> entry : remotePerson.linkedAccounts.entrySet()) {
            Account account = entry.getValue();
            linkedAccounts.put(entry.getKey(), new LocalAccount(account));
        }
        return linkedAccounts;
    }

    @Override
    public Account createLinkedAccount(String subId) {
        final String id = getAccountId(subId);
        final Account account = new RemoteAccount(id);
        System.out.println("Creating linked account for " + getLastName() + " " + getFirstName() +
                " (id = " + id + ", local)");
        if (linkedAccounts.putIfAbsent(id, account) == null) {
            return account;
        } else {
            return getLinkedAccount(subId);
        }
    }
}
