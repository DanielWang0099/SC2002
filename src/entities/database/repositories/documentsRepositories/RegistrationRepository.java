package entities.database.repositories.documentsRepositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import entities.documents.approvableDocuments.*;
import entities.database.repositories.*;

public class RegistrationRepository implements IRepository<ProjectRegistration, String> {
    private final Map<String, ProjectRegistration> registrationMap = new ConcurrentHashMap<>();

    // Package-private constructor
    public RegistrationRepository() {}

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

}