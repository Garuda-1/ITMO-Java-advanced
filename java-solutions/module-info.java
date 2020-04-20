module ru.ifmo.rain.dolzhanskii.implementor {
    requires info.kgeorgiy.java.advanced.walk;
    requires info.kgeorgiy.java.advanced.arrayset;
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;

    requires java.compiler;

    opens ru.ifmo.rain.dolzhanskii.implementor;
    exports ru.ifmo.rain.dolzhanskii.implementor;
}