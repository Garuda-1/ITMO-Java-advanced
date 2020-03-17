package ru.ifmo.rain.dolzhanskii.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static ru.ifmo.rain.dolzhanskii.implementor.FileUtils.*;
import static ru.ifmo.rain.dolzhanskii.implementor.JarUtils.compileCode;
import static ru.ifmo.rain.dolzhanskii.implementor.JarUtils.createJar;

/**
 * Class implementing {@link JarImpler} and extending {@link Implementor}. Adds functionality
 * to create <code>JAR</code> containing compiled generated implementation of given class.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 */
public class JarImplementor extends Implementor implements JarImpler {
    /**
     * Default constructor.
     */
    public JarImplementor() {
    }

    /**
     * Produces <code>.jar</code> file implementing class or interface specified by provided <code>token</code>.
     * <p>
     * Generated class classes name should be same as classes name of the type token with <code>Impl</code> suffix
     * added.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <code>.jar</code> file.
     * @throws ImplerException when implementation cannot be generated.
     *
     * @see #implement(Class, Path) Implementation method
     * @see JarUtils#compileCode(Class, Path) Compilation method
     * @see JarUtils#createJar(Class, Path, Path) <code>JAR</code> collector method
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Arguments must not be null");
        }

        Path parentDir = createParentDirectories(jarFile);
        Path tmpDir = createTmpDir(parentDir);

        try {
            implement(token, tmpDir);
            compileCode(token, tmpDir);
            createJar(token, tmpDir, jarFile);
        } finally {
            deleteTmpDir(tmpDir);
        }
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
     * {@link JarImpler#implementJar(Class, Path)} is invoked.
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
