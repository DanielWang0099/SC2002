package entities.database.repositories.usersRepositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import entities.user.*;
import entities.database.repositories.IRepository;
import utilities.*;

/**
 * This entity class is a repository for managing HdbOfficer entities.
 */
public class HdbOfficerRepository implements IRepository<HdbOfficer, String> {

    private final Map<String, HdbOfficer> officerMap = new ConcurrentHashMap<>();
    private final String filename = "data/users/hdb_officers.csv"; // Define filename

    // Package-private constructor, managed by UsersRepository facade
    public HdbOfficerRepository() {}

    // --- Load and Save Methods ---
    public void loadFromFile() {
        List<HdbOfficer> loadedOfficers = CsvUtil.readCsv(filename, this::mapRowToOfficer, true); // skipHeader=true
        loadedOfficers.forEach(this::save); // Use save to populate map correctly
        System.out.println("Loaded " + officerMap.size() + " HDB officers from " + filename);
    }

    public void saveToFile() {
        // Assuming format: Name,NRIC,Password,Age,MaritalStatus (Role is implicit)
        String[] header = {"Name", "NRIC", "Password", "Age", "MaritalStatus"};
        CsvUtil.writeCsv(filename, findAll(), this::mapOfficerToRow, header);
    }

    // --- Mappers for CSV ---
    private HdbOfficer mapRowToOfficer(String[] row) {
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
                System.err.println("Skipping officer row with invalid NRIC format: " + nric);
                return null;
            }
            // Use the correct HdbOfficer constructor
            return new HdbOfficer(name, nric, age, maritalStatus, password, Role.HDB_OFFICER);
        } catch (Exception e) {
            System.err.println("Error mapping row to HdbOfficer: " + String.join(",", row) + " | Error: " + e.getMessage());
            return null; // Skip invalid rows
        }
    }

    private String[] mapOfficerToRow(HdbOfficer officer) {
        return new String[]{
                officer.getName(),
                officer.getNric(),
                officer.getPassword(), // Saving plain password as required
                String.valueOf(officer.getAge()),
                officer.getMaritalStatus().name() // Get enum name as string
        };
    }

    // --- IRepository Methods (save uses map directly) ---
    @Override
    public HdbOfficer save(HdbOfficer officer) {
        if (officer == null || officer.getNric() == null) {
            throw new IllegalArgumentException("Officer/NRIC cannot be null.");
        }
        officerMap.put(officer.getNric().toUpperCase(), officer);
        // Note: saveToFile() is not called here for performance; called on shutdown/explicitly
        return officer;
    }

    @Override
    public Optional<HdbOfficer> findById(String nric) {
        if (nric == null) return Optional.empty();
        return Optional.ofNullable(officerMap.get(nric.toUpperCase()));
    }

    @Override
    public List<HdbOfficer> findAll() {
        return new ArrayList<>(officerMap.values());
    }

    @Override
    public boolean deleteById(String nric) {
        if (nric == null) return false;
        boolean removed = officerMap.remove(nric.toUpperCase()) != null;
         // Note: saveToFile() is not called here
         return removed;
    }

    @Override
    public boolean delete(HdbOfficer officer) {
         if (officer == null || officer.getNric() == null) return false;
         return deleteById(officer.getNric());
    }

     @Override
    public long count() {
        return officerMap.size();
    }


    // --- Requirement Specific Methods (Stubs - Require Service Layer) ---

     /**
     * Checks if an officer is eligible to register for a specific project team.
     * Rules:
     * - Cannot have applied for the project as an Applicant.
     * - Cannot be an approved Officer for another project during the same application period.
     * This requires access to Project, Application, and Registration data. **Belongs in a Service layer.**
     * @param officerNric The NRIC of the officer.
     * @param projectId The ID of the project to register for.
     * @return true if eligible based on available data, false otherwise (full check needs service).
     */
     public boolean isEligibleToRegister(String officerNric, String projectId) {
        System.out.println("HdbOfficerRepository: Checking eligibility for officer " + officerNric + " for project " + projectId + " (Stub - Requires Service Layer for full check)");
        // Basic check: Does the officer exist?
        if (findById(officerNric).isEmpty()){
            System.err.println("Eligibility check failed: Officer " + officerNric + " not found.");
            return false;
        }
         // TODO: Implement full cross-repository checks in a Service Layer:
         // 1. Check ApplicationRepository: Does officer have an application for projectId?
         // 2. Check RegistrationRepository/ProjectsRepository: Does officer have an APPROVED registration
         //    for another project whose application period overlaps with projectId's period?
         return true; // Placeholder - Assume eligible until service layer implements checks
     }

     /**
      * Finds officers assigned to a specific project.
      * Requires Project data access or a direct link in HdbOfficer model. **Belongs in Service Layer or requires model change.**
      * @param projectId The ID of the project.
      * @return List of officers handling the project.
      */
     public List<HdbOfficer> findByHandledProject(String projectId) {
        System.out.println("HdbOfficerRepository: Finding officers for project " + projectId + " (Stub - Requires Service Layer or Officer<->Project Link)");
        // This logic requires iterating through Project objects or having a direct link
        // Example (if accessing Project repo here - not ideal):
        // Optional<Project> projOpt = Database.getProjectsRepository().findById(projectId);
        // if (projOpt.isPresent()) {
        //     return projOpt.get().getAssignedOfficers();
        // }
        // return new ArrayList<>();
        return new ArrayList<>(); // Placeholder - Implement in Service
     }
}