package entities.database.repositories.usersRepositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import entities.user.*;
import entities.database.repositories.IRepository;


/**
 * Repository specifically for managing Applicant entities.
 */
public class ApplicantRepository implements IRepository<Applicant, String> {

    private final Map<String, Applicant> applicantMap = new ConcurrentHashMap<>();

    // Package-private constructor, managed by UsersRepository facade
    public ApplicantRepository() { }

    @Override
    public Applicant save(Applicant applicant) {
        if (applicant == null || applicant.getNric() == null) {
            throw new IllegalArgumentException("Applicant and Applicant NRIC cannot be null.");
        }
        applicantMap.put(applicant.getNric().toUpperCase(), applicant);
        return applicant;
    }

    @Override
    public Optional<Applicant> findById(String nric) {
        if (nric == null) return Optional.empty();
        return Optional.ofNullable(applicantMap.get(nric.toUpperCase()));
    }

    @Override
    public List<Applicant> findAll() {
        return new ArrayList<>(applicantMap.values());
    }

    @Override
    public boolean deleteById(String nric) {
        if (nric == null) return false;
        return applicantMap.remove(nric.toUpperCase()) != null;
    }

    @Override
    public boolean delete(Applicant applicant) {
         if (applicant == null || applicant.getNric() == null) return false;
         return deleteById(applicant.getNric());
    }

     @Override
    public long count() {
        return applicantMap.size();
    }

    // --- Requirement Specific Methods (Stubs) ---

    /**
     * Finds applicants eligible for specific flat types based on age and marital status.
     * [cite: 10, 12]
     * @param flatType E.g., "2-Room", "3-Room" (Consider using an Enum)
     * @return List of eligible applicants.
     */
    public List<Applicant> findEligibleApplicants(String flatType) {
        System.out.println("ApplicantRepository: Finding eligible applicants for " + flatType + " (Stub)");
        // TODO: Implement logic based on age/marital status rules from PDF [cite: 12]
        // Example Rule: Singles >= 35 only for 2-Room [cite: 12]
        // Example Rule: Married >= 21 for any type [cite: 12]
        return findAll().stream()
                // .filter(applicant -> checkEligibility(applicant, flatType)) // Implement checkEligibility
                .collect(Collectors.toList()); // Placeholder: returns all for now
    }

     /**
     * Checks if an applicant has already submitted an application that is not in a final rejected/withdrawn state.
     * [cite: 11] - Cannot apply for multiple projects.
     * Needs access to Application data. This might be better placed in an ApplicationRepository
     * or a service layer that uses both repositories. Keeping a stub here for now.
     * @param applicantNric The NRIC of the applicant.
     * @return true if the applicant has an active/pending application, false otherwise.
     */
    public boolean hasActiveApplication(String applicantNric) {
         System.out.println("ApplicantRepository: Checking active application for " + applicantNric + " (Stub - Requires Application Data)");
         // TODO: Query Application Repository/Service for non-final applications by this applicantNric
         return false; // Placeholder
    }
}

//---