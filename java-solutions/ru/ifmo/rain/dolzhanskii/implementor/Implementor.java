package ru.ifmo.rain.dolzhanskii.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static ru.ifmo.rain.dolzhanskii.implementor.FileUtils.prepareSourceCodePath;

/**
 * Class implementing {@link Impler}. Provides public methods to generate abstract class or
 * interface basic implementation. Includes main method providing console interface.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 0.9
 */
public class Implementor implements Impler {
    /**
     * Usage hint for user. Printed when arguments format is invalid.
     */
    static final String USAGE = "Usage: Implementor [-jar] className path";

    /**
     * Default constructor.
     */
    public Implementor() {
    }

    /**
     * Validates whether {@link Class} attributes and modifiers allow common implementation.
     *
     * @param token {@link Class} which implementation is required
     * @throws ImplerException In case check fails
     */
    private void validateToken(Class<?> token) throws ImplerException {
        int modifiers = token.getModifiers();
        if (token.isPrimitive() || token.isArray() || token == Enum.class || Modifier.isFinal(modifiers)
                || Modifier.isPrivate(modifiers)) {
            throw new ImplerException("Unsupported token given");
        }
    }

    /**
     * Produces code implementing class or interface specified by provided <code>token</code>.
     * <p>
     * Generated class classes name should be same as classes name of the type token with <code>Impl</code> suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * <code>root</code> directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} should go to <code>$root/java/util/ListImpl.java</code>
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException when implementation cannot be generated.
     *
     * @see #validateToken(Class) Token implementation support validator
     * @see SourceCodeUtils#generateSourceCode(Class) Source code generation method
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Arguments must not be null");
        }

        validateToken(token);

        Path sourceCodePath = prepareSourceCodePath(token, root);

        try (BufferedWriter sourceCodeWriter = Files.newBufferedWriter(sourceCodePath)) {
            sourceCodeWriter.write(SourceCodeUtils.generateSourceCode(token));
        } catch (IOException e) {
            throw new ImplerException("I/O error occurred", e);
        }
    }

    /**
     * Main function to provide console interface of the program.
     * <p>
     * Allowed signature: <code>token outputPath</code>
     * All arguments must not be null. Any errors and warnings are printed to <code>STDOUT</code> and
     * <code>STDERR</code>.
     *
     * @param args Provided to program arguments
     */
    public static void main(String[] args) {
        try {
            Objects.requireNonNull(args);
            if (args.length != 2) {
                System.out.println(USAGE);
                return;
            }
            Objects.requireNonNull(args[0]);
            Objects.requireNonNull(args[1]);
        } catch (NullPointerException e) {
            System.err.println("Error: Null arguments are not allowed");
            return;
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
            new Implementor().implement(token, root);
        } catch (ImplerException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
