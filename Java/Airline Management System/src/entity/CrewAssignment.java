package entity;

import java.time.LocalDateTime;

public class CrewAssignment {
    private Route route;
    private LocalDateTime reportingTime;
    private LocalDateTime dutyStart;
    private LocalDateTime dutyEnd;

    public CrewAssignment(Route route, LocalDateTime reportingTime, LocalDateTime dutyStart, LocalDateTime dutyEnd) {
        this.route = route;
        this.reportingTime = reportingTime;
        this.dutyStart = dutyStart;
        this.dutyEnd = dutyEnd;
    }

    public Route getRoute() {
        return route;
    }

    public LocalDateTime getReportingTime() {
        return reportingTime;
    }

    public LocalDateTime getDutyStart() {
        return dutyStart;
    }

    public LocalDateTime getDutyEnd() {
        return dutyEnd;
    }

    @Override
    public String toString() {
        return route.getSource().getCity() + " -> " + route.getDestination().getCity()
                + " | Reporting: " + reportingTime
                + " | Duty: " + dutyStart + " to " + dutyEnd;
    }
}
