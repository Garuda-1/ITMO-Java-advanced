package ru.ifmo.rain.dolzhanskii.i18n.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import ru.ifmo.rain.dolzhanskii.i18n.src.TextStatistics;

import java.util.Locale;
import java.util.Map;

public class TextStatisticsTest extends Assert {
    @Test
    @DisplayName("TEMPORARY")
    public void tempTest() {
        Locale.setDefault(Locale.US);
        final String text = "Added $100 at 05/20/2019, 10/10 requests remaining.";

        Map<TextStatistics.StatisticsType, TextStatistics.StatisticsData<?>> results = TextStatistics.getStatistics(text, Locale.US);
        System.out.println(results);
    }
}