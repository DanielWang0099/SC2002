package controller;

import entities.database.Database;
import entities.project.*; // Project, FlatType, User, HdbManager, HdbOfficer etc.
import entities.documents.approvableDocuments.*; // Needed for checking related docs on delete
import entities.documents.repliableDocuments.*;
import entities.documents.*;
import entities.user.*;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.*;


public class HdbOfficerController {

    private final ProjectController projectController;
    private final ApplicantController applicantController; // To reuse applicant functions

    public HdbOfficerController(ProjectController projectController, ApplicantController applicantController) {
        this.projectController = projectController;
        this.applicantController = applicantController;
    }

    // --- Officer Specific Actions ---

    /**
     * Allows an HDB Officer to register interest in managing a specific project.
     * Performs eligibility checks. [cite: 18]
     * @param officer     The HDB Officer user.
     * @param projectName The name of the project to register for.
     * @return The created ProjectRegistration object if successful, null otherwise.
     */
    public ProjectRegistration registerForProjectTeam(HdbOfficer officer, String projectName) {
        // 1. Find Project
        Optional<Project> projectOpt = Database.getProjectsRepository().findById(projectName);
        if (projectOpt.isEmpty()) {
            System.err.println("Registration Error: Project '" + projectName + "' not found.");
            return null;
        }
        Project project = projectOpt.get();

        // 2. Check Eligibility [cite: 18]
        if (!isOfficerEligibleToRegister(officer, project)) {
             // Error message printed within helper method
             return null;
        }

         // 3. Check if officer already registered for this project
         List<ProjectRegistration> existingRegs = Database.getDocumentsRepository().getRegistrationRepository().findByOfficerNric(officer.getNric());
         boolean alreadyRegistered = existingRegs.stream()
                                         .filter(reg -> reg.getProjectName() != null && reg.getProjectName().equals(projectName))
                                         .anyMatch(reg -> reg.getStatus() != DocumentStatus.REJECTED && reg.getStatus() != DocumentStatus.CLOSED); // Check active/pending registration
         if (alreadyRegistered) {
              System.err.println("Registration Error: Officer " + officer.getNric() + " has already registered for project '" + projectName + "'.");
              return null;
         }


        // 4. Create and Save Registration Request
        ProjectRegistration registration = ProjectRegistration.createNewProjectRegistration(officer, project);
        registration.submit(officer); // Set status to PENDING_APPROVAL

        Database.getDocumentsRepository().saveDocument(registration);
        System.out.println("Registration request submitted by Officer " + officer.getNric() + " for project '" + projectName + "'. ID: " + registration.getDocumentID());
        return registration;
    }

    /**
     * Retrieves registration requests submitted by the officer.
     * @param officer The HDB Officer.
     * @return List of their registration requests.
     */
    public List<ProjectRegistration> viewMyRegistrations(HdbOfficer officer) {
        return Database.getDocumentsRepository().getRegistrationRepository().findByOfficerNric(officer.getNric());
    }


