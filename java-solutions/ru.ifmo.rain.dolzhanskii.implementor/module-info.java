/**
 * {@link info.kgeorgiy.java.advanced.implementor.Impler} and
 * {@link info.kgeorgiy.java.advanced.implementor.JarImpler} implementation and
 * auxiliary utilities.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 */
module ru.ifmo.rain.dolzhanskii.implementor {
    requires info.kgeorgiy.java.advanced.implementor;
    requires java.compiler;

    opens ru.ifmo.rain.dolzhanskii.implementor;
    exports ru.ifmo.rain.dolzhanskii.implementor;
}