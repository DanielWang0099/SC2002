package entities.database.repositories.documentsRepositories;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import entities.documents.DocumentStatus;
import entities.documents.approvableDocuments.*;
import entities.project.Project;
import entities.user.HdbOfficer;
import entities.user.User;
import utilities.CsvUtil;
import entities.database.Database;
import entities.database.repositories.*;

/**
 * This entity class is a repository for managing Registration entities.
 */
public class RegistrationRepository implements IRepository<ProjectRegistration, String> {
    private final Map<String, ProjectRegistration> registrationMap = new ConcurrentHashMap<>();
    private final String filename = "data/documents/registrations.csv";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static { DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC")); }
    // Package-private constructor

    public RegistrationRepository() {}

    public void loadFromFile() {
        List<ProjectRegistration> loaded = CsvUtil.readCsv(filename, this::mapRowToRegistration, true);
        loaded.forEach(reg -> registrationMap.putIfAbsent(reg.getDocumentID(), reg));
        System.out.println("Loaded " + registrationMap.size() + " registrations from " + filename);
    }

    public void saveToFile() {
        String[] header = {"DocumentID", "OfficerNRIC", "ProjectName", "Status",
                           "SubmissionDate", "LastModifiedDate", "LastModifiedByNRIC", "RejectionReason"};
        CsvUtil.writeCsv(filename, findAll(), this::mapRegistrationToRow, header);
    }

    private ProjectRegistration mapRowToRegistration(String[] row) {
        try {
            if (row.length < 8)
                throw new IllegalArgumentException("Registration CSV: Incorrect number of columns. Expected at least 8, got " + row.length);
    
            String documentID = row[0];
            String officerNric = row[1];
            String projectName = row[2];
            DocumentStatus status = DocumentStatus.valueOf(row[3].toUpperCase());
            Date submissionDate = parseDate(row[4]);    // Use helper to convert String to Date
            Date lastModDate = parseDate(row[5]);         // Use helper to convert String to Date
            String lastModByNric = row[6];
            String rejectionReason = row[7];
    
            // Lookup officer and ensure it is an HdbOfficer
            Optional<User> officerOpt = Database.getUsersRepository().findUserByNric(officerNric);
            if (officerOpt.isEmpty() || !(officerOpt.get() instanceof HdbOfficer)) {
                System.err.println("Skipping registration row [" + documentID + "]: Officer NRIC '" + officerNric + "' not found or not an Officer.");
                return null;
            }
            // Lookup Project
            Optional<Project> projectOpt = Database.getProjectsRepository().findById(projectName);
            if (projectOpt.isEmpty()) {
                System.err.println("Project not found for project name: " + projectName);
                return null;
            }
            // Lookup Last Modifier (essential)
            Optional<User> lastModByOpt = Database.getUsersRepository().findUserByNric(lastModByNric);
            if (lastModByOpt.isEmpty()) {
                System.err.println("Last modifier not found for NRIC: " + lastModByNric);
                return null;
            }
    
            // Extract objects from the Optionals
            User officer = officerOpt.get();
            Project project = projectOpt.get();
            User lastModBy = lastModByOpt.get();
    
            // Create a new registration instance (convert Dates to LocalDateTime using toLocalDateTime helper)
            ProjectRegistration reg = new ProjectRegistration(
                    documentID,
                    officer,
                    project,
                    status,
                    toLocalDateTime(submissionDate),
                    toLocalDateTime(lastModDate),
                    lastModBy,
                    rejectionReason
            );
            return reg;
        } catch (Exception e) {
            System.err.println("Error mapping row to ProjectRegistration: " + String.join(",", row) +
                               " | Error: " + e.getMessage());
            return null;
        }
    }

    private String[] mapRegistrationToRow(ProjectRegistration reg) {
        return new String[]{
                reg.getDocumentID(),
                reg.getSubmitter() != null ? reg.getSubmitter().getNric() : "",
                reg.getProjectName() != null ? reg.getProjectName() : "", // Requires getter
                reg.getStatus() != null ? reg.getStatus().name() : "",
                reg.getSubmissionDate() != null ? formatDate(reg.getSubmissionDate()) : "", // Requires getter
                reg.getLastModifiedDate() != null ? formatDate(reg.getLastModifiedDate()) : "", // Requires getter
                reg.getLastModifiedBy() != null ? reg.getLastModifiedBy().getNric() : "", // Requires getter/field
                reg.getRejectionReason() != null ? reg.getRejectionReason() : ""
                 // Requires getter/field
        };
    }

    @Override
    public ProjectRegistration save(ProjectRegistration registration) {
         if (registration == null || registration.getDocumentID() == null) {
            throw new IllegalArgumentException("Registration and Document ID cannot be null.");
        }
        registrationMap.put(registration.getDocumentID(), registration);
        return registration;
    }

    @Override
    public Optional<ProjectRegistration> findById(String documentId) {
        return Optional.ofNullable(registrationMap.get(documentId));
    }

    @Override
    public List<ProjectRegistration> findAll() {
        return new ArrayList<>(registrationMap.values());
    }

     @Override
    public boolean deleteById(String documentId) {
        return registrationMap.remove(documentId) != null;
    }

    @Override
    public boolean delete(ProjectRegistration registration) {
         if (registration == null || registration.getDocumentID() == null) return false;
         return deleteById(registration.getDocumentID());
    }

    @Override
    public long count() {
        return registrationMap.size();
    }

    // --- Requirement Specific Methods ---

    /**
     * Finds registrations submitted by a specific HDB officer.
     * @param officerNric NRIC of the officer.
     * @return List of registrations by the officer.
     */
    public List<ProjectRegistration> findByOfficerNric(String officerNric) {
        return registrationMap.values().stream()
                .filter(reg -> reg.getSubmitter() != null && reg.getSubmitter().getNric().equalsIgnoreCase(officerNric))
                .collect(Collectors.toList());
    }

     /**
     * Finds registrations related to a specific project ID.
     * Assumes ProjectRegistration has a reference to the Project.
     * @param projectId The unique ID (e.g., name) of the project.
     * @return List of registrations for that project.
     */
    public List<ProjectRegistration> findByProjectId(String projectId) {
        System.out.println("RegistrationRepository: Finding registrations for project " + projectId + " (Stub - Requires ProjectRegistration <-> Project Link)");
        // TODO: Implement filtering based on ProjectRegistration having a getProject().getName() method
        // return registrationMap.values().stream()
        //         .filter(reg -> reg.getProject() != null && reg.getProject().getName().equals(projectId))
        //         .collect(Collectors.toList());
        return new ArrayList<>(); // Placeholder
    }

    /**
     * Finds PENDING registrations for a specific project ID[cite: 29].
     * Assumes ProjectRegistration has a reference to the Project.
     * @param projectId The unique ID (e.g., name) of the project.
     * @return List of pending registrations for that project.
     */
    public List<ProjectRegistration> findPendingByProjectId(String projectId) {
         System.out.println("RegistrationRepository: Finding PENDING registrations for project " + projectId + " (Stub - Requires ProjectRegistration <-> Project Link)");
         // TODO: Implement filtering based on ProjectRegistration having a getProject().getName() method and status
         // return findByProjectId(projectId).stream()
         //        .filter(reg -> reg.getStatus() == DocumentStatus.PENDING_APPROVAL)
         //        .collect(Collectors.toList());
         return new ArrayList<>(); // Placeholder
    }

     /**
     * Finds APPROVED registrations for a specific project ID[cite: 29].
     * Assumes ProjectRegistration has a reference to the Project.
     * @param projectId The unique ID (e.g., name) of the project.
     * @return List of approved registrations for that project.
     */
    public List<ProjectRegistration> findApprovedByProjectId(String projectId) {
         System.out.println("RegistrationRepository: Finding APPROVED registrations for project " + projectId + " (Stub - Requires ProjectRegistration <-> Project Link)");
         // TODO: Implement filtering based on ProjectRegistration having a getProject().getName() method and status
         // return findByProjectId(projectId).stream()
         //        .filter(reg -> reg.getStatus() == DocumentStatus.APPROVED)
         //        .collect(Collectors.toList());
         return new ArrayList<>(); // Placeholder
    }

    /**
     * Finds an officer's APPROVED registration for any project overlapping a given period.
     * Used for eligibility check[cite: 18]. Requires Project data access. Likely better in a service layer.
     * @param officerNric NRIC of the officer.
     * @param startDate Start date of the period to check.
     * @param endDate End date of the period to check.
     * @return Optional containing the conflicting approved registration if found.
     */
    public Optional<ProjectRegistration> findApprovedRegistrationInPeriod(String officerNric, java.util.Date startDate, java.util.Date endDate) {
         System.out.println("RegistrationRepository: Finding APPROVED registration for officer " + officerNric + " in period (Stub - Requires Project Data Access)");
        // TODO:
        // 1. Find all APPROVED registrations for the officerNric.
        // 2. For each registration, get its associated Project.
        // 3. Get the Project's application period (open/close dates).
        // 4. Check if the project's period overlaps with the given startDate/endDate.
        // 5. Return the first one found.
        return Optional.empty(); // Placeholder
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