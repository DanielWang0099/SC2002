package entities.database.repositories.documentsRepositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import entities.documents.approvableDocuments.*;
import entities.database.repositories.*;
import entities.documents.*;

/**
 * Repository specifically for managing ProjectApplication entities.
 */
public class ApplicationRepository implements IRepository<ProjectApplication, String> {
    private final Map<String, ProjectApplication> applicationMap = new ConcurrentHashMap<>();

    // Package-private constructor
    public ApplicationRepository() {}

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

}