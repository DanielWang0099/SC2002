package entities.database.repositories.usersRepositories;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import entities.user.*;
import entities.database.repositories.IRepository;

public class HdbManagerRepository implements IRepository<HdbManager, String> {

    private final Map<String, HdbManager> managerMap = new ConcurrentHashMap<>();

    // Package-private constructor
   HdbManagerRepository() {}

   @Override
   public HdbManager save(HdbManager manager) {
       if (manager == null || manager.getNric() == null) {
           throw new IllegalArgumentException("Manager and Manager NRIC cannot be null.");
       }
       managerMap.put(manager.getNric().toUpperCase(), manager);
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
       return managerMap.remove(nric.toUpperCase()) != null;
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

    // --- Requirement Specific Methods (Stubs) ---

    /**
    * Checks if a manager is already managing a project within a given application period.
    * [cite: 25] - Can only handle one project within an application period.
    * Requires Project data access (specifically application dates). Better in a service layer.
    * @param managerNric The NRIC of the manager.
    * @param projectApplicationStartDate The start date of the *new* project's application period.
    * @param projectApplicationEndDate The end date of the *new* project's application period.
    * @return true if the manager is already busy during this period, false otherwise.
    */
   public boolean isManagingProjectDuringPeriod(String managerNric, java.time.LocalDate projectApplicationStartDate, java.time.LocalDate projectApplicationEndDate) {
       System.out.println("HdbManagerRepository: Checking if manager " + managerNric + " is busy during period (Stub - Requires Project Data)");
       // TODO: Query Project repository/service for projects managed by this manager
       // TODO: Check if any existing project's application period overlaps with the given dates
       return false; // Placeholder
   }
}