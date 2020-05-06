package ru.ifmo.rain.dolzhanskii.bank.test;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.ifmo.rain.dolzhanskii.bank.source.*;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

@DisplayName("Person tests")
class PersonTests extends Assert {
    private static final int PORT = 8888;
    private static Bank bank;

    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Smith";
    private static final String TEST_PASSPORT = "1111 111111";
    private static final String TEST_SUB_ID = "ALPHA";
    private static final int TEST_AMOUNT_DELTA = 100;

    private static Person safeCreatePerson() throws RemoteException {
        final Person person = bank.createPerson(TEST_FIRST_NAME, TEST_LAST_NAME, TEST_PASSPORT);
        assertNotNull(person);
        return person;
    }

    private static Account safeAddALinkedAccount(final Person person) throws RemoteException {
        final Account account = person.createLinkedAccount(TEST_SUB_ID);
        assertNotNull(account);
        return account;
    }

    private static void validateDefaultPerson(final Person person) throws RemoteException {
        assertNotNull(person);
        assertEquals(TEST_FIRST_NAME, person.getFirstName());
        assertEquals(TEST_LAST_NAME, person.getLastName());
        assertEquals(TEST_PASSPORT, person.getPassport());
    }

    private static Account prepareLinkedAccount() throws RemoteException {
        final Person person = safeCreatePerson();
        final Account account = safeAddALinkedAccount(person);
        account.setAmount(account.getAmount() + TEST_AMOUNT_DELTA);
        return account;
    }

    private static void validateAccountsSync(final Account account1, final Account account2)
            throws RemoteException {
        assertNotNull(account1);
        assertNotNull(account2);

        assertEquals(TEST_AMOUNT_DELTA, account1.getAmount());
        assertEquals(TEST_AMOUNT_DELTA, account2.getAmount());

        account2.setAmount(account2.getAmount() + TEST_AMOUNT_DELTA);
        assertEquals(2 * TEST_AMOUNT_DELTA, account1.getAmount());
        assertEquals( 2 * TEST_AMOUNT_DELTA, account2.getAmount());
    }

    private static void validateAccountsDesync(final Account account1, final Account account2) throws RemoteException {
        assertNotNull(account1);
        assertNotNull(account2);

        assertEquals(TEST_AMOUNT_DELTA, account1.getAmount());
        assertEquals(TEST_AMOUNT_DELTA, account2.getAmount());

        account1.setAmount(2 * TEST_AMOUNT_DELTA);
        assertEquals(2 * TEST_AMOUNT_DELTA, account1.getAmount());
        assertEquals(TEST_AMOUNT_DELTA, account2.getAmount());

        account2.setAmount(3 * TEST_AMOUNT_DELTA);
        assertEquals(2 * TEST_AMOUNT_DELTA, account1.getAmount());
        assertEquals(3 * TEST_AMOUNT_DELTA, account2.getAmount());
    }

    @BeforeAll
    static void beforeAll() throws RemoteException {
        LocateRegistry.createRegistry(PORT);
    }

    @BeforeEach
    void beforeEach() throws RemoteException, MalformedURLException {
        bank = new RemoteBank(PORT);
        UnicastRemoteObject.exportObject(bank, PORT);
        Naming.rebind("//localhost:8888/bank", bank);
    }

    @Test
    @DisplayName("Demo application")
    void testPersonDemo() throws RemoteException {
        String[] empty = new String[0];
        ClientAccountDemo.main(empty);
    }

    @Test
    @DisplayName("Create person")
    void testCreatePerson() throws RemoteException {
        final Person person = bank.createPerson(TEST_FIRST_NAME, TEST_LAST_NAME, TEST_PASSPORT);
        validateDefaultPerson(person);
    }

    @Test
    @DisplayName("Create linked account")
    void testCreateLinkedAccount() throws RemoteException {
        final Person person = safeCreatePerson();
        final Account account = safeAddALinkedAccount(person);
        assertEquals(0, account.getAmount());
        assertEquals(person.getPassport() + ":" + TEST_SUB_ID, account.getId());
    }

    @Test
    @DisplayName("Non existing remote person")
    void testNonExistingRemotePerson() throws RemoteException {
        final Person person = bank.getRemotePerson(TEST_PASSPORT);
        assertNull(person);
    }

    @Test
    @DisplayName("Non existing local person")
    void testNonExistingLocalPerson() throws RemoteException {
        final Person person = bank.getLocalPerson(TEST_PASSPORT);
        assertNull(person);
    }

    @Test
    @DisplayName("Not existing linked account")
    void testNonExistingLinkedAccount() throws RemoteException {
        final Person person = safeCreatePerson();
        safeAddALinkedAccount(person);
    }

