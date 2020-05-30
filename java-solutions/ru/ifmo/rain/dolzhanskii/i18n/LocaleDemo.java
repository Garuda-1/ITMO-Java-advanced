package ru.ifmo.rain.dolzhanskii.i18n;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ItemListener;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public class LocaleDemo {
    private static final Locale[] LOCALES = Locale.getAvailableLocales();
    static {
        Arrays.sort(LOCALES, Comparator.comparing(Locale::toString));
    }

    private static void resizeColumnWidth(final JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        final TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        IntStream.range(0, table.getColumnCount()).forEach(
                column -> columnModel.getColumn(column).setPreferredWidth(Math.max(
                        width(headerRenderer.getTableCellRendererComponent(table, table.getColumnName(column), false, false, -1, column)),
                        IntStream.range(0, table.getRowCount())
                                .map(row -> width(table.prepareRenderer(table.getCellRenderer(row, column), row, column)))
                                .max().getAsInt()
                ))
        );
    }

    private static int width(final Component component) {
        return component.getPreferredSize().width;
    }

    private static void updateModel(final DefaultTableModel model, final Column[] columns) {
        model.setDataVector(
                Arrays.stream(LOCALES)
                        .map(locale -> Arrays.stream(columns).map(c -> c.renderer.apply(locale)).toArray())
                        .toArray(Object[][]::new),
                Arrays.stream(columns)
                        .map(c -> localize("column", c.name))
                        .toArray()
        );
    }

    private static JComponent createControls(final JFrame frame, final JTable table, final DefaultTableModel model, final Table... tables) {
        final DefaultComboBoxModel<Table> tableChooserModel = new DefaultComboBoxModel<>(tables);
        final JComboBox<Table> tableChooser = new JComboBox<>(tableChooserModel);

        final JComboBox<Locale> localeChooser = new JComboBox<>(LOCALES);
        localeChooser.setSelectedItem(Locale.getDefault());

        final JComboBox<Integer> fontSize = new JComboBox<>(new Integer[]{5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 24, 28, 32, 40, 48, 60, 72});
        fontSize.setSelectedItem(14);


        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        final List<JLabel> labels = Arrays.asList(
                addLabel(panel, "table", 'V', tableChooser),
                addLabel(panel, "locale", 'L', localeChooser),
                addLabel(panel, "font", 'F', fontSize)
        );

        final ItemListener itemListener = e -> {
            final Locale locale = localeChooser.getItemAt(localeChooser.getSelectedIndex());
            Locale.setDefault(locale);

            bundle = PropertyResourceBundle.getBundle("ru.ifmo.rain.dolzhanskii.i18n.bundle.LocaleDemo");
            frame.applyComponentOrientation(ComponentOrientation.getOrientation(locale));

            updateModel(model, tableChooser.getItemAt(tableChooser.getSelectedIndex()).columns);
            setTableSize(table, fontSize.getItemAt(fontSize.getSelectedIndex()));

            frame.setTitle(localize("frame", "title"));
            labels.forEach(label -> label.setText(localize("label", label.getName())));
            tableChooser.setModel(tableChooserModel);
        };
        tableChooser.addItemListener(itemListener);
        localeChooser.addItemListener(itemListener);
        fontSize.addItemListener(itemListener);

        itemListener.itemStateChanged(null);

        return panel;
    }

    private static ResourceBundle bundle;
    private static String localize(final String prefix, final String name) {
        return bundle.getString(prefix + "." + name);
    }

    private static JLabel addLabel(final JPanel panel, final String name, final char mnemonic, final JComponent component) {
        final JLabel label = new JLabel(name);
        label.setName(name);
        label.setLabelFor(component);
        label.setDisplayedMnemonic(mnemonic);
        panel.add(label);
        panel.add(component);
        return label;
    }

    private static void setTableSize(final JTable table, final int size) {
        table.setFont(new Font(Font.DIALOG, Font.PLAIN, size));
        table.setRowHeight(size);
        table.getTableHeader().setFont(new Font(Font.DIALOG, Font.BOLD, size));
        resizeColumnWidth(table);
    }

    public static Column column(final String name, final Function<Locale, String> f) {
        return new Column(name, f);
    }

    private static String getISO3Country(final Locale locale) {
        try {
            return locale.getISO3Country();
        } catch (final MissingResourceException e) {
            return "???";
        }
    }

    private static Column dateColumn(final String name, final int type, final Date date) {
        return column(name, locale -> DateFormat.getDateInstance(type, locale).format(date));
    }

    private static Column timeColumn(final String name, final int type, final Date date) {
        return column(name, locale -> DateFormat.getTimeInstance(type, locale).format(date));
    }

    public static void main(final String[] args) {
        final Date date = new Date();

        final JFrame frame = new JFrame();
        final DefaultTableModel model = new DefaultTableModel();
        final JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        final JComponent controls = createControls(
                frame,
                table,
                model,
                new Table(
                        "locale",
                        column("locale", Locale::toString),
                        column("locale-name", Locale::getDisplayName),
                        column("language", Locale::getDisplayLanguage),
                        column("country", Locale::getDisplayCountry),
                        column("variant", Locale::getDisplayVariant),
                        column("iso-language", Locale::getISO3Language),
                        column("iso-country", LocaleDemo::getISO3Country)
                ),
                new Table(
                        "number",
                        column("locale", Locale::getDisplayName),
                        column("number", locale -> NumberFormat.getNumberInstance(locale).format(10789.8)),
                        column("integer", locale -> NumberFormat.getIntegerInstance(locale).format(10789.8)),
                        column("currency", locale -> NumberFormat.getCurrencyInstance(locale).format(10789.8)),
                        column("percent", locale -> NumberFormat.getPercentInstance(locale).format(10789.8))
                ),
                new Table(
                        "date",
                        column("locale", Locale::getDisplayName),
                        dateColumn("default", DateFormat.DEFAULT, date),
                        dateColumn("full", DateFormat.FULL, date),
                        dateColumn("long", DateFormat.LONG, date),
                        dateColumn("medium", DateFormat.MEDIUM, date),
                        dateColumn("short", DateFormat.SHORT, date)
                ),
                new Table(
                        "time",
                        column("locale", Locale::getDisplayName),
                        timeColumn("default", DateFormat.DEFAULT, date),
                        timeColumn("full", DateFormat.FULL, date),
                        timeColumn("long", DateFormat.LONG, date),
                        timeColumn("medium", DateFormat.MEDIUM, date),
                        timeColumn("short", DateFormat.SHORT, date)
                )
        );

        final Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());
        pane.add(new JScrollPane(table), BorderLayout.CENTER);
        pane.add(controls, BorderLayout.PAGE_START);

        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public static class Table {
        final String name;
        final Column[] columns;

        public Table(final String name, final Column... columns) {
            this.name = name;
            this.columns = columns;
        }

        @Override
        public String toString() {
            return localize("table", name);
        }
    }

    public static class Column {
        private final String name;
        private final Function<Locale, String> renderer;

        public Column(final String name, final Function<Locale, String> renderer) {
            this.name = name;
            this.renderer = renderer;
        }
    }
}
