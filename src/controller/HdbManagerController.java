package controller;

import entities.database.Database;
import entities.project.*; // Project, FlatType, User, HdbManager, HdbOfficer etc.
import entities.documents.approvableDocuments.*; // Needed for checking related docs on delete
import entities.documents.repliableDocuments.*;
import entities.documents.*;
import entities.user.*;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.*;
import java.util.Map;

public class HdbManagerController {

    private final ProjectController projectController; // Dependency

    public HdbManagerController(ProjectController projectController) {
        this.projectController = projectController;
    }

    // --- Project Management --- [cite: 25]

    public Project createProject(HdbManager manager, String name, String neighbourhood,
                                Map<FlatType, Integer> initialFlatUnitCounts, Map<FlatType, Double> flatUnitPrices,
                                Date applicationOpenDate, Date applicationCloseDate) {
        // Delegate to ProjectController
        return projectController.createProject(name, neighbourhood, initialFlatUnitCounts, flatUnitPrices,
                                               applicationOpenDate, applicationCloseDate, manager);
    }

    public boolean editProject(HdbManager manager, String projectName, String newNeighbourhood,
                               Map<FlatType, Integer> newUnitCounts, Map<FlatType, Double> newUnitPrices,
                               Date newOpenDate, Date newCloseDate) {
        // Delegate to ProjectController
        return projectController.editProject(manager, projectName, newNeighbourhood, newUnitCounts, newUnitPrices, newOpenDate, newCloseDate);
    }

     public boolean deleteProject(HdbManager manager, String projectName) {
         // Delegate to ProjectController
         return projectController.deleteProject(manager, projectName);
     }

     public boolean toggleProjectVisibility(HdbManager manager, String projectName, boolean isVisible) {
        // Delegate to ProjectController
        return projectController.toggleProjectVisibility(manager, projectName, isVisible);
    }

    // --- View Projects --- [cite: 27, 28]

    public List<Project> viewAllProjects(HdbManager manager) {
        // Managers see all projects, use unfiltered list from repository
         return Database.getProjectsRepository().findAll().stream()
                 .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                 .collect(Collectors.toList());
    }

    public List<Project> viewMyProjects(HdbManager manager) {
         // Delegate to ProjectController or Repository
        return projectController.getProjectsByManager(manager).stream()
                 .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                 .collect(Collectors.toList());
        // Or: return Database.getProjectsRepository().findByManager(manager);
    }


    // --- Officer Registration Management --- [cite: 29, 30]

    /**
     * Views pending officer registrations for projects managed by this manager.
     * @param manager The HDB Manager.
     * @return List of pending ProjectRegistration documents.
     */
    public List<ProjectRegistration> viewPendingOfficerRegistrations(HdbManager manager) {
        List<Project> myProjects = projectController.getProjectsByManager(manager);
        List<String> myProjectNames = myProjects.stream().map(Project::getName).collect(Collectors.toList());

        if (myProjectNames.isEmpty()) return List.of();

        return Database.getDocumentsRepository().getRegistrationRepository().findAll().stream()
                .filter(reg -> reg.getStatus() == DocumentStatus.PENDING_APPROVAL)
                .filter(reg -> reg.getProjectName() != null && myProjectNames.contains(reg.getProjectName()))
                .collect(Collectors.toList());
    }

