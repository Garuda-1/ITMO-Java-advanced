package ru.ifmo.rain.dolzhanskii.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Assisting class to {@link Implementor}. Provides tools for necessary operations
 * for implementation source code generation.
 *
 * @author Ian Dolzhanskii (yan.dolganskiy@mail.ru)
 * @version 0.9
 */
class SourceCodeUtils {
    /**
     * Empty string token.
     */
    private static final String EMPTY = "";
    /**
     * Single space token.
     */
    private static final String SPACE = " ";
    /**
     * Comma plus space token.
     */
    private static final String COMMA = ", ";
    /**
     * Tabulation token.
     */
    private static final String TAB = "\t";
    /**
     * Line separator token.
     */
    private static final String NL = System.lineSeparator();
    /**
     * End of instruction token.
     */
    private static final String EOI = ";";
    /**
     * Round brace opening token.
     */
    private static final String BRACES_OPEN = "(";
    /**
     * Round brace closing token.
     */
    private static final String BRACES_CLOSE = ")";
    /**
     * Block opening token.
     */
    private static final String BLOCK_OPEN = "{";
    /**
     * Block closing token.
     */
    private static final String BLOCK_CLOSE = "}";

    /**
     * Package declaration token.
     */
    private static final String PACKAGE = "package";
    /**
     * Class declaration token.
     */
    private static final String CLASS = "class";
    /**
     * Interface implementation declaration token.
     */
    private static final String IMPLEMENTS = "implements";
    /**
     * Super class extension declaration token.
     */
    private static final String EXTENDS = "extends";
    /**
     * Possible exceptions declaration token.
     */
    private static final String THROWS = "throws";
    /**
     * Super class reference token.
     */
    private static final String SUPER = "super";
    /**
     * Return value token.
     */
    private static final String RETURN = "return";

    /**
     * <code>null</code> token.
     */
    private static final String NULL = "null";
    /**
     * <code>false</code> token.
     */
    private static final String FALSE = "false";
    /**
     * <code>0</code> token.
     */
    private static final String ZERO = "0";

    /**
     * Implementation class name suffix.
     */
    private static final String IMPL_SUFFIX = "Impl";
    /**
     * Constructors of implementation comment marker.
     */
    private static final String CONSTRUCTORS_COMMENT = "/* Generated constructors. */";
    /**
     * Methods of implementation comment marker.
     */
    private static final String METHODS_COMMENT = "/* Generated methods. */";

    /**
     * Default constructor.
     */
    public SourceCodeUtils() {
    }

    /**
     * Name supplier. Used to generate arguments names. One instance should be used per listing.
     */
    private static class ArgNameGenerator implements Supplier<String> {
        /**
         * Default name prefix.
         */
        private static final String NAME_PREFIX = "arg";
        /**
         * Name index.
         */
        private int id = 0;

        /**
         * Default constructor.
         */
        ArgNameGenerator() {
        }

        /**
         * Subsequent name generator.
         *
         * @return {@link String} of next name
         */
        @Override
        public String get() {
            return NAME_PREFIX + id++;
        }
    }

    /**
     * Concatenates given {@link String} array elements in a single {@link String} using
     * given delimiter.
     *
     * @param delimiter {$link String} which is put between the elements
     * @param args      {@link String} array to concat
     * @return Collected {@link String}
     */
    private static String collectBlock(final String delimiter, final String... args) {
        return Arrays.stream(args).filter(s -> !s.isEmpty()).collect(Collectors.joining(delimiter));
    }