    @Test
    @DisplayName("Already existing person")
    void testAlreadyExistingPerson() throws RemoteException {
        final int amount = 100;

        final Person person1 = safeCreatePerson();
        final Person person2 = safeCreatePerson();

        final Account account1 = safeAddALinkedAccount(person1);
        account1.setAmount(amount);

        final Account account2 = safeAddALinkedAccount(person2);
        assertEquals(amount, account1.getAmount());
        assertEquals(amount, account2.getAmount());
        assertEquals(person1.getPassport() + ":" + TEST_SUB_ID, account1.getId());
        assertEquals(person2.getPassport() + ":" + TEST_SUB_ID, account2.getId());
    }

    @Test
    @DisplayName("Remote person lookup")
    void testRemotePersonLookup() throws RemoteException {
        safeCreatePerson();
        final Person person = bank.getRemotePerson(TEST_PASSPORT);
        validateDefaultPerson(person);
    }

    @Test
    @DisplayName("Remote person lookup")
    void testLocalPersonLookup() throws RemoteException {
        safeCreatePerson();
        final Person person = bank.getLocalPerson(TEST_PASSPORT);
        validateDefaultPerson(person);
    }

    @Test
    @DisplayName("Mismatched person details")
    void testAlreadyExistingLocalPerson() throws RemoteException {
        safeCreatePerson();
        final Person person2 = bank.createPerson("Tyler", "Wellick", TEST_PASSPORT);
        assertNotNull(person2);
        validateDefaultPerson(person2);
    }

    @Test
    @DisplayName("Remote person single account synchronization (bank external changes)")
    void testRemotePersonSyncWithBank() throws RemoteException {
        final Account account1 = prepareLinkedAccount();
        final Account account2 = bank.getAccount(TEST_PASSPORT + ":" + TEST_SUB_ID);
        validateAccountsSync(account1, account2);
    }

    @Test
    @DisplayName("Remote person single account synchronization (changes among persons)")
    void testRemotePersonSyncAmongPersons() throws RemoteException {
        final Account account1 = prepareLinkedAccount();
        final Person person = safeCreatePerson();
        final Account account2 = person.getLinkedAccount(TEST_SUB_ID);
        validateAccountsSync(account1, account2);
    }

    @Test
    @DisplayName("Local person single account desynchronization (bank external changes)")
    void testLocalPersonDesyncWithBank() throws RemoteException {
        prepareLinkedAccount();
        final Person person = bank.getLocalPerson(TEST_PASSPORT);
        assertNotNull(person);
        final Account account1 = bank.getAccount(TEST_PASSPORT + ":" + TEST_SUB_ID);
        final Account account2 = person.getLinkedAccount(TEST_SUB_ID);

        validateAccountsDesync(account1, account2);
    }

    @Test
    @DisplayName("Local person single account desynchronization (changes among persons)")
    void testLocalPersonDesyncAmongPersons() throws RemoteException {
        final Account account1 = prepareLinkedAccount();
        final Person person = bank.getLocalPerson(TEST_PASSPORT);
        assertNotNull(person);
        final Account account2 = person.getLinkedAccount(TEST_SUB_ID);

        validateAccountsDesync(account1, account2);
    }

    @Test
    @DisplayName("Async")
    void testAsync() throws RemoteException  {
        Person person = bank.createPerson(TEST_FIRST_NAME, TEST_LAST_NAME, TEST_PASSPORT);
        assertNotNull(person);
        person.createLinkedAccount(TEST_SUB_ID);

        Person remotePerson = bank.getRemotePerson(TEST_PASSPORT);
        assertNotNull(remotePerson);
        Person localPerson1 = bank.getLocalPerson(TEST_PASSPORT);
        assertNotNull(localPerson1);
        Person localPerson2 = bank.getLocalPerson(TEST_PASSPORT);
        assertNotNull(localPerson1);

        Account remoteAccount = remotePerson.getLinkedAccount(TEST_SUB_ID);
        assertNotNull(remoteAccount);

        Account localAccount1 = localPerson1.getLinkedAccount(TEST_SUB_ID);
        assertNotNull(localAccount1);

        Account localAccount2 = localPerson2.getLinkedAccount(TEST_SUB_ID);
        assertNotNull(localAccount2);

        remoteAccount.setAmount(100);

        assertEquals(100, remoteAccount.getAmount());
        assertEquals(0, localAccount1.getAmount());
        assertEquals(0, localAccount2.getAmount());

        localAccount1.setAmount(200);

        assertEquals(100, remoteAccount.getAmount());
        assertEquals(200, localAccount1.getAmount());
        assertEquals(0, localAccount2.getAmount());

        localAccount2.setAmount(300);

        assertEquals(100, remoteAccount.getAmount());
        assertEquals(200, localAccount1.getAmount());
        assertEquals(300, localAccount2.getAmount());
    }
}
