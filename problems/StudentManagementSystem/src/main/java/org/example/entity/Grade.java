package org.example.entity;

public class Grade {

    private final int id;
    private final int courseId;
    private int obtainedMarks;
    private final int totalMarks;

    public Grade(int id, int courseId, int totalMarks) {
        this.id = id;
        this.courseId = courseId;
        this.obtainedMarks = 0;
        this.totalMarks = totalMarks;
    }

    public void SetObtainerMark(int obtainedMarks){
        this.obtainedMarks = obtainedMarks;
    }


    public int getId() {
        return id;
    }

    public int getCourseId() {
        return courseId;
    }

    public int getObtainedMarks() {
        return obtainedMarks;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public double getPercentage() {
        if (totalMarks == 0) {
            return 0;
        }

        return ((double) obtainedMarks / totalMarks) * 100;
    }

    @Override
    public String toString() {
        return "Grade{" +
                "id=" + id +
                ", courseId=" + courseId +
                ", obtainedMarks=" + obtainedMarks +
                ", totalMarks=" + totalMarks +
                '}';
    }
}