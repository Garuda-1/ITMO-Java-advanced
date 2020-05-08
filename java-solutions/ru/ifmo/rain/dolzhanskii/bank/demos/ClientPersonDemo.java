package ru.ifmo.rain.dolzhanskii.bank.demos;

import ru.ifmo.rain.dolzhanskii.bank.source.Account;
import ru.ifmo.rain.dolzhanskii.bank.source.Bank;
import ru.ifmo.rain.dolzhanskii.bank.source.Person;

import java.rmi.RemoteException;

public class ClientPersonDemo {
    public static void main(final String... args) throws RemoteException {
        final Bank bank = CommonUtils.retrieveRemoteBank();
        if (bank == null) {
            return;
        }

        final String firstName = args.length > 0 ? args[0] : "Ian";
        final String lastName = args.length > 1 ? args[1] : "Dolzhanskii";
        final String passport = args.length > 2 ? args[2] : "5555 444333";
        final String subId = args.length > 3 ? args[3] : "Alpha";
        final int amount = args.length > 4 ? Integer.parseInt(args[4]) : 100;

        Person person = bank.getRemotePerson(passport);
        if (person == null) {
            System.out.println("Creating person");
            person = bank.createPerson(firstName, lastName, passport);
        } else {
            System.out.println("Person already exists");
            if (person.getFirstName().equals(firstName) &&
                    person.getLastName().equals(lastName) && person.getPassport().equals(passport)) {
                System.out.println("Verified");
            } else {
                System.out.println("Verification failed");
                return;
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
        account.setAmount(account.getAmount() + amount);
        System.out.println("Updated account info: id = " + account.getId() + ", amount = " + account.getAmount());
    }
}
