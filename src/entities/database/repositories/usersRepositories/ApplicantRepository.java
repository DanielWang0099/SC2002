package entities.database.repositories.usersRepositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import entities.user.*;
import entities.database.repositories.IRepository;
import utilities.*;
import entities.project.*;

/**
 * This entity class is a repository for managing Applicant entities.
 */
public class ApplicantRepository implements IRepository<Applicant, String> {

    private final Map<String, Applicant> applicantMap = new ConcurrentHashMap<>();
    private final String filename = "data/users/applicants.csv"; // Define filename

    // Package-private constructor, managed by UsersRepository facade
    public ApplicantRepository() {}

    // --- Load and Save Methods ---
    public void loadFromFile() {
        List<Applicant> loadedApplicants = CsvUtil.readCsv(filename, this::mapRowToApplicant, true); // skipHeader=true
        loadedApplicants.forEach(this::save); // Use save to populate map correctly
        System.out.println("Loaded " + applicantMap.size() + " applicants from " + filename);
    }

    public void saveToFile() {
        String[] header = {"Name", "NRIC", "Password", "Age", "MaritalStatus"};
        CsvUtil.writeCsv(filename, findAll(), this::mapApplicantToRow, header);
    }

    // --- Mappers for CSV ---
    private Applicant mapRowToApplicant(String[] row) {
        try {
            // Assuming format: Name,NRIC,Password,Age,MaritalStatus
            if (row.length < 5) throw new IllegalArgumentException("Incorrect number of columns");
            String name = row[0];
            String nric = row[1];
            String password = row[2];
            int age = Integer.parseInt(row[3]);
            MaritalStatus maritalStatus = MaritalStatus.valueOf(row[4].toUpperCase());
            // NRIC format validation (optional here if done elsewhere)
            if (!nric.matches("^[ST]\\d{7}[A-Z]$")) {
                System.err.println("Skipping applicant row with invalid NRIC format: " + nric);
                return null;
            }
            return new Applicant(name, nric, age, maritalStatus, password, Role.APPLICANT);
        } catch (Exception e) {
            System.err.println("Error mapping row to Applicant: " + String.join(",", row) + " | Error: " + e.getMessage());
            return null; // Skip invalid rows
        }
    }

    private String[] mapApplicantToRow(Applicant applicant) {
        return new String[]{
                applicant.getName(),
                applicant.getNric(),
                applicant.getPassword(), // Saving plain password as required
                String.valueOf(applicant.getAge()),
                applicant.getMaritalStatus().name() // Get enum name as string
        };
    }

    // --- IRepository Methods (save uses map directly) ---
    @Override
    public Applicant save(Applicant applicant) {
        if (applicant == null || applicant.getNric() == null) {
            throw new IllegalArgumentException("Applicant/NRIC cannot be null.");
        }
        applicantMap.put(applicant.getNric().toUpperCase(), applicant);
        // Note: saveToFile() is not called here for performance; called on shutdown/explicitly
        return applicant;
    }

    @Override
    public Optional<Applicant> findById(String nric) { /* unchanged */
        if (nric == null) return Optional.empty();
        return Optional.ofNullable(applicantMap.get(nric.toUpperCase()));
    }
     @Override
    public List<Applicant> findAll() { /* unchanged */
        return new ArrayList<>(applicantMap.values());
    }
    @Override
    public boolean deleteById(String nric) { /* unchanged */
        if (nric == null) return false;
        boolean removed = applicantMap.remove(nric.toUpperCase()) != null;
         // Note: saveToFile() is not called here
         return removed;
    }
    @Override
    public boolean delete(Applicant applicant) { /* unchanged */
         if (applicant == null || applicant.getNric() == null) return false;
         return deleteById(applicant.getNric());
    }
     @Override
    public long count() { /* unchanged */
        return applicantMap.size();
    }


    // --- Requirement Specific Methods (Implemented) ---

    /**
     * Finds applicants eligible for specific flat types based on age and marital status.
     * - Singles, 35 years old and above, can ONLY apply for 2-Room.
     * - Married, 21 years old and above, can apply for any flat types (2-Room or 3-Room).
     * @param flatType The flat type to check eligibility for.
     * @return List of eligible applicants.
     */
    public List<Applicant> findEligibleApplicants(FlatType flatType) {
        return findAll().stream()
                .filter(applicant -> checkEligibility(applicant, flatType))
                .collect(Collectors.toList());
    }

    private boolean checkEligibility(Applicant applicant, FlatType flatType) {
        int age = applicant.getAge();
        MaritalStatus status = applicant.getMaritalStatus();

        if (status == MaritalStatus.MARRIED) {
            // Married, 21 years old and above, can apply for any flat types
            return age >= 21; // Can apply for TWO_ROOM or THREE_ROOM if >= 21
        } else if (status == MaritalStatus.SINGLE) {
            // Singles, 35 years old and above, can ONLY apply for 2-Room
            if (age >= 35 && flatType == FlatType.TWO_ROOM) {
                return true;
            } else {
                return false; // Singles < 35 or applying for 3-Room are ineligible
            }
        } else {
            return false; // Should not happen if enum is used correctly
        }
    }

     /**
     * Checks if an applicant has already submitted an application that is not in a final rejected/withdrawn state.
     * Requires access to Application data - better in a Service layer.
     * @param applicantNric The NRIC of the applicant.
     * @return true if the applicant has an active/pending application, false otherwise.
     */
    public boolean hasActiveApplication(String applicantNric) {
         System.out.println("ApplicantRepository: Checking active application for " + applicantNric + " (Requires ApplicationRepository Access - Delegate to Service Layer)");
         // This logic *must* be moved to a service that can access ApplicationRepository
         // Example (if done here, tightly coupled):
         // Optional<ProjectApplication> activeApp = Database.getDocumentsRepository().getApplicationRepository().findActiveApplicationByApplicantNric(applicantNric);
         // return activeApp.isPresent();
         return false; // Placeholder - Implement in Service
    }
}