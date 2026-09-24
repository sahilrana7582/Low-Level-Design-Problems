package org.example.exception;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(int studentId) {
        super("Student not found: " + studentId);
    }
}