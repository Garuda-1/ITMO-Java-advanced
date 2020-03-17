package ru.ifmo.rain.dolzhanskii.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import java.nio.file.Path;

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
}
