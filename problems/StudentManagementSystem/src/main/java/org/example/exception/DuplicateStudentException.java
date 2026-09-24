package org.example.exception;

public class DuplicateStudentException extends RuntimeException {

    public DuplicateStudentException(int studentId) {
        super("Student already exists: " + studentId);
    }
}