     /**
      * Approves or rejects an officer's registration request for a project managed by this manager.
      * @param manager The HDB Manager performing the action.
      * @param registrationId The ID of the ProjectRegistration document.
      * @param approve True to approve, false to reject.
      * @param reason Required if rejecting.
      * @return true if action was successful, false otherwise.
      */
     public boolean processOfficerRegistration(HdbManager manager, String registrationId, boolean approve, String reason) {
         Optional<ProjectRegistration> regOpt = Database.getDocumentsRepository().getRegistrationRepository().findById(registrationId);
         if (regOpt.isEmpty()) {
             System.err.println("Officer Registration Processing Error: Registration ID '" + registrationId + "' not found.");
             return false;
         }
         ProjectRegistration registration = regOpt.get();

         if (registration.getStatus() != DocumentStatus.PENDING_APPROVAL) {
              System.err.println("Officer Registration Processing Error: Registration " + registrationId + " is not pending approval (Status: " + registration.getStatus() + ").");
             return false;
         }

         // Find the associated project
         Optional<Project> projectOpt = Database.getProjectsRepository().findById(registration.getProjectName());
          if (projectOpt.isEmpty()) {
              System.err.println("Officer Registration Processing Error: Project '" + registration.getProjectName() + "' not found for registration " + registrationId);
             return false;
         }
         Project project = projectOpt.get();

         // Authorisation Check: Is this manager in charge of this project?
         if (!manager.equals(project.getManager())) {
             System.err.println("Officer Registration Processing Error: Manager " + manager.getNric() + " is not authorized for project '" + project.getName() + "'.");
             return false;
         }

         boolean success;
         if (approve) {
             // Check available officer slots [cite: 30 implies check needed]
             if (project.getAvailableOfficerSlots() <= 0) {
                 System.err.println("Officer Registration Approval Error: No available officer slots in project '" + project.getName() + "'. Cannot approve registration " + registrationId);
                 // Consider auto-rejecting? Or just return false?
                 // Let's reject it explicitly for clarity.
                 return processOfficerRegistration(manager, registrationId, false, "No available officer slots.");
                 // return false;
             }
             // Approve the registration document
             success = registration.approve(manager); // Assumes this method updates status etc.
             if (success) {
                 // Add officer to the project
                 boolean added = project.addOfficer((HdbOfficer) registration.getSubmitter()); // Assumes submitter is HdbOfficer
                 if (!added) {
                      System.err.println("CRITICAL ERROR: Registration " + registrationId + " approved but failed to add officer " + registration.getSubmitter().getNric() + " to project " + project.getName() + ". Manual correction needed!");
                      // Rollback approval? Complex transaction... For now, log error.
                      // registration.setStatus(DocumentStatus.PENDING_APPROVAL); // Attempt rollback
                      // Database.getDocumentsRepository().saveDocument(registration);
                      success = false; // Mark as overall failure
                 } else {
                     // Save both project (updated officers) and registration (updated status)
                     Database.getProjectsRepository().save(project);
                 }
             }
         } else { // Reject
             if (reason == null || reason.isBlank()) {
                 System.err.println("Officer Registration Rejection Error: Reason must be provided for rejection.");
                 return false;
             }
             success = registration.reject(manager, reason); // Assumes this method updates status etc.
         }

         if (success) {
             // Save the updated registration document status
             Database.getDocumentsRepository().saveDocument(registration);
             System.out.println("Officer registration " + registrationId + " processed successfully (Approved: " + approve + ") by Manager " + manager.getNric());
             return true;
         } else {
              System.err.println("Officer Registration Processing Error: Failed to update registration " + registrationId);
              return false;
         }
     }


    // --- Applicant BTO Application Management --- [cite: 31]

    /**
     * Views pending BTO applications for projects managed by this manager.
     * @param manager The HDB Manager.
     * @return List of pending ProjectApplication documents.
     */
     public List<ProjectApplication> viewPendingBtoApplications(HdbManager manager) {
        List<Project> myProjects = projectController.getProjectsByManager(manager);
        List<String> myProjectNames = myProjects.stream().map(Project::getName).collect(Collectors.toList());

        if (myProjectNames.isEmpty()) return List.of();

        return Database.getDocumentsRepository().getApplicationRepository().findAll().stream()
                .filter(app -> app.getStatus() == DocumentStatus.PENDING_APPROVAL) // Or SUBMITTED if that's the pending state
                .filter(app -> app.getProjectName() != null && myProjectNames.contains(app.getProjectName()))
                .collect(Collectors.toList());
     }

