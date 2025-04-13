package controller;

import entities.database.Database;
import entities.project.*; // Project, FlatType, User, HdbManager, HdbOfficer etc.
import entities.documents.approvableDocuments.*; // Needed for checking related docs on delete
import entities.user.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.*;
import java.util.Comparator;

/**
 * Controller handling business logic related to BTO Projects.
 */
public class ProjectController {

    /**
     * Creates a new BTO project listing.
     * Only HDB Managers can perform this action.
     * @param name                 Project Name.
     * @param neighbourhood        Project Neighbourhood.
     * @param initialFlatUnitCounts Map of initial unit counts per flat type.
     * @param flatUnitPrices       Map of unit prices per flat type.
     * @param applicationOpenDate  Application open date.
     * @param applicationCloseDate Application close date.
     * @param creatingManager      The HDB Manager creating the project.
     * @return The created Project object, or null if creation failed (e.g., validation error, manager busy).
     */
    public Project createProject(String name, String neighbourhood,
                                Map<FlatType, Integer> initialFlatUnitCounts, Map<FlatType, Double> flatUnitPrices,
                                Date applicationOpenDate, Date applicationCloseDate, HdbManager creatingManager) {

        // 1. Validation
        if (name == null || name.trim().isEmpty() || neighbourhood == null || neighbourhood.trim().isEmpty() ||
            initialFlatUnitCounts == null || initialFlatUnitCounts.isEmpty() || flatUnitPrices == null ||
            applicationOpenDate == null || applicationCloseDate == null || creatingManager == null) {
            System.err.println("Project Creation Error: Missing required fields.");
            return null;
        }
        if (applicationOpenDate.after(applicationCloseDate)) {
             System.err.println("Project Creation Error: Open date cannot be after close date.");
             return null;
        }
        // Check if project name already exists
        if (Database.getProjectsRepository().findById(name).isPresent()) {
            System.err.println("Project Creation Error: Project name '" + name + "' already exists.");
            return null;
        }

        // 2. Check Manager Availability [cite: 25] - Can only handle one project within an application period
        boolean managerBusy = isManagerBusyDuringPeriod(creatingManager.getNric(), applicationOpenDate, applicationCloseDate);
         if (managerBusy) {
             System.err.println("Project Creation Error: Manager " + creatingManager.getNric() + " is already managing another project during this application period.");
             return null;
         }


        // 3. Create Project Object
        try {
            Project newProject = new Project(name, neighbourhood, initialFlatUnitCounts, flatUnitPrices,
                                             applicationOpenDate, applicationCloseDate, creatingManager);
             // 4. Save Project
            Database.getProjectsRepository().save(newProject);
            System.out.println("Project '" + name + "' created successfully by Manager " + creatingManager.getNric());
            return newProject;
        } catch (Exception e) {
            System.err.println("Project Creation Error: Failed to create project object. " + e.getMessage());
            return null;
        }
    }

     /**
     * Edits details of an existing project.
     * Only the HDB Manager in charge should perform this (or potentially other managers?).
     * Restrictions may apply (e.g., cannot edit certain fields after applications open).
     * @param editor The HDB Manager attempting the edit.
     * @param projectName The name of the project to edit.
     * @param newNeighbourhood Optional new neighbourhood.
     * @param newUnitCounts Optional new unit counts map.
     * @param newUnitPrices Optional new unit prices map.
     * @param newOpenDate Optional new open date.
     * @param newCloseDate Optional new close date.
     * @return true if editing was successful, false otherwise.
     */
     public boolean editProject(HdbManager editor, String projectName, String newNeighbourhood,
                               Map<FlatType, Integer> newUnitCounts, Map<FlatType, Double> newUnitPrices,
                               Date newOpenDate, Date newCloseDate) {

        Optional<Project> projectOpt = Database.getProjectsRepository().findById(projectName);
        if (projectOpt.isEmpty()) {
            System.err.println("Project Edit Error: Project '" + projectName + "' not found.");
            return false;
        }
        Project project = projectOpt.get();

        // 1. Authorization Check (Only assigned manager? Or any manager?)
        // PDF [cite: 25] implies creator manages it. Let's assume only assigned manager can edit.
        if (!editor.equals(project.getManager())) {
             System.err.println("Project Edit Error: Manager " + editor.getNric() + " is not authorized to edit project '" + projectName + "'.");
             return false;
        }

        // 2. Business Rule Checks (e.g., cannot edit after applications open?)
        // TODO: Define specific rules. For now, allow edits.
        boolean changed = false;
        try {
            if (newNeighbourhood != null && !newNeighbourhood.trim().isEmpty()) {
                project.setNeighbourhood(newNeighbourhood.trim());
                changed = true;
            }
            if (newUnitCounts != null) {
                // Update counts carefully - this resets remaining units
                 for (Map.Entry<FlatType, Integer> entry : newUnitCounts.entrySet()) {
                     project.updateFlatUnitCount(entry.getKey(), entry.getValue());
                     changed = true;
                 }
            }
             if (newUnitPrices != null) {
                 for (Map.Entry<FlatType, Double> entry : newUnitPrices.entrySet()) {
                     project.updateFlatUnitPrice(entry.getKey(), entry.getValue());
                     changed = true;
                 }
            }
            if (newOpenDate != null) {
                project.setApplicationOpenDate(newOpenDate);
                changed = true;
            }
            if (newCloseDate != null) {
                project.setApplicationCloseDate(newCloseDate);
                 changed = true;
            }
            // Validate date logic if both changed
            if (project.getApplicationOpenDate().after(project.getApplicationCloseDate())) {
                throw new IllegalArgumentException("Open date cannot be after close date.");
            }


            if (changed) {
                // 3. Save Changes
                Database.getProjectsRepository().save(project);
                System.out.println("Project '" + projectName + "' updated successfully by Manager " + editor.getNric());
                return true;
            } else {
                 System.out.println("Project Edit: No changes detected for project '" + projectName + "'.");
                 return true; // No changes, but operation technically succeeded.
            }
        } catch (Exception e) {
             System.err.println("Project Edit Error for '" + projectName + "': " + e.getMessage());
             // Consider reverting changes if transactionality was needed
             return false;
        }
     }


