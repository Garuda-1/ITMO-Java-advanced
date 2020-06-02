package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson<A extends Account> implements Person, Serializable {
    private final String firstName, lastName, passport;
    protected final ConcurrentMap<String, A> linkedAccounts;

    AbstractPerson(final String firstName, final String lastName, final String passport,
                   final ConcurrentMap<String, A> linkedAccounts) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passport = passport;
        this.linkedAccounts = linkedAccounts;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPassport() {
        return passport;
    }

    @Override
    public synchronized A getLinkedAccount(final String subId) {
        final String id = getAccountId(subId);
        System.out.println("Retrieving linked account for " + getLastName() + " " + getFirstName() +
                " (id = " + id + ")");
        return linkedAccounts.get(id);
    }

    String getAccountId(final String subId) {
        return passport + ':' + subId;
    }
}
