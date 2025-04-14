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

    public static final String SORT_BY_NAME = "NAME";
    public static final String SORT_BY_NEIGHBOURHOOD = "NEIGHBOURHOOD";
    public static final String SORT_BY_MANAGER = "MANAGER";
    public static final String SORT_BY_OPEN_DATE = "OPENDATE";


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

        if (!manager.equals(project.getManager())) {
            System.err.println("Visibility Toggle Error: Not authorized.");
            return false;
       }

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
/*      public List<Project> getFilteredProjects(User requestingUser, String neighborhood, FlatType flatType,
            String managerNric, Date[] dateRange,
            String sortByField, boolean sortAscending) {

        // 1. Initial Filter based on USER-SPECIFIED criteria (excluding role-based visibility for now)
        //    Call repository with visibilityFilter = null to get all potential matches.
        List<Project> potentiallyRelevantProjects = Database.getProjectsRepository()
        .findByCriteria(neighborhood, flatType, managerNric, null, dateRange); // Visibility = null

        // 2. Apply Role-Based Access/Visibility Rules (using Streams for filtering the initial list)
        Stream<Project> viewableProjectsStream;
        Role userRole = (requestingUser != null) ? requestingUser.getRole() : null;

        if (userRole == Role.HDB_MANAGER) {
        // Managers see all projects matching the filters, regardless of visibility [cite: 27]
        viewableProjectsStream = potentiallyRelevantProjects.stream();
        // Note: If managerNric filter was applied, they only see that manager's projects.
        // If they selected "View My Projects", the Boundary should pass manager's NRIC here.

        } else if (userRole == Role.HDB_OFFICER) {
        // Officers see projects they handle OR projects that are visible [cite: 9, 21]
        final String officerNric = requestingUser.getNric();
        viewableProjectsStream = potentiallyRelevantProjects.stream()
        .filter(p -> p.isVisible() || isOfficerAssigned(p, officerNric));

        } else { // Includes Applicant role and null (unauthenticated) users
        // Applicants only see projects where visibility is true [cite: 10]
        viewableProjectsStream = potentiallyRelevantProjects.stream()
        .filter(Project::isVisible);
        }

        // 3. Collect the viewable projects into a list
        List<Project> finalViewableProjects = viewableProjectsStream.collect(Collectors.toList());

        // 4. Perform Sorting on the final list
        sortProjects(finalViewableProjects, sortByField, sortAscending);

        return finalViewableProjects;
    } */

    public List<Project> getFilteredProjects(User requestingUser, String neighborhood, FlatType flatType,
                                             String managerNric, Date[] dateRange,
                                             String sortByField, boolean sortAscending) {

        // 1. Initial Filter based on USER-SPECIFIED criteria (excluding role-based visibility for now)
        List<Project> potentiallyRelevantProjects = Database.getProjectsRepository()
                .findByCriteria(neighborhood, flatType, managerNric, null, dateRange); // Visibility = null initially

        // 2. Apply Role-Based Access/Visibility Rules AND Applicant Flat Type Eligibility
        Stream<Project> viewableProjectsStream;
        Role userRole = (requestingUser != null) ? requestingUser.getRole() : null;

        if (userRole == Role.HDB_MANAGER) {
            // Managers see all projects matching the filters, regardless of visibility or flat types.
            viewableProjectsStream = potentiallyRelevantProjects.stream();

        } else if (userRole == Role.HDB_OFFICER) {
            // Officers see projects they handle OR projects that are visible.
            // The flat type eligibility rule doesn't strictly apply to their *viewing* capability for handled projects.
            // For non-handled projects, they view like applicants, so eligibility rules apply there.
            final String officerNric = requestingUser.getNric();
            viewableProjectsStream = potentiallyRelevantProjects.stream()
                    .filter(p -> {
                        boolean isHandled = isOfficerAssigned(p, officerNric);
                        if (isHandled) return true; // See handled projects regardless of visibility/type eligibility
                        if (!p.isVisible()) return false; // If not handled, must be visible

                        // If visible & not handled, check applicant flat type eligibility
                        if (requestingUser instanceof Applicant) { // Check if officer object can be cast
                             return isApplicantEligibleToViewProject( (Applicant) requestingUser, p);
                        } else {
                             // Should not happen if model is correct, but default to visible if cast fails
                             return true;
                        }
                    });

        } else { // Applicant role or null (unauthenticated) user
            // Applicants only see visible projects AND projects offering flats they are eligible for.
            viewableProjectsStream = potentiallyRelevantProjects.stream()
                    .filter(Project::isVisible) // Must be visible
                    .filter(p -> { // Must meet eligibility
                        if (requestingUser instanceof Applicant) {
                           return isApplicantEligibleToViewProject((Applicant) requestingUser, p);
                        } else {
                           // Unauthenticated users? Assume they can see all visible projects
                           // Or apply some default eligibility? Let's allow all visible for now.
                           return true;
                        }
                     });
        }

        // 3. Collect the viewable projects into a list
        List<Project> finalViewableProjects = viewableProjectsStream.collect(Collectors.toList());

        // 4. Perform Sorting on the final list
        sortProjects(finalViewableProjects, sortByField, sortAscending);

        return finalViewableProjects;
    }

    private boolean isApplicantEligibleToViewProject(Applicant applicant, Project project) {
        if (applicant == null || project == null) return false; // Or maybe true for project==null? Default false.

        int age = applicant.getAge();
        MaritalStatus status = applicant.getMaritalStatus();

        if (status == MaritalStatus.MARRIED && age >= 21) {
            // Married >= 21 can view any project (that offers any flats)
            return project.getInitialUnitCount(FlatType.TWO_ROOM) > 0 || project.getInitialUnitCount(FlatType.THREE_ROOM) > 0;
        } else if (status == MaritalStatus.SINGLE && age >= 35) {
            // Single >= 35 can ONLY view projects that offer 2-Room flats.
            return project.getInitialUnitCount(FlatType.TWO_ROOM) > 0;
        } else {
            // Includes Single < 35, or Married < 21.
            // PDF rule [cite: 12] is about *applying*. PDF rule [cite: 10] says view based on user group & visibility.
            // Let's interpret this as: If you don't meet the specific application criteria (Married>=21 or Single>=35),
            // you can still VIEW any *visible* project (as per [cite: 10]), but you won't be able to apply later.
            // Therefore, return true here for viewing purposes. The apply logic will block later.
             return project.getInitialUnitCount(FlatType.TWO_ROOM) > 0 || project.getInitialUnitCount(FlatType.THREE_ROOM) > 0;
            // --- OR --- Stricter Interpretation (cannot view if cannot apply):
                // return false; // If Single < 35 or Married < 21 cannot even view? Less likely based on wording.
        }
    }

    public List<Project> getProjectsByManager(HdbManager manager, String neighborhood, FlatType flatType, Date[] dateRange,
                    String sortByField, boolean sortAscending) {
            if (manager == null) return List.of();
            // Use the repository method with the manager NRIC filter applied
            // Manager sees all their projects regardless of visibility (visibilityFilter = null)
            List<Project> filteredProjects = Database.getProjectsRepository()
            .findByCriteria(neighborhood, flatType, manager.getNric(), null, dateRange);

            // Perform Sorting
            sortProjects(filteredProjects, sortByField, sortAscending);

            return filteredProjects;
    }

    private void sortProjects(List<Project> projects, String sortByField, boolean sortAscending) {
        Comparator<Project> comparator = null;
        // Determine the primary comparator based on sortByField
        if (sortByField != null) {
            switch (sortByField.toUpperCase()) {
                case SORT_BY_NEIGHBOURHOOD:
                    comparator = Comparator.comparing(Project::getNeighbourhood, String.CASE_INSENSITIVE_ORDER);
                    break;
                case SORT_BY_MANAGER:
                    comparator = Comparator.comparing(
                        p -> (p.getManager() != null ? p.getManager().getNric() : ""),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    );
                    break;
                case SORT_BY_OPEN_DATE:
                     comparator = Comparator.comparing(
                         Project::getApplicationOpenDate,
                         Comparator.nullsLast(Comparator.naturalOrder())
                     );
                    break;
                case SORT_BY_NAME: // Fallthrough default
                default:
                     comparator = Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER);
                     break;
             }
        } else {
             comparator = Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER); // Default
        }
        // Apply descending order if requested
        if (!sortAscending && comparator != null) {
            comparator = comparator.reversed();
        }
        // Sort the list
        if (comparator != null) {
           projects.sort(comparator);
        }
    }

    private boolean isOfficerAssigned(Project project, String officerNric) {
        if (project == null || officerNric == null) return false;
        // Assumes Project.getAssignedOfficers() returns the list of HdbOfficer objects
        return project.getAssignedOfficers().stream()
                      .anyMatch(officer -> officer.getNric().equalsIgnoreCase(officerNric));
    }

    // --- Helper method for checking manager availability (unchanged) ---
     private boolean isManagerBusyDuringPeriod(String managerNric, Date startDate, Date endDate) {
         // ... unchanged logic using findProjectsInApplicationPeriod ...
           Optional<User> userOpt = Database.getUsersRepository().findUserByNric(managerNric);
           if(userOpt.isEmpty() || !(userOpt.get() instanceof HdbManager)) return false;
           HdbManager manager = (HdbManager) userOpt.get();
           List<Project> managedProjects = Database.getProjectsRepository().findByManagerNric(manager.getNric()); // Use NRIC version

           for (Project existingProject : managedProjects) {
               boolean overlap = !existingProject.getApplicationOpenDate().after(endDate) &&
                                 !existingProject.getApplicationCloseDate().before(startDate);
               if (overlap) return true;
           }
           return false;
     }
    
}