package entities.database.repositories;

// import com.yourgroup.bto.model.Project; // Assuming Project model exists
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import entities.project.*;
import entities.user.*;
import java.util.Date;

/**
 * Repository for managing Project entities.
 * Uses Project Name (String) as the ID. Assumes Project Names are unique.
 */
public class ProjectsRepository implements IRepository<Project, String> {

    // Use Project Name (String) as the key, assuming it's unique
    private final Map<String, Project> projectMap = new ConcurrentHashMap<>();

    // Package-private constructor
    public ProjectsRepository() {
        System.out.println("Initializing ProjectsRepository...");
        loadInitialProjects(); // Load sample/persistent data if needed
        System.out.println("ProjectsRepository initialized with " + projectMap.size() + " projects.");
    }

    private void loadInitialProjects() {
        // TODO: Load projects from file or create initial sample projects if required
        // Example (replace with actual data loading or remove if not needed):
        /*
        try {
            HdbManager sampleManager = (HdbManager) Database.getUsersRepository().findUserByNric("T3333333E").orElse(null);
            if (sampleManager != null) {
                Map<FlatType, Integer> units1 = Map.of(FlatType.TWO_ROOM, 50, FlatType.THREE_ROOM, 100);
                Project p1 = new Project("SkyVille@Dawson", "Queenstown", units1,
                                         new Date(System.currentTimeMillis() - 1000000000L), // Sample past dates
                                         new Date(System.currentTimeMillis() - 500000000L),
                                         sampleManager);
                p1.setVisibility(true); // Make sample visible

                 Map<FlatType, Integer> units2 = Map.of(FlatType.TWO_ROOM, 80);
                 Project p2 = new Project("Woodlands Glade", "Woodlands", units2,
                                         new Date(System.currentTimeMillis() + 1000000000L), // Sample future dates
                                         new Date(System.currentTimeMillis() + 1500000000L),
                                         sampleManager);

                save(p1);
                save(p2);
                System.out.println("Sample projects loaded into ProjectsRepository.");
            } else {
                 System.err.println("Could not load sample projects: Sample Manager T3333333E not found.");
            }
        } catch (Exception e) {
             System.err.println("Error creating sample projects: " + e.getMessage());
        }
        */
    }

    @Override
    public Project save(Project project) {
        if (project == null || project.getName() == null || project.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project and Project Name cannot be null or empty.");
        }
        // Consider adding checks if a project with the same name already exists if updates aren't intended via save
        projectMap.put(project.getName(), project);
        System.out.println("Project saved/updated: " + project.getName());
        return project;
    }

    @Override
    public Optional<Project> findById(String projectName) {
        if (projectName == null) return Optional.empty();
        return Optional.ofNullable(projectMap.get(projectName));
    }

    // findByName is the same as findById for this repository
    public Optional<Project> findByName(String projectName) {
        return findById(projectName);
    }


    @Override
    public List<Project> findAll() {
        return new ArrayList<>(projectMap.values());
    }

    @Override
    public boolean deleteById(String projectName) {
         if (projectName == null) return false;
        Project removed = projectMap.remove(projectName);
        if (removed != null) {
            System.out.println("Project deleted: " + projectName);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(Project project) {
        if (project == null || project.getName() == null) return false;
        return deleteById(project.getName());
    }

    @Override
    public long count() {
        return projectMap.size();
    }

    // --- Requirement Specific Finders (Stubs / Basic Implementation) ---

    /**
     * Finds projects managed by a specific HDB Manager. [cite: 28]
     * @param manager The HDB Manager.
     * @return A list of projects managed by this manager.
     */
    public List<Project> findByManager(HdbManager manager) {
        if (manager == null) return new ArrayList<>();
        return projectMap.values().stream()
                         .filter(project -> manager.equals(project.getManager()))
                         .collect(Collectors.toList());
    }

    /**
     * Finds projects that are currently set to be visible to applicants. [cite: 10, 26]
     * @return A list of visible projects.
     */
    public List<Project> findVisibleToApplicants() {
        return projectMap.values().stream()
                         .filter(Project::isVisible) // Use the getter isVisible()
                         .collect(Collectors.toList());
    }

     /**
     * Finds projects based on filter criteria like neighborhood and flat types offered. [cite: 34]
     * (Basic implementation - more complex filtering might be needed).
     * @param neighborhood Optional neighborhood to filter by (null or empty to ignore).
     * @param flatType Optional FlatType that must be available in the project (null to ignore).
     * @return A list of projects matching the criteria.
     */
    public List<Project> findByFilterCriteria(String neighborhood, FlatType flatType) {
        System.out.println("ProjectsRepository: Filtering projects (Neighborhood: " + neighborhood + ", FlatType: " + flatType + ")");
         return projectMap.values().stream()
                 .filter(project -> (neighborhood == null || neighborhood.trim().isEmpty() || project.getNeighbourhood().equalsIgnoreCase(neighborhood.trim())))
                 .filter(project -> (flatType == null || project.getInitialUnitCount(flatType) > 0)) // Check if flat type exists with units > 0
                 .collect(Collectors.toList());
    }

     /**
     * Finds projects whose application period overlaps with the given dates.
     * Needed for checking HDB staff availability constraints. [cite: 18, 25]
     * @param startDate The start date of the period to check.
     * @param endDate The end date of the period to check.
     * @return A list of projects with overlapping application periods.
     */
     public List<Project> findProjectsInApplicationPeriod(Date startDate, Date endDate) {
         System.out.println("ProjectsRepository: Finding projects overlapping period " + startDate + " - " + endDate + " (Requires careful Date comparison)");
         if (startDate == null || endDate == null || startDate.after(endDate)) {
             return new ArrayList<>(); // Invalid date range
         }
         // Note: java.util.Date comparison is tricky. Using java.time would be cleaner.
         // This logic checks for overlap: (ProjStart <= End) and (ProjEnd >= Start)
         return projectMap.values().stream()
                 .filter(project -> project.getApplicationOpenDate() != null && project.getApplicationCloseDate() != null)
                 .filter(project -> !project.getApplicationOpenDate().after(endDate) && !project.getApplicationCloseDate().before(startDate))
                 .collect(Collectors.toList());
     }

}