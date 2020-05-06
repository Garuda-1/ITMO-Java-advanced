package ru.ifmo.rain.dolzhanskii.bank.source;

public abstract class AbstractAccount implements Account {
    private final String id;
    private int amount;

    AbstractAccount(final String id) {
        this.id = id;
        this.amount = 0;
    }

    AbstractAccount(final String id, final int amount) {
        this(id);
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    public synchronized void setAmount(final int amount) {
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
    }
}
