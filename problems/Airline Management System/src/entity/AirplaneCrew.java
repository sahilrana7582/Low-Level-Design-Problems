package entity;

import enums.PassengerType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AirplaneCrew extends Passenger{
    private String id;
    private List<CrewAssignment> crewAssignmentList;

    public AirplaneCrew(String id, String firstName, String lastName, String email, String phNumber,
                         Identity identityProof, List<CrewAssignment> crewAssignmentList) {
        super(firstName, lastName, email, phNumber, identityProof, PassengerType.CREW);
        this.id = id;
        this.crewAssignmentList = crewAssignmentList != null ? crewAssignmentList : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public List<CrewAssignment> getCrewAssignmentList() {
        return crewAssignmentList;
    }

    public void addCrewAssignment(CrewAssignment crewAssignment) {
        this.crewAssignmentList.add(crewAssignment);
        System.out.println("[CREW] Assignment added for " + getFirstName() + " " + getLastName()
                + " (ID: " + id + ") -> " + crewAssignment);
    }

    @Override
    public String getPassengerInfo() {
        return "Crew[id=" + id + ", name=" + getFirstName() + " " + getLastName()
                + ", email=" + getEmail() + ", phone=" + getPhNumber()
                + ", identity=" + getIdentityProof().getIdentityNumber()
                + ", type=" + getPassengerType()
                + ", totalAssignments=" + crewAssignmentList.size() + "]";
    }

    public CrewAssignment getUpcomingAssignments(){
        LocalDateTime now = LocalDateTime.now();
        CrewAssignment upcoming = null;
        for (CrewAssignment assignment : crewAssignmentList) {
            if (assignment.getDutyStart().isAfter(now)) {
                if (upcoming == null || assignment.getDutyStart().isBefore(upcoming.getDutyStart())) {
                    upcoming = assignment;
                }
            }
        }
        if (upcoming == null) {
            System.out.println("[CREW] No upcoming assignments found for " + getFirstName() + " " + getLastName());
        } else {
            System.out.println("[CREW] Next upcoming assignment for " + getFirstName() + " " + getLastName()
                    + " -> " + upcoming);
        }
        return upcoming;
    }
}
