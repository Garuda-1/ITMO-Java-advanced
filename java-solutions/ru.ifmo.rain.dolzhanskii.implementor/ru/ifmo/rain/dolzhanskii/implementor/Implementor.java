package ru.ifmo.rain.dolzhanskii.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import static ru.ifmo.rain.dolzhanskii.implementor.FileUtils.*;

/**
 * Class implementing {@link Impler}. Provides public methods to generate abstract class or
 * interface basic implementation.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 0.9
 */
public class Implementor implements Impler {
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
}
