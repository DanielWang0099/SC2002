package entities.database.repositories.documentsRepositories;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import entities.database.Database;
import entities.database.repositories.*;
import entities.documents.*;
import entities.documents.repliableDocuments.*;
import entities.project.Project;
import entities.user.User;
import utilities.CsvUtil;

public class EnquiryRepository implements IRepository<Enquiry, String> {
    private final Map<String, Enquiry> enquiryMap = new ConcurrentHashMap<>();
    private final String filename = "src/data/documents/enquiries.csv";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static { DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC")); }

    // Package-private constructor
    public EnquiryRepository() {}

    public void loadFromFile() {
        List<Enquiry> loaded = CsvUtil.readCsv(filename, this::mapRowToEnquiry, true);
        loaded.forEach(e -> enquiryMap.putIfAbsent(e.getDocumentID(), e));
        System.out.println("Loaded " + enquiryMap.size() + " enquiries from " + filename);
    }

    public void saveToFile() {
        String[] header = {"DocumentID", "SubmitterNRIC", "ProjectName", "EnquiryContent",
                           "ReplyContent", "ReplierNRIC", "Status", "SubmissionDate",
                           "LastModifiedDate", "LastModifiedByNRIC", "ReplyDate"};
        CsvUtil.writeCsv(filename, findAll(), this::mapEnquiryToRow, header);
    }

    private Enquiry mapRowToEnquiry(String[] row) {
                try {
            if (row.length < 11)
                throw new IllegalArgumentException("Incorrect number of columns for enquiry");

            String docId = row[0];
            String submitterNric = row[1];
            String projectName = row[2];
            String enquiryContent = row[3];
            DocumentStatus status = DocumentStatus.valueOf(row[4].toUpperCase());
            Date submissionDate = parseDate(row[5]); // Use helper
            Date lastModDate = parseDate(row[6]);    // Use helper
            String lastModByNric = row[7];
            String replyContent = row[8];
            String replierNric = row[9];
            Date replyDate = parseDate(row[10]);

            // Lookup Submitter, Project, and Last Modifier
            Optional<User> submitterOpt = Database.getUsersRepository().findUserByNric(submitterNric);
            Optional<Project> projectOpt = Database.getProjectsRepository().findById(projectName);
            Optional<User> lastModByOpt = Database.getUsersRepository().findUserByNric(lastModByNric);
            // Replier lookup is optional when reply content is empty.
            Optional<User> replierOpt = (replierNric == null || replierNric.isEmpty())
                                          ? Optional.empty()
                                          : Database.getUsersRepository().findUserByNric(replierNric);

            if (!submitterOpt.isPresent() || !projectOpt.isPresent() || !lastModByOpt.isPresent()) {
                if (!submitterOpt.isPresent()) {
                    System.err.println("Submitter not found for NRIC: " + submitterNric);
                }
                if (!projectOpt.isPresent()) {
                    System.err.println("Project not found for project name: " + projectName);
                }
                if (!lastModByOpt.isPresent()) {
                    System.err.println("Last modifier not found for NRIC: " + lastModByNric);
                }
                return null;
            } else {
                User submitter = submitterOpt.get();
                Project project = projectOpt.get();
                User lastModBy = lastModByOpt.get();
                User replier = replierOpt.orElse(null);

                Enquiry enquiry = new Enquiry(
                    docId,
                    submitter,
                    project,
                    enquiryContent,
                    status,
                    toLocalDateTime(submissionDate),
                    toLocalDateTime(lastModDate),
                    lastModBy,
                    replyContent,
                    replier,
                    toLocalDateTime(replyDate)
                );
                return enquiry;
            }
        } catch (Exception e) {
            System.err.println("Error mapping row to Enquiry: " + String.join(",", row) +
                               " | Error: " + e.getMessage());
            return null;
        }
    }

    private String[] mapEnquiryToRow(Enquiry enquiry) {
        return new String[]{
            enquiry.getDocumentID(),
            enquiry.getSubmitter() != null ? enquiry.getSubmitter().getNric() : "",
            enquiry.getProjectName() != null ? enquiry.getProjectName() : "",
            enquiry.getEnquiryContent() != null ? enquiry.getEnquiryContent() : "",
            enquiry.getStatus() != null ? enquiry.getStatus().name() : "",
            enquiry.getSubmissionDate() != null ? formatDate(enquiry.getSubmissionDate()) : "",
            enquiry.getLastModifiedDate() != null ? formatDate(enquiry.getLastModifiedDate()) : "",
            enquiry.getLastModifiedBy() != null ? enquiry.getLastModifiedBy().getNric() : "",
            enquiry.getReplyContent() != null ? enquiry.getReplyContent() : "",
            enquiry.getReplier() != null ? enquiry.getReplier().getNric() : "",
            enquiry.getReplyDate() != null ? formatDate(enquiry.getReplyDate()) : ""
        };
    }

    @Override
    public Enquiry save(Enquiry enquiry) {
        if (enquiry == null || enquiry.getDocumentID() == null) {
            throw new IllegalArgumentException("Enquiry and Document ID cannot be null.");
        }
        enquiryMap.put(enquiry.getDocumentID(), enquiry);
        return enquiry;
    }

    @Override
    public Optional<Enquiry> findById(String documentId) {
        return Optional.ofNullable(enquiryMap.get(documentId));
    }

    @Override
    public List<Enquiry> findAll() {
        return new ArrayList<>(enquiryMap.values());
    }

     @Override
    public boolean deleteById(String documentId) {
        // Check if deletion is allowed based on status/role might happen in Controller/Service
        return enquiryMap.remove(documentId) != null;
    }

    @Override
    public boolean delete(Enquiry enquiry) {
        if (enquiry == null || enquiry.getDocumentID() == null) return false;
        // Deletion logic might depend on who is deleting and the enquiry status [cite: 17]
        // For repository, we just remove it if asked. Controller should verify permissions.
        return deleteById(enquiry.getDocumentID());
    }

    @Override
    public long count() {
        return enquiryMap.size();
    }

    // --- Requirement Specific Methods ---

    /**
     * Finds enquiries submitted by a specific user. [cite: 17]
     * @param submitterNric The NRIC of the user who submitted enquiries.
     * @return List of enquiries submitted by the user.
     */
    public List<Enquiry> findBySubmitterNric(String submitterNric) {
         return enquiryMap.values().stream()
                .filter(e -> e.getSubmitter() != null && e.getSubmitter().getNric().equalsIgnoreCase(submitterNric))
                .collect(Collectors.toList());
    }

     /**
     * Finds enquiries related to a specific project ID. [cite: 22, 33]
     * Assumes Enquiry has a reference to the Project.
     * @param projectId The unique ID (e.g., name) of the project.
     * @return List of enquiries for that project.
     */
    public List<Enquiry> findByProjectId(String projectId) {
         System.out.println("EnquiryRepository: Finding enquiries for project " + projectId + " (Stub - Requires Enquiry <-> Project Link)");
         // TODO: Implement filtering based on Enquiry having a getProject().getName() method
         // return enquiryMap.values().stream()
         //        .filter(e -> e.getProject() != null && e.getProject().getName().equals(projectId))
         //        .collect(Collectors.toList());
         return new ArrayList<>(); // Placeholder
    }

     /**
     * Finds enquiries that have not yet been replied to (status is SUBMITTED).
     * @return List of unreplied enquiries.
     */
    public List<Enquiry> findUnrepliedEnquiries() {
        return enquiryMap.values().stream()
                .filter(e -> e.getStatus() == DocumentStatus.SUBMITTED) // Assuming SUBMITTED means awaiting reply
                .collect(Collectors.toList());
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