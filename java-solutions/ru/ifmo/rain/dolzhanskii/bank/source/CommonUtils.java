package ru.ifmo.rain.dolzhanskii.bank.source;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

class CommonUtils {
    static Bank retrieveRemoteBank() throws RemoteException {
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost:8888/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return null;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return null;
        }
        return bank;
    }
}
