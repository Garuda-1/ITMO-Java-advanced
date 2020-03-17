package ru.ifmo.rain.dolzhanskii.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Main class providing console functionality of {@link info.kgeorgiy.java.advanced.implementor.Impler} and
 * {@link info.kgeorgiy.java.advanced.implementor.JarImpler}.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 0.9
 */
public class Main {
    /**
     * Usage hint for user. Printed when arguments format is invalid.
     */
    private static final String USAGE = "Usage: Implementor [-jar] className path";

    /**
     * Default constructor.
     */
    public Main() {
    }

    /**
     * Main function to provide console interface of the program.
     * <p>
     * Allowed signature: <code>[-jar] token outputPath</code>
     * <p>
     * When <code>-jar</code> is omitted, the program runs in Implementation mode and
     * {@link info.kgeorgiy.java.advanced.implementor.Impler#implement(Class, Path)} is invoked.
     * <p>
     * When <code>-jar</code> is used, the program runs in JarImplementation mode and
     * {@link info.kgeorgiy.java.advanced.implementor.JarImpler#implementJar(Class, Path)} is invoked.
     * <p>
     * All arguments must not be null. Any errors and warnings are printed to <code>STDOUT</code> and
     * <code>STDERR</code>.
     *
     * @param args Provided to program arguments
     */
    public static void main(String[] args) {
        try {
            Objects.requireNonNull(args);
            if (args.length != 2 && args.length != 3) {
                System.out.println(USAGE);
                return;
            }
            Objects.requireNonNull(args[0]);
            Objects.requireNonNull(args[1]);
            if (args.length == 3) Objects.requireNonNull(args[2]);
        } catch (NullPointerException e) {
            System.err.println("Error: Null arguments are not allowed");
            return;
        }

        boolean jarOption;
        if (args.length == 2) {
            jarOption = false;
        } else {
            if ("--jar".equals(args[0])) {
                jarOption = true;
                args[0] = args[1];
                args[1] = args[2];
            } else {
                System.out.println(USAGE);
                return;
            }
        }

        Class<?> token;
        try {
            token = Class.forName(args[0]);
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Class not found by name");
            return;
        }

        Path root;
        try {
            root = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.err.println("Error: Invalid root directory");
            return;
        }

        try {
            if (jarOption) {
                new JarImplementor().implementJar(token, root);
            } else {
                new Implementor().implement(token, root);
            }
        } catch (ImplerException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