     /**
     * Deletes a project.
     * Only the HDB Manager in charge should perform this (or any manager?).
     * Restrictions: Cannot delete if applications exist? Or registrations? Needs clarification.
     * @param deleter The HDB Manager attempting deletion.
     * @param projectName The name of the project to delete.
     * @return true if deletion successful, false otherwise.
     */
     public boolean deleteProject(HdbManager deleter, String projectName) {
         Optional<Project> projectOpt = Database.getProjectsRepository().findById(projectName);
        if (projectOpt.isEmpty()) {
            System.err.println("Project Delete Error: Project '" + projectName + "' not found.");
            return false;
        }
        Project project = projectOpt.get();

        // 1. Authorization Check (Assuming only assigned manager)
        if (!deleter.equals(project.getManager())) {
             System.err.println("Project Delete Error: Manager " + deleter.getNric() + " is not authorized to delete project '" + projectName + "'.");
             return false;
        }

        // 2. Business Rule Check (e.g., prevent deletion if active applications/registrations?)
        // This requires checking other repositories.
        List<ProjectApplication> apps = Database.getDocumentsRepository().getApplicationRepository().findByProjectId(projectName);
        List<ProjectRegistration> regs = Database.getDocumentsRepository().getRegistrationRepository().findByProjectId(projectName);
        if (!apps.isEmpty() || !regs.isEmpty()) {
            System.err.println("Project Delete Error: Cannot delete project '" + projectName + "' as it has associated applications or officer registrations.");
            return false;
        }

        // 3. Perform Deletion
        boolean deleted = Database.getProjectsRepository().deleteById(projectName);
        if (deleted) {
             System.out.println("Project '" + projectName + "' deleted successfully by Manager " + deleter.getNric());
             return true;
        } else {
             System.err.println("Project Delete Error: Failed to delete project '" + projectName + "' from repository.");
             return false;
        }
     }

    /**
     * Toggles the visibility of a project for applicants.
     * @param manager The HDB Manager performing the action.
     * @param projectName The name of the project.
     * @param isVisible The desired visibility state (true for visible, false for hidden).
     * @return true if successful, false otherwise.
     */
    public boolean toggleProjectVisibility(HdbManager manager, String projectName, boolean isVisible) {
        Optional<Project> projectOpt = Database.getProjectsRepository().findById(projectName);
        if (projectOpt.isEmpty()) {
            System.err.println("Visibility Toggle Error: Project '" + projectName + "' not found.");
            return false;
        }
        Project project = projectOpt.get();

        // Authorization: Assume only assigned manager can toggle? PDF isn't explicit[cite: 26]. Let's allow for now.
        // if (!manager.equals(project.getManager())) {
        //     System.err.println("Visibility Toggle Error: Not authorized.");
        //     return false;
        // }

        project.setVisibility(isVisible);
        Database.getProjectsRepository().save(project); // Persist change
        System.out.println("Project '" + projectName + "' visibility set to " + isVisible + " by Manager " + manager.getNric());
        return true;
    }


