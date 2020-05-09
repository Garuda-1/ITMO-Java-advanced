package ru.ifmo.rain.dolzhanskii.bank.demos;

import ru.ifmo.rain.dolzhanskii.bank.source.Bank;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteCredentials;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

class CommonUtils {
    static Bank contactBank() throws BankDemoException {
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup(RemoteCredentials.getBankUrl());
        } catch (final NotBoundException e) {
            throw new BankDemoException("Bank is not found", e);
        } catch (final MalformedURLException e) {
            throw new BankDemoException("Bank URL is invalid", e);
        } catch (final RemoteException e) {
            throw new BankDemoException("Remote exception occurred", e);
        }
        return bank;
    }
}
