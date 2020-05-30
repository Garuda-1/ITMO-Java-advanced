package ru.ifmo.rain.dolzhanskii.walk;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class HashingFileVisitor extends SimpleFileVisitor<Path> {
    /* package-private */ static final int ERROR_HASH = 0;

    private Writer outputFileWriter;

    HashingFileVisitor(Writer fileWriter) {
        this.outputFileWriter = fileWriter;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        int hash;
        boolean erroneous = false;

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(path))) {
            hash = fnvHash(bufferedInputStream);
        } catch (IOException e) {
            hash = ERROR_HASH;
            erroneous = true;
        }

        logHash(path.toString(), hash, erroneous);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException exc) throws IOException {
        logHash(path.toString(), ERROR_HASH, true);
        return FileVisitResult.CONTINUE;
    }

    void logHash(String pathName, int hash, boolean erroneous) throws IOException {
        // Warn if hash value is erroneous
        if (erroneous) {
            System.err.println("Warning: Failed to read file '" + pathName + "'");
        }
        outputFileWriter.write(String.format("%08x %s%n", hash, pathName));
    }

    private static int fnvHash(InputStream inputStream) throws IOException {
        final int FNV_PRIME = 0x01000193;
        final int BLOCK_SIZE = 8192;

        int hash = 0x811c9dc5;
        int readCount;
        byte[] block = new byte[BLOCK_SIZE];

        while ((readCount = inputStream.read(block)) >= 0) {
            for (int i = 0; i < readCount; i++) {
                hash *= FNV_PRIME;
                hash ^= Byte.toUnsignedInt(block[i]);
            }
        }

        return hash;
    }
}