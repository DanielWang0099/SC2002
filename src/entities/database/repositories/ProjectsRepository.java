package entities.database.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import entities.project.*;
import entities.user.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Comparator;
import utilities.*;
import entities.database.*;

/**
 * Repository for managing Project entities.
 * Uses Project Name (String) as the ID. Assumes Project Names are unique.
 */
public class ProjectsRepository implements IRepository<Project, String> {

    private final Map<String, Project> projectMap = new ConcurrentHashMap<>();
    private final String filename = "data/projects.csv"; // Define filename
    // Define a consistent date format for CSV read/write
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public ProjectsRepository() {}

     // --- Load and Save Methods ---
     public void loadFromFile() {
        // IMPORTANT: Requires Database.getUsersRepository() to be ready for lookups!
        // Ensure correct initialization order in Database facade if lookups needed during load.
        List<Project> loadedProjects = CsvUtil.readCsv(filename, this::mapRowToProject, true); // skipHeader=true
        loadedProjects.forEach(this::save);
        System.out.println("Loaded " + projectMap.size() + " projects from " + filename);
    }

    public void saveToFile() {
         // Define header dynamically based on max officers or fixed columns
         // Simplified: Fixed header assuming max 2 flat types for now
        String[] header = {"Name", "Neighbourhood",
                           "FlatType1", "InitialUnits1", "Price1",
                           "FlatType2", "InitialUnits2", "Price2", // Allow for null/empty if only 1 type
                           "OpenDate", "CloseDate", "ManagerNRIC", "Visibility",
                           "OfficerNRIC1", "OfficerNRIC2", "OfficerNRIC3", "OfficerNRIC4", "OfficerNRIC5",
                           "OfficerNRIC6", "OfficerNRIC7", "OfficerNRIC8", "OfficerNRIC9", "OfficerNRIC10"};
        CsvUtil.writeCsv(filename, findAll(), this::mapProjectToRow, header);
    }

     // --- Mappers for CSV ---
    private Project mapRowToProject(String[] row) {
        try {
            // Match the assumed header order
            if (row.length < 12) throw new IllegalArgumentException("Incorrect number of columns for project");

            String name = row[0];
            String neighbourhood = row[1];

            Map<FlatType, Integer> initialUnits = new HashMap<>();
            Map<FlatType, Double> prices = new HashMap<>();

            // Parse Flat Type 1 block
            if (!row[2].isEmpty()) {
                 FlatType type1 = FlatType.valueOf(row[2].toUpperCase());
                 int count1 = Integer.parseInt(row[3]);
                 double price1 = Double.parseDouble(row[4]);
                 initialUnits.put(type1, count1);
                 prices.put(type1, price1);
            }
             // Parse Flat Type 2 block (optional)
             if (row.length > 7 && !row[5].isEmpty()) {
                 FlatType type2 = FlatType.valueOf(row[5].toUpperCase());
                 int count2 = Integer.parseInt(row[6]);
                 double price2 = Double.parseDouble(row[7]);
                  if (initialUnits.containsKey(type2)) {
                      System.err.println("Warning: Duplicate flat type " + type2 + " defined for project " + name + ". Ignoring second entry.");
                  } else {
                      initialUnits.put(type2, count2);
                      prices.put(type2, price2);
                  }
             }

            Date openDate = DATE_FORMAT.parse(row[8]); // Adjust indices based on final column count
            Date closeDate = DATE_FORMAT.parse(row[9]);
            String managerNric = row[10];
            boolean visibility = Boolean.parseBoolean(row[11]);

            // Find Manager - Requires UsersRepository to be available!
            Optional<User> managerOpt = Database.getUsersRepository().findUserByNric(managerNric);
            if (managerOpt.isEmpty() || !(managerOpt.get() instanceof HdbManager)) {
                 System.err.println("Skipping project row: Manager NRIC '" + managerNric + "' not found or not a Manager.");
                 return null;
            }
            HdbManager manager = (HdbManager) managerOpt.get();

            Project project = new Project(name, neighbourhood, initialUnits, prices, openDate, closeDate, manager);
            project.setVisibility(visibility); // Set loaded visibility

            // Load assigned officers
            for (int i = 0; i < 10; i++) {
                 int officerColIndex = row.length - 8 + i; // Index for OfficerNRIC[i+1]
                 if (officerColIndex < row.length && !row[officerColIndex].isEmpty()) {
                     String officerNric = row[officerColIndex];
                      Optional<User> officerOpt = Database.getUsersRepository().findUserByNric(officerNric);
                       if (officerOpt.isPresent() && officerOpt.get() instanceof HdbOfficer) {
                           project.addOfficer((HdbOfficer) officerOpt.get()); // Use addOfficer to manage count
                       } else {
                            System.err.println("Warning: Officer NRIC '" + officerNric + "' not found or not an Officer for project " + name);
                       }
                 }
            }
            return project;

        } catch (Exception e) {
            System.err.println("Error mapping row to Project: " + String.join(",", row) + " | Error: " + e.getMessage());
            return null; // Skip invalid rows
        }
    }


