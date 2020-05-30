package ru.ifmo.rain.dolzhanskii.i18n;

import java.util.*;
import java.text.*;

public class Test {
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        System.out.println(
            MessageFormat.format(
                "{0,date}: {1,number,currency} money available",
                new Date(), 10.8
            )
        );
    }
}