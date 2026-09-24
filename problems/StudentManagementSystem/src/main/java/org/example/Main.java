package org.example;

import org.example.entity.Course;
import org.example.entity.Enrollment;
import org.example.entity.EnrollmentStatus;
import org.example.entity.Grade;
import org.example.entity.Student;
import org.example.service.CourseService;
import org.example.service.EnrollmentService;
import org.example.service.StudentService;

public class Main {

    public static void main(String[] args) {

        // =========================================================
        // SERVICES
        // =========================================================

        CourseService courseService = new CourseService();

        StudentService studentService = new StudentService();

        EnrollmentService enrollmentService =
                new EnrollmentService(
                        courseService,
                        studentService
                );

        // =========================================================
        // 1. REGISTER STUDENTS
        // =========================================================

        System.out.println("========== REGISTER STUDENTS ==========");

        Student sahil =
                new Student(
                        1,
                        "Sahil",
                        25,
                        "sahil@gmail.com"
                );

        Student rahul =
                new Student(
                        2,
                        "Rahul",
                        24,
                        "rahul@gmail.com"
                );

        Student amit =
                new Student(
                        3,
                        "Amit",
                        23,
                        "amit@gmail.com"
                );

        studentService.registerStudent(sahil);
        studentService.registerStudent(rahul);
        studentService.registerStudent(amit);

        System.out.println(
                "Registered Students:"
        );

        System.out.println(
                studentService.getRegisteredStudents()
        );

        // =========================================================
        // 2. ADD COURSES
        // =========================================================

        System.out.println("\n========== ADD COURSES ==========");

        Course java =
                new Course(
                        101,
                        "Java",
                        "Advanced Java and OOP",
                        2
                );

        Course systemDesign =
                new Course(
                        102,
                        "System Design",
                        "LLD and HLD fundamentals",
                        3
                );

        courseService.addCourse(java);
        courseService.addCourse(systemDesign);

        System.out.println("Courses added.");

        // =========================================================
        // 3. ENROLL STUDENTS
        // =========================================================

        System.out.println("\n========== ENROLL STUDENTS ==========");

        enrollmentService.newEnrollment(
                1,
                101
        );

        enrollmentService.newEnrollment(
                2,
                101
        );

        enrollmentService.newEnrollment(
                1,
                102
        );

        enrollmentService.newEnrollment(
                3,
                102
        );

        System.out.println("Enrollments created.");

        // =========================================================
        // 4. CURRENT COURSE ROSTER
        // =========================================================

        System.out.println("\n========== CURRENT ROSTER ==========");

        System.out.println(
                "Java:"
        );

        System.out.println(
                enrollmentService.getEnrolledStudents(101)
        );

        System.out.println(
                "System Design:"
        );

        System.out.println(
                enrollmentService.getEnrolledStudents(102)
        );

        // =========================================================
        // 5. COURSE-WISE STANDING
        // =========================================================

        System.out.println("\n========== COURSE STANDING ==========");

        System.out.println(
                "Java - Progressing:"
        );

        System.out.println(
                enrollmentService.getStanding(
                        101,
                        EnrollmentStatus.PROGRESSING
                )
        );

        System.out.println(
                "System Design - Progressing:"
        );

        System.out.println(
                enrollmentService.getStanding(
                        102,
                        EnrollmentStatus.PROGRESSING
                )
        );

        // =========================================================
        // 6. COMPLETE SAHIL'S JAVA COURSE
        // =========================================================

        System.out.println(
                "\n========== COMPLETE COURSE =========="
        );

        enrollmentService.completeCourse(
                1,
                101,
                85
        );

        System.out.println(
                "Sahil completed Java."
        );

        // =========================================================
        // 7. ROSTER AFTER COMPLETION
        // =========================================================

        System.out.println(
                "\n========== ROSTER AFTER COMPLETION =========="
        );

        System.out.println(
                "Java:"
        );

        System.out.println(
                enrollmentService.getEnrolledStudents(101)
        );

        // =========================================================
        // 8. SAHIL'S COURSE-SPECIFIC STANDING
        // =========================================================

        System.out.println(
                "\n========== SAHIL COURSE STANDING =========="
        );

        System.out.println(
                "Sahil - Java - COMPLETED:"
        );

        System.out.println(
                enrollmentService.getStudentCourseStanding(
                        1,
                        101,
                        EnrollmentStatus.COMPLETED
                )
        );

        // =========================================================
        // 9. COURSE-WIDE COMPLETED STANDING
        // =========================================================

        System.out.println(
                "\n========== COMPLETED STUDENTS =========="
        );

        System.out.println(
                enrollmentService.getStanding(
                        101,
                        EnrollmentStatus.COMPLETED
                )
        );

        // =========================================================
        // 10. RAHUL DROPS JAVA
        // =========================================================

        System.out.println(
                "\n========== DROP COURSE =========="
        );

        enrollmentService.dropCourse(
                2,
                101
        );

        System.out.println(
                "Rahul dropped Java."
        );

        // =========================================================
        // 11. ROSTER AFTER DROP
        // =========================================================

        System.out.println(
                "\n========== ROSTER AFTER DROP =========="
        );

        System.out.println(
                enrollmentService.getEnrolledStudents(101)
        );

        // =========================================================
        // 12. COURSE-WIDE DROPPED STANDING
        // =========================================================

        System.out.println(
                "\n========== DROPPED STUDENTS =========="
        );

        System.out.println(
                enrollmentService.getStanding(
                        101,
                        EnrollmentStatus.DROPPED
                )
        );

        // =========================================================
        // 13. STUDENT COURSE-SPECIFIC DROPPED STANDING
        // =========================================================

        System.out.println(
                "\n========== RAHUL COURSE STANDING =========="
        );

        System.out.println(
                "Rahul - Java - DROPPED:"
        );

        System.out.println(
                enrollmentService.getStudentCourseStanding(
                        2,
                        101,
                        EnrollmentStatus.DROPPED
                )
        );

        // =========================================================
        // 14. COMPLETE ENROLLMENT HISTORY
        // =========================================================

        System.out.println(
                "\n========== ENROLLMENT HISTORY =========="
        );

        System.out.println(
                "Sahil:"
        );

        System.out.println(
                enrollmentService.getStudentEnrollments(
                        1,
                        null
                )
        );

        System.out.println(
                "\nRahul:"
        );

        System.out.println(
                enrollmentService.getStudentEnrollments(
                        2,
                        null
                )
        );

        System.out.println(
                "\nAmit:"
        );

        System.out.println(
                enrollmentService.getStudentEnrollments(
                        3,
                        null
                ));

        // =========================================================
        // 15. STATUS-FILTERED HISTORY
        // =========================================================

        System.out.println(
                "\n========== FILTERED HISTORY =========="
        );

        System.out.println(
                "Sahil - COMPLETED:"
        );

        System.out.println(
                enrollmentService.getStudentEnrollments(
                        1,
                        EnrollmentStatus.COMPLETED
                )
        );

        System.out.println(
                "\nRahul - DROPPED:"
        );

        System.out.println(
                enrollmentService.getStudentEnrollments(
                        2,
                        EnrollmentStatus.DROPPED
                )
        );

        // =========================================================
        // 16. FINAL COURSE STANDINGS
        // =========================================================

        System.out.println(
                "\n========== FINAL STANDINGS =========="
        );

        System.out.println(
                "Java - PROGRESSING:"
        );

        System.out.println(
                enrollmentService.getStanding(
                        101,
                        EnrollmentStatus.PROGRESSING
                )
        );

        System.out.println(
                "Java - COMPLETED:"
        );

        System.out.println(
                enrollmentService.getStanding(
                        101,
                        EnrollmentStatus.COMPLETED
                )
        );

        System.out.println(
                "Java - DROPPED:"
        );

        System.out.println(
                enrollmentService.getStanding(
                        101,
                        EnrollmentStatus.DROPPED
                )
        );

        // =========================================================
        // 17. FINAL SYSTEM STATE
        // =========================================================

        System.out.println(
                "\n========== FINAL SYSTEM STATE =========="
        );

        System.out.println(
                "Java roster:"
        );

        System.out.println(
                enrollmentService.getEnrolledStudents(101)
        );

        System.out.println(
                "\nSystem Design roster:"
        );

        System.out.println(
                enrollmentService.getEnrolledStudents(102)
        );

        System.out.println(
                "\nSahil history:"
        );

        System.out.println(
                enrollmentService.getStudentEnrollments(
                        1,
                        null
                )
        );

        System.out.println(
                "\nRahul history:"
        );

        System.out.println(
                enrollmentService.getStudentEnrollments(
                        2,
                        null
                )
        );
    }
}