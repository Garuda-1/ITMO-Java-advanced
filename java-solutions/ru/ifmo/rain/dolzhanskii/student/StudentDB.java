package ru.ifmo.rain.dolzhanskii.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class StudentDB implements AdvancedStudentGroupQuery {
    
    

    /*
     * Student Queries
     */

    // Auxiliary functions and variables

    private static final Comparator<Student> STUDENT_BY_NAME_COMPARATOR =
            Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName)
                    .thenComparingInt(Student::getId);

    private static final Comparator<Student> STUDENT_BY_ID_COMPARATOR =
            Comparator.comparingInt(Student::getId)
                    .thenComparing(Student::getLastName)
                    .thenComparing(Student::getFirstName);

    private static <R> Stream<R> mappingStream(List<Student> students,
                                               Function<Student, R> mapper) {
        return students
                .stream()
                .map(mapper);
    }

    private static <R> List<R> mappingQuery(List<Student> students,
                                            Function<Student, R> mapper) {
        return mappingStream(students, mapper)
                .collect(Collectors.toList());
    }

    private static List<Student> sortingQuery(Collection<Student> students,
                                              Comparator<Student> comparator) {
        return students
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private static <R> R findQuery(Collection<Student> students,
                                   Predicate<Student> predicate,
                                   Comparator<Student> postComparator,
                                   Collector<Student, ?, R> collector) {
        return students
                .stream()
                .filter(predicate)
                .sorted(postComparator)
                .collect(collector);
    }

    private List<Student> simpleFindQuery(Collection<Student> students,
                                          Predicate<Student> filter) {
        return findQuery(students, filter, STUDENT_BY_NAME_COMPARATOR, Collectors.toList());
    }

    private String getFullName(Student s) {
        return s.getFirstName() + " " + s.getLastName();
    }

    // Methods implementation

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mappingQuery(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mappingQuery(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mappingQuery(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mappingQuery(students, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mappingStream(students, Student::getFirstName)
                .sorted()
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students
                .stream()
                .min(Comparator.comparingInt(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortingQuery(students, Comparator.comparingInt(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortingQuery(students, STUDENT_BY_NAME_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return simpleFindQuery(students, (Student s) -> name.equals(s.getFirstName()));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return simpleFindQuery(students, (Student s) -> name.equals(s.getLastName()));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return simpleFindQuery(students, (Student s) -> group.equals(s.getGroup()));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return findQuery(
                students,
                (Student s) -> group.equals(s.getGroup()),
                Comparator.naturalOrder(),
                Collectors.toMap(
                        Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo))
        );
    }

    /*
     * Student group queries
     */

    // Auxiliary functions and variables

    private static final Comparator<Group> GROUP_COMPARATOR =
            Comparator.comparing(Group::getName);

    private static <K, V> Stream<Map.Entry<K, V>> studentEntryStream(Collection<Student> students,
                                                                     Function<Student, K> keyFunction,
                                                                     Collector<Student, ?, V> valueCollector) {
        return students
                .stream()
                .collect(Collectors.groupingBy(keyFunction, valueCollector))
                .entrySet()
                .stream();
    }

    private static Stream<Group> groupStream(Collection<Student> students,
                                             Function<Map.Entry<String, List<Student>>, Group> groupConstructor) {
        return studentEntryStream(students, Student::getGroup, Collectors.toList())
                .map(groupConstructor)
                .sorted(GROUP_COMPARATOR);
    }

    private static List<Group> getGroupQuery(Collection<Student> students, Comparator<Student> comparator) {
        return groupStream(students, (Map.Entry<String, List<Student>> e) ->
                new Group(
                        e.getKey(), e.getValue().stream().sorted(comparator).collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("SameParameterValue")
    private static <T, R> R mappedMaxQuery(Stream<T> stream, Comparator<T> comparator, Function<T, R> mapper,
                                           R defaultValue) {
        return stream
                .max(comparator)
                .map(mapper)
                .orElse(defaultValue);
    }

    private static String maxGroupNameQuery(Collection<Student> students, Comparator<Group> comparator) {
        return mappedMaxQuery(
                groupStream(
                        students,
                        (Map.Entry<String, List<Student>> e) -> new Group(e.getKey(), e.getValue())),
                comparator, Group::getName, "");
    }

    // Methods implementation

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupQuery(students, STUDENT_BY_NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupQuery(students, STUDENT_BY_ID_COMPARATOR);
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return maxGroupNameQuery(students,
                Comparator.comparingInt((Group g) -> g.getStudents().size()).
                        thenComparing(Comparator.comparing(Group::getName).reversed()));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return mappedMaxQuery(
                students
                .stream()
                .collect(Collectors.groupingBy(Student::getGroup, Collectors.mapping(Student::getFirstName,
                                Collectors.collectingAndThen(Collectors.toSet(), Set::size))))
                .entrySet()
                .stream(),
                Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.<String, Integer>comparingByKey().reversed()),
                Map.Entry::getKey, "");
    }

    /*
     * Advanced queries
     */

    // Auxiliary functions and variables

    private static <R> List<R> filterIndicesQuery(List<Student> students, int[] indices,
                                                  Function<Student, R> function) {
        return Arrays
                .stream(indices)
                .mapToObj(List.copyOf(students)::get)
                .map(function)
                .collect(Collectors.toList());
    }

    // Methods implementation

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return mappedMaxQuery(
                studentEntryStream(students, this::getFullName, Collectors.mapping(Student::getGroup,
                Collectors.collectingAndThen(Collectors.toSet(), Set::size))),
                Map.Entry.<String, Integer>comparingByValue(Integer::compareTo).thenComparing(
                        Map.Entry.comparingByKey(String::compareTo)),
                Map.Entry::getKey,
                "");
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return filterIndicesQuery(new ArrayList<>(students), indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return filterIndicesQuery(new ArrayList<>(students), indices, Student::getLastName);
    }

    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return filterIndicesQuery(new ArrayList<>(students), indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return filterIndicesQuery(new ArrayList<>(students), indices, this::getFullName);
    }
}
