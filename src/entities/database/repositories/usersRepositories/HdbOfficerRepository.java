package entities.database.repositories.usersRepositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import entities.user.*;
import entities.database.repositories.IRepository;

public class HdbOfficerRepository implements IRepository<HdbOfficer, String> {

    private final Map<String, HdbOfficer> officerMap = new ConcurrentHashMap<>();

     // Package-private constructor
    HdbOfficerRepository() {}

    @Override
    public HdbOfficer save(HdbOfficer officer) {
        if (officer == null || officer.getNric() == null) {
            throw new IllegalArgumentException("Officer and Officer NRIC cannot be null.");
        }
        officerMap.put(officer.getNric().toUpperCase(), officer);
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
        return officerMap.remove(nric.toUpperCase()) != null;
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

     // --- Requirement Specific Methods (Stubs) ---

     /**
     * Checks if an officer is eligible to register for a specific project team.
     * Rules[cite: 18]:
     * - Cannot have applied for the project as an Applicant.
     * - Cannot be an approved Officer for another project during the same application period.
     * This requires access to Project, Application, and Registration data. Better in a service layer.
     * @param officerNric The NRIC of the officer.
     * @param projectId The ID of the project to register for.
     * @return true if eligible, false otherwise.
     */
     public boolean isEligibleToRegister(String officerNric, String projectId) {
        System.out.println("HdbOfficerRepository: Checking eligibility for officer " + officerNric + " for project " + projectId + " (Stub - Requires Project/App/Reg Data)");
         // TODO: Implement checks against Application and Registration repositories/services
         // 1. Check if user submitted an Application for projectId
         // 2. Check if user has an approved Registration for *another* project overlapping application period
         return true; // Placeholder
     }

     /**
      * Finds officers assigned to a specific project.
      * Requires a link between Officer and Project (e.g., a field in HdbOfficer or a separate mapping).
      * @param projectId The ID of the project.
      * @return List of officers handling the project.
      */
     public List<HdbOfficer> findByHandledProject(String projectId) {
        System.out.println("HdbOfficerRepository: Finding officers for project " + projectId + " (Stub - Requires Officer<->Project Link)");
         // TODO: Implement filtering based on officer's handled project field/mapping
         return new ArrayList<>(); // Placeholder
     }
}

//---