    private String[] mapProjectToRow(Project project) {
        // Base array size matches the expected header columns
        String[] row = new String[22];

        // Basic Info
        row[0] = project.getName() != null ? project.getName() : ""; // Handle potential null name
        row[1] = project.getNeighbourhood() != null ? project.getNeighbourhood() : ""; // Handle potential null neighbourhood

        // Flat Types and Details (Handles 0, 1, or 2 types gracefully)
        List<FlatType> types = new ArrayList<>(project.getInitialFlatUnitCounts().keySet());
        // Sort ensures consistent order (e.g., TWO_ROOM then THREE_ROOM)
        types.sort(Comparator.comparing(Enum::name));

        // Type 1 details (indices 2, 3, 4)
        if (types.size() > 0) {
            FlatType type1 = types.get(0);
            row[2] = type1.name(); // Enum name
            row[3] = String.valueOf(project.getInitialUnitCount(type1)); // Initial count
            row[4] = String.valueOf(project.getUnitPrice(type1));       // Price
        } else {
             // No flat types defined (shouldn't happen if constructor validates)
             row[2] = ""; row[3] = ""; row[4] = "";
        }

        // Type 2 details (indices 5, 6, 7)
        if (types.size() > 1) {
            FlatType type2 = types.get(1);
            row[5] = type2.name(); // Enum name
            row[6] = String.valueOf(project.getInitialUnitCount(type2)); // Initial count
            row[7] = String.valueOf(project.getUnitPrice(type2));       // Price
        } else {
            // Only one or zero flat types defined
             row[5] = ""; row[6] = ""; row[7] = "";
        }

        // Dates (indices 8, 9) - CORRECTED with null checks
        row[8] = project.getApplicationOpenDate() != null ? DATE_FORMAT.format(project.getApplicationOpenDate()) : "";
        row[9] = project.getApplicationCloseDate() != null ? DATE_FORMAT.format(project.getApplicationCloseDate()) : "";

        // Manager NRIC (index 10)
        row[10] = project.getManager() != null ? project.getManager().getNric() : "";

        // Visibility (index 11)
        row[11] = String.valueOf(project.isVisible()); // "true" or "false"

        // Officers (indices 12 to 21)
        List<HdbOfficer> assignedOfficers = project.getAssignedOfficers(); // Gets the list of non-null officers
        for(int i = 0; i < 10; i++) { // Loop exactly 10 times for the 10 columns
            if (i < assignedOfficers.size()) {
                // If there is an officer at this position in the list, get their NRIC
                row[12 + i] = assignedOfficers.get(i).getNric();
            } else {
                // Otherwise, fill the remaining CSV columns with empty strings
                row[12 + i] = "";
            }
        }

        return row;
    }


    // --- IRepository Methods ---
    @Override
    public Project save(Project project) { /* unchanged */
        if (project == null || project.getName() == null || project.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project/Name cannot be null or empty.");
        }
        projectMap.put(project.getName(), project);
        return project;
    }
    
