package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class LocalPerson extends AbstractPerson implements Serializable {
    LocalPerson(final RemotePerson remotePerson, ConcurrentMap<String, Account> map) {
        super(remotePerson.getFirstName(), remotePerson.getLastName(), remotePerson.getPassport(), map);
    }

    static ConcurrentMap<String, Account> exportAccounts(final RemotePerson remotePerson) {
        final ConcurrentHashMap<String, Account> linkedAccounts = new ConcurrentHashMap<>(remotePerson.linkedAccounts);
        remotePerson.linkedAccounts.forEach((key, account) -> {
            linkedAccounts.put(key, new LocalAccount(((RemoteAccount) account).getId(),
                    ((RemoteAccount) account).getAmount()));
        });
        return linkedAccounts;
    }

    @Override
    public synchronized Account createLinkedAccount(final String subId) {
        final String id = getAccountId(subId);
        System.out.println("Creating linked account for " + getLastName() + " " + getFirstName() +
                " (id = " + id + ", local)");
        return linkedAccounts.computeIfAbsent(id, LocalAccount::new);
    }
}
