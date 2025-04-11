package entity.document;

import enums.ApplicationStatus;
import java.time.LocalDateTime;

public class Application implements BaseDocument {
    private final String applicantNric;
    private final String projectName;
    private ApplicationStatus status;
    private final LocalDateTime applicationDate;

    public Application(String applicantNric, String projectName) {
        this.applicantNric = applicantNric;
        this.projectName = projectName;
        this.status = ApplicationStatus.PENDING;
        this.applicationDate = LocalDateTime.now();
    }

    // Implement Document interface
    @Override
    public String getType() { return "Application"; }

    // Getters
    public String getApplicantNric() { return applicantNric; }
    public String getProjectName() { return projectName; }
    public ApplicationStatus getStatus() { return status; }
    public LocalDateTime getApplicationDate() { return applicationDate; }

    // Setters
    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
}