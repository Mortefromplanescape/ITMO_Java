package ru.ifmo.rain.rykunov.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {

    private Comparator<Student> studentsCompare =
            Comparator
                    .comparing(Student::getLastName, String::compareTo)
                    .thenComparing(Student::getFirstName, String::compareTo)
                    .thenComparingInt(Student::getId);

    private List<String> getStudentsParameters(List<Student> students, Function<Student, String> func) {
        return getStudentsParametersStream(students, func)
                .collect(Collectors.toList());
    }

    private Stream<String> getStudentsParametersStream(List<Student> students, Function<Student, String> func) {
        return students.stream()
                .map(func);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getStudentsParameters(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getStudentsParameters(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getStudentsParameters(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getStudentsParameters(students, s -> String.format("%s %s", s.getFirstName(), s.getLastName()));
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getStudentsParametersStream(students, Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Comparator.comparingInt(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    private List<Student> sortedStudentsBy(Collection<Student> students, Comparator<Student> comparator) {
        return getSortedStudentStreamFromCollection(students, comparator)
                .collect(Collectors.toList());
    }

    private Stream<Student> getSortedStudentStreamFromCollection(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator);
    }

    private Stream<Student> getSortedStudentStreamFromStream(Stream<Student> students, Comparator<Student> comparator) {
        return students.sorted(comparator);
    }

    private Stream<Student> getFilteredStudentStreamFromCollection(Collection<Student> students, Predicate<Student> pred) {
        return students.stream().filter(pred);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortedStudentsBy(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortedStudentsBy(students, studentsCompare);
    }

    private List<Student> filteredStudents(Collection<Student> students, Predicate<Student> predicate) {
        return getSortedStudentStreamFromStream(
                getFilteredStudentStreamFromCollection(students, predicate), studentsCompare)
                .collect(Collectors.toList());
    }

    private Predicate<Student> getFirstNamePredicate(String name) {
        return s -> s.getFirstName().equals(name);
    }

    private Predicate<Student> getLastNamePredicate(String name) {
        return s -> s.getLastName().equals(name);
    }

    private Predicate<Student> getGroupPredicate(String group) {
        return s -> s.getGroup().equals(group);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filteredStudents(students, getFirstNamePredicate(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filteredStudents(students, getLastNamePredicate(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return filteredStudents(students, getGroupPredicate(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return getFilteredStudentStreamFromCollection(students, getGroupPredicate(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }
}