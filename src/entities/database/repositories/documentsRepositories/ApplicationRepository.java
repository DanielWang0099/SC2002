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

import java.time.LocalDateTime;
import java.time.ZoneId;

import entities.project.*;

/**
 * This entity class is a repository for managing ProjectApplication entities.
 */
public class ApplicationRepository implements IRepository<ProjectApplication, String> {
    private final Map<String, ProjectApplication> applicationMap = new ConcurrentHashMap<>();
    private final String filename = "data/documents/applications.csv";


    // Package-private constructor
    public ApplicationRepository() {}

    public void loadFromFile() {
        // Note: Assumes Users and Projects Repositories are already loaded for lookups!
        List<ProjectApplication> loaded = CsvUtil.readCsv(filename, this::mapRowToApplication, true);
        loaded.forEach(this::save); // Populate map via save method
        System.out.println("Loaded " + applicationMap.size() + " applications from " + filename);
    }

    public void saveToFile() {
        // Added BookedFlatType column
        String[] header = {"DocumentID", "ApplicantNRIC", "ProjectName", "Status",
                           "SubmissionDate", "LastModifiedDate", "LastModifiedByNRIC",
                           "RejectionReason", "BookedFlatType"};
        CsvUtil.writeCsv(filename, findAll(), this::mapApplicationToRow, header);
    }

    private ProjectApplication mapRowToApplication(String[] row) {
        try {
            // Expect 9 columns now
            if (row.length < 9) throw new IllegalArgumentException("Application CSV: Incorrect number of columns. Expected 9+, got " + row.length);

            String docId = row[0];
            String applicantNric = row[1];
            String projectName = row[2];
            DocumentStatus status = DocumentStatus.valueOf(row[3].toUpperCase());
            // Use helper methods that return LocalDateTime or null
            LocalDateTime submissionDate = toLocalDateTime(parseDate(row[4]));
            LocalDateTime lastModDate = toLocalDateTime(parseDate(row[5]));
            String lastModByNric = row[6];
            String rejectionReason = row[7];
            FlatType bookedFlatType = row[8].isEmpty() ? null : FlatType.valueOf(row[8].toUpperCase());

            Optional<User> applicantOpt = Database.getUsersRepository().findUserByNric(applicantNric);
            if (applicantOpt.isEmpty()) {
                System.err.println("Skipping application row [" + docId + "]: Applicant NRIC '" + applicantNric + "' not found.");
                return null;
            }
            // We might only have the NRIC for lastModifiedBy, not the User object yet
            // Pass null for lastModifiedBy User object, but pass the NRIC string
            ProjectApplication app = new ProjectApplication(
                 docId,
                 applicantOpt.get(), // applicant User object
                 projectName,        // projectName String
                 status,
                 submissionDate,     // LocalDateTime or null
                 lastModDate,        // LocalDateTime or null
                 null,               // Pass null for lastModifiedBy User object initially
                 lastModByNric,      // Pass the NRIC string
                 rejectionReason,
                 bookedFlatType      // Pass loaded value
             );

            return app;

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
                formatDate(app.getSubmissionDate()),
                formatDate(app.getLastModifiedDate()),
                app.getLastModifiedByNric() != null ? app.getLastModifiedByNric() : "",
                app.getRejectionReason() != null ? app.getRejectionReason() : "",
                // Add booked flat type (handle null)
                app.getBookedFlatType() != null ? app.getBookedFlatType().name() : ""
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
        return applicationMap.values().stream()
                 .filter(app -> app.getProject() != null && app.getProject().getName().equals(projectId))
                 .collect(Collectors.toList());
    }

private String formatDate(LocalDateTime ldt) {
         if (ldt == null) return "";
         // Using ISO format which SimpleDateFormat can also parse/format if needed
         // Or use DateTimeFormatter
         return ldt.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME);
     }
     private Date parseDate(String dateString) throws ParseException {
         // Keep using SimpleDateFormat for consistency with other repos, parse ISO format
          if (dateString == null || dateString.isEmpty()) return null;
          // Adjust SimpleDateFormat to parse ISO format saved by formatDate
          SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
          // May need to handle timezone or variations if saved differently
          return isoFormat.parse(dateString);
     }
      private LocalDateTime toLocalDateTime(Date date) {
         if (date == null) return null;
         // Convert java.util.Date to LocalDateTime via Instant
         return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
     }

     public Optional<ProjectApplication> findBookedApplicationByApplicantNric(String applicantNric) {
        if (applicantNric == null || applicantNric.isBlank()) {
            return Optional.empty();
        }
        return applicationMap.values().stream()
                .filter(app -> app.getSubmitter() != null &&
                               app.getSubmitter().getNric().equalsIgnoreCase(applicantNric) &&
                               app.getStatus() == DocumentStatus.BOOKED)
                .findFirst();
    }
}