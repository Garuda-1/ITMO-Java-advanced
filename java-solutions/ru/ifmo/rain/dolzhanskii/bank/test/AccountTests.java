package ru.ifmo.rain.dolzhanskii.bank.test;

import org.junit.Assert;
import org.junit.jupiter.api.*;
import ru.ifmo.rain.dolzhanskii.bank.demos.ClientAccountDemo;
import ru.ifmo.rain.dolzhanskii.bank.source.*;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@DisplayName("Account tests")
class AccountTests extends Assert {
    private static final int PORT = 8888;
    private static Bank bank;
    private static final String TEST_ACCOUNT_ID = "test";

    @BeforeAll
    static void beforeAll() throws RemoteException {
        try {
            LocateRegistry.createRegistry(PORT);
        } catch (final ExportException e) {
            // Ignored
        }
    }

    @BeforeEach
    void beforeEach() throws RemoteException, MalformedURLException {
        bank = new RemoteBank(PORT);
        UnicastRemoteObject.exportObject(bank, PORT);
        Naming.rebind("//localhost:8888/bank", bank);
    }

    @AfterEach
    void afterEach() throws NoSuchObjectException {
        UnicastRemoteObject.unexportObject(bank, false);
    }

    @Test
    @DisplayName("Demo application")
    void testClientApp() throws RemoteException  {
        String[] empty = new String[0];
        ClientAccountDemo.main(empty);
    }

    @Test
    @DisplayName("Crete account")
    void testCreateAccount() throws RemoteException {
        final Account account = bank.createAccount(TEST_ACCOUNT_ID);
        assertNotNull(account);
        assertEquals(TEST_ACCOUNT_ID, account.getId());
        assertEquals(0, account.getAmount());
    }

    @Test
    @DisplayName("Non existing account")
    void testNonExistingAccount() throws RemoteException {
        final Account account = bank.getAccount(TEST_ACCOUNT_ID);
        assertNull(account);
    }

    @Test
    @DisplayName("Already existing account")
    void testAlreadyExistingAccount() throws RemoteException {
        final int amount = 100;

        final Account account1 = bank.createAccount(TEST_ACCOUNT_ID);
        assertNotNull(account1);

        account1.setAmount(amount);
        final Account account2 = bank.createAccount(TEST_ACCOUNT_ID);
        assertNotNull(account2);

        assertEquals(amount, account2.getAmount());
    }

    @Test
    @DisplayName("Create and get account")
    void testCreateAndGetAccount() throws RemoteException {
        bank.createAccount(TEST_ACCOUNT_ID);
        final Account account = bank.getAccount(TEST_ACCOUNT_ID);

        assertNotNull(account);
        assertEquals(TEST_ACCOUNT_ID, account.getId());
        assertEquals(0, account.getAmount());
    }

    @Test
    @DisplayName("Amount set")
    void testSetAmount() throws RemoteException {
        final int amount = 100;

        final Account account = bank.createAccount(TEST_ACCOUNT_ID);
        assertNotNull(account);

        account.setAmount(amount);
        assertEquals(amount, account.getAmount());
    }

    @Test
    @DisplayName("Remote account sync")
    void testRemoteAccountSync() throws RemoteException {
        final int amount1 = 100;
        final int amount2 = 200;

        final Account account1 = bank.createAccount(TEST_ACCOUNT_ID);
        assertNotNull(account1);
        final Account account2 = bank.getAccount(TEST_ACCOUNT_ID);
        assertNotNull(account2);

        account1.setAmount(amount1);
        assertEquals(amount1, account1.getAmount());
        assertEquals(amount1, account2.getAmount());

        account2.setAmount(amount2);
        assertEquals(amount2, account1.getAmount());
        assertEquals(amount2, account2.getAmount());
    }

    @Test
    @DisplayName("Multi thread requests single account")
    void testMultiThreadRequestsSingle() throws RemoteException, InterruptedException {
        final int delta = 100;
        final int threadsCount = 500;

        final Account accountBasic = bank.createAccount(TEST_ACCOUNT_ID);
        assertNotNull(accountBasic);

        final ExecutorService pool = Executors.newFixedThreadPool(threadsCount);
        final Lock lock = new ReentrantLock();
        IntStream.range(0, threadsCount).forEach(i -> pool.submit(() -> {
            try {
                final Account account = bank.getAccount(TEST_ACCOUNT_ID);
                assertNotNull(account);

                lock.lock();
                account.setAmount(account.getAmount() + delta);
                lock.unlock();
            } catch (final RemoteException e) {
                // Ignored
            }
        }));
        pool.awaitTermination(200, TimeUnit.MILLISECONDS);

        assertEquals(delta * threadsCount, accountBasic.getAmount());
    }

    private List<String> createMultipleAccounts(final int countOfAccounts) {
        final List<String> ids = IntStream.range(0, countOfAccounts)
                .mapToObj(i -> "test" + i).collect(Collectors.toCollection(ArrayList::new));

        ids.forEach(id -> {
            try {
                bank.createAccount(id);
            } catch (final RemoteException e) {
                // Ignored
            }
        });

        return ids;
    }

    @Test
    @DisplayName("Multiple accounts simple")
    void testMultipleAccounts() {
        final int countOfAccounts = 100;
        final List<String> ids = createMultipleAccounts(countOfAccounts);

        IntStream.range(0, countOfAccounts).forEach(i -> {
            try {
                final Account account = bank.getAccount(ids.get(i));
                assertNotNull(account);
                account.setAmount(i + 1);
            } catch (final RemoteException e) {
                // Ignored
            }
        });

        IntStream.range(0, countOfAccounts).forEach(i -> {
            try {
                final Account account = bank.getAccount(ids.get(i));
                assertNotNull(account);
                assertEquals(i + 1, account.getAmount());
            } catch (final RemoteException e) {
                // Ignored
            }
        });
    }

    @Test
    @DisplayName("Multiple accounts multi threaded")
    void testMultipleAccountMultiThread() throws InterruptedException {
        final int countOfAccounts = 50;
        final List<String> ids = createMultipleAccounts(countOfAccounts);
        final List<Integer> deltas = IntStream.range(1, countOfAccounts + 1).boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        final int requestsPerAccount = 50;

        final ExecutorService pool = Executors.newFixedThreadPool(requestsPerAccount);
        final Lock lock = new ReentrantLock();
        IntStream.range(0, requestsPerAccount).forEach(i -> IntStream.range(0, countOfAccounts).forEach(j -> pool.submit(() -> {
            try {
                final Account account = bank.getAccount(ids.get(j));
                assertNotNull(account);

                lock.lock();
                account.setAmount(account.getAmount() + deltas.get(j));
                lock.unlock();
            } catch (final RemoteException e) {
                // Ignored
            }
        })));
        pool.awaitTermination(200, TimeUnit.MILLISECONDS);

        IntStream.range(0, countOfAccounts).forEach(i -> {
            try {
                final Account account = bank.getAccount(ids.get(i));
                assertNotNull(account);
                assertEquals(deltas.get(i) * requestsPerAccount, account.getAmount());
            } catch (final RemoteException e) {
                // Ignored
            }
        });
    }
}