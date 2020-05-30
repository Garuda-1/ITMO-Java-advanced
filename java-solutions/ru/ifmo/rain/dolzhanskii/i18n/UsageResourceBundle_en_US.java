package ru.ifmo.rain.dolzhanskii.i18n;

import java.util.*;

public class UsageResourceBundle_en_US
        extends ListResourceBundle {
    private final Object[][] CONTENTS = {
        {"usage", "Usage:"},
        {"options", "<options>"},
        {"commands", "<commands>"},
        {"Options", "Options"},
        {"-o", "Write output"},
    };

    protected Object[][] getContents() {
        return CONTENTS;
    }
}