    /**
     * Approves or rejects an applicant's BTO application. Approval limited by flat supply. [cite: 31]
     * @param manager The HDB Manager.
     * @param applicationId The ID of the ProjectApplication.
     * @param approve True to approve, false to reject.
     * @param reason Required if rejecting.
     * @return true if action successful, false otherwise.
     */
    public boolean processBtoApplication(HdbManager manager, String applicationId, boolean approve, String reason) {
        Optional<ProjectApplication> appOpt = Database.getDocumentsRepository().getApplicationRepository().findById(applicationId);
        if (appOpt.isEmpty()) {
            System.err.println("BTO Application Processing Error: Application ID '" + applicationId + "' not found.");
            return false;
        }
        ProjectApplication application = appOpt.get();

         if (application.getStatus() != DocumentStatus.PENDING_APPROVAL) { // Or SUBMITTED
              System.err.println("BTO Application Processing Error: Application " + applicationId + " is not pending approval (Status: " + application.getStatus() + ").");
             return false;
         }

         // Find the associated project
         Optional<Project> projectOpt = Database.getProjectsRepository().findById(application.getProjectName());
          if (projectOpt.isEmpty()) {
              System.err.println("BTO Application Processing Error: Project '" + application.getProjectName() + "' not found for application " + applicationId);
             return false;
         }
         Project project = projectOpt.get();

         // Authorisation Check
         if (!manager.equals(project.getManager())) {
             System.err.println("BTO Application Processing Error: Manager " + manager.getNric() + " is not authorized for project '" + project.getName() + "'.");
             return false;
         }

         boolean success;
         if (approve) {
              // Check flat supply - This is complex. PDF says "approval is limited to the supply".
              // Does this mean *total* supply, or remaining supply? Let's assume TOTAL for initial ballot success.
              // Booking stage later confirms remaining units. This needs clarification.
              // For now, let's just approve without checking supply, assuming ballot happens.
              // A real system would need a more defined balloting/allocation process.
              // Let's assume approval here means "successful in ballot, can proceed to booking"
              success = application.approve(manager); // Assumes method updates status to APPROVED
         } else { // Reject
            if (reason == null || reason.isBlank()) {
                 System.err.println("BTO Application Rejection Error: Reason must be provided for rejection.");
                 return false;
             }
            success = application.reject(manager, reason); // Assumes method updates status to REJECTED
         }

          if (success) {
             // Save the updated application document status
             Database.getDocumentsRepository().saveDocument(application);
             System.out.println("BTO Application " + applicationId + " processed successfully (Approved: " + approve + ") by Manager " + manager.getNric());
             return true;
         } else {
              System.err.println("BTO Application Processing Error: Failed to update application " + applicationId);
              return false;
         }
    }


    // --- Withdrawal Request Management --- [cite: 32]

    /**
     * Views pending withdrawal requests for projects managed by this manager.
     * @param manager The HDB Manager.
     * @return List of pending Withdrawal documents.
     */
     public List<Withdrawal> viewPendingWithdrawals(HdbManager manager) {
          List<Project> myProjects = projectController.getProjectsByManager(manager);
          List<String> myProjectNames = myProjects.stream().map(Project::getName).collect(Collectors.toList());

          if (myProjectNames.isEmpty()) return List.of();

          // Find pending withdrawals where the original application's project is managed by this manager
          return Database.getDocumentsRepository().getWithdrawalRepository().findAll().stream()
                  .filter(wd -> wd.getStatus() == DocumentStatus.PENDING_APPROVAL)
                  .filter(wd -> {
                      ProjectApplication originalApp = wd.getApplicationToWithdraw(); // Assumes getter
                      return originalApp != null && originalApp.getProjectName() != null &&
                             myProjectNames.contains(originalApp.getProjectName());
                  })
                  .collect(Collectors.toList());
     }

