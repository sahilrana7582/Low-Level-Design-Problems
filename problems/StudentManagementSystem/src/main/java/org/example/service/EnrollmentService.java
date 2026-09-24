package org.example.service;

import org.example.entity.Course;
import org.example.entity.Enrollment;
import org.example.entity.EnrollmentStatus;
import org.example.entity.Grade;
import org.example.entity.Student;
import org.example.exception.ActiveEnrollmentException;
import org.example.exception.CourseFullException;
import org.example.exception.DuplicateEnrollmentException;
import org.example.exception.EnrollmentNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnrollmentService {

    private final CourseService courseService;
    private final StudentService studentService;

    /*
     * Current roster.
     *
     * courseId -> students currently progressing
     */
    private final Map<Integer, List<Student>> enrolledStudents =
            new HashMap<>();

    /*
     * Complete immutable enrollment history.
     *
     * studentId -> all enrollment snapshots
     */
    private final Map<Integer, List<Enrollment>> studentEnrollments =
            new HashMap<>();

    private int enrollmentId = 1;

    public EnrollmentService(
            CourseService courseService,
            StudentService studentService
    ) {
        this.courseService = courseService;
        this.studentService = studentService;
    }

    public void newEnrollment(
            int studentId,
            int courseId
    ) {

        // 1. Verify student
        Student student =
                studentService.getStudent(studentId);

        // 2. Verify course
        Course course =
                courseService.getCourse(courseId);

        /*
         * Re-enrollment is NOT allowed.
         *
         * This checks the complete history, not just
         * the latest enrollment.
         */
        if (hasEnrollmentHistory(studentId, courseId)) {
            throw new DuplicateEnrollmentException(
                    studentId,
                    courseId
            );
        }

        // 3. Get current course roster
        List<Student> students =
                enrolledStudents.computeIfAbsent(
                        courseId,
                        key -> new ArrayList<>()
                );

        // 4. Capacity check
        if (students.size() >= course.getMaxSeats()) {
            throw new CourseFullException(courseId);
        }

        /*
         * 5. Create enrollment snapshot.
         */
        Enrollment enrollment =
                new Enrollment(
                        enrollmentId++,
                        studentId,
                        courseId,
                        EnrollmentStatus.PROGRESSING,
                        new Grade(enrollmentId+299,courseId, 100),
                        LocalDateTime.now(),
                        null
                );

        /*
         * 6. Update both structures.
         */
        students.add(student);

        studentEnrollments
                .computeIfAbsent(
                        studentId,
                        key -> new ArrayList<>()
                )
                .add(enrollment);
    }

    public void removeCourse(int courseId) {

        courseService.getCourse(courseId);

        if (hasActiveEnrollments(courseId)) {
            throw new ActiveEnrollmentException(courseId);
        }

        courseService.remove(courseId);
    }

    public void dropCourse(
            int studentId,
            int courseId
    ) {

        // Verify both entities exist.
        studentService.getStudent(studentId);
        courseService.getCourse(courseId);

        Enrollment currentEnrollment =
                getCurrentEnrollment(
                        studentId,
                        courseId
                );

        if (currentEnrollment == null ||
                currentEnrollment.getStatus()
                        != EnrollmentStatus.PROGRESSING) {

            throw new EnrollmentNotFoundException(
                    studentId,
                    courseId
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        /*
         * Create a NEW immutable snapshot.
         */
        Enrollment droppedEnrollment =
                new Enrollment(
                        enrollmentId++,
                        studentId,
                        courseId,
                        EnrollmentStatus.DROPPED,
                        currentEnrollment.getGrade(),
                        currentEnrollment.getEnrolledAt(),
                        now
                );

        /*
         * Append the new state to history.
         */
        studentEnrollments
                .get(studentId)
                .add(droppedEnrollment);

        /*
         * Remove from current roster.
         */
        List<Student> students =
                enrolledStudents.get(courseId);

        if (students != null) {
            students.removeIf(
                    student ->
                            student.getId() == studentId
            );
        }
    }

    public void completeCourse(
            int studentId,
            int courseId,
            int totalMarksObtained
    ) {

        // Verify both entities exist.
        Student student = studentService.getStudent(studentId);
        Course course = courseService.getCourse(courseId);

        Enrollment currentEnrollment =
                getCurrentEnrollment(
                        studentId,
                        courseId
                );

        if (currentEnrollment == null ||
                currentEnrollment.getStatus()
                        != EnrollmentStatus.PROGRESSING) {

            throw new EnrollmentNotFoundException(
                    studentId,
                    courseId
            );
        }

        Enrollment enrollment = getCurrentEnrollment(studentId, courseId);

        if(enrollment == null){
            throw new EnrollmentNotFoundException(studentId, courseId);
        }

        Grade grade = enrollment.getGrade();
        /*
         * The grade must belong to the same course.
         */
        if (grade == null || grade.getCourseId() != courseId) {
            throw new IllegalArgumentException(
                    "Grade does not belong to course " +
                            courseId
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        /*
         * Create COMPLETED snapshot
         * with the actual grade.
         */
        Enrollment completedEnrollment =
                new Enrollment(
                        enrollmentId++,
                        studentId,
                        courseId,
                        EnrollmentStatus.COMPLETED,
                        grade,
                        currentEnrollment.getEnrolledAt(),
                        now
                );

        /*
         * Append completed state to history.
         */
        studentEnrollments
                .get(studentId)
                .add(completedEnrollment);

        /*
         * Student is no longer in the active roster.
         */
        List<Student> students =
                enrolledStudents.get(courseId);

        if (students != null) {
            students.removeIf(
                    s ->
                            s.getId() == studentId
            );
        }
    }

    public List<Student> getEnrolledStudents(
            int courseId
    ) {

        courseService.getCourse(courseId);

        return new ArrayList<>(
                enrolledStudents.getOrDefault(
                        courseId,
                        new ArrayList<>()
                )
        );
    }

    public List<Enrollment> getStudentEnrollments(
            int studentId,
            EnrollmentStatus filter
    ) {

        studentService.getStudent(studentId);

        List<Enrollment> history =
                studentEnrollments.getOrDefault(
                        studentId,
                        new ArrayList<>()
                );

        if (filter == null) {
            return new ArrayList<>(history);
        }

        List<Enrollment> result =
                new ArrayList<>();

        for (Enrollment enrollment : history) {

            if (enrollment.getStatus() == filter) {
                result.add(enrollment);
            }
        }

        return result;
    }

    /*
     * Standing of a course means the current
     * active students in that course.
     *
     * Completed/dropped students are no longer
     * part of the current standing.
     */
    public List<Enrollment> getStanding(
            int courseId,
            EnrollmentStatus status
    ) {

        courseService.getCourse(courseId);

        List<Enrollment> standing = new ArrayList<>();

        for (List<Enrollment> history : studentEnrollments.values()) {

            for (int i = history.size() - 1; i >= 0; i--) {

                Enrollment enrollment = history.get(i);

                if (enrollment.getCourseId() != courseId) {
                    continue;
                }

                if (enrollment.getStatus() == status) {
                    standing.add(enrollment);
                }

                /*
                 * We found the latest state for this student
                 * in this course, so stop looking at older
                 * snapshots for this student.
                 */
                break;
            }
        }

        return standing;
    }

    public List<Enrollment> getStudentCourseStanding(
            int studentId,
            int courseId,
            EnrollmentStatus status
    ) {

        studentService.getStudent(studentId);
        courseService.getCourse(courseId);

        List<Enrollment> history =
                studentEnrollments.getOrDefault(
                        studentId,
                        new ArrayList<>()
                );

        List<Enrollment> standing =
                new ArrayList<>();

        for (int i = history.size() - 1; i >= 0; i--) {

            Enrollment enrollment =
                    history.get(i);

            if (enrollment.getCourseId() != courseId) {
                continue;
            }

            if (enrollment.getStatus() == status) {
                standing.add(enrollment);
            }

            /*
             * Since history is append-only, this is the
             * latest state for this student + course.
             */
            break;
        }

        return standing;
    }

    /*
     * Student standing means the student's
     * current active enrollments.
     */
    public List<Enrollment> getStudentStanding(
            int studentId,
            EnrollmentStatus status
    ) {

        studentService.getStudent(studentId);

        List<Enrollment> history =
                studentEnrollments.getOrDefault(
                        studentId,
                        new ArrayList<>()
                );

        List<Enrollment> standing =
                new ArrayList<>();

        /*
         * We only want the latest state for each course.
         *
         * Since the history is append-only, walk
         * backwards and take the first occurrence
         * of every course.
         */
        Map<Integer, Boolean> processedCourses =
                new HashMap<>();

        for (int i = history.size() - 1; i >= 0; i--) {

            Enrollment enrollment =
                    history.get(i);

            int courseId =
                    enrollment.getCourseId();

            if (processedCourses.containsKey(courseId)) {
                continue;
            }

            processedCourses.put(
                    courseId,
                    true
            );

            if (enrollment.getStatus()
                    == status) {

                standing.add(enrollment);
            }
        }

        return standing;
    }

    /*
     * Used by CourseService before removing a course.
     */
    public boolean hasActiveEnrollments(
            int courseId
    ) {

        List<Student> students =
                enrolledStudents.get(courseId);

        return students != null &&
                !students.isEmpty();
    }

    /*
     * IMPORTANT:
     *
     * This checks the COMPLETE enrollment history.
     *
     * Therefore:
     *
     * PROGRESSING -> cannot enroll again
     * DROPPED     -> cannot enroll again
     * COMPLETED   -> cannot enroll again
     */
    private boolean hasEnrollmentHistory(
            int studentId,
            int courseId
    ) {

        List<Enrollment> history =
                studentEnrollments.get(studentId);

        if (history == null) {
            return false;
        }

        for (Enrollment enrollment : history) {

            if (enrollment.getCourseId() == courseId) {
                return true;
            }
        }

        return false;
    }

    /*
     * Latest enrollment snapshot for a particular
     * student/course pair.
     */
    private Enrollment getCurrentEnrollment(
            int studentId,
            int courseId
    ) {

        List<Enrollment> history =
                studentEnrollments.get(studentId);

        if (history == null ||
                history.isEmpty()) {

            return null;
        }

        for (int i = history.size() - 1; i >= 0; i--) {

            Enrollment enrollment =
                    history.get(i);

            if (enrollment.getCourseId() == courseId) {
                return enrollment;
            }
        }

        return null;
    }
}