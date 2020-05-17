package ru.ifmo.rain.dolzhanskii.bank.test;

import org.junit.jupiter.api.*;
import ru.ifmo.rain.dolzhanskii.bank.source.*;

import java.rmi.RemoteException;

@DisplayName("Account tests")
class AccountTests extends RuntimeTests {
    @Test
    @DisplayName("Crete account")
    void testCreateAccount() throws RemoteException {
        final Account account = safeCreateRemoteAccount(TEST_ACCOUNT_ID);
        assertEquals(TEST_ACCOUNT_ID, account.getId());
        assertEquals(0, account.getAmount());
    }

    @Test
    @DisplayName("Non existing account")
    void testNonExistingAccount() throws RemoteException {
        assertNull(bank.getRemoteAccount(TEST_ACCOUNT_ID));
    }

    @Test
    @DisplayName("Already existing account")
    void testAlreadyExistingAccount() throws RemoteException {
        final int amount = 100;

        final Account account1 = safeCreateRemoteAccount(TEST_ACCOUNT_ID);
        account1.setAmount(amount);
        final Account account2 = safeCreateRemoteAccount(TEST_ACCOUNT_ID);
        assertEquals(amount, account2.getAmount());
    }

    @Test
    @DisplayName("Create and get account")
    void testCreateAndGetAccount() throws RemoteException {
        bank.createAccount(TEST_ACCOUNT_ID);
        final Account account = safeGetRemoteAccount(TEST_ACCOUNT_ID);
        assertEquals(TEST_ACCOUNT_ID, account.getId());
        assertEquals(0, account.getAmount());
    }

    @Test
    @DisplayName("Amount set")
    void testSetAmount() throws RemoteException {
        final int amount = 100;

        final Account account = safeCreateRemoteAccount(TEST_ACCOUNT_ID);
        account.setAmount(amount);
        assertEquals(amount, account.getAmount());
    }

    @Test
    @DisplayName("Remote account sync")
    void testRemoteAccountSync() throws RemoteException {
        final int amount1 = 100;
        final int amount2 = 200;

        final Account account1 = safeCreateRemoteAccount(TEST_ACCOUNT_ID);
        final Account account2 = safeCreateRemoteAccount(TEST_ACCOUNT_ID);

        account1.setAmount(amount1);
        assertEquals(amount1, account1.getAmount());
        assertEquals(amount1, account2.getAmount());

        account2.setAmount(amount2);
        assertEquals(amount2, account1.getAmount());
        assertEquals(amount2, account2.getAmount());
    }

    @Test
    @DisplayName("Remote and local accounts desync")
    void testRemoteAndLocalAccountsDesync() throws RemoteException {
        final Account remoteAccount = safeCreateRemoteAccount(TEST_ACCOUNT_ID);
        final Account localAccount1 = safeGetLocalAccount();
        final Account localAccount2 = safeGetLocalAccount();

        validateLocalAndRemoteBehavior(remoteAccount, localAccount1, localAccount2);
    }

    @Test
    @DisplayName("Multiple accounts simple")
    void testMultipleAccounts() throws InterruptedException, RemoteException {
        multiThreadAccountQueries(1, 100, 10);
    }

    @Test
    @DisplayName("Multi thread requests single account")
    void testMultiThreadRequestsSingle() throws InterruptedException, RemoteException {
        multiThreadAccountQueries(10, 10, 1);
    }

    @Test
    @DisplayName("Multiple accounts multi threaded")
    void testMultipleAccountMultiThread() throws InterruptedException, RemoteException {
        multiThreadAccountQueries(10, 10, 10);
    }
}