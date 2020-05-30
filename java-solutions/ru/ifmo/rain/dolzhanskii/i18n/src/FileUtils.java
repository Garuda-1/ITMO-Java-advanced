package ru.ifmo.rain.dolzhanskii.i18n.src;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    static void createParentDirectories(final String outputFileName) {
        Path outputFilePath;
        try {
            outputFilePath = Paths.get(outputFileName);
            Path parentDirectory = outputFilePath.getParent();
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }
        } catch (InvalidPathException | IOException e) {
            throw new RuntimeException("Unable to create output file parent directories '" + outputFileName + "'");
        }
    }

    static String readFile(final String inputFileName) throws IOException {
        return Files.readString(Paths.get(inputFileName));
    }
}