    @Override
    public Optional<Project> findById(String projectName) { /* unchanged */
         if (projectName == null) return Optional.empty();
        return Optional.ofNullable(projectMap.get(projectName));
    }

    public Optional<Project> findByName(String projectName) { /* unchanged */ return findById(projectName); }

    @Override
    public List<Project> findAll() { /* unchanged */ return new ArrayList<>(projectMap.values()); }

     @Override
    public boolean deleteById(String projectName) { /* unchanged */
         if (projectName == null) return false;
         return projectMap.remove(projectName) != null;
    }
    @Override
    public boolean delete(Project project) { /* unchanged */
         if (project == null || project.getName() == null) return false;
        return deleteById(project.getName());
    }
    @Override
    public long count() { /* unchanged */ return projectMap.size(); }


    // --- Requirement Specific Finders (Implementations) ---

    public List<Project> findVisibleToApplicants() { /* unchanged */
        return projectMap.values().stream()
                         .filter(Project::isVisible)
                         .collect(Collectors.toList());
    }

    public List<Project> findByCriteria(String neighborhoodFilter, FlatType flatTypeFilter,
                                        String managerNricFilter, Boolean visibilityFilter,
                                        Date[] dateRangeFilter) {

        Stream<Project> projectStream = projectMap.values().stream();

        // Apply filters... (logic as implemented previously)
        if (neighborhoodFilter != null && !neighborhoodFilter.trim().isEmpty()) {
            final String finalNeighFilter = neighborhoodFilter.trim(); // For use in lambda
            projectStream = projectStream.filter(project -> project.getNeighbourhood()
                                            .equalsIgnoreCase(finalNeighFilter));
            }

        if (flatTypeFilter != null) {
        projectStream = projectStream.filter(project -> project.getInitialUnitCount(flatTypeFilter) > 0);
        }
        if (managerNricFilter != null && !managerNricFilter.trim().isEmpty()) {
            final String finalManagerNric = managerNricFilter.trim(); // For use in lambda
            projectStream = projectStream.filter(project -> project.getManager() != null &&
                                project.getManager().getNric().equalsIgnoreCase(finalManagerNric));
            }
        
        if (visibilityFilter != null) {
                projectStream = projectStream.filter(project -> project.isVisible() == visibilityFilter);
                }

        
        if (dateRangeFilter != null && dateRangeFilter.length == 2 &&
                dateRangeFilter[0] != null && dateRangeFilter[1] != null &&
                !dateRangeFilter[0].after(dateRangeFilter[1])) { // Check for valid range
                    final Date filterStart = dateRangeFilter[0];
                    final Date filterEnd = dateRangeFilter[1];
                    projectStream = projectStream.filter(project ->
                        project.getApplicationOpenDate() != null && project.getApplicationCloseDate() != null &&
                        !project.getApplicationOpenDate().after(filterEnd) && // Project starts before or when filter ends
                        !project.getApplicationCloseDate().before(filterStart) // Project ends after or when filter starts
                    );
            }
        // Return filtered list (sorting removed)
        return projectStream.collect(Collectors.toList()); // COLLECT WITHOUT SORTING HERE
    }


        // Return sorted list
        public List<Project> findByManagerNric(String managerNric) {
            return findByCriteria(null, null, managerNric, null, null);
        }
    
        // findProjectsInApplicationPeriod can also use findByCriteria
        public List<Project> findProjectsInApplicationPeriod(Date startDate, Date endDate) {
            if (startDate == null || endDate == null) return new ArrayList<>();
            return findByCriteria(null, null, null, null, new Date[]{startDate, endDate});
         }

         public List<Project> findByVisibility(Date startDate, Date endDate) {
            if (startDate == null || endDate == null) return new ArrayList<>();
            return findByCriteria(null, null, null, null, new Date[]{startDate, endDate});
         }

         public List<Project> findByManager(HdbManager manager) { /* unchanged */
            if (manager == null) return new ArrayList<>();
            return projectMap.values().stream()
                             .filter(project -> manager.equals(project.getManager()))
                             .collect(Collectors.toList());
        }
}