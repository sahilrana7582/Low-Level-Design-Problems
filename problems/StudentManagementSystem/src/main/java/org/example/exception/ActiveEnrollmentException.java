package org.example.exception;

public class ActiveEnrollmentException extends RuntimeException {

    public ActiveEnrollmentException(int courseId) {
        super(
                "Cannot remove course " +
                        courseId +
                        " because students are still enrolled"
        );
    }
}