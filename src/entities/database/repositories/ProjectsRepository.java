package entities.database.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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

    public ProjectsRepository() {
        System.out.println("Initializing ProjectsRepository...");
        loadFromFile();
        System.out.println("ProjectsRepository initialized with " + projectMap.size() + " projects.");
    }

     // --- Load and Save Methods ---
    private void loadFromFile() {
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
                 FlatType type2 = FlatType.valueOf(row[6].toUpperCase());
                 int count2 = Integer.parseInt(row[7]);
                 double price2 = Double.parseDouble(row[8]);
                  if (initialUnits.containsKey(type2)) {
                      System.err.println("Warning: Duplicate flat type " + type2 + " defined for project " + name + ". Ignoring second entry.");
                  } else {
                      initialUnits.put(type2, count2);
                      prices.put(type2, price2);
                  }
             }

            Date openDate = DATE_FORMAT.parse(row[row.length - 12]); // Adjust indices based on final column count
            Date closeDate = DATE_FORMAT.parse(row[row.length - 11]);
            String managerNric = row[row.length - 10];
            boolean visibility = Boolean.parseBoolean(row[row.length - 9]);

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
        String[] row = new String[22]; // Based on header size: 2 + 3 + 3 + 4 + 10

        row[0] = project.getName();
        row[1] = project.getNeighbourhood();

        // Flat Type 1 - Assuming max 2 types, get them sorted/consistently
        List<FlatType> types = new ArrayList<>(project.getInitialFlatUnitCounts().keySet());
        types.sort(Comparator.comparing(Enum::name)); // Consistent order

        if (types.size() > 0) {
            FlatType type1 = types.get(0);
            row[2] = type1.name();
            row[3] = String.valueOf(project.getInitialUnitCount(type1));
            row[4] = String.valueOf(project.getUnitPrice(type1));
        } else {
             row[2] = ""; row[3] = ""; row[4] = "";
        }
        // Flat Type 2
        if (types.size() > 1) {
            FlatType type2 = types.get(1);
            row[5] = type2.name();
            row[6] = String.valueOf(project.getInitialUnitCount(type2));
            row[7] = String.valueOf(project.getUnitPrice(type2));
        } else {
             row[5] = ""; row[6] = ""; row[7] = "";
        }

        row[8] = DATE_FORMAT.format(project.getApplicationOpenDate());
        row[9] = DATE_FORMAT.format(project.getApplicationCloseDate());
        row[10] = project.getManager() != null ? project.getManager().getNric() : "";
        row[11] = String.valueOf(project.isVisible());

        // Officers
        List<HdbOfficer> assignedOfficers = project.getAssignedOfficers();
        for(int i = 0; i < 10; i++) {
            if (i < assignedOfficers.size()) {
                row[12 + i] = assignedOfficers.get(i).getNric();
            } else {
                row[12 + i] = ""; // Empty string for empty slots
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
    public List<Project> findByManager(HdbManager manager) { /* unchanged */
        if (manager == null) return new ArrayList<>();
        return projectMap.values().stream()
                         .filter(project -> manager.equals(project.getManager()))
                         .collect(Collectors.toList());
    }
    public List<Project> findVisibleToApplicants() { /* unchanged */
        return projectMap.values().stream()
                         .filter(Project::isVisible)
                         .collect(Collectors.toList());
    }

    public List<Project> findByFilterCriteria(String neighborhood, FlatType flatType) { /* unchanged */
        System.out.println("ProjectsRepository: Filtering projects (Neighborhood: " + neighborhood + ", FlatType: " + flatType + ")");
         return projectMap.values().stream()
                 .filter(project -> (neighborhood == null || neighborhood.trim().isEmpty() || project.getNeighbourhood().equalsIgnoreCase(neighborhood.trim())))
                 .filter(project -> (flatType == null || project.getInitialUnitCount(flatType) > 0))
                 .collect(Collectors.toList());
    }

    public List<Project> findProjectsInApplicationPeriod(Date startDate, Date endDate) { /* unchanged */
         System.out.println("ProjectsRepository: Finding projects overlapping period " + startDate + " - " + endDate + " (Requires careful Date comparison)");
         if (startDate == null || endDate == null || startDate.after(endDate)) {
             return new ArrayList<>(); // Invalid date range
         }
         return projectMap.values().stream()
                 .filter(project -> project.getApplicationOpenDate() != null && project.getApplicationCloseDate() != null)
                 .filter(project -> !project.getApplicationOpenDate().after(endDate) && !project.getApplicationCloseDate().before(startDate))
                 .collect(Collectors.toList());
     }
}