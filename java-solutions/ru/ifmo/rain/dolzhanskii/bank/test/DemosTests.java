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
import ru.ifmo.rain.dolzhanskii.bank.source.Bank;
import ru.ifmo.rain.dolzhanskii.bank.source.Person;
import ru.ifmo.rain.dolzhanskii.bank.source.RemoteCredentials;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.ifmo.rain.dolzhanskii.bank.demos.CommonUtils.contactBank;

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
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Normal behavior (Account demo)")
    void testNormalBehaviorAccount() throws RemoteException, BankDemoException {
        Server.main();
        ClientAccountDemo.main();
    }

    @Test
    @DisplayName("Normal behavior (Person demo)")
    void testNormalBehaviorPerson() throws RemoteException, BankDemoException {
        Server.main();
        ClientPersonDemo.main();
    }

    @Test
    @DisplayName("No server started (Account demo)")
    void testNoServerAccount() throws BankDemoException, RemoteException {
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
    void testInvalidAmountDeltaArg() throws RemoteException {
        Server.main();
        final String[] args = {"X", "X", "X", "X", "X"};
        Throwable throwable = assertThrows(BankDemoException.class, () -> ClientPersonDemo.main(args));
        assertNotNull(throwable);
        assertEquals("Amount parameter is expected to be a number", throwable.getMessage());
    }

    @Test
    @DisplayName("Multiple queries (Account demo)")
    void testMultipleQueriesAccount() throws RemoteException, BankDemoException, MalformedURLException {
        final int countOfQueries = 100;
        Server.main();

        String[] args = {"Bravo"};
        for (int i = 0; i < countOfQueries; i++) {
            ClientAccountDemo.main(args);
        }

        final Bank bank = contactBank();
        final Account account = bank.getAccount(args[0]);
        assertNotNull(account);
        assertEquals(countOfQueries * 100, account.getAmount());
    }

    @Test
    @DisplayName("Multiple queries (Person demo)")
    void testMultipleQueriesPerson() throws RemoteException, BankDemoException, MalformedURLException {
        final int countOfQueries = 100;
        final int deltaAmount = 500;
        Server.main();

        String[] args = {"Tyler", "Wellick", "2A4B 34XY2D", "Bravo", Integer.toString(deltaAmount)};
        for (int i = 0; i < countOfQueries; i++) {
            ClientPersonDemo.main(args);
        }

        final Bank bank = contactBank();
        final Person person = bank.getRemotePerson(args[2]);
        assertNotNull(person);

        final Account account = person.getLinkedAccount(args[3]);
        assertNotNull(account);
        assertEquals(countOfQueries * deltaAmount, account.getAmount());
    }
}
