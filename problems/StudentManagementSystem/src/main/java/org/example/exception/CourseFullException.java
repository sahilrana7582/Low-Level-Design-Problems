package org.example.exception;

public class CourseFullException extends RuntimeException {

    public CourseFullException(int courseId) {
        super("Course is full: " + courseId);
    }
}