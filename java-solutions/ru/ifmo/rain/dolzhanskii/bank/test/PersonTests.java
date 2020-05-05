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
public class PersonTests extends Assert {
    private static final int PORT = 8888;
    private static Bank bank;
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Smith";
    private static final String TEST_PASSPORT = "1111 111111";
    private static final String TEST_SUB_ID = "ALPHA";

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
