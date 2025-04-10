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
import entities.user.User;
import utilities.CsvUtil;
import entities.database.Database;
import entities.database.repositories.*;

public class WithdrawalRepository implements IRepository<Withdrawal, String> {
    private final Map<String, Withdrawal> withdrawalMap = new ConcurrentHashMap<>();
    private final String filename = "data/withdrawals.csv";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
     static { DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC")); }

     // Package-private constructor
    public WithdrawalRepository() {
        loadFromFile();
    }

    private void loadFromFile() {
        // Important: Load AFTER Applications are loaded
        List<Withdrawal> loaded = CsvUtil.readCsv(filename, this::mapRowToWithdrawal, true);
        loaded.forEach(w -> withdrawalMap.putIfAbsent(w.getDocumentID(), w));
        System.out.println("Loaded " + withdrawalMap.size() + " withdrawals from " + filename);
    }

    public void saveToFile() {
        String[] header = {"DocumentID", "ApplicantNRIC", "OriginalApplicationID", "Status",
                           "SubmissionDate", "LastModifiedDate", "LastModifiedByNRIC", "RejectionReason"};
        CsvUtil.writeCsv(filename, findAll(), this::mapWithdrawalToRow, header);
    }

    private Withdrawal mapRowToWithdrawal(String[] row) {
        try {
        if (row.length < 8)
            throw new IllegalArgumentException("Withdrawal CSV: Incorrect number of columns. Expected at least 8, got " + row.length);

        String docId = row[0];
        String applicantNric = row[1];
        String originalAppId = row[2];
        DocumentStatus status = DocumentStatus.valueOf(row[3].toUpperCase());
        Date submissionDate = parseDate(row[4]); // Convert String to Date using helper
        Date lastModDate = parseDate(row[5]);      // Convert String to Date using helper
        String lastModByNric = row[6];
        String rejectionReason = row[7];

        // Lookup applicant
        Optional<User> applicantOpt = Database.getUsersRepository().findUserByNric(applicantNric);
        if (applicantOpt.isEmpty()) {
            System.err.println("Skipping withdrawal row [" + docId + "]: Applicant NRIC '" + applicantNric + "' not found.");
            return null;
        }

        // Lookup original application - CRITICAL DEPENDENCY
        Optional<ProjectApplication> origAppOpt = Database.getDocumentsRepository().getApplicationRepository().findById(originalAppId);
        if (origAppOpt.isEmpty()) {
            System.err.println("Skipping withdrawal row [" + docId + "]: Original Application ID '" + originalAppId + "' not found.");
            return null;
        }

        // Lookup Last Modifier (essential)
        Optional<User> lastModByOpt = Database.getUsersRepository().findUserByNric(lastModByNric);
        if (lastModByOpt.isEmpty()) {
            System.err.println("Skipping withdrawal row [" + docId + "]: Last modifier NRIC '" + lastModByNric + "' not found.");
            return null;
        }

        // Extract objects from the Optionals
        User applicant = applicantOpt.get();
        ProjectApplication origApp = origAppOpt.get();
        User lastModBy = lastModByOpt.get();

        // Lookup the Project associated with the original application.
        // This is needed for the Withdrawal constructor.
        Optional<Project> projectOpt = Database.getProjectsRepository().findById(origApp.getProjectName());
        if (projectOpt.isEmpty()) {
            System.err.println("Project not found for project name: " + origApp.getProjectName());
            return null;
        }
        Project project = projectOpt.get();

        // Create a new Withdrawal instance (convert Dates to LocalDateTime using toLocalDateTime helper)
        Withdrawal withdrawal = new Withdrawal(
                docId,
                applicant,
                origApp,
                project,
                status,
                toLocalDateTime(submissionDate),
                toLocalDateTime(lastModDate),
                lastModBy,
                rejectionReason
        );
        return withdrawal;
    } catch (Exception e) {
        System.err.println("Error mapping row to Withdrawal: " + String.join(",", row) +
                           " | Error: " + e.getMessage());
        return null;
    }
    }

    private String[] mapWithdrawalToRow(Withdrawal w) {
        return new String[]{
                w.getDocumentID(),
                w.getSubmitter() != null ? w.getSubmitter().getNric() : "",
                w.getApplicationToWithdraw() != null ? w.getApplicationToWithdraw().getDocumentID() : "", // Requires getter
                w.getStatus() != null ? w.getStatus().name() : "",
                w.getSubmissionDate() != null ? formatDate(w.getSubmissionDate()) : "", // Requires getter
                w.getLastModifiedDate() != null ? formatDate(w.getLastModifiedDate()) : "", // Requires getter
                w.getLastModifiedBy() != null ? w.getLastModifiedBy().getNric() : "", // Requires getter/field
                w.getRejectionReason() != null ? w.getRejectionReason() : "" // Requires getter/field
        };
    }

    @Override
    public Withdrawal save(Withdrawal withdrawal) {
         if (withdrawal == null || withdrawal.getDocumentID() == null) {
            throw new IllegalArgumentException("Withdrawal and Document ID cannot be null.");
        }
        withdrawalMap.put(withdrawal.getDocumentID(), withdrawal);
        return withdrawal;
    }

    @Override
    public Optional<Withdrawal> findById(String documentId) {
        return Optional.ofNullable(withdrawalMap.get(documentId));
    }

    @Override
    public List<Withdrawal> findAll() {
        return new ArrayList<>(withdrawalMap.values());
    }

     @Override
    public boolean deleteById(String documentId) {
        return withdrawalMap.remove(documentId) != null;
    }

    @Override
    public boolean delete(Withdrawal withdrawal) {
        if (withdrawal == null || withdrawal.getDocumentID() == null) return false;
        return deleteById(withdrawal.getDocumentID());
    }

    @Override
    public long count() {
        return withdrawalMap.size();
    }

     // --- Requirement Specific Methods ---

     /**
      * Find withdrawal requests submitted by a specific applicant.
      * @param applicantNric The NRIC of the applicant.
      * @return List of withdrawal requests by the applicant.
      */
    public List<Withdrawal> findByApplicantNric(String applicantNric) {
         return withdrawalMap.values().stream()
                .filter(w -> w.getSubmitter() != null && w.getSubmitter().getNric().equalsIgnoreCase(applicantNric))
                .collect(Collectors.toList());
    }

    /**
     * Find a withdrawal request associated with a specific ProjectApplication ID.
     * @param originalApplicationId The Document ID of the ProjectApplication being withdrawn.
     * @return Optional containing the Withdrawal request if found.
     */
    public Optional<Withdrawal> findByApplicationId(String originalApplicationId) {
         return withdrawalMap.values().stream()
                .filter(w -> w.getApplicationToWithdraw() != null && w.getApplicationToWithdraw().getDocumentID().equals(originalApplicationId))
                .findFirst(); // Assuming only one withdrawal request per application
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