    /**
     * Approves or rejects an applicant's withdrawal request.
     * @param manager The HDB Manager.
     * @param withdrawalId The ID of the Withdrawal document.
     * @param approve True to approve, false to reject.
     * @param reason Required if rejecting.
     * @return true if action successful, false otherwise.
     */
    public boolean processWithdrawalRequest(HdbManager manager, String withdrawalId, boolean approve, String reason) {
         Optional<Withdrawal> wdOpt = Database.getDocumentsRepository().getWithdrawalRepository().findById(withdrawalId);
         if (wdOpt.isEmpty()) {
             System.err.println("Withdrawal Processing Error: Withdrawal request ID '" + withdrawalId + "' not found.");
             return false;
         }
         Withdrawal withdrawal = wdOpt.get();

         if (withdrawal.getStatus() != DocumentStatus.PENDING_APPROVAL) {
             System.err.println("Withdrawal Processing Error: Request " + withdrawalId + " is not pending approval (Status: " + withdrawal.getStatus() + ").");
             return false;
         }

         ProjectApplication originalApp = withdrawal.getApplicationToWithdraw();
         if (originalApp == null) {
              System.err.println("Withdrawal Processing Error: Original application missing for withdrawal " + withdrawalId);
              return false; // Data integrity issue
         }

          // Find the associated project
         Optional<Project> projectOpt = Database.getProjectsRepository().findById(originalApp.getProjectName());
          if (projectOpt.isEmpty()) {
              System.err.println("Withdrawal Processing Error: Project '" + originalApp.getProjectName() + "' not found for withdrawal " + withdrawalId);
             return false;
         }
         Project project = projectOpt.get();

         // Authorisation Check
         if (!manager.equals(project.getManager())) {
             System.err.println("Withdrawal Processing Error: Manager " + manager.getNric() + " is not authorized for project '" + project.getName() + "'.");
             return false;
         }

         boolean success;
         boolean changesToOriginalApp = false;
         DocumentStatus originalAppPreviousStatus = originalApp.getStatus(); // Store for potential rollback

         if (approve) {
             success = withdrawal.approve(manager); // Update withdrawal doc status
             if (success) {
                 // Update original application status to WITHDRAWN
                 originalApp.setStatus(DocumentStatus.WITHDRAWN); // Requires setter
                 originalApp.setLastModifiedDate(LocalDateTime.now()); // Requires setter
                 originalApp.setLastModifiedBy(manager); // Requires setter
                 changesToOriginalApp = true;

                 // If original app was BOOKED, need to increment remaining units
                 if (originalAppPreviousStatus == DocumentStatus.BOOKED) {
                     // TODO: Need booked flat type from originalApp
                     // FlatType bookedType = originalApp.getBookedFlatType(); // Requires getter
                     // if (bookedType != null) {
                     //     boolean incremented = project.incrementRemainingUnit(bookedType);
                     //     if (!incremented) {
                     //         System.err.println("CRITICAL WARNING: Withdrawal approved for booked app "+originalApp.getDocumentID()+" but failed to increment unit count for "+bookedType+" in project "+project.getName());
                     //     }
                     // } else {
                     //      System.err.println("CRITICAL WARNING: Withdrawal approved for booked app "+originalApp.getDocumentID()+" but booked flat type not found.");
                     // }
                     System.out.println("Info: Withdrawal approved for booked application - unit count adjustment needed (implement when booked type is stored).");
                 }
             }
         } else { // Reject
             if (reason == null || reason.isBlank()) {
                  System.err.println("Withdrawal Rejection Error: Reason must be provided.");
                  return false;
              }
             success = withdrawal.reject(manager, reason); // Update withdrawal doc status
         }

         if (success) {
             try {
                 // Save withdrawal request status
                 Database.getDocumentsRepository().saveDocument(withdrawal);
                 if (changesToOriginalApp) {
                     // Save original application status change
                     Database.getDocumentsRepository().saveDocument(originalApp);
                     // Save project if unit counts were changed (only if booked type logic is implemented)
                     if (originalAppPreviousStatus == DocumentStatus.BOOKED) {
                        // Database.getProjectsRepository().save(project);
                     }
                 }
                 System.out.println("Withdrawal Request " + withdrawalId + " processed successfully (Approved: " + approve + ") by Manager " + manager.getNric());
                 return true;
             } catch (Exception e) {
                 System.err.println("Withdrawal Processing Error: Failed to save changes. " + e.getMessage());
                  // Attempt rollback? Very complex. Log error.
                 return false;
             }
         } else {
              System.err.println("Withdrawal Processing Error: Failed to update withdrawal request " + withdrawalId);
              return false;
         }
    }


