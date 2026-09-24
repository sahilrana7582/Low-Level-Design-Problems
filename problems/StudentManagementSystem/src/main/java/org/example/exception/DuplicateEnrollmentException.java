package org.example.exception;

public class DuplicateEnrollmentException extends RuntimeException {

    public DuplicateEnrollmentException(int studentId, int courseId) {
        super(
                "Student " +
                        studentId +
                        " is already enrolled in course " +
                        courseId
        );
    }
}