    /**
     * Appends <code>size</code> symbols <code>indentString</code> to <code>args</code>;
     *
     * @param indentString {@link String} to be appended in the beginning of result
     * @param size         Count of {@link} indent sunstrings to append in front
     * @param line         <code>JAR</code> where an archive shoudl be locates.
     * @return {@link String} of collected of <code>line</code> tokens and eparated by and <code>indent</code>indent string.
     */
    private static String indent(final String indentString, final int size, final String... line) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < size; i++) {
            result.append(indentString);
        }
        for (final String s : line) {
            result.append(s);
        }
        return result.toString();
    }

    /**
     * Generates source code of default value.
     *
     * @param clazz {@link Class} to find default value of
     * @return {@link String} source code of default value
     */
    private static String getDefaultValue(final Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return NULL;
        } else if (clazz.equals(void.class)) {
            return "";
        } else if (clazz.equals(boolean.class)) {
            return FALSE;
        } else {
            // :NOTE: Либо прямо написать "0" либо NUMBER_RETURN_VALUE
            return ZERO;
        }
    }

    /**
     * Filters modifiers for implementation class. Excludes general modifiers both from
     * {@link Class} and {@link Executable}.
     *
     * @param modifiers Modifiers of {@link Class} or {@link Executable}
     * @return {@link String} representation of filtered modifiers
     */
    private static String getModifiers(final int modifiers) {
        return Modifier.toString(modifiers & ~Modifier.ABSTRACT);
    }

    /**
     * Filters modifiers for implementation class. Excludes modifiers of {@link Class}.
     *
     * @param token {@link Class} to filter modifiers of
     * @return {@link String} representation of filtered modifiers
     */
    private static String getClassModifiers(final Class<?> token) {
        return getModifiers(token.getModifiers() & ~Modifier.INTERFACE & ~Modifier.STATIC & ~Modifier.PROTECTED);
    }

    /**
     * Filters modifiers for implementation class. Excludes modifiers of {@link Executable}.
     *
     * @param executable {@link Executable} to filter modifiers of
     * @return {@link String} representation of filtered modifiers
     */
    private static String getExecutableModifiers(final Executable executable) {
        return getModifiers(executable.getModifiers() & ~Modifier.NATIVE & ~Modifier.TRANSIENT);
    }

    /**
     * Generates implementation class simple name.
     *
     * @param token {@link Class} which implementation is required
     * @return Name {@link String} of implementation class
     */
    private static String getClassImplementationName(final Class<?> token) {
        return token.getSimpleName() + IMPL_SUFFIX;
    }

    /**
     * Generates {@link String} declaring or enumerating {@link Executable} arguments
     * separated by comma.
     *
     * @param executable  {@link Executable} which arguments are required
     * @param declaration If <code>True</code> then get declaration {@link String}
     * @return {@link String} of arguments list
     */
    private static String getArguments(final Executable executable, final boolean declaration) {
        final ArgNameGenerator nameGenerator = new ArgNameGenerator();
        final int countOfArgs = executable.getParameterCount();
        final String[] names = Stream.generate(nameGenerator).limit(countOfArgs).toArray(String[]::new);
        if (declaration) {
            // :NOTE: Можно было сделать в один проход без циклов
            final String[] types = Arrays.stream(executable.getParameterTypes())
                    .map(Class::getCanonicalName).toArray(String[]::new);
            for (int index = 0; index < countOfArgs; index++) {
                names[index] = collectBlock(SPACE, types[index], names[index]);
            }
        }

        return String.join(COMMA, names);
    }

    /**
     * Generates {@link String} enumerating {@link Executable} exceptions.
     *
     * @param executable {@link Executable} which exceptions are required
     * @return {@link String} exceptions list
     */
    private static String getThrowingExceptions(final Executable executable) {
        final Class[] exceptionTypes = executable.getExceptionTypes();
        if (exceptionTypes.length != 0) {
            return collectBlock(SPACE,
                    THROWS,
                    Arrays.stream(exceptionTypes).map(Class::getCanonicalName).collect(Collectors.joining(COMMA)));
        } else {
            return "";
        }
    }

    // :NOTE: Типы и так видны в сигнатуре
    /**
     * Generates {@link String} declaring implementation class package if available.
     *
     * @param token {@link Class} which implementation is required
     * @return Package declaration {@link String} if token is in package or empty
     * {@link String} otherwise
     */
    private static String generatePackageLine(final Class<?> token) {
        final String pkgName = token.getPackage().getName();
        return pkgName.isEmpty() ? "" : collectBlock(SPACE, PACKAGE, pkgName, EOI);
    }

    /**
     * Generates class opening line. Includes modifiers, name and super class.
     *
     * @param token {@link Class} which implementation is required
     * @return Implementation class opening line
     */
    private static String generateClassOpeningLine(final Class<?> token) {
        return collectBlock(SPACE,
                getClassModifiers(token),
                CLASS,
                getClassImplementationName(token),
                token.isInterface() ? IMPLEMENTS : EXTENDS,
                token.getCanonicalName(),
                BLOCK_OPEN);
    }

    /**
     * Generates executable opening line. Includes modifiers, return code if it is
     * an instance of {@link Method}, name, generated args and possible exceptions.
     *
     * @param executable {@link Executable} which opening line is required
     * @return Opening {@link String} of requested {@link Executable}
     */
    private static String generateExecutableOpeningLine(final Executable executable) {
        final String executableName;
        final String returnType;
        if (executable instanceof Constructor) {
            executableName = getClassImplementationName(executable.getDeclaringClass());
            returnType = "";
        } else {
            executableName = executable.getName();
            returnType = ((Method) executable).getReturnType().getCanonicalName();
        }

        return collectBlock(SPACE,
                getExecutableModifiers(executable),
                returnType,
                executableName,
                BRACES_OPEN,
                getArguments(executable, true),
                BRACES_CLOSE,
                getThrowingExceptions(executable),
                BLOCK_OPEN);
    }

    /**
     * Generates {@link Executable} complete code.
     *
     * @param executable {@link Executable} which body is required
     * @param body       {@link String} representing method body
     * @return Implementation {@link String} of required {@link Executable}
     */
    private static String generateExecutable(final Executable executable, final String body) {
        return collectBlock(NL,
                indent(TAB, 1, generateExecutableOpeningLine(executable)),
                indent(TAB, 2, body),
                indent(TAB, 1, BLOCK_CLOSE),
                NL);
    }

    /**
     * Generates {@link Constructor} body code. By default, super class
     * constructor is called.
     *
     * @param constructor {@link Constructor} which implementation is needed
     * @return Body implementation {@link String} of required {@link Constructor}
     */
    private static String generateConstructorBody(final Constructor constructor) {
        return collectBlock(SPACE,
                SUPER,
                BRACES_OPEN,
                getArguments(constructor, false),
                BRACES_CLOSE,
                EOI);
    }

    /**
     * Generated {@link Constructor} code.
     *
     * @param constructor {@link Constructor} which implementation is needed
     * @return Body implementation {@link String} of required {@link Constructor}
     */
    private static String generateConstructor(final Constructor constructor) {
        return generateExecutable(constructor, generateConstructorBody(constructor));
    }

    /**
     * Generates all available constructors source code for the class.
     *
     * @param token {@link Class} which implementation is required
     * @return {@link List} of constructors source code
     * @throws ImplerException In case non-private constructors are missing
     */
    private static List<String> generateAllConstructors(final Class<?> token) throws ImplerException {
        if (token.isInterface()) {
            return new ArrayList<>();
        }
        final List<Constructor<?>> nonPrivateConstructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .collect(Collectors.toList());
        if (nonPrivateConstructors.isEmpty()) {
            throw new ImplerException("At least one non-private constructor required");
        }

        return nonPrivateConstructors
                .stream()
                .map(SourceCodeUtils::generateConstructor)
                .collect(Collectors.toList());
    }

    /**
     * {@link Method} wrapping class. Used to override comparison by signature
     * (name, arguments types, return type).
     */
    private static class SignatureComparedMethod {
        /**
         * Enclosed {@link Method}.
         */
        private final Method method;

        /**
         * Polynomial hash power.
         */
        private static final int POW = 31;
        /**
         * Polynomial hash module.
         */
        private static final int MOD = 1000000007;

        /**
         * Wrapping constructor.
         *
         * @param method {@link Method} to wrap
         */
        SignatureComparedMethod(final Method method) {
            this.method = method;
        }

        /**
         * Method getter.
         *
         * @return Wrapped {@link #method}
         */
        Method getMethod() {
            return method;
        }

        /**
         * Hash code calculator. Calculates polynomial hash of {@link #method} signature.
         *
         * @return integer hash code value
         */
        @Override
        public int hashCode() {
            int hash = method.getReturnType().hashCode() % MOD;
            hash = (hash + POW * method.getName().hashCode()) % MOD;
            hash = (hash + POW * POW * Arrays.hashCode(method.getParameterTypes()));
            return hash;
        }

        /**
         * Compare to another object. An object is considered equal if and only if that {@link #method} signature matches.
         *
         * @param o {@link Object} to compare with
         * @return <code>True</code> if objects are equal, <code>False</code> otherwise
         */
        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final SignatureComparedMethod that = (SignatureComparedMethod) o;
            return Objects.equals(method.getReturnType(), that.method.getReturnType()) &&
                    method.getName().equals(that.method.getName()) &&
                    Arrays.equals(method.getParameterTypes(), that.method.getParameterTypes());
        }
    }

    /**
     * Generates {@link Method} body. By default, all methods return the default value of an appropriate type.
     *
     * @param method {@link Method} to generate body of
     * @return Method implementation body as {@link String}
     * @see #getDefaultValue(Class)
     */
    private static String generateMethodBody(final Method method) {
        return collectBlock(SPACE,
                RETURN,
                getDefaultValue(method.getReturnType()),
                EOI);
    }

    /**
     * Generates {@link Method} code.
     *
     * @param method {@link Method} to generate body of
     * @return Method implementation body as {@link String}
     */
    private static String generateMethod(final Method method) {
        return generateExecutable(method, generateMethodBody(method));
    }

    /**
     * Collects {@link Set}. Removes overridden {@link Method}s.
     *
     * @param methods {@link Method} array that should be filtered
     * @return {@link Set} of final versions of each {@link Method}
     */
    private static Set<SignatureComparedMethod> getSignatureDistinctMethods(final Method[] methods) {
        return Arrays.stream(methods)
                .map(SignatureComparedMethod::new)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Generates {@link List} of implementations of each final version {@link Method} among given ones.
     *
     * @param token {@link Class} which implementation is required
     * @return {@link List} of {@link Method} implementations.
     */
    private static List<String> generateAllMethods(Class<?> token) {
        final Set<SignatureComparedMethod> methodsSet = getSignatureDistinctMethods(token.getMethods());
        for (; token != null; token = token.getSuperclass()) {
            methodsSet.addAll(getSignatureDistinctMethods(token.getDeclaredMethods()));
        }
        return methodsSet
                .stream()
                .filter(m -> Modifier.isAbstract(m.getMethod().getModifiers()))
                .map(m -> generateMethod(m.getMethod()))
                .collect(Collectors.toList());
    }

    /**
     * Translates input {@link String} characters into universal unicode representation.
     *
     * @param input {@link String} to be translated
     * @return Translation result
     */
    private static String unicodeTranslator(final String input) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final char c : input.toCharArray()) {
            stringBuilder.append(c < 128 ? String.valueOf(c) : String.format("\\u%04x", (int) c));
        }
        return stringBuilder.toString();
    }

    /**
     * Generated complete source code of given {@link Class}.
     *
     * @param token {@link Class} which implementation is required
     * @return {@link String} containing complete generated source code
     * @throws ImplerException In case non-private constructors are missing
     *
     * @see #generatePackageLine(Class) Package declaration generation method
     * @see #generateClassOpeningLine(Class) Class declaration generation method
     * @see #generateAllConstructors(Class) Constructors generation method
     * @see #generateAllMethods(Class) Methods generation method
     * @see #unicodeTranslator(String) Unicode characters translator
     */
    static String generateSourceCode(final Class<?> token) throws ImplerException {
        return unicodeTranslator(collectBlock(NL,
                generatePackageLine(token),
                indent(NL, 1, generateClassOpeningLine(token)),
                indent(TAB, 1, CONSTRUCTORS_COMMENT),
                String.join(EMPTY, generateAllConstructors(token)),
                indent(TAB, 1, METHODS_COMMENT),
                String.join(EMPTY, generateAllMethods(token)),
                BLOCK_CLOSE));
    }
}
