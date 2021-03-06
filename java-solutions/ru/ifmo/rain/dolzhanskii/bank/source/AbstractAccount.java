package ru.ifmo.rain.dolzhanskii.bank.source;

import java.io.Serializable;

public abstract class AbstractAccount implements Account, Serializable {
    private final String id;
    private int amount;

    AbstractAccount(final String id) {
        this(id, 0);
    }

    AbstractAccount(final String id, final int amount) {
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
    }

    @Override
    public synchronized void addAmount(int delta) {
        System.out.println("Adding amount of money for account " + id);
        this.amount += delta;
    }
}
