package entity;

import enums.PassengerType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AirplanePilot extends Passenger{
    private String id;
    private int planeExperience;
    private List<CrewAssignment> crewAssignmentList;

    public AirplanePilot(String id, String firstName, String lastName, String email, String phNumber,
                          Identity identityProof, int planeExperience, List<CrewAssignment> crewAssignmentList) {
        super(firstName, lastName, email, phNumber, identityProof, PassengerType.PILOT);
        this.id = id;
        this.planeExperience = planeExperience;
        this.crewAssignmentList = crewAssignmentList != null ? crewAssignmentList : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public int getPlaneExperience() {
        return planeExperience;
    }

    public List<CrewAssignment> getCrewAssignmentList() {
        return crewAssignmentList;
    }

    public void addCrewAssignment(CrewAssignment crewAssignment) {
        this.crewAssignmentList.add(crewAssignment);
        System.out.println("[PILOT] Assignment added for " + getFirstName() + " " + getLastName()
                + " (ID: " + id + ") -> " + crewAssignment);
    }

    @Override
    public String getPassengerInfo() {
        return "Pilot[id=" + id + ", name=" + getFirstName() + " " + getLastName()
                + ", email=" + getEmail() + ", phone=" + getPhNumber()
                + ", identity=" + getIdentityProof().getIdentityNumber()
                + ", experience=" + planeExperience + " yrs"
                + ", type=" + getPassengerType()
                + ", totalAssignments=" + crewAssignmentList.size() + "]";
    }

    public CrewAssignment getUpcomingFlights(){
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
            System.out.println("[PILOT] No upcoming flights found for " + getFirstName() + " " + getLastName());
        } else {
            System.out.println("[PILOT] Next upcoming flight for " + getFirstName() + " " + getLastName()
                    + " -> " + upcoming);
        }
        return upcoming;
    }
}
