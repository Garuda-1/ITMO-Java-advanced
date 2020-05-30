package ru.ifmo.rain.dolzhanskii.bank.demos;

import ru.ifmo.rain.dolzhanskii.bank.source.Account;
import ru.ifmo.rain.dolzhanskii.bank.source.Bank;
import ru.ifmo.rain.dolzhanskii.bank.source.Person;

import java.rmi.RemoteException;

import static ru.ifmo.rain.dolzhanskii.bank.demos.CommonUtils.contactBank;

public class ClientPersonDemo {
    private static String[] DEFAULT_ARGS = {"John", "Smith", "1111 111111", "Alpha", "100"};

    public static void main(final String... args) throws BankDemoException {
        final String[] actualArgs = new String[DEFAULT_ARGS.length];
        for (int i = 0; i < DEFAULT_ARGS.length; i++) {
            if (args != null && args.length > i) {
                actualArgs[i] = args[i];
            } else {
                actualArgs[i] = DEFAULT_ARGS[i];
            }
        }

        final String firstName = actualArgs[0];
        final String lastName = actualArgs[1];
        final String passport = actualArgs[2];
        final String subId = actualArgs[3];
        int amount;
        try {
            amount = Integer.parseInt(actualArgs[4]);
        } catch (final NumberFormatException e) {
            throw new BankDemoException("Amount parameter is expected to be a number", e);
        }

        final Bank bank = contactBank();

        Person person;
        try {
            person = bank.getRemotePerson(passport);
        } catch (final RemoteException e) {
            throw new BankDemoException("Failed to get person remote reference", e);
        }

        try {
            if (person == null) {
                System.out.println("Creating person");
                person = bank.createPerson(firstName, lastName, passport);
            } else {
                System.out.println("Person already exists");
                if (person.getFirstName().equals(firstName) &&
                        person.getLastName().equals(lastName) && person.getPassport().equals(passport)) {
                    System.out.println("Verified");
                } else {
                    throw new BankDemoException("Person verification failed");
                }
            }

            Account account = person.getLinkedAccount(subId);
            if (account == null) {
                System.out.println("Creating account");
                account = person.createLinkedAccount(subId);
            } else {
                System.out.println("Account already exists");
            }

            System.out.println("Account info: id = " + account.getId() + ", amount = " + account.getAmount());
            System.out.println("Adding " + amount);

            account.addAmount(amount);

            System.out.println("Updated account info: id = " + account.getId() + ", amount = " + account.getAmount());
        } catch (final RemoteException e) {
            throw new BankDemoException("Remote exception occurred", e);
        }
    }
}
