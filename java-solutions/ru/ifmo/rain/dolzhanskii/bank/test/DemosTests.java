package ru.ifmo.rain.dolzhanskii.bank.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.ifmo.rain.dolzhanskii.bank.demos.BankDemoException;
import ru.ifmo.rain.dolzhanskii.bank.demos.ClientAccountDemo;
import ru.ifmo.rain.dolzhanskii.bank.demos.ClientPersonDemo;
import ru.ifmo.rain.dolzhanskii.bank.demos.Server;
import ru.ifmo.rain.dolzhanskii.bank.source.Account;
import ru.ifmo.rain.dolzhanskii.bank.source.Person;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteCredentials;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.ifmo.rain.dolzhanskii.bank.demos.CommonUtils.contactBank;
import static ru.ifmo.rain.dolzhanskii.bank.source.BankUtils.checkException;

@DisplayName("Demo applications tests")
class DemosTests extends CommonTests {
    @BeforeEach
    void beforeEach() {
        try {
            LocateRegistry.createRegistry(RemoteCredentials.getBankPort());
        } catch (RemoteException e) {
            // Ignored
        }
    }

    @AfterEach
    void afterEach() throws RemoteException, MalformedURLException {
        try {
            Naming.unbind(RemoteCredentials.getBankUrl());
        } catch (NotBoundException e) {
            // Ignored
        }
    }

    @Test
    @DisplayName("Normal behavior (Account demo)")
    void testNormalBehaviorAccount() throws BankDemoException {
        Server.main();
        ClientAccountDemo.main();
    }

    @Test
    @DisplayName("Normal behavior (Person demo)")
    void testNormalBehaviorPerson() throws BankDemoException {
        Server.main();
        ClientPersonDemo.main();
    }

    @Test
    @DisplayName("No server started (Account demo)")
    void testNoServerAccount() {
        Throwable throwable = assertThrows(BankDemoException.class, ClientAccountDemo::main);
        assertNotNull(throwable);
        assertEquals("Bank is not found", throwable.getMessage());
    }

    @Test
    @DisplayName("No server started (Person demo)")
    void testNoServerPerson() {
        Throwable throwable = assertThrows(BankDemoException.class, ClientPersonDemo::main);
        assertNotNull(throwable);
        assertEquals("Bank is not found", throwable.getMessage());
    }

    @Test
    @DisplayName("Invalid amount delta argument (Person demo)")
    void testInvalidAmountDeltaArg() {
        Server.main();
        final String[] args = {"X", "X", "X", "X", "X"};
        Throwable throwable = assertThrows(BankDemoException.class, () -> ClientPersonDemo.main(args));
        assertNotNull(throwable);
        assertEquals("Amount parameter is expected to be a number", throwable.getMessage());
    }

    @Test
    @DisplayName("Multiple queries (Account demo)")
    void testMultipleQueriesAccount() throws RemoteException, BankDemoException {
        final int countOfQueries = 100;
        Server.main();

        String[] args = {"Bravo"};
        for (int i = 0; i < countOfQueries; i++) {
            ClientAccountDemo.main(args);
        }

        bank = contactBank();
        final Account account = bank.getRemoteAccount(args[0]);
        assertNotNull(account);
        assertEquals(countOfQueries * 100, account.getAmount());
    }

    @Test
    @DisplayName("Multiple queries (Person demo)")
    void testMultipleQueriesPerson() throws RemoteException, BankDemoException {
        final int countOfQueries = 100;
        final int deltaAmount = 500;
        Server.main();

        String[] args = {"Tyler", "Wellick", "2A4B 34XY2D", "Bravo", Integer.toString(deltaAmount)};
        for (int i = 0; i < countOfQueries; i++) {
            ClientPersonDemo.main(args);
        }

        bank = contactBank();
        final Person person = bank.getRemotePerson(args[2]);
        assertNotNull(person);

        final Account account = person.getLinkedAccount(args[3]);
        assertNotNull(account);
        assertEquals(countOfQueries * deltaAmount, account.getAmount());
    }

    @Test
    @DisplayName("Multi thread requests (Account demo)")
    void testMultiThreadAccountDemo() throws BankDemoException, RemoteException, InterruptedException {
        Server.main();
        final int countOfAccounts = 10;
        final int requestsPerItem = 10;
        final int countOfThreads = 10;

        final List<String> ids = generateTestIds(countOfAccounts);

        final ExecutorService pool = Executors.newFixedThreadPool(countOfThreads);
        final BankDemoException exception = new BankDemoException();

        IntStream.range(0, requestsPerItem).forEach(u -> IntStream.range(0, countOfAccounts)
                .forEach(i -> pool.submit(() -> {
                    try {
                        final String[] args = {ids.get(i)};
                        ClientAccountDemo.main(args);
                    } catch (final BankDemoException e) {
                        exception.addSuppressed(e);
                    }
                })));
        pool.awaitTermination(countOfAccounts * requestsPerItem * countOfAccounts, TimeUnit.MILLISECONDS);

        checkException(exception);

        bank = contactBank();
        validateAccountAmounts(countOfAccounts, ids, i -> 100 * requestsPerItem);
    }

    @Test
    @DisplayName("Multi thread requests (Person demo)")
    void testMultiThreadPersonDemo() throws InterruptedException, BankDemoException, RemoteException {
        Server.main();
        final int countOfPersons = 10;
        final int countOfAccounts = 5;
        final int requestsPerItem = 10;
        final int countOfThreads = 15;

        final MultiThreadPersonData data = new MultiThreadPersonData(countOfPersons, countOfAccounts);

        final ExecutorService pool = Executors.newFixedThreadPool(countOfThreads);
        final BankDemoException exception = new BankDemoException();

        IntStream.range(0, requestsPerItem).forEach(u -> IntStream.range(0, countOfPersons)
                .forEach(i -> IntStream.range(0, countOfAccounts).forEach(j -> pool.submit(() -> {
                    try {
                        final String[] args = {"Tyler", "Wellick", data.passports.get(i), data.subIds.get(j),
                                Integer.toString(data.deltas.get(i).get(j))};
                        ClientPersonDemo.main(args);
                    } catch (final BankDemoException e) {
                        exception.addSuppressed(e);
                    }
                }))));
        pool.awaitTermination(requestsPerItem * countOfAccounts * countOfPersons,
                TimeUnit.MILLISECONDS);

        checkException(exception);

        bank = contactBank();
        validatePersonAccountAmounts(countOfPersons, countOfAccounts, data.passports, data.subIds,
                (i, j) -> data.deltas.get(i).get(j) * requestsPerItem);
    }
}
