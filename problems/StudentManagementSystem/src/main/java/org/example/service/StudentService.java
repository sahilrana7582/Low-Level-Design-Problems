package org.example.service;

import org.example.entity.Student;
import org.example.exception.DuplicateStudentException;
import org.example.exception.StudentNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentService {

    private final Map<Integer, Student> registeredStudents =
            new HashMap<>();

    public void registerStudent(Student student) {

        if (registeredStudents.containsKey(student.getId())) {
            throw new DuplicateStudentException(
                    student.getId()
            );
        }

        registeredStudents.put(
                student.getId(),
                student
        );
    }

    public Student getStudent(int studentId) {

        Student student = registeredStudents.get(studentId);

        if (student == null) {
            throw new StudentNotFoundException(studentId);
        }

        return student;
    }

    public boolean exists(int studentId) {
        return registeredStudents.containsKey(studentId);
    }

    public List<Student> getRegisteredStudents() {
        return new ArrayList<>(
                registeredStudents.values()
        );
    }
}