    /**
     * Retrieves projects based on filter criteria.
     * Handles visibility rules for applicants.
     * @param requestingUser The user making the request (can be null for general public view).
     * @param neighborhood Optional filter by neighborhood.
     * @param flatType Optional filter by flat type availability.
     * @return List of matching projects.
     */
    public List<Project> getFilteredProjects(User requestingUser, String neighborhood, FlatType flatType) {
        List<Project> allProjects = Database.getProjectsRepository().findAll(); // Get all projects first

        Stream<Project> projectStream = allProjects.stream();

        // Filter by neighborhood
        if (neighborhood != null && !neighborhood.trim().isEmpty()) {
            projectStream = projectStream.filter(p -> p.getNeighbourhood().equalsIgnoreCase(neighborhood.trim()));
        }

        // Filter by flat type availability
        if (flatType != null) {
            // Check if the project offers this flat type with initial units > 0
            projectStream = projectStream.filter(p -> p.getInitialUnitCount(flatType) > 0);
        }

        // Filter by Visibility and Role
        if (requestingUser == null || requestingUser.getRole() == Role.APPLICANT) {
             // Applicants only see visible projects [cite: 10, 26]
            projectStream = projectStream.filter(Project::isVisible);
        } else if (requestingUser.getRole() == Role.HDB_OFFICER) {
            // Officers see handled projects regardless of visibility, others based on visibility
             final String officerNric = requestingUser.getNric();
             projectStream = projectStream.filter(p -> p.isVisible() || isOfficerAssigned(p, officerNric));
        }
        // Managers see all projects regardless of visibility [cite: 27] - no extra filter needed

        // Apply default sorting (e.g., alphabetical by name) [cite: 34]
        return projectStream
                .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves only the projects created by a specific manager.
     * @param manager The HDB Manager.
     * @return List of projects created by the manager.
     */
    public List<Project> getProjectsByManager(HdbManager manager) {
         if (manager == null) return List.of();
         return Database.getProjectsRepository().findByManager(manager); // Use repository method
    }


    // --- Helper methods ---

    /**
     * Helper to check if a manager is busy managing another project during a specific period.
     * @param managerNric NRIC of the manager.
     * @param startDate Start date of the period.
     * @param endDate End date of the period.
     * @return true if busy, false otherwise.
     */
     private boolean isManagerBusyDuringPeriod(String managerNric, Date startDate, Date endDate) {
         Optional<User> userOpt = Database.getUsersRepository().findUserByNric(managerNric);
         if(userOpt.isEmpty() || !(userOpt.get() instanceof HdbManager)) return false; // Should not happen if called correctly

         HdbManager manager = (HdbManager) userOpt.get();
         List<Project> managedProjects = Database.getProjectsRepository().findByManager(manager);

         // Check for overlap with other projects managed by this manager
         for (Project existingProject : managedProjects) {
             // Check if periods overlap: (StartA <= EndB) and (EndA >= StartB)
             boolean overlap = !existingProject.getApplicationOpenDate().after(endDate) &&
                               !existingProject.getApplicationCloseDate().before(startDate);
             if (overlap) {
                 // Found an overlapping project period
                 return true;
             }
         }
         return false; // No overlapping periods found
     }

     /**
      * Helper to check if a specific officer is assigned to a project.
      * @param project Project object
      * @param officerNric NRIC of the officer
      * @return true if assigned, false otherwise
      */
     private boolean isOfficerAssigned(Project project, String officerNric) {
         if (project == null || officerNric == null) return false;
         return project.getAssignedOfficers().stream()
                       .anyMatch(officer -> officer.getNric().equalsIgnoreCase(officerNric));
     }
     public List<Project> getFilteredProjects(User requestingUser, String neighborhood, FlatType flatType,
                                             String managerNric, Date[] dateRange) { // Added filters

        Boolean visibilityFilter = null; // Managers & Officers (initially) see all visibility
        HdbManager managerFilter = null; // Specific manager object if needed for filtering handled projects

        if (requestingUser != null && requestingUser.getRole() == Role.HDB_MANAGER) {
            // If manager is filtering by "my projects", set managerNric filter
            // We handle the "my projects" view via getProjectsByManager instead for clarity.
        } else if (requestingUser == null || requestingUser.getRole() == Role.APPLICANT) {
            visibilityFilter = true; // Applicants only see visible=true projects
        }
        // Officers handled below

        // Call repository finder with all applicable filters
        List<Project> filteredProjects = Database.getProjectsRepository()
                .findByCriteria(neighborhood, flatType, managerNric, visibilityFilter, dateRange);

        // Apply Officer specific visibility logic AFTER basic filtering
        if (requestingUser != null && requestingUser.getRole() == Role.HDB_OFFICER) {
             final String officerNric = requestingUser.getNric();
             // Re-fetch ALL matching filters first, then apply visibility logic for officers
             List<Project> allMatching = Database.getProjectsRepository()
                     .findByCriteria(neighborhood, flatType, managerNric, null, dateRange); // Visibility null
             return allMatching.stream()
                        .filter(p -> p.isVisible() || isOfficerAssigned(p, officerNric)) // Visible OR Handled by officer
                        .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                        .collect(Collectors.toList());
        }

        return filteredProjects; // Already sorted by repository for Applicant/Manager/null
    }

    public List<Project> getProjectsByManager(HdbManager manager, String neighborhood, FlatType flatType, Date[] dateRange) {
        if (manager == null) return List.of();
        // Use the repository method with the manager NRIC filter applied
        return Database.getProjectsRepository()
                       .findByCriteria(neighborhood, flatType, manager.getNric(), null, dateRange); // Visibility null = manager sees all their projects
   }
}