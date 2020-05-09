package ru.ifmo.rain.dolzhanskii.bank.test;

import org.junit.jupiter.api.*;
import ru.ifmo.rain.dolzhanskii.bank.source.*;

import java.rmi.RemoteException;

@DisplayName("Account tests")
class AccountTests extends RuntimeTests {
    @Test
    @DisplayName("Crete account")
    void testCreateAccount() throws RemoteException {
        final Account account = safeCreateAccount(TEST_ACCOUNT_ID);
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

        final Account account1 = safeCreateAccount(TEST_ACCOUNT_ID);
        account1.setAmount(amount);
        final Account account2 = safeCreateAccount(TEST_ACCOUNT_ID);
        assertEquals(amount, account2.getAmount());
    }

    @Test
    @DisplayName("Create and get account")
    void testCreateAndGetAccount() throws RemoteException {
        bank.createAccount(TEST_ACCOUNT_ID);
        final Account account = safeGetAccount(TEST_ACCOUNT_ID);
        assertEquals(TEST_ACCOUNT_ID, account.getId());
        assertEquals(0, account.getAmount());
    }

    @Test
    @DisplayName("Amount set")
    void testSetAmount() throws RemoteException {
        final int amount = 100;

        final Account account = safeCreateAccount(TEST_ACCOUNT_ID);
        account.setAmount(amount);
        assertEquals(amount, account.getAmount());
    }

    @Test
    @DisplayName("Remote account sync")
    void testRemoteAccountSync() throws RemoteException {
        final int amount1 = 100;
        final int amount2 = 200;

        final Account account1 = safeCreateAccount(TEST_ACCOUNT_ID);
        final Account account2 = safeCreateAccount(TEST_ACCOUNT_ID);

        account1.setAmount(amount1);
        assertEquals(amount1, account1.getAmount());
        assertEquals(amount1, account2.getAmount());

        account2.setAmount(amount2);
        assertEquals(amount2, account1.getAmount());
        assertEquals(amount2, account2.getAmount());
    }

    @Test
    @DisplayName("Multiple accounts simple")
    void testMultipleAccounts() throws InterruptedException {
        multiThreadAccountQueries(1, 100, 10);
    }

    @Test
    @DisplayName("Multi thread requests single account")
    void testMultiThreadRequestsSingle() throws InterruptedException {
        multiThreadAccountQueries(10, 10, 1);
    }

    @Test
    @DisplayName("Multiple accounts multi threaded")
    void testMultipleAccountMultiThread() throws InterruptedException {
        multiThreadAccountQueries(10, 10, 10);
    }
}