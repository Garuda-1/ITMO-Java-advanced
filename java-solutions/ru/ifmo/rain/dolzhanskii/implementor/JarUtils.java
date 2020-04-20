package ru.ifmo.rain.dolzhanskii.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import static ru.ifmo.rain.dolzhanskii.implementor.FileUtils.*;

/**
 * Assisting class to {@link Implementor}. Provides tools for necessary operations
 * with <code>JAR</code>.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 0.9
 */
class JarUtils {
    /**
     * Default constructor.
     */
    public JarUtils() {
    }

    /**
     * Compiles code of token implementation stored in temporary directory.
     * Searches for abstract super class or interface and adds it to classpath.
     * Requires compiler to be available in the system.
     *
     * @param token  {@link Class} to compile implementation of
     * @param tmpDir {@link Path} of directory where implementation source code is
     *               stored
     * @throws ImplerException In case generated path to source code is invalid
     * @throws ImplerException In case no compiler is provided
     * @throws ImplerException In case compilation finished with non-zero return code
     */
    static void compileCode(final Class<?> token, final Path tmpDir) throws ImplerException {
        final String subPath;
        try {
            subPath = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException e) {
            throw new ImplerException("Failed to retrieve location path");
        }

        final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler == null) {
            throw new ImplerException("No Java compiler provided");
        }

        final String[] compilerArgs = {
                "-cp",
                subPath,
                tmpDir.resolve(getImplementationPath(token, File.separator) + IMPL_SUFFIX + JAVA_EXTENSION).toString(),
        };

        final int returnCode = javaCompiler.run(null, null, null, compilerArgs);
        if (returnCode != 0) {
            throw new ImplerException("Implementation compilation returned non-zero code " + returnCode);
        }
    }

    /**
     * Creates <code>JAR</code> containing compiled implementation of <code>token</code>
     * at given {@link Path}.
     *
     * @param token      {@link Class} to pack implementation of
     * @param tmpDir     {@link Path} where source code is stored
     * @param targetPath {@link Path} where resulting <code>JAR</code> must be created
     * @throws ImplerException In case I/O error occurred
     */
    static void createJar(final Class<?> token, final Path tmpDir, final Path targetPath) throws ImplerException {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (final JarOutputStream stream = new JarOutputStream(Files.newOutputStream(targetPath), manifest)) {
            final String implementationPath = getImplementationPath(token, "/") + IMPL_SUFFIX + CLASS_EXTENSION;
            stream.putNextEntry(new ZipEntry(implementationPath));
            Files.copy(Path.of(tmpDir.toString(), implementationPath), stream);
        } catch (final IOException e) {
            throw new ImplerException("Failed to write JAR", e);
        }
    }
}
