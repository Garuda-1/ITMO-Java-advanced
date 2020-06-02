package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class LocalPerson extends AbstractPerson<LocalAccount> implements Serializable {
    LocalPerson(final RemotePerson remotePerson, ConcurrentMap<String, LocalAccount> map) {
        super(remotePerson.getFirstName(), remotePerson.getLastName(), remotePerson.getPassport(), map);
    }

    static ConcurrentMap<String, LocalAccount> exportAccounts(final RemotePerson remotePerson) {
        final ConcurrentHashMap<String, LocalAccount> linkedAccounts = new ConcurrentHashMap<>();
        remotePerson.linkedAccounts.forEach((key, account) ->
                linkedAccounts.put(key, new LocalAccount(account.getId(), account.getAmount())));
        return linkedAccounts;
    }

    @Override
    public synchronized Account createLinkedAccount(final String subId) {
        final String id = getAccountId(subId);
        System.out.println("Creating linked account for " + getLastName() + " " + getFirstName() +
                " (id = " + id + ", local)");
        return linkedAccounts.computeIfAbsent(id, (idTmp) -> new LocalAccount(id));
    }
}
