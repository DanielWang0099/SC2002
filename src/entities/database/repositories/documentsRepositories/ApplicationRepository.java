package entities.database.repositories.documentsRepositories;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Date;
import entities.documents.approvableDocuments.*;
import entities.user.User;
import utilities.CsvUtil;
import entities.database.Database;
import entities.database.repositories.*;
import entities.documents.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import entities.project.*;

/**
 * Repository specifically for managing ProjectApplication entities.
 */
public class ApplicationRepository implements IRepository<ProjectApplication, String> {
    private final Map<String, ProjectApplication> applicationMap = new ConcurrentHashMap<>();
    private final String filename = "data/applications.csv";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    // Package-private constructor
    public ApplicationRepository() {
        loadFromFile();
    }

    private void loadFromFile() {
        // Note: Assumes Users and Projects Repositories are already loaded for lookups!
        List<ProjectApplication> loaded = CsvUtil.readCsv(filename, this::mapRowToApplication, true);
        loaded.forEach(this::save); // Populate map via save method
        System.out.println("Loaded " + applicationMap.size() + " applications from " + filename);
    }

    public void saveToFile() {
        String[] header = {"DocumentID", "ApplicantNRIC", "ProjectName", "Status",
                           "SubmissionDate", "LastModifiedDate", "LastModifiedByNRIC", "RejectionReason"};
        CsvUtil.writeCsv(filename, findAll(), this::mapApplicationToRow, header);
    }

    private ProjectApplication mapRowToApplication(String[] row) {
        try {
            if (row.length < 8) throw new IllegalArgumentException("Incorrect number of columns for application");

            String docId = row[0];
            String applicantNric = row[1];
            String projectName = row[2];
            DocumentStatus status = DocumentStatus.valueOf(row[3].toUpperCase());
            Date submissionDate = parseDate(row[4]); // Use helper
            Date lastModDate = parseDate(row[5]);    // Use helper
            String lastModByNric = row[6];
            String rejectionReason = row[7];

            // Lookup Applicant (essential)
            Optional<User> applicantOpt = Database.getUsersRepository().findUserByNric(applicantNric);
            Optional<Project> projectOpt = Database.getProjectsRepository().findById(projectName);
            Optional<User> lastModByOpt = Database.getUsersRepository().findUserByNric(lastModByNric);

            if (!applicantOpt.isPresent() || !projectOpt.isPresent() || !lastModByOpt.isPresent()) {
                if (!applicantOpt.isPresent()) {
                    System.err.println("Applicant not found for NRIC: " + applicantNric);
                }
                if (!projectOpt.isPresent()) {
                    System.err.println("Project not found for project name: " + projectName);
                }
                if (!lastModByOpt.isPresent()) {
                    System.err.println("Last modifier not found for NRIC: " + lastModByNric);
                }
                return null;
            } else {
                User applicant = applicantOpt.get();
                Project project = projectOpt.get();
                User lastModBy = lastModByOpt.get();
                
                ProjectApplication app = new ProjectApplication(docId, applicant, project, status, 
                                              toLocalDateTime(submissionDate), toLocalDateTime(lastModDate), 
                                              lastModBy, rejectionReason);
                return app;
            }

        } catch (Exception e) {
            System.err.println("Error mapping row to ProjectApplication: " + String.join(",", row) + " | Error: " + e.getMessage());
            return null;
        }
    }

     private String[] mapApplicationToRow(ProjectApplication app) {
        return new String[]{
                app.getDocumentID(),
                app.getSubmitter() != null ? app.getSubmitter().getNric() : "",
                app.getProjectName() != null ? app.getProjectName() : "",
                app.getStatus() != null ? app.getStatus().name() : "",
                app.getSubmissionDate() != null ? formatDate(app.getSubmissionDate()) : "", // Add getter if missing
                app.getLastModifiedDate() != null ? formatDate(app.getLastModifiedDate()) : "", // Add getter if missing
                app.getLastModifiedBy() != null ? app.getLastModifiedBy().getNric() : "", // Add getter/field if missing
                app.getRejectionReason() != null ? app.getRejectionReason() : ""
        };
    }

