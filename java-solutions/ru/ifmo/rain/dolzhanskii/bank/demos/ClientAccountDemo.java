package ru.ifmo.rain.dolzhanskii.bank.demos;

import ru.ifmo.rain.dolzhanskii.bank.source.Account;
import ru.ifmo.rain.dolzhanskii.bank.source.Bank;

import java.rmi.RemoteException;

public class ClientAccountDemo {
    public static void main(final String... args) throws RemoteException {
        final Bank bank = CommonUtils.retrieveRemoteBank();
        if (bank == null) {
            return;
        }

        final String accountId = args.length >= 1 ? args[0] : "geo";

        Account account = bank.getAccount(accountId);
        if (account == null) {
            System.out.println("Creating account");
            account = bank.createAccount(accountId);
        } else {
            System.out.println("Account already exists");
        }
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + 100);
        System.out.println("Money: " + account.getAmount());
    }
}
