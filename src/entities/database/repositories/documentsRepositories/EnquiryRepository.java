package entities.database.repositories.documentsRepositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import entities.database.repositories.*;
import entities.documents.*;
import entities.documents.repliableDocuments.*;

public class EnquiryRepository implements IRepository<Enquiry, String> {
    private final Map<String, Enquiry> enquiryMap = new ConcurrentHashMap<>();

    // Package-private constructor
    EnquiryRepository() {}

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


}