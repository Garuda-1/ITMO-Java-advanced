package ru.ifmo.rain.dolzhanskii.i18n;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class TextStatistics {
    public enum StatisticsType {
        SENTENCE,
        LINE,
        WORD,
        NUMBER,
        MONEY,
        DATE
    }

    public static class StatisticsData<T> {
        private StatisticsType type;
        private int countTotal;
        private int countUnique;
        private T minValue;
        private T maxValue;
        private T meanValue;
        private int minLength;
        private T minLengthValue;
        private int maxLength;
        private T maxLengthValue;
        private double meanLength;

        private StatisticsData() {}

        private static <T> StatisticsData<T> calculateCommonStatistics(final StatisticsType type, final List<T> samples,
                                                               final Function<T, Integer> sampleLength,
                                                               final Comparator<T> valueComparator) {
            final StatisticsData<T> stats = new StatisticsData<>();
            stats.type = type;
            if (samples.isEmpty()) {
                return stats;
            }

            stats.countTotal = samples.size();

            samples.sort(Comparator.comparing(sampleLength));

            stats.minLength = sampleLength.apply(samples.get(0));
            stats.minLengthValue = samples.get(0);
            stats.maxLength = sampleLength.apply(samples.get(samples.size() - 1));
            stats.maxLengthValue = samples.get(samples.size() - 1);
            stats.meanLength = (double) samples.stream().map(sampleLength).reduce(0, Integer::sum) /
                    samples.size();

            samples.sort(valueComparator);

            stats.minValue = samples.get(0);
            stats.maxValue = samples.get(samples.size() - 1);

            stats.countUnique = 1;
            for (int i = 0; i < samples.size() - 1; i++) {
                if (valueComparator.compare(samples.get(i), samples.get(i + 1)) != 0) {
                    stats.countUnique++;
                }
            }

            return stats;
        }

        static StatisticsData<String> calculateStringStatistics(final StatisticsType type, final List<String> samples,
                                                                final Locale locale) {
            return calculateCommonStatistics(type, samples, String::length,
                    (String s, String t) -> Collator.getInstance(locale).compare(s, t));
        }

        static StatisticsData<Number> calculateNumberStatistics(final StatisticsType type, final List<Number> samples) {
            StatisticsData<Number> stats = calculateCommonStatistics(type, samples, n -> n.toString().length(),
                    Comparator.comparingDouble(Number::doubleValue));
            stats.meanValue = samples.isEmpty() ? null : samples.stream().map(n -> BigDecimal.valueOf(n.doubleValue()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(stats.countTotal), RoundingMode.HALF_EVEN);
            return stats;
        }

        static StatisticsData<Date> calculateDateStatistics(final List<Date> samples) {
            StatisticsData<Date> stats = calculateCommonStatistics(StatisticsType.DATE, samples,
                    n -> n.toString().length(), Comparator.comparingLong(Date::getTime));
            stats.meanValue = samples.isEmpty() ? null : new Date(samples.stream().map(n -> BigInteger.valueOf(n.getTime()))
                    .reduce(BigInteger.ZERO, BigInteger::add)
                    .divide(BigInteger.valueOf(stats.countTotal)).longValue());
            return stats;
        }

        public StatisticsType getType() {
            return type;
        }

        private Format getAppropriateFormat(final Locale locale, boolean isNumeric) {
            if (isNumeric) {
                Format format = NumberFormat.getNumberInstance(locale);
                ((DecimalFormat) format).applyPattern("#0.###");
                return format;
            }
            switch (type) {
                case NUMBER:
                    return NumberFormat.getNumberInstance(locale);

                case MONEY:
                    return NumberFormat.getCurrencyInstance(locale);

                case DATE:
                    return DateFormat.getDateInstance(DateFormat.SHORT, locale);

                default:
                    return null;
            }
        }

        private String getFormatted(final Locale locale, T value, final String notAvailable) {
            if (value == null) {
                return notAvailable;
            }
            final Format format = getAppropriateFormat(locale, false);
            return format == null ? value.toString() : format.format(value);
        }

        private String getFormatted(final Locale locale, Number value) {
            final Format format = getAppropriateFormat(locale, true);
            return format == null ? value.toString() : format.format(value);
        }

        String getCountTotal(final Locale outputLocale) {
            return getFormatted(outputLocale, countTotal);
        }

        String getCountUnique(final Locale outputLocale) {
            return getFormatted(outputLocale, countUnique);
        }

        String getMinValue(final Locale locale, final String notAvailable) {
            return getFormatted(locale, minValue, notAvailable);
        }

        String getMaxValue(final Locale locale, final String notAvailable) {
            return getFormatted(locale, maxValue, notAvailable);
        }

        String getMeanValue(final Locale locale, final String notAvailable) {
            return getFormatted(locale, meanValue, notAvailable);
        }

        String getMinLength(final Locale outputLocale) {
            return getFormatted(outputLocale, minLength);
        }

        String getMinLengthValue(final Locale locale, final String notAvailable) {
            return getFormatted(locale, minLengthValue, notAvailable);
        }

        String getMaxLength(final Locale outputLocale) {
            return getFormatted(outputLocale, maxLength);
        }

        String getMaxLengthValue(final Locale locale, final String notAvailable) {
            return getFormatted(locale, maxLengthValue, notAvailable);
        }

        String getMeanLength(final Locale outputLocale) {
            return getFormatted(outputLocale, meanLength);
        }

        public int getCountTotal() {
            return countTotal;
        }

        public int getCountUnique() {
            return countUnique;
        }

        public T getMinValue() {
            return minValue;
        }

        public T getMaxValue() {
            return maxValue;
        }

        public T getMeanValue() {
            return meanValue;
        }

        public int getMinLength() {
            return minLength;
        }

        public T getMinLengthValue() {
            return minLengthValue;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public T getMaxLengthValue() {
            return maxLengthValue;
        }

        public double getMeanLength() {
            return meanLength;
        }
    }

    private static StatisticsData<String> getStringStatistics(final StatisticsType type,
                                                              final BreakIterator breakIterator, final String text,
                                                              final Locale locale, final Predicate<String> filter) {
        breakIterator.setText(text);
        final List<String> samples = new ArrayList<>();
        for (int start = breakIterator.first(), end = breakIterator.next(); end != BreakIterator.DONE;
             start = end, end = breakIterator.next()) {
            final String sample = text.substring(start, end);
            if (filter.test(sample)) {
                samples.add(type == StatisticsType.WORD ? sample.toLowerCase() : sample);
            }
        }
        return StatisticsData.calculateStringStatistics(type, samples, locale);
    }

    private static Number parseMoney(final String text, final ParsePosition position, final Locale locale) {
        final NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        return format.parse(text, position);
    }

    private static Date parseDate(final String text, final ParsePosition position, final Locale locale) {
        final DateFormat formatFull = DateFormat.getDateInstance(DateFormat.FULL, locale);
        final DateFormat formatLong = DateFormat.getDateInstance(DateFormat.LONG, locale);
        final DateFormat formatMedium = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        final DateFormat formatShort = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        Date date;
        if ((date = formatFull.parse(text, position)) == null) {
            if ((date = formatLong.parse(text, position)) == null) {
                if ((date = formatMedium.parse(text, position)) == null) {
                    date = formatShort.parse(text, position);
                }
            }
        }
        return date;
    }

    private static StatisticsData<Number> getNumberStatistics(final String text, final Locale locale) {
        final BreakIterator breakIterator = BreakIterator.getWordInstance(locale);
        breakIterator.setText(text);
        final List<Number> samples = new ArrayList<>();
        final NumberFormat format = NumberFormat.getNumberInstance(locale);

        for (int start = breakIterator.first(), end = breakIterator.next(), ignoreLimit = 0;
             end != BreakIterator.DONE;
             start = end, end = breakIterator.next()) {
            if (start < ignoreLimit) {
                continue;
            }
            final ParsePosition position = new ParsePosition(start);
            if (parseDate(text, position, locale) != null || parseMoney(text, position, locale) != null) {
                ignoreLimit = position.getIndex();
                continue;
            }
            final Number sample = format.parse(text, new ParsePosition(start));
            if (sample != null) {
                samples.add(sample);
            }
        }

        return StatisticsData.calculateNumberStatistics(StatisticsType.NUMBER, samples);
    }

    private static <T> List<T> getParsableSamples(final String text, final Locale locale,
                                                  final BiFunction<String, ParsePosition, T> parser) {
        final BreakIterator breakIterator = BreakIterator.getWordInstance(locale);
        breakIterator.setText(text);
        final List<T> samples = new ArrayList<>();

        for (int start = breakIterator.first(), end = breakIterator.next(), ignoreLimit = 0;
             end != BreakIterator.DONE;
             start = end, end = breakIterator.next()) {
            if (start < ignoreLimit) {
                continue;
            }
            final ParsePosition position = new ParsePosition(start);
            final T sample = parser.apply(text, position);
            if (sample != null) {
                samples.add(sample);
                ignoreLimit = position.getIndex();
            }
        }

        return samples;
    }

    private static StatisticsData<Number> getMoneyStatistics(final String text, final Locale locale) {
        List<Number> samples = getParsableSamples(text, locale, (t, p) -> parseMoney(t, p, locale));
        return StatisticsData.calculateNumberStatistics(StatisticsType.MONEY, samples);
    }

    private static StatisticsData<Date> getDateStatistics(final String text, final Locale locale) {
        List<Date> samples = getParsableSamples(text, locale, (t, p) -> parseDate(t, p, locale));
        return StatisticsData.calculateDateStatistics(samples);
    }

    public static Map<StatisticsType, StatisticsData<?>> getStatistics(final String text, final Locale inputLocale) {
        final Map<StatisticsType, StatisticsData<?>> map = new HashMap<>();

        map.put(StatisticsType.SENTENCE, getStringStatistics(StatisticsType.SENTENCE,
                BreakIterator.getSentenceInstance(inputLocale), text, inputLocale, s -> true));
        map.put(StatisticsType.LINE, getStringStatistics(StatisticsType.LINE,
                BreakIterator.getLineInstance(inputLocale), text, inputLocale, s -> true));
        map.put(StatisticsType.WORD, getStringStatistics(StatisticsType.WORD,
                BreakIterator.getWordInstance(inputLocale), text, inputLocale,
                s -> Character.isLetter(s.charAt(0))));

        map.put(StatisticsType.NUMBER, getNumberStatistics(text, inputLocale));
        map.put(StatisticsType.MONEY, getMoneyStatistics(text, inputLocale));

        map.put(StatisticsType.DATE, getDateStatistics(text, inputLocale));

        return map;
    }

    private static Locale getLocaleByArg(String arg) {
        String[] nameParts = arg.split("_");
        switch (nameParts.length) {
            case 0: {
                return Locale.getDefault();
            }
            case 1: {
                return new Locale(nameParts[0]);
            }
            case 2: {
                return new Locale(nameParts[0], nameParts[1]);
            }
            case 3: {
                return new Locale(nameParts[0], nameParts[1], nameParts[2]);
            }
            default: {
                throw new IllegalArgumentException("Invalid locale provided");
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 4 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.out.println("Usage: TextStatistics inputLocale outputLocale inputFileName outputFileName");
            return;
        }
        final Locale inputLocale = getLocaleByArg(args[0]);
        final Locale outputLocale = getLocaleByArg(args[1]);
        if (!(outputLocale.getLanguage().equals("ru") || outputLocale.getLanguage().equals("en"))) {
            System.err.println("Error: Unsupported output locale provided");
        }

        final String text;
        try {
            text = FileUtils.readFile(Path.of(args[2]));
        } catch (final IOException | InvalidPathException e) {
            System.err.println("Error: Failed to read input file, " + e.getMessage());
            return;
        }
        Map<TextStatistics.StatisticsType, TextStatistics.StatisticsData<?>> statistics =
                TextStatistics.getStatistics(text, inputLocale);

        final String report;
        try {
            report = FileUtils.generateReport(inputLocale, outputLocale, statistics, args[2]);
        } catch (final IOException e) {
            System.err.println("Error: Failed to generate report, " + e.getMessage());
            return;
        }

        try {
            FileUtils.writeFile(args[3], report);
        } catch (final IOException | InvalidPathException e) {
            System.err.println("Error: Failed to write a report file, " + e.getMessage());
        }
    }
}
