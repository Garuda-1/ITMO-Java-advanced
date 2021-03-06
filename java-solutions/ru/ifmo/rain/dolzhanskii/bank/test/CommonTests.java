package ru.ifmo.rain.dolzhanskii.bank.test;

import org.junit.Assert;
import org.junit.jupiter.api.function.Executable;
import ru.ifmo.rain.dolzhanskii.bank.source.Account;
import ru.ifmo.rain.dolzhanskii.bank.source.Bank;
import ru.ifmo.rain.dolzhanskii.bank.source.Person;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteCredentials;

import java.io.UncheckedIOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

    static <T extends Throwable> void validateException(final Class<T> exceptionClass, final String exceptionMessage,
                                  final Executable executable) {
        T exception = assertThrows(exceptionClass, executable);
        assertEquals(exceptionMessage, exception.getMessage());
    }

    static List<String> generateTestIds(final int count) {
        return IntStream.range(0, count).mapToObj(i -> "test" + i).collect(Collectors.toCollection(ArrayList::new));
    }

    static Account safeGetRemoteAccount(final String id) throws RemoteException {
        final Account account = bank.getRemoteAccount(id);
        assertNotNull(account);
        return account;
    }

    static Account safeGetLinkedAccount(final String passport, final String subId) throws RemoteException {
        final Person person = bank.getRemotePerson(passport);
        assertNotNull(person);
        final Account account = person.getLinkedAccount(subId);
        assertNotNull(account);
        return account;
    }

    static void validateAccountAmounts(final int countOfAccounts, final List<String> ids,
                                       final Function<Integer, Integer> expected) throws RemoteException {
        try {
            IntStream.range(0, countOfAccounts).forEach(i -> {
                try {
                    final Account account = safeGetRemoteAccount(ids.get(i));
                    System.out.println("Checking " + ids.get(i));
                    assertEquals((int) expected.apply(i), account.getAmount());
                } catch (final RemoteException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final UncheckedIOException e) {
            throw new RemoteException("Remote exception occurred", e.getCause());
        }
    }

    static class MultiThreadPersonData {
        final List<List<Integer>> deltas;
        final List<String> passports;
        final List<String> subIds;

        MultiThreadPersonData(final int countOfPersons, final int countOfAccounts) {
            deltas = IntStream.range(0, countOfPersons).boxed()
                    .map(i -> IntStream.range(i * countOfAccounts + 1, (i + 1) * countOfAccounts + 1).boxed()
                            .collect(Collectors.toCollection(ArrayList::new)))
                    .collect(Collectors.toCollection(ArrayList::new));
            passports = generateTestIds(countOfPersons);
            subIds = generateTestIds(countOfAccounts);
        }
    }

    static void multiThreadBase(final int countOfThreads, final int requestsPerItem, final int countOfPersons,
                                final int countOfAccounts, final BiConsumer<Integer, Integer> jobs) throws RemoteException, InterruptedException {
        final ExecutorService pool = Executors.newFixedThreadPool(countOfThreads);

        try {
            IntStream.range(0, requestsPerItem).forEach(u -> IntStream.range(0, countOfPersons)
                    .forEach(i -> IntStream.range(0, countOfAccounts)
                            .forEach(j -> pool.submit(() -> jobs.accept(i, j)))));
        } catch (final UncheckedIOException e) {
            throw new RemoteException("Exception occurred", e.getCause());
        }
        pool.awaitTermination(countOfThreads * requestsPerItem * countOfAccounts * countOfPersons,
                TimeUnit.MILLISECONDS);
    }

    static void validatePersonAccountAmounts(final int countOfPersons, final int countOfAccounts,
                                             final List<String> passports,
                                             final List<String> subIds,
                                             final BiFunction<Integer, Integer, Integer> expected)
            throws RemoteException {
        try {
            IntStream.range(0, countOfPersons).forEach(i -> IntStream.range(0, countOfAccounts).forEach(j -> {
                try {
                    final Account account = safeGetLinkedAccount(passports.get(i), subIds.get(j));
                    assertEquals((int) expected.apply(i, j), account.getAmount());
                } catch (final RemoteException e) {
                    throw new UncheckedIOException(e);
                }
            }));
        } catch (final UncheckedIOException e) {
            throw new RemoteException("Remote exception occurred", e.getCause());
        }
    }
}