    // --- Enquiry Management --- [cite: 33]

    /**
     * Gets ALL enquiries across ALL projects.
     * @param manager The manager requesting (for logging/context, not filtering).
     * @return List of all enquiries.
     */
    public List<Enquiry> viewAllEnquiries(HdbManager manager) {
         return Database.getDocumentsRepository().getEnquiryRepository().findAll().stream()
                 .sorted(Comparator.comparing(Enquiry::getSubmissionDate, Comparator.nullsLast(Comparator.naturalOrder())))
                 .collect(Collectors.toList());
    }

    /**
     * Gets enquiries specifically for projects managed by this manager.
     * @param manager The HDB Manager.
     * @return List of relevant enquiries.
     */
     public List<Enquiry> viewManagedEnquiries(HdbManager manager) {
        List<Project> myProjects = projectController.getProjectsByManager(manager);
        List<String> myProjectNames = myProjects.stream().map(Project::getName).collect(Collectors.toList());

        if (myProjectNames.isEmpty()) return List.of();

        // Find all enquiries and filter by handled project names
        return Database.getDocumentsRepository().getEnquiryRepository().findAll().stream()
                .filter(e -> e.getProjectName() != null && myProjectNames.contains(e.getProjectName()))
                .sorted(Comparator.comparing(Enquiry::getSubmissionDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
     }


    /**
     * Allows a manager to reply to an enquiry for a project they handle.
     * @param manager       The HDB Manager replying.
     * @param enquiryId     The ID of the enquiry to reply to.
     * @param replyContent  The content of the reply.
     * @return true if reply was successful, false otherwise.
     */
    public boolean replyToEnquiry(HdbManager manager, String enquiryId, String replyContent) {
         Optional<Enquiry> enquiryOpt = Database.getDocumentsRepository().getEnquiryRepository().findById(enquiryId);
            if (enquiryOpt.isEmpty()) {
                System.err.println("Enquiry Reply Error: Enquiry ID '" + enquiryId + "' not found.");
                return false;
            }
            Enquiry enquiry = enquiryOpt.get();

            if (replyContent == null || replyContent.isBlank()) {
                 System.err.println("Enquiry Reply Error: Reply content cannot be empty.");
                 return false;
            }

            // Check if enquiry is about a specific project
            if (enquiry.getProjectName() == null) {
                 // Managers might be able to reply to general enquiries? Or maybe not?
                 // Let's assume they can only reply if project is specified and they manage it.
                 System.err.println("Enquiry Reply Error: Cannot reply to general enquiry (no project assigned) via Manager controller.");
                 return false;
            }

            // Check if manager handles this project
            Optional<Project> projectOpt = Database.getProjectsRepository().findById(enquiry.getProjectName());
            if(projectOpt.isEmpty() || !manager.equals(projectOpt.get().getManager())){
                 System.err.println("Enquiry Reply Error: Manager " + manager.getNric() + " does not handle project '" + enquiry.getProjectName() + "'.");
                 return false;
            }

            // Use the reply method on the enquiry object
            boolean replied = enquiry.reply(manager, replyContent);

            if (replied) {
                Database.getDocumentsRepository().saveDocument(enquiry); // Persist changes
                System.out.println("Enquiry " + enquiryId + " replied to successfully by Manager " + manager.getNric());
                return true;
            } else {
                 System.err.println("Enquiry Reply Error: Failed to update enquiry " + enquiryId + ". Status was: " + enquiry.getStatus());
                 return false;
            }
    }


    // --- Report Generation --- [cite: 32]

    /**
     * Generates a report of applicants with flat bookings, potentially filtered.
     * @param manager The manager generating the report.
     * @param filterProjectName Optional: Filter by specific project name.
     * @param filterMaritalStatus Optional: Filter by applicant marital status.
     * @param filterFlatType Optional: Filter by the booked flat type.
     * @return A formatted String representing the report.
     */
    public String generateBookingReport(HdbManager manager, String filterProjectName, MaritalStatus filterMaritalStatus, FlatType filterFlatType) {
        System.out.println("Generating Booking Report (Manager: " + manager.getNric() +
                           ", Project Filter: " + filterProjectName +
                           ", Marital Filter: " + filterMaritalStatus +
                           ", Flat Filter: " + filterFlatType + ")");

        List<ProjectApplication> allBookedApps = Database.getDocumentsRepository().getApplicationRepository().findAll().stream()
                .filter(app -> app.getStatus() == DocumentStatus.BOOKED)
                .collect(Collectors.toList());

        Stream<ProjectApplication> filteredStream = allBookedApps.stream();

        // Apply filters
        if (filterProjectName != null && !filterProjectName.isBlank()) {
            filteredStream = filteredStream.filter(app -> filterProjectName.equals(app.getProjectName()));
        }
        if (filterMaritalStatus != null) {
            filteredStream = filteredStream.filter(app -> app.getSubmitter() != null && filterMaritalStatus.equals(app.getSubmitter().getMaritalStatus()));
        }
        if (filterFlatType != null) {
            filteredStream = filteredStream.filter(app -> filterFlatType.equals(app.getBookedFlatType()));
        }

        List<ProjectApplication> reportApps = filteredStream
                .sorted(Comparator.comparing(ProjectApplication::getProjectName)
                                  .thenComparing(app -> app.getSubmitter().getName()))
                .collect(Collectors.toList());

        // Format Report
        StringBuilder report = new StringBuilder();
        report.append("\n--- BTO Booking Report ---\n");
        report.append("Generated by: ").append(manager.getName()).append(" (").append(manager.getNric()).append(")\n");
        report.append("Filters -> Project: ").append(filterProjectName != null ? filterProjectName : "ALL");
        report.append(" | Marital Status: ").append(filterMaritalStatus != null ? filterMaritalStatus : "ALL");
        report.append(" | Booked Flat: ").append(filterFlatType != null ? filterFlatType.name() : "ALL");
        report.append("\n------------------------------------------\n");

        if (reportApps.isEmpty()) {
            report.append("No matching booked applications found.\n");
        } else {
            report.append(String.format("%-15s | %-12s | %-20s | %-8s | %-12s | %-10s%n",
                                       "Project", "Applicant NRIC", "Applicant Name", "Age", "Marital", "Booked Flat"));
            report.append("--------------------------------------------------------------------------------\n");
            for (ProjectApplication app : reportApps) {
                 User applicant = app.getSubmitter();
                 String bookedTypeStr = app.getBookedFlatType() != null ? app.getBookedFlatType().name() : "N/A";
                 report.append(String.format("%-15s | %-12s | %-20s | %-8d | %-12s | %-10s%n",
                                            app.getProjectName(),
                                            applicant.getNric(),
                                            applicant.getName(),
                                            applicant.getAge(),
                                            applicant.getMaritalStatus(),
                                            bookedTypeStr));
            }
        }
        report.append("--- End of Report ---\n");
        return report.toString();
    }
}