    @Override
    public ProjectApplication save(ProjectApplication application) {
        if (application == null || application.getDocumentID() == null) {
            throw new IllegalArgumentException("Application and Document ID cannot be null.");
        }
        applicationMap.put(application.getDocumentID(), application);
        return application;
    }

    @Override
    public Optional<ProjectApplication> findById(String documentId) {
        return Optional.ofNullable(applicationMap.get(documentId));
    }

    @Override
    public List<ProjectApplication> findAll() {
        return new ArrayList<>(applicationMap.values());
    }

    @Override
    public boolean deleteById(String documentId) {
        return applicationMap.remove(documentId) != null;
    }

    @Override
    public boolean delete(ProjectApplication application) {
        if (application == null || application.getDocumentID() == null) return false;
        return deleteById(application.getDocumentID());
    }

     @Override
    public long count() {
        return applicationMap.size();
    }

    // --- Requirement Specific Methods ---

    /**
     * Finds all applications submitted by a specific applicant.
     * @param applicantNric The NRIC of the applicant.
     * @return List of applications submitted by the user.
     */
    public List<ProjectApplication> findByApplicantNric(String applicantNric) {
        return applicationMap.values().stream()
                .filter(app -> app.getSubmitter() != null && app.getSubmitter().getNric().equalsIgnoreCase(applicantNric))
                .collect(Collectors.toList());
    }

    /**
     * Finds an applicant's application that is NOT in a final state (Rejected, Withdrawn, potentially Booked depending on rules).
     * Used to check the "cannot apply for multiple projects" rule[cite: 11].
     * @param applicantNric The NRIC of the applicant.
     * @return Optional containing the active/pending application if found, empty otherwise.
     */
    public Optional<ProjectApplication> findActiveApplicationByApplicantNric(String applicantNric) {
        return applicationMap.values().stream()
                .filter(app -> app.getSubmitter() != null && app.getSubmitter().getNric().equalsIgnoreCase(applicantNric))
                .filter(app -> {
                    DocumentStatus status = app.getStatus();
                    // Define which statuses count as "active" or "blocking a new application"
                    return status == DocumentStatus.DRAFT ||
                           status == DocumentStatus.PENDING_APPROVAL ||
                           status == DocumentStatus.APPROVED || // Approved but not booked might block
                           status == DocumentStatus.SUBMITTED; // If using SUBMITTED status
                           // Potentially status == DocumentStatus.BOOKED depending on interpretation
                })
                .findFirst();
    }

    /**
     * Finds applications related to a specific project ID.
     * Assumes ProjectApplication has a reference to the Project.
     * @param projectId The unique ID (e.g., name) of the project.
     * @return List of applications for that project.
     */
    public List<ProjectApplication> findByProjectId(String projectId) {
        System.out.println("ApplicationRepository: Finding applications for project " + projectId + " (Stub - Requires ProjectApplication <-> Project Link)");
        // TODO: Implement filtering based on ProjectApplication having a getProject().getName() method
        // return applicationMap.values().stream()
        //         .filter(app -> app.getProject() != null && app.getProject().getName().equals(projectId))
        //         .collect(Collectors.toList());
        return new ArrayList<>(); // Placeholder
    }

    private String formatDate(LocalDateTime ldt) {
        if (ldt == null) return "";
        Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant(); // Or ZoneId.of("UTC")
        return DATE_FORMAT.format(Date.from(instant));
    }

    private Date parseDate(String dateString) throws ParseException {
        if (dateString == null || dateString.isEmpty()) return null;
        return DATE_FORMAT.parse(dateString);
    }

     private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(); // Or ZoneId.of("UTC")
    }
}