package ru.ifmo.rain.dolzhanskii.bank.demos;

import ru.ifmo.rain.dolzhanskii.bank.source.Account;
import ru.ifmo.rain.dolzhanskii.bank.source.Bank;

import java.rmi.RemoteException;

import static ru.ifmo.rain.dolzhanskii.bank.demos.CommonUtils.contactBank;

/**
 * Client
 */
public class ClientAccountDemo {

    public static void main(final String... args) throws BankDemoException {
        final String defaultId = "Alpha";
        final String accountId = (args != null && args.length >= 1) ? args[0] : defaultId;

        final Bank bank = contactBank();

        try {
            Account account = bank.getRemoteAccount(accountId);
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
        } catch (final RemoteException e) {
            throw new BankDemoException("Remote exception occurred", e);
        }
    }
}
