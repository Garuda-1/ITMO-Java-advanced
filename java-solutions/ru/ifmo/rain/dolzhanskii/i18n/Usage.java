package ru.ifmo.rain.dolzhanskii.i18n;

import java.util.*;

public class Usage {
    private static void printUsage(ResourceBundle bundle) {
        System.out.println(String.format(
            "%s Test [%s] %s\n" +
            "%s\n" +
            "     -o %s\n"+
            "...",
            bundle.getString("usage"),// Usage:
            bundle.getString("options"),// <options>
            bundle.getString("commands"),// <commands>
            bundle.getString("Options"),// Options:
            bundle.getString("-o")// Write output
        ));
    }
    public static void main(String[] args) {
        Locale locale;
        switch (args.length) {
            case 0:
                locale = Locale.getDefault();
                break;
            case 1:
                locale = new Locale(args[0]);
                break;
            case 2:
                locale = new Locale(args[0], args[1]);
                break;
            default:
                locale = new Locale(args[0], args[1], args[2]);
                break;
        }

        locale = Locale.US;

        ResourceBundle bundle = 
            ResourceBundle.getBundle("ru.ifmo.rain.dolzhanskii.i18n.UsageResourceBundle", locale);
        printUsage(bundle);
    }
}
