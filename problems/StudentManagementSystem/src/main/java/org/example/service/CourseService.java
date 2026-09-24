package org.example.service;

import org.example.entity.Course;
import org.example.exception.ActiveEnrollmentException;
import org.example.exception.CourseNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class CourseService {

    private final Map<Integer, Course> courses =
            new HashMap<>();

    public void addCourse(Course course) {

        if (courses.containsKey(course.getId())) {
            throw new IllegalArgumentException(
                    "Course already exists: " +
                            course.getId()
            );
        }

        courses.put(
                course.getId(),
                course
        );
    }

    public void remove(
            int courseId
    ) {

        getCourse(courseId);

        courses.remove(courseId);
    }

    public Course getCourse(int courseId) {

        Course course = courses.get(courseId);

        if (course == null) {
            throw new CourseNotFoundException(courseId);
        }

        return course;
    }

    public int getMaxSeats(int courseId) {
        return getCourse(courseId).getMaxSeats();
    }
}