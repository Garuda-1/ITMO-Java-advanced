package ru.ifmo.rain.dolzhanskii.i18n.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.ifmo.rain.dolzhanskii.i18n.FileUtils;
import ru.ifmo.rain.dolzhanskii.i18n.TextStatistics;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class TextStatisticsTest extends Assert {
    private Locale locale;
    private final String resourceDirectory = "/home/oktet/IdeaProjects/JA/java-advanced-2020-solutions/java-solutions/ru/ifmo/rain/dolzhanskii/i18n/test/resources/";

    @Parameterized.Parameters(name = "{0}_{1}")
    public static Collection languages() {
        return Arrays.asList(new Object[][] {
                {"ru", "RU"},
                {"en", "US"}
        });
    }

    public TextStatisticsTest(String currentLanguage, String currentCountry) {
        this.locale = new Locale(currentLanguage, currentCountry);
    }

    private void validateStatistics(
            final Map<TextStatistics.StatisticsType, TextStatistics.StatisticsData<?>> statistics,
            final String testName) {

        ResourceBundle bundle = ResourceBundle
                .getBundle("ru.ifmo.rain.dolzhanskii.i18n.test.resources." + testName, locale);

        final Class<?> clazz = TextStatistics.StatisticsData.class;
        final List<String> fieldNames = Arrays.stream(clazz.getDeclaredFields()).map(Field::getName)
                .collect(Collectors.toList());
        for (TextStatistics.StatisticsType type : TextStatistics.StatisticsType.values()) {
            final TextStatistics.StatisticsData<?> data = statistics.get(type);
            for (String fieldName : fieldNames) {
                try {
                    Method fieldGetter = clazz.getMethod("get" + Character.toUpperCase(fieldName.charAt(0))
                            + fieldName.substring(1));
                    final Object gotObject = fieldGetter.invoke(data);
                    final String gotString = (gotObject == null) ? "null" : gotObject.toString();

                    try {
                        final String expectedString = bundle.getString(type.toString().toLowerCase() + "_" + fieldName);
//                        System.out.println(gotString + " --- " + expectedString);
                        assertEquals(expectedString, gotString);
                    } catch (final MissingResourceException e) {
                        // Ignored.
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void testRoutine(final String testName) throws IOException, IllegalAccessException {
        final String text = FileUtils.readFile(resourceDirectory + testName + "_" +
                locale.getLanguage() + "_" + locale.getCountry() + ".txt");
        final Map<TextStatistics.StatisticsType, TextStatistics.StatisticsData<?>> statistics
                = TextStatistics.getStatistics(text, locale);
//        TextStatistics.printStats(statistics);
        validateStatistics(statistics, testName);
    }

    @Test
    @DisplayName("Simple test")
    public void simpleTest() throws IOException, IllegalAccessException {
        testRoutine("simple");
    }

    @Test
    @DisplayName("Poem test")
    public void poemTest() throws IOException, IllegalAccessException {
        testRoutine("poem");
    }

    @Test
    @DisplayName("Biography test")
    public void biographyTest() throws IOException, IllegalAccessException {
        testRoutine("biography");
    }

    @Test
    @DisplayName("Dates test")
    public void datesTest() throws IOException, IllegalAccessException {
        testRoutine("dates");
    }

    @Test
    @DisplayName("Numerical test")
    public void numericalTest() throws IOException, IllegalAccessException {
        testRoutine("numerical");
    }
}
