package ru.ifmo.rain.dolzhanskii.walk;

import java.io.*;
import java.nio.file.*;

public class RecursiveWalk {
    private static void recursiveWalk(String inputFileName, String outputFileName) throws RecursiveWalkException {
        // Create parent directories
        Path outputFilePath;
        try {
            outputFilePath = Paths.get(outputFileName);
            Path parentDirectory = outputFilePath.getParent();
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }
        } catch (InvalidPathException | IOException e) {
            throw new RecursiveWalkException("Unable to create output file parent directories '" + outputFileName + "'");
        }

        // Initialize input and output streams
        try (BufferedReader inputFileReader = Files.newBufferedReader(Paths.get(inputFileName))) {
            try (BufferedWriter outputFileWriter = Files.newBufferedWriter(outputFilePath)) {
                HashingFileVisitor visitor = new HashingFileVisitor(outputFileWriter);
                while (true) {
                    String pathName;
                    try {
                        pathName = inputFileReader.readLine();
                    } catch (IOException e) {
                        throw new RecursiveWalkException("Reading input file failure '" + inputFileName + "'", e);
                    }

                    if (pathName == null) {
                        break;
                    }

                    try {
                        try {
                            Files.walkFileTree(Paths.get(pathName), visitor);
                        } catch (InvalidPathException e) {
                            visitor.logHash(pathName, HashingFileVisitor.ERROR_HASH, true);
                        }
                    } catch (IOException e) {
                        throw new RecursiveWalkException("Unable to write to output file '" + outputFileName + "'", e);
                    }
                }
            } catch (IOException e) {
                throw new RecursiveWalkException("Unable to create output file '" + outputFileName + "'" , e);
            }
        } catch (InvalidPathException | IOException e) {
            throw new RecursiveWalkException("Unable to open input file '" + inputFileName + "'", e);
        }
    }

    public static void main(String[] args) {
        // Validate arguments
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Usage: java RecursiveWalk INPUT_FILE_PATH OUTPUT_FILE_PATH");
            return;
        }

        try {
            recursiveWalk(args[0], args[1]);
        } catch (RecursiveWalkException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
