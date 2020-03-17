package ru.ifmo.rain.dolzhanskii.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Assisting class to {@link Implementor}. Provides tools for necessary operations
 * within file system.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 0.9
 */
class FileUtils {
    /**
     * Default temporary directory name prefix.
     */
    private static final String TMP_DIR_NAME = "impler-tmp";
    /**
     * Implementation file name suffix.
     */
    static final String IMPL_SUFFIX = "Impl";

    /**
     * Source code file extension.
     */
    static final String JAVA_EXTENSION = ".java";
    /**
     * Compiled code file extension.
     */
    static final String CLASS_EXTENSION = ".class";

    /**
     * Default constructor.
     */
    public FileUtils() {
    }

    /**
     * Provides valid source code directories. Formed considering package
     * and name.
     *
     * @param token {@link Class} which implementation is required
     * @return Path to source code as {@link String}
     */
    static String getImplementationPath(Class<?> token, String separator) {
        return String.join(separator, token.getPackageName().split("\\.")) +
                separator +
                token.getSimpleName();
    }

    /**
     * Creates missing parent directories of given path.
     *
     * @param path {@link Path} to generate parent directories for
     * @return Generated parent directory {@link Path}
     * @throws ImplerException In case I/O exception occurred in process
     */
    static Path createParentDirectories(Path path) throws ImplerException {
        Path parentPath = path.toAbsolutePath().normalize().getParent();
        if (parentPath != null) {
            try {
                Files.createDirectories(parentPath);
            } catch (IOException e) {
                throw new ImplerException("Failed to prepare source code directory", e);
            }
        }
        return parentPath;
    }

    /**
     * Creates {@link Path} of implementation source code and create missing parent directories.
     *
     * @param token {@link Class} which implementation is required
     * @param root  Root {@link Path} for implementation files
     * @return {@link Path} where implementation must be created
     * @throws ImplerException In case generated path is invalid
     */
    static Path prepareSourceCodePath(Class<?> token, Path root) throws ImplerException {
        String sourceCodePath = getImplementationPath(token, File.separator) + IMPL_SUFFIX + JAVA_EXTENSION;

        Path path;

        try {
            path = root.resolve(Path.of(sourceCodePath));
        } catch (InvalidPathException e) {
            throw new ImplerException("Invalid path generated", e);
        }

        createParentDirectories(path);

        return path;
    }

    /**
     * Creates temporary directory in given {@link Path}.
     *
     * @param root {@link Path} where temporary directory needs to be created
     * @return {@link Path} of created temporary directory
     * @throws ImplerException In case I/O exception occurred in process
     */
    static Path createTmpDir(Path root) throws ImplerException {
        Path tmpPath;
        try {
            tmpPath = Files.createTempDirectory(root, TMP_DIR_NAME);
        } catch (IOException e) {
            throw new ImplerException("Failed to create temporary directory", e);
        }
        return tmpPath;
    }

    /**
     * Deletes all contents of directory {@link File} recursively.
     *
     * @param file Target directory {@link Path}
     * @return <code>true</code> if all files have been successfully deleted
     * @see #deleteTmpDir
     */
    private static boolean deleteDirectory(File file) {
        boolean result = true;
        File[] files = file.listFiles();
        if (files != null) {
            for (File subFile : files) {
                result &= deleteDirectory(subFile);
            }
        }
        return result & file.delete();
    }

    /**
     * Deletes given temporary directory recursively. Prints warning message if some files
     * were not deleted.
     *
     * @param tmpPath {@link Path} of temporary directory
     */
    static void deleteTmpDir(Path tmpPath) {
        if (!deleteDirectory(tmpPath.toFile())) {
            System.err.println("Warning: Not all temporary files have been deleted");
        }
    }
}
