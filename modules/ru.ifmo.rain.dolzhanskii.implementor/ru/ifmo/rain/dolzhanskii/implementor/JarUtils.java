package ru.ifmo.rain.dolzhanskii.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
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
import java.nio.file.Paths;
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
    static void compileCode(Class<?> token, Path tmpDir) throws ImplerException {
        Path superPath;
        try {
            CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                throw new ImplerException("Failed to retrieve super class source code");
            }
            URL sourceCodeUrl = codeSource.getLocation();
            if (sourceCodeUrl == null) {
                throw new ImplerException("Failed to retrieve super class code source location");
            }
            String sourceCodePath = sourceCodeUrl.getPath();
            if (sourceCodePath.isEmpty()) {
                throw new ImplerException("Failed to convert source code location");
            }
            if (sourceCodePath.startsWith("/")) {
                sourceCodePath = sourceCodePath.substring(1);
            }
            superPath = Path.of(sourceCodePath);
        } catch (InvalidPathException e) {
            throw new ImplerException("Failed to retrieve super class source code");
        }

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler == null) {
            throw new ImplerException("No Java compiler provided");
        }

        String[] compilerArgs = {
                "-cp",
                tmpDir.toString() + File.pathSeparator + superPath.toString(),
                tmpDir.resolve(getImplementationPath(token) + IMPL_SUFFIX + JAVA_EXTENSION).toString(),
        };

        int returnCode = javaCompiler.run(null, null, null, compilerArgs);
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
    static void createJar(Class<?> token, Path tmpDir, Path targetPath) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(targetPath), manifest)) {
            String implementationPath = getImplementationPath(token) + IMPL_SUFFIX + CLASS_EXTENSION;
            stream.putNextEntry(new ZipEntry(implementationPath));
            Files.copy(Path.of(tmpDir.toString(), implementationPath), stream);
        } catch (IOException e) {
            throw new ImplerException("Failed to write JAR", e);
        }
    }
}