    /**
     * Gets details of projects the officer is *handling* (approved registration).
     * Ignores visibility settings for these projects. [cite: 21]
     * @param officer The HDB Officer.
     * @return List of projects the officer is handling.
     */
    public List<Project> viewHandledProjects(HdbOfficer officer) {
        // 1. Find all APPROVED registrations for this officer
        List<ProjectRegistration> approvedRegistrations = Database.getDocumentsRepository().getRegistrationRepository()
                .findByOfficerNric(officer.getNric()).stream()
                .filter(reg -> reg.getStatus() == DocumentStatus.APPROVED)
                .collect(Collectors.toList());

        // 2. Get the corresponding projects from the registrations
        return approvedRegistrations.stream()
                .map(ProjectRegistration::getProjectName) // Assumes getter exists
                .filter(Objects::nonNull)
                .distinct()
                .map(projectName -> Database.getProjectsRepository().findById(projectName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

     /**
      * Simulates the flat selection process performed by an officer for a successful applicant.
      * Updates application status, project unit counts. [cite: 23]
      * @param officer The HDB Officer performing the action.
      * @param application The successful ProjectApplication.
      * @param chosenFlatType The FlatType chosen by the applicant.
      * @return true if booking successful, false otherwise.
      */
     public boolean processFlatBooking(HdbOfficer officer, ProjectApplication application, FlatType chosenFlatType) {
         if (application == null || chosenFlatType == null || officer == null) {
             System.err.println("Booking Error: Invalid input.");
             return false;
         }
         Applicant applicant = (Applicant) application.getSubmitter();
         if (applicant == null) { System.err.println("Booking Error: Applicant missing from application."); return false; }


         // 1. Validate Application Status (Must be APPROVED - or whatever status signifies "invited to select")
         if (application.getStatus() != DocumentStatus.APPROVED) { // Assuming APPROVED means ready for booking
              System.err.println("Booking Error: Application " + application.getDocumentID() + " is not in the correct state ("+application.getStatus()+") for booking.");
              return false;
         }

                  // *** ADDED: Check if this applicant ALREADY HAS another booked flat ***
        Optional<ProjectApplication> existingBooking = Database.getDocumentsRepository()
                  .getApplicationRepository()
                  .findBookedApplicationByApplicantNric(applicant.getNric());
 

        if (existingBooking.isPresent() && !existingBooking.get().getDocumentID().equals(application.getDocumentID())) {
               System.err.println("Booking Error: Applicant " + applicant.getNric() +
                                  " already has a booked flat (Application ID: " + existingBooking.get().getDocumentID() +
                                  " for Project: " + existingBooking.get().getProjectName() +
                                  "). Cannot book another flat.");
              return false; // Cannot book if already booked elsewhere
        }

         // 2. Find the Project
         Optional<Project> projectOpt = Database.getProjectsRepository().findById(application.getProjectName());
         if (projectOpt.isEmpty()) {
              System.err.println("Booking Error: Project '" + application.getProjectName() + "' associated with application not found.");
             return false;
         }
         Project project = projectOpt.get();

         // 3. Check Officer Authorization (Is this officer assigned to this project?)
         boolean assigned = project.getAssignedOfficers().stream().anyMatch(o -> o.equals(officer));
         if (!assigned) {
              System.err.println("Booking Error: Officer " + officer.getNric() + " is not assigned to handle project '" + project.getName() + "'.");
              return false;
         }

          // 4. Check Flat Type Eligibility (Redundant check? ApplicantController should ensure initial eligibility)
          if (!applicantController.checkEligibility(applicant, chosenFlatType)) {
              System.err.println("Booking Error: Applicant " + applicant.getNric() + " is not eligible for flat type " + chosenFlatType);
              return false;
          }

         // 5. Check Unit Availability and Decrement [cite: 23]
         if (!project.decrementRemainingUnit(chosenFlatType)) {
             System.err.println("Booking Error: No remaining units of type " + chosenFlatType + " available in project '" + project.getName() + "'.");
             return false;
         }

        // 6. Update Application Status to Booked AND SET BOOKED TYPE
        application.setStatus(DocumentStatus.BOOKED);
        application.setBookedFlatType(chosenFlatType); // <-- ADDED THIS LINE
        application.setLastModifiedDate(LocalDateTime.now());
        application.setLastModifiedByNric(officer.getNric());
        application.setLastModifiedBy(officer); // Update transient field too if used

        // 7. Save Changes
        try {
            Database.getProjectsRepository().save(project);
            Database.getDocumentsRepository().saveDocument(application);
            System.out.println("Booking successful for Application " + application.getDocumentID() + ". Flat Type: " + chosenFlatType + ". Processed by Officer " + officer.getNric());
            return true;
        } catch (Exception e) {
            // ... (Rollback attempt) ...
            System.err.println("Booking Error: Failed to save changes after booking. " + e.getMessage());
            // Attempt simple rollback
            project.incrementRemainingUnit(chosenFlatType);
            application.setStatus(DocumentStatus.APPROVED);
            application.setBookedFlatType(null); // Rollback booked type
            // Try saving reverted state
            try {
                Database.getProjectsRepository().save(project);
                Database.getDocumentsRepository().saveDocument(application);
            } catch (Exception ex) {
                System.err.println("CRITICAL Error during booking rollback save: " + ex.getMessage());
            }
            return false;
        }
     }

     /**
      * Generates receipt details for a booked application. [cite: 23]
      * In a real app, this might format a string or return a dedicated Receipt object.
      * @param officer The HDB Officer.
      * @param application The booked ProjectApplication.
      * @return A string containing receipt details, or null if invalid input/state.
      */
     public String generateBookingReceipt(HdbOfficer officer, ProjectApplication application) {
          if (application == null || application.getStatus() != DocumentStatus.BOOKED) {
               System.err.println("Receipt Error: Application not found or not in BOOKED state.");
               return null;
          }
          // Added: Retrieve project and verify officer is assigned to it
          Optional<Project> projectOpt = Database.getProjectsRepository().findById(application.getProjectName());
          if (projectOpt.isEmpty()) {
               System.err.println("Receipt Error: Project data missing for application " + application.getDocumentID());
               return null;
          }
          Project project = projectOpt.get();
          if (project.getAssignedOfficers().stream().noneMatch(o -> o.equals(officer))) {
              System.err.println("Receipt Error: Officer " + officer.getNric() + " is not assigned to project '" + project.getName() + "'.");
              return null;
          }
          User applicant = application.getSubmitter();
          if (applicant == null) {
               System.err.println("Receipt Error: Applicant data missing for application " + application.getDocumentID());
               return null;
          }
          // Added: Retrieve booked flat type
          FlatType bookedType = application.getBookedFlatType();
          
          // Format the receipt string
          StringBuilder receipt = new StringBuilder();
          receipt.append("\n--- BTO Flat Booking Receipt ---");
          receipt.append("\nApplication ID: ").append(application.getDocumentID());
          receipt.append("\nApplicant Name: ").append(applicant.getName());
          receipt.append("\nApplicant NRIC: ").append(applicant.getNric());
          receipt.append("\nApplicant Age: ").append(applicant.getAge());
          receipt.append("\nApplicant Marital Status: ").append(applicant.getMaritalStatus());
          receipt.append("\n--- Project Details ---");
          receipt.append("\nProject Name: ").append(project.getName());
          receipt.append("\nNeighbourhood: ").append(project.getNeighbourhood());
          receipt.append("\nBooked Flat Type: ").append(bookedType);
          receipt.append("\nBooking confirmed on: ").append(application.getLastModifiedDate());
          receipt.append("\n--- End of Receipt ---");
          return receipt.toString();
     }


    // --- Enquiry Management --- [cite: 22]

     /**
      * Gets enquiries for projects handled by the officer.
      * @param officer The HDB officer.
      * @return List of relevant enquiries.
      */
     public List<Enquiry> getHandledEnquiries(HdbOfficer officer) {
          List<Project> handledProjects = viewHandledProjects(officer); // Get projects they handle
          List<String> handledProjectNames = handledProjects.stream().map(Project::getName).collect(Collectors.toList());

          if (handledProjectNames.isEmpty()) {
               return List.of(); // Return empty list if no projects handled
          }

          // Find all enquiries and filter by handled project names
          return Database.getDocumentsRepository().getEnquiryRepository().findAll().stream()
                  .filter(e -> e.getProjectName() != null && handledProjectNames.contains(e.getProjectName()))
                  .sorted(Comparator.comparing(Enquiry::getSubmissionDate, Comparator.nullsLast(Comparator.naturalOrder()))) // Sort by submission date
                  .collect(Collectors.toList());
     }

      /**
       * Allows an officer to reply to an enquiry for a project they handle.
       * @param officer       The HDB Officer replying.
       * @param enquiryId     The ID of the enquiry to reply to.
       * @param replyContent  The content of the reply.
       * @return true if reply was successful, false otherwise.
       */
      public boolean replyToEnquiry(HdbOfficer officer, String enquiryId, String replyContent) {
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
                 System.err.println("Enquiry Reply Error: Cannot reply to general enquiry (no project assigned) via Officer controller.");
                 // Or handle general enquiries differently? Maybe only Managers?
                 return false;
            }

            // Check if officer handles this project
            List<Project> handledProjects = viewHandledProjects(officer);
            boolean handlesProject = handledProjects.stream().anyMatch(p -> p.getName().equals(enquiry.getProjectName()));
            if (!handlesProject) {
                System.err.println("Enquiry Reply Error: Officer " + officer.getNric() + " does not handle project '" + enquiry.getProjectName() + "'.");
                return false;
            }

            // Use the reply method on the enquiry object
            boolean replied = enquiry.reply(officer, replyContent); // Assumes Enquiry.reply updates status, replier, content, dates

            if (replied) {
                Database.getDocumentsRepository().saveDocument(enquiry); // Persist changes
                System.out.println("Enquiry " + enquiryId + " replied to successfully by Officer " + officer.getNric());
                return true;
            } else {
                 System.err.println("Enquiry Reply Error: Failed to update enquiry " + enquiryId + ". Status was: " + enquiry.getStatus());
                 return false;
            }
      }


    // --- Applicant Capability Access --- [cite: 9]
    // Delegate calls to ApplicantController

    public List<Project> getAvailableProjectsForViewing(HdbOfficer officer) {
        // Officers view projects like applicants, but also see handled ones regardless of visibility
         List<Project> visibleProjects = projectController.getFilteredProjects(officer, null, null, null, null, null, true);
         // viewHandledProjects already gets projects regardless of visibility
         // Combine and distinct
         List<Project> handled = viewHandledProjects(officer);
         return Stream.concat(visibleProjects.stream(), handled.stream())
                      .distinct() // Ensure no duplicates if handled project is also visible
                      .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                      .collect(Collectors.toList());
    }

    // Reuse applicant apply logic - BUT officer cannot apply for project they handle [cite: 18, 20]
    public ProjectApplication applyForProjectAsApplicant(HdbOfficer officer, String projectName) {
        // Check if officer handles this project
         List<Project> handledProjects = viewHandledProjects(officer);
         boolean handlesThisProject = handledProjects.stream().anyMatch(p -> p.getName().equals(projectName));
         if(handlesThisProject){
              System.err.println("Application Error: Officer " + officer.getNric() + " cannot apply for project '" + projectName + "' as they are handling it.");
              return null;
         }
         // Check if officer registered for this project (even if not approved yet?) PDF implies intention matters [cite: 18]
         boolean registeredForThisProject = Database.getDocumentsRepository().getRegistrationRepository()
                                             .findByOfficerNric(officer.getNric()).stream()
                                             .filter(reg -> reg.getProjectName() != null && reg.getProjectName().equals(projectName))
                                             .anyMatch(reg -> reg.getStatus() != DocumentStatus.REJECTED && reg.getStatus() != DocumentStatus.CLOSED);
         if(registeredForThisProject){
               System.err.println("Application Error: Officer " + officer.getNric() + " cannot apply for project '" + projectName + "' as they have registered for it.");
              return null;
         }


        // Delegate to applicant controller if eligible
        Applicant applicantView = officer; // Treat officer as applicant for this context
        return applicantController.applyForProject(applicantView, projectName);
    }

     public List<ProjectApplication> viewMyApplicationsAsApplicant(HdbOfficer officer) {
        return applicantController.viewMyApplications(officer);
    }

     public Withdrawal requestWithdrawalAsApplicant(HdbOfficer officer, String applicationId) {
        return applicantController.requestWithdrawal(officer, applicationId);
    }

     // Enquiries: Officer uses getHandledEnquiries and replyToEnquiry for project-specific ones.
     // If they need to manage their *own* submitted enquiries (as an applicant), use applicantController.
     public Enquiry createEnquiryAsApplicant(HdbOfficer officer, String projectName, String content){
         return applicantController.createEnquiry(officer, projectName, content);
     }
     public List<Enquiry> viewMySubmittedEnquiries(HdbOfficer officer){
         return applicantController.viewMyEnquiries(officer);
     }
      public boolean editEnquiryAsApplicant(HdbOfficer officer, String enquiryId, String newContent){
         return applicantController.editEnquiry(officer, enquiryId, newContent);
     }
      public boolean deleteEnquiryAsApplicant(HdbOfficer officer, String enquiryId){
         return applicantController.deleteEnquiry(officer, enquiryId);
     }


    // --- Helper ---
    private boolean isOfficerEligibleToRegister(HdbOfficer officer, Project project) {
        String officerNric = officer.getNric();
        String projectName = project.getName();

        // 1. Check if applied for the project as Applicant [cite: 18]
        boolean hasApplied = Database.getDocumentsRepository().getApplicationRepository()
                              .findByApplicantNric(officerNric).stream()
                              .anyMatch(app -> app.getProjectName() != null && app.getProjectName().equals(projectName));
        if (hasApplied) {
            System.err.println("Registration Eligibility Error: Officer " + officerNric + " has applied for project '" + projectName + "' as an applicant.");
            return false;
        }

        // 2. Check if handling another project in the same application period [cite: 18]
        Date projectOpenDate = project.getApplicationOpenDate();
        Date projectCloseDate = project.getApplicationCloseDate();

        Optional<ProjectRegistration> conflictingReg = Database.getDocumentsRepository().getRegistrationRepository()
                .findApprovedRegistrationInPeriod(officerNric, projectOpenDate, projectCloseDate); // This relies on stub/service

        if (conflictingReg.isPresent()) {
            // Check if the conflicting registration is for a DIFFERENT project
             if (!conflictingReg.get().getProjectName().equals(projectName)){
                 System.err.println("Registration Eligibility Error: Officer " + officerNric + " is already handling another project (Reg ID: " + conflictingReg.get().getDocumentID() + ") during the application period of project '" + projectName + "'.");
                 return false;
             }
        }

        // 3. Check available slots (though registration is just a request, approval depends on slots)
        // No check needed here, Manager checks on approval.

        return true; // Eligible to submit registration request
    }
}