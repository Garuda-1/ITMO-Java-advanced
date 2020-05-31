package ru.ifmo.rain.dolzhanskii.i18n.src;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.PropertyResourceBundle;

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

    static String generateReport(final PropertyResourceBundle bundle,
                                 final Map<TextStatistics.StatisticsType, TextStatistics.StatisticsData<?>> data,
                                 final String fileName)
            throws IOException {
        final String report = readFile("../resources/report-template.html");

        final String head = readFile("../resources/head-template.html");
        final String title = readFile("../resources/title-template.html");
        final String summary = readFile("../resources/summary-template.html");
        final String section = readFile("../resources/section-template.html");

        final String generatedHead = String.format(head,
                bundle.getString("title"));

        final String generatedTitle = String.format(title,
                bundle.getString("analyzedFile"),
                fileName);

        final String generatedSummary = String.format(summary,
                bundle.getString("summaryStats"),
                bundle.getString("sumSentences"),
                data.get(TextStatistics.StatisticsType.SENTENCE).getCountTotal(),
                bundle.getString("sumLines"),
                data.get(TextStatistics.StatisticsType.LINE).getCountTotal(),
                bundle.getString("sumWords"),
                data.get(TextStatistics.StatisticsType.WORD).getCountTotal(),
                bundle.getString("sumNumbers"),
                data.get(TextStatistics.StatisticsType.NUMBER).getCountTotal(),
                bundle.getString("sumMoney"),
                data.get(TextStatistics.StatisticsType.CURRENCY).getCountTotal(),
                bundle.getString("sumDates"),
                data.get(TextStatistics.StatisticsType.DATE));

        Arrays.stream(TextStatistics.StatisticsType.values())
                .map(type -> {
                    final String sectionName = type.toString().charAt(0) + type.toString().substring(1).toLowerCase();
                    final TextStatistics.StatisticsData stats = data.get(type);
                    return String.format(section,
                            bundle.getString("stats" + sectionName),
                            bundle.getString("sum" + sectionName),
                            )
                })
    }
}
