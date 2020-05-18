package ru.ifmo.rain.dolzhanskii.bank.test;

import org.junit.Assert;
import ru.ifmo.rain.dolzhanskii.bank.source.Account;
import ru.ifmo.rain.dolzhanskii.bank.source.Bank;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteCredentials;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

abstract class CommonTests extends Assert {
    protected static Bank bank;

    static int PORT = RemoteCredentials.getBankPort();

    static void safeCreateRegistry() throws RemoteException {
        try {
            LocateRegistry.createRegistry(RemoteCredentials.getBankPort());
        } catch (final ExportException e) {
            // Ignored
        }
    }

    static <E extends Exception> void checkException(final E exception) throws E {
        if (exception.getSuppressed().length != 0) {
            throw exception;
        }
    }

    static List<String> generateTestIds(final int count) {
        return IntStream.range(0, count).mapToObj(i -> "test" + i).collect(Collectors.toCollection(ArrayList::new));
    }

    static Account safeGetRemoteAccount(final String id) throws RemoteException {
        final Account account = bank.getRemoteAccount(id);
        assertNotNull(account);
        return account;
    }

    static void validateAccountAmounts(final int countOfAccounts, final List<String> ids,
                                       final Function<Integer, Integer> expected) throws RemoteException {
        final RemoteException exception = new RemoteException();

        IntStream.range(0, countOfAccounts).forEach(i -> {
            try {
                final Account account = safeGetRemoteAccount(ids.get(i));
                System.out.println("Checking " + ids.get(i));
//                assertEquals(deltas.get(i) * requestsPerItem, account.getAmount());
                assertEquals((int) expected.apply(i), account.getAmount());
            } catch (final RemoteException e) {
                exception.addSuppressed(e);
            }
        });

        checkException(exception);
    }
}
