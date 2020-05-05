package ru.ifmo.rain.dolzhanskii.bank.source;

import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson implements Person {
    private final String firstName, lastName, passport;
    final ConcurrentMap<String, Account> linkedAccounts;

    public AbstractPerson(String firstName, String lastName, String passport,
                   ConcurrentMap<String, Account> linkedAccounts) {
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
    public Account getLinkedAccount(String subId) {
        String id = getAccountId(subId);
        System.out.println("Retrieving linked account for " + getLastName() + " " + getFirstName() +
                " (id = " + id + ")");
        return linkedAccounts.get(id);
    }

    protected String getAccountId(String subId) {
        return passport + ':' + subId;
    }
}
