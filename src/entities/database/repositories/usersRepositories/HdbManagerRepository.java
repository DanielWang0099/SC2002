package entities.database.repositories.usersRepositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import entities.user.*;
import entities.database.repositories.IRepository;
import utilities.*;
import java.util.Date;

public class HdbManagerRepository implements IRepository<HdbManager, String> {

    private final Map<String, HdbManager> managerMap = new ConcurrentHashMap<>();
    private final String filename = "src/data/users/hdb_managers.csv"; // Define filename

    // Package-private constructor
   public HdbManagerRepository() {}

   // --- Load and Save Methods ---
   public void loadFromFile() {
       List<HdbManager> loadedManagers = CsvUtil.readCsv(filename, this::mapRowToManager, true); // skipHeader=true
       loadedManagers.forEach(this::save); // Use save to populate map correctly
       System.out.println("Loaded " + managerMap.size() + " HDB managers from " + filename);
   }

   public void saveToFile() {
       // Assuming format: Name,NRIC,Password,Age,MaritalStatus (Role is implicit)
       String[] header = {"Name", "NRIC", "Password", "Age", "MaritalStatus"};
       CsvUtil.writeCsv(filename, findAll(), this::mapManagerToRow, header);
   }

   // --- Mappers for CSV ---
   private HdbManager mapRowToManager(String[] row) {
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
               System.err.println("Skipping manager row with invalid NRIC format: " + nric);
               return null;
           }
            // Use the correct HdbManager constructor
           return new HdbManager(name, nric, age, maritalStatus, password, Role.HDB_MANAGER);
       } catch (Exception e) {
           System.err.println("Error mapping row to HdbManager: " + String.join(",", row) + " | Error: " + e.getMessage());
           return null; // Skip invalid rows
       }
   }

   private String[] mapManagerToRow(HdbManager manager) {
       return new String[]{
               manager.getName(),
               manager.getNric(),
               manager.getPassword(), // Saving plain password as required
               String.valueOf(manager.getAge()),
               manager.getMaritalStatus().name() // Get enum name as string
       };
   }

   // --- IRepository Methods (save uses map directly) ---
   @Override
   public HdbManager save(HdbManager manager) {
       if (manager == null || manager.getNric() == null) {
           throw new IllegalArgumentException("Manager/NRIC cannot be null.");
       }
       managerMap.put(manager.getNric().toUpperCase(), manager);
        // Note: saveToFile() is not called here
       return manager;
   }

   @Override
   public Optional<HdbManager> findById(String nric) {
        if (nric == null) return Optional.empty();
       return Optional.ofNullable(managerMap.get(nric.toUpperCase()));
   }

   @Override
   public List<HdbManager> findAll() {
       return new ArrayList<>(managerMap.values());
   }

   @Override
   public boolean deleteById(String nric) {
        if (nric == null) return false;
       boolean removed = managerMap.remove(nric.toUpperCase()) != null;
        // Note: saveToFile() is not called here
        return removed;
   }

    @Override
   public boolean delete(HdbManager manager) {
        if (manager == null || manager.getNric() == null) return false;
        return deleteById(manager.getNric());
   }

    @Override
   public long count() {
       return managerMap.size();
   }

    // --- Requirement Specific Methods (Stubs - Require Service Layer) ---

    /**
    * Checks if a manager is already managing a project within a given application period.
    * - Can only handle one project within an application period.
    * Requires Project data access. **Belongs in a Service Layer.**
    * @param managerNric The NRIC of the manager.
    * @param projectApplicationStartDate The start date of the *new* project's application period.
    * @param projectApplicationEndDate The end date of the *new* project's application period.
    * @return true if the manager is already busy during this period, false otherwise (full check needs service).
    */
   public boolean isManagingProjectDuringPeriod(String managerNric, Date projectApplicationStartDate, Date projectApplicationEndDate) {
       System.out.println("HdbManagerRepository: Checking if manager " + managerNric + " is busy during period (Stub - Requires Service Layer for full check)");
       // Basic check: Does the manager exist?
       if(findById(managerNric).isEmpty()){
            System.err.println("Availability check failed: Manager " + managerNric + " not found.");
            return true; // Treat non-existent manager as 'busy' to prevent assignment? Or return false? Needs definition. Let's say false.
            // return false;
       }
       // TODO: Implement full cross-repository checks in a Service Layer:
       // 1. Get all projects managed by managerNric (using ProjectsRepository.findByManager).
       // 2. For each managed project, get its application start/end dates.
       // 3. Check if any of those periods overlap with the given projectApplicationStartDate/EndDate.
       // 4. Return true if overlap found, false otherwise.
       return false; // Placeholder - Assume not busy until service layer implements checks
   }
}
