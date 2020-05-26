package ru.ifmo.rain.dolzhanskii.bank.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.ifmo.rain.dolzhanskii.bank.source.Account;
import ru.ifmo.rain.dolzhanskii.bank.source.Person;

import java.rmi.RemoteException;

@DisplayName("Person tests")
class PersonTests extends RuntimeTests {

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
        final Account account = safeAddLinkedAccount(person);
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
        safeAddLinkedAccount(person);
    }

    @Test
    @DisplayName("Already existing person")
    void testAlreadyExistingPerson() throws RemoteException {
        final int amount = 100;

        final Person person1 = safeCreatePerson();
        final Person person2 = safeCreatePerson();

        final Account account1 = safeAddLinkedAccount(person1);
        account1.setAmount(amount);

        final Account account2 = safeAddLinkedAccount(person2);
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
    @DisplayName("Local person lookup")
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
    @DisplayName("Account bonding")
    void testAccountBonding() throws RemoteException {
        final int amount = 100;

        final Account account1 = safeCreateRemoteAccount(TEST_PASSPORT + ":" + TEST_SUB_ID);
        account1.setAmount(amount);
        final Person person = safeCreatePerson();
        assertNull(person.getLinkedAccount(TEST_SUB_ID));

        final Account account2 = safeAddLinkedAccount(person);
        assertEquals(amount, account2.getAmount());
    }

    @Test
    @DisplayName("Remote person single account synchronization (bank external changes)")
    void testRemotePersonSyncWithBank() throws RemoteException {
        final Account account1 = safeCreatePersonWithLinkedAccount();
        final Account account2 = bank.getRemoteAccount(TEST_PASSPORT + ":" + TEST_SUB_ID);
        validateAccountsSync(account1, account2);
    }

    @Test
    @DisplayName("Remote person single account synchronization (changes among persons)")
    void testRemotePersonSyncAmongPersons() throws RemoteException {
        final Account account1 = safeCreatePersonWithLinkedAccount();
        final Person person = safeCreatePerson();
        final Account account2 = person.getLinkedAccount(TEST_SUB_ID);
        validateAccountsSync(account1, account2);
    }

    @Test
    @DisplayName("Local person single account desynchronization (bank external changes)")
    void testLocalPersonDesyncWithBank() throws RemoteException {
        safeCreatePersonWithLinkedAccount();
        final Person person = bank.getLocalPerson(TEST_PASSPORT);
        assertNotNull(person);
        final Account account1 = bank.getRemoteAccount(TEST_PASSPORT + ":" + TEST_SUB_ID);
        final Account account2 = person.getLinkedAccount(TEST_SUB_ID);

        validateAccountsDesync(account1, account2);
    }

    @Test
    @DisplayName("Local person single account desynchronization (changes among persons)")
    void testLocalPersonDesyncAmongPersons() throws RemoteException {
        final Account account1 = safeCreatePersonWithLinkedAccount();
        final Person person = bank.getLocalPerson(TEST_PASSPORT);
        assertNotNull(person);
        final Account account2 = person.getLinkedAccount(TEST_SUB_ID);

        validateAccountsDesync(account1, account2);
    }

    @Test
    @DisplayName("Two local and remote persons independence")
    void testLocalAndRemotePersonsIndependence() throws RemoteException  {
        Person person = safeCreatePerson();
        safeAddLinkedAccount(person);

        Person remotePerson = bank.getRemotePerson(TEST_PASSPORT);
        assertNotNull(remotePerson);
        Person localPerson1 = bank.getLocalPerson(TEST_PASSPORT);
        assertNotNull(localPerson1);
        Person localPerson2 = bank.getLocalPerson(TEST_PASSPORT);
        assertNotNull(localPerson1);

        Account remoteAccount = safeGetLinkedAccount(remotePerson);
        Account localAccount1 = safeGetLinkedAccount(localPerson1);
        Account localAccount2 = safeGetLinkedAccount(localPerson2);

        validateLocalAndRemoteBehavior(remoteAccount, localAccount1, localAccount2);
    }

    @Test
    @DisplayName("Multiple linked accounts")
    void testMultipleLinkedAccounts() throws InterruptedException, RemoteException {
        multiThreadPersonQueries(1, 1, 1, 50);
    }

    @Test
    @DisplayName("Multi thread requests single person single account")
    void testMultiThreadRequestsSinglePersonSingleAccount() throws InterruptedException, RemoteException {
        multiThreadPersonQueries(10, 10, 1, 1);
    }

    @Test
    @DisplayName("Multi thread requests single person multiple accounts")
    void testMultiThreadRequestsSinglePersonMultipleAccounts() throws InterruptedException, RemoteException {
        multiThreadPersonQueries(10, 10, 1, 10);
    }

    @Test
    @DisplayName("Multi thread requests multiple persons single account")
    void testMultiThreadRequestsMultiplePersonsSingleAccount() throws InterruptedException, RemoteException {
        multiThreadPersonQueries(10, 10, 10, 1);
    }

    @Test
    @DisplayName("Multi thread requests multiple persons multiple accounts")
    void testMultiThreadRequestsMultiplePersonsMultipleAccounts() throws InterruptedException, RemoteException {
        multiThreadPersonQueries(5, 10, 5, 5);
    }
}
