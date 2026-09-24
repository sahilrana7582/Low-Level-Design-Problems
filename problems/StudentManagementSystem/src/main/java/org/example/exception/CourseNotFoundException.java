package org.example.exception;

public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(int courseId) {
        super("Course not found: " + courseId);
    }
}