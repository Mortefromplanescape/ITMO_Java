package ru.ifmo.rain.rykunov.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StudentDB implements StudentQuery {

    private Comparator<Student> studentsCompare =
            Comparator
                    .comparing(Student::getLastName, String::compareTo)
                    .thenComparing(Student::getFirstName, String::compareTo)
                    .thenComparingInt(Student::getId);

    private List<String> getStudentsParameters(List<Student> students, Function<Student, String> func) {
        return students.parallelStream()
                .map(func)
                .collect(Collectors.toList());
    }

    public List<String> getFirstNames(List<Student> students) {
        return getStudentsParameters(students, Student::getFirstName);
    }

    public List<String> getLastNames(List<Student> students) {
        return getStudentsParameters(students, Student::getLastName);
    }

    public List<String> getGroups(List<Student> students) {
        return getStudentsParameters(students, Student::getGroup);
    }

    public List<String> getFullNames(List<Student> students) {
        return getStudentsParameters(students, s -> s.getFirstName() + " " + s.getLastName());
    }

    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.parallelStream()
                .map(Student::getFirstName)
                .collect(Collectors.toSet());
    }

    public String getMinStudentFirstName(List<Student> students) {
        return students.parallelStream()
                .min(Comparator.comparingInt(Student::getId))
                .orElse(new Student(0, "", "", "0"))
                // or .get() but unchecked empty List
                .getFirstName();
    }

    private List<Student> sortedStudentsBy(Collection<Student> students, Comparator<Student> comparator) {
        return students.parallelStream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortedStudentsBy(students, Comparator.comparingInt(Student::getId));
    }

    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortedStudentsBy(students, studentsCompare);
    }

    private List<Student> filteredStudents(Collection<Student> students, Predicate<Student> predicate) {
        return students.parallelStream()
                .filter(predicate)
                .sorted(studentsCompare)
                .collect(Collectors.toList());
    }

    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filteredStudents(students, s -> s.getFirstName().equals(name));
    }

    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filteredStudents(students, s -> s.getLastName().equals(name));
    }

    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return filteredStudents(students, s -> s.getGroup().equals(group));
    }

    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.parallelStream()
                .filter(s -> s.getGroup().equals(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, (String s1, String s2) -> s1.compareTo(s2) < 0 ? s1 : s2));
    }
}
