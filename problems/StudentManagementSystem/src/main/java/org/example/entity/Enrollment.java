package org.example.entity;

import java.time.LocalDateTime;

public class Enrollment {

    private final int id;
    private final int studentId;
    private final int courseId;
    private final EnrollmentStatus status;
    private final Grade grade;
    private final LocalDateTime enrolledAt;
    private final LocalDateTime completedAt;

    public Enrollment(
            int id,
            int studentId,
            int courseId,
            EnrollmentStatus status,
            Grade grade,
            LocalDateTime enrolledAt,
            LocalDateTime completedAt
    ) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.status = status;
        this.grade = grade;
        this.enrolledAt = enrolledAt;
        this.completedAt = completedAt;
    }

    public int getId() {
        return id;
    }

    public int getStudentId() {
        return studentId;
    }

    public int getCourseId() {
        return courseId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public Grade getGrade() {
        return grade;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    @Override
    public String toString() {
        return "Enrollment{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", courseId=" + courseId +
                ", status=" + status +
                ", grade=" + grade +
                ", enrolledAt=" + enrolledAt +
                ", completedAt=" + completedAt +
                '}';
    }
}