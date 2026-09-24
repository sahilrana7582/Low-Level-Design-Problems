package org.example.exception;

public class EnrollmentNotFoundException extends RuntimeException {

    public EnrollmentNotFoundException(int studentId, int courseId) {
        super(
                "Enrollment not found for student " +
                        studentId +
                        " in course " +
                        courseId
        );
    }
}