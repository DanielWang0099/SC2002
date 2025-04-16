package controller.usersController;

import entities.database.Database;
import entities.project.*; // Project, FlatType, User, HdbManager, HdbOfficer etc.
import entities.documents.approvableDocuments.*; // Needed for checking related docs on delete
import entities.documents.repliableDocuments.*;
import entities.documents.*;
import entities.user.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import controller.ProjectController;

public class ApplicantController {

    private final ProjectController projectController; // Dependency for filtering

    public ApplicantController(ProjectController projectController) {
        this.projectController = projectController;
    }

    /**
     * Gets projects viewable by an applicant, considering eligibility and visibility.
     * @param applicant The applicant user.
     * @return List of viewable projects.
     */
    public List<Project> getAvailableProjects(Applicant applicant) {
        // Use ProjectController's filtering, passing the applicant and default sort order (ascending)
        return projectController.getFilteredProjects(applicant, null, null, null, null, null, true);
    }

    /**
     * Allows an applicant to apply for a specific project.
     * Performs eligibility checks.
     * @param applicant   The applicant user.
     * @param projectName The name of the project to apply for.
     * @return The created ProjectApplication object if successful, null otherwise.
     */
/*     public ProjectApplication applyForProject(Applicant applicant, String projectName) {
        // 1. Find Project
        Optional<Project> projectOpt = Database.getProjectsRepository().findById(projectName);
        if (projectOpt.isEmpty()) {
            System.err.println("Application Error: Project '" + projectName + "' not found.");
            return null;
        }
        Project project = projectOpt.get();

        // 2. Check Visibility [cite: 10]
        if (!project.isVisible()) {
            System.err.println("Application Error: Project '" + projectName + "' is not currently open for applications.");
            return null;
        }

        // 3. Check Eligibility (Age/Marital Status for Flat Types offered) [cite: 12]
        // Get flat types offered by the project
        boolean eligibleForAnyType = project.getInitialFlatUnitCounts().keySet().stream()
                                        .anyMatch(flatType -> checkEligibility(applicant, flatType));
        if (!eligibleForAnyType) {
             System.err.println("Application Error: Applicant " + applicant.getNric() + " is not eligible for any flat type in project '" + projectName + "' based on age/marital status.");
             return null;
        }


        // 4. Check if applicant already has an active application [cite: 11]
        Optional<ProjectApplication> existingApp = Database.getDocumentsRepository().getApplicationRepository().findActiveApplicationByApplicantNric(applicant.getNric());
        if (existingApp.isPresent()) {
            System.err.println("Application Error: Applicant " + applicant.getNric() + " already has an active application (ID: " + existingApp.get().getDocumentID() + "). Cannot apply for multiple projects.");
            return null;
        }

        // 5. Create and Save Application
        // Assume constructor links project correctly
        ProjectApplication application = ProjectApplication.createNewProjectApplication(applicant, project);
        // Submit it immediately (or require a separate submit step?) - PDF implies apply is one step.
        application.submit(applicant); // Change status to PENDING_APPROVAL

        Database.getDocumentsRepository().saveDocument(application);
        System.out.println("Application submitted successfully by " + applicant.getNric() + " for project '" + projectName + "'. Application ID: " + application.getDocumentID());
        return application;
    } */

    public ProjectApplication applyForProject(Applicant applicant, String projectName) {
        // 1. Find Project
        Optional<Project> projectOpt = Database.getProjectsRepository().findById(projectName);
        if (projectOpt.isEmpty()) {
            System.err.println("Application Error: Project '" + projectName + "' not found.");
            return null;
        }
        Project project = projectOpt.get();

        // 2. Check Visibility (Applicants can only apply to visible projects)
        if (!project.isVisible()) {
            System.err.println("Application Error: Project '" + projectName + "' is not currently open for applications (not visible).");
            return null;
        }

        Optional<ProjectApplication> existingBooking = Database.getDocumentsRepository()
            .getApplicationRepository()
            .findBookedApplicationByApplicantNric(applicant.getNric());
        if (existingBooking.isPresent()) {
            System.err.println("Application Error: Applicant " + applicant.getNric() +
                            " already has a booked flat (Application ID: " + existingBooking.get().getDocumentID() +
                            " for Project: " + existingBooking.get().getProjectName() + "). Cannot apply for a new project.");
            return null; // Cannot apply if already booked
        }

        // 3. *** ADDED: Strict Eligibility Check based on PDF Rules ***
        if (!isEligibleToApply(applicant, project)) {
             System.err.println("Application Error: Applicant " + applicant.getNric() +
                                " (Age: " + applicant.getAge() + ", Status: " + applicant.getMaritalStatus() +
                                ") is not eligible to apply for project '" + projectName +
                                "' based on offered flat types (" + project.getInitialFlatUnitCounts().keySet() + ").");
             // Provide more specific reason based on rules if desired
             if(applicant.getMaritalStatus() == MaritalStatus.SINGLE && applicant.getAge() >= 35) {
                  System.err.println("Reason: Singles aged 35+ can only apply for projects offering 2-Room flats.");
             } else if (applicant.getMaritalStatus() == MaritalStatus.MARRIED && applicant.getAge() < 21) {
                  System.err.println("Reason: Married applicants must be 21 or older.");
             } else if (applicant.getMaritalStatus() == MaritalStatus.SINGLE && applicant.getAge() < 35) {
                  System.err.println("Reason: Single applicants must be 35 or older.");
             }
             return null; // Application rejected due to eligibility
        }
        // --- End of Eligibility Check ---

        // 4. Check if applicant already has an active application
        Optional<ProjectApplication> existingApp = Database.getDocumentsRepository().getApplicationRepository().findActiveApplicationByApplicantNric(applicant.getNric());
        if (existingApp.isPresent()) {
            System.err.println("Application Error: Applicant " + applicant.getNric() + " already has an active/pending application (ID: " + existingApp.get().getDocumentID() + "). Cannot apply for multiple projects.");
            return null;
        }

        // 5. Create and Save Application (Eligibility passed)
        // Use the static factory method for clarity
        ProjectApplication application = ProjectApplication.createNewProjectApplication(applicant, project);

        // Submit it immediately
        boolean submitted = application.submit(applicant);
        if (!submitted) {
             System.err.println("Application Error: Failed to set application status to submitted for " + application.getDocumentID());
             return null; // Should not happen if created in DRAFT
        }

        Database.getDocumentsRepository().saveDocument(application); // Save via facade
        System.out.println("Application submitted successfully by " + applicant.getNric() + " for project '" + projectName + "'. Application ID: " + application.getDocumentID());
        return application;
    }
    /**
     * Retrieves the status and details of applications submitted by the applicant.
     * @param applicant The applicant user.
     * @return List of the applicant's project applications.
     */
    public List<ProjectApplication> viewMyApplications(Applicant applicant) {
        return Database.getDocumentsRepository().getApplicationRepository().findByApplicantNric(applicant.getNric());
    }

    /**
     * Creates a withdrawal request for a specific application.
     * @param applicant     The applicant user.
     * @param applicationId The ID of the application to withdraw.
     * @return The created Withdrawal object if successful, null otherwise.
     */
    public Withdrawal requestWithdrawal(Applicant applicant, String applicationId) {
        // 1. Find the application
        Optional<ProjectApplication> appOpt = Database.getDocumentsRepository().getApplicationRepository().findById(applicationId);
        if (appOpt.isEmpty()) {
             System.err.println("Withdrawal Error: Application ID '" + applicationId + "' not found.");
             return null;
        }
        ProjectApplication application = appOpt.get();

        // 2. Verify ownership
        if (!application.getSubmitter().equals(applicant)) {
             System.err.println("Withdrawal Error: Applicant " + applicant.getNric() + " did not submit application " + applicationId);
             return null;
        }

        // 3. Check if already withdrawn or in a non-withdrawable state?
        DocumentStatus currentStatus = application.getStatus();
        if (currentStatus == DocumentStatus.WITHDRAWN || currentStatus == DocumentStatus.REJECTED /*|| maybe others?*/) {
             System.err.println("Withdrawal Error: Application " + applicationId + " is already in a final state ("+currentStatus+") and cannot be withdrawn.");
             return null;
        }

        // 4. Check if a withdrawal request already exists
        Optional<Withdrawal> existingWithdrawal = Database.getDocumentsRepository().getWithdrawalRepository().findByApplicationId(applicationId);
        if(existingWithdrawal.isPresent() && existingWithdrawal.get().getStatus() != DocumentStatus.REJECTED) {
            System.err.println("Withdrawal Error: A withdrawal request (ID: " + existingWithdrawal.get().getDocumentID() + ") already exists and is pending/approved for application " + applicationId);
            return null;
        }


        // 5. Create and Submit Withdrawal Request [cite: 16]
        Withdrawal withdrawalRequest = Withdrawal.createNewWithdrawal(applicant, application, Database.getProjectsRepository().findByName(application.getProjectName()).get());
        withdrawalRequest.submit(applicant); // Mark as PENDING_APPROVAL by manager

        Database.getDocumentsRepository().saveDocument(withdrawalRequest);
        System.out.println("Withdrawal request submitted for application " + applicationId + ". Request ID: " + withdrawalRequest.getDocumentID());
        return withdrawalRequest;
    }

    // --- Enquiry Management --- [cite: 17]

    public Enquiry createEnquiry(Applicant applicant, String projectName, String content) {
         Optional<Project> projectOpt = Optional.empty();
         if(projectName != null && !projectName.isBlank()){
             projectOpt = Database.getProjectsRepository().findById(projectName);
             if(projectOpt.isEmpty()){
                 System.err.println("Enquiry Creation Error: Project '" + projectName + "' not found.");
                 return null; // Or allow general enquiries without a project? PDF implies project focus.
             }
         }
         if(content == null || content.isBlank()){
             System.err.println("Enquiry Creation Error: Enquiry content cannot be empty.");
             return null;
         }

         Enquiry enquiry = Enquiry.createNewEnquiry(applicant, projectOpt.orElse(null), content); // Project can be null if general
         enquiry.submit(applicant); // Submit immediately

         Database.getDocumentsRepository().saveDocument(enquiry);
         System.out.println("Enquiry created and submitted by " + applicant.getNric() + ". Enquiry ID: " + enquiry.getDocumentID());
         return enquiry;
    }

    public List<Enquiry> viewMyEnquiries(Applicant applicant) {
        return Database.getDocumentsRepository().getEnquiryRepository().findBySubmitterNric(applicant.getNric());
    }

    public boolean editEnquiry(Applicant applicant, String enquiryId, String newContent) {
        Optional<Enquiry> enquiryOpt = Database.getDocumentsRepository().getEnquiryRepository().findById(enquiryId);
        if (enquiryOpt.isEmpty()) {
            System.err.println("Enquiry Edit Error: Enquiry ID '" + enquiryId + "' not found.");
            return false;
        }
        Enquiry enquiry = enquiryOpt.get();

        boolean edited = enquiry.edit(applicant, newContent);

    // 4. If edit was successful in the model, save the changes via repository
        if (edited) {
            try {
                Database.getDocumentsRepository().saveDocument(enquiry); // Persist changes
                System.out.println("Enquiry " + enquiryId + " edit saved successfully.");
                return true;
            } catch (Exception e) {
                System.err.println("Error saving edited enquiry " + enquiryId + ": " + e.getMessage());
                // Consider rolling back the change in the model object if save fails? Complex.
                return false;
            }
        } else {
            // Error message (e.g., wrong status, auth failure, invalid content) printed by Enquiry.edit()
            System.err.println("Enquiry Edit Error: Failed to apply edits for enquiry " + enquiryId + " (check previous errors).");
            return false;
        }
    }

        /* // Authorisation & Status Check
        if (!enquiry.getSubmitter().equals(applicant)) {
             System.err.println("Enquiry Edit Error: Applicant " + applicant.getNric() + " did not submit this enquiry.");
            return false;
        }
        // PDF allows edit[cite: 17]. Assume only editable before reply? Or always? Let's assume before reply (DRAFT/SUBMITTED).
        if (enquiry.getStatus() != DocumentStatus.DRAFT && enquiry.getStatus() != DocumentStatus.SUBMITTED) {
             System.err.println("Enquiry Edit Error: Enquiry " + enquiryId + " cannot be edited as it has been replied to or closed.");
             return false;
        }
        if(newContent == null || newContent.isBlank()){
            System.err.println("Enquiry Edit Error: New content cannot be empty.");
            return false;
        }


        boolean edited = enquiry.edit(applicant, newContent); // Assumes Enquiry.edit updates content and lastMod fields
        if (edited) {
            Database.getDocumentsRepository().saveDocument(enquiry); // Persist changes
            System.out.println("Enquiry " + enquiryId + " edited successfully.");
            return true;
        } else {
             System.err.println("Enquiry Edit Error: Failed to apply edits for enquiry " + enquiryId);
             return false;
        }
    } */

    private boolean isEligibleToApply(Applicant applicant, Project project) {
        if (applicant == null || project == null) return false;

        int age = applicant.getAge();
        MaritalStatus maritalStatus = applicant.getMaritalStatus();
        Set<FlatType> offeredTypes = project.getInitialFlatUnitCounts().keySet(); // Get offered types

        if (maritalStatus == MaritalStatus.MARRIED && age >= 21) {
            // Married >= 21 can apply if project offers ANY flats
            return !offeredTypes.isEmpty();
        } else if (maritalStatus == MaritalStatus.SINGLE && age >= 35) {
            // Single >= 35 can ONLY apply if project offers TWO_ROOM flats
            return offeredTypes.contains(FlatType.TWO_ROOM);
        } else {
            // All other cases (Single < 35, Married < 21) are ineligible to apply
            return false;
        }
    }
     public boolean deleteEnquiry(Applicant applicant, String enquiryId) {
         Optional<Enquiry> enquiryOpt = Database.getDocumentsRepository().getEnquiryRepository().findById(enquiryId);
        if (enquiryOpt.isEmpty()) {
            System.err.println("Enquiry Delete Error: Enquiry ID '" + enquiryId + "' not found.");
            return false;
        }
        Enquiry enquiry = enquiryOpt.get();
        
        // Authorisation & Status Check
         if (!enquiry.getSubmitter().equals(applicant)) {
             System.err.println("Enquiry Delete Error: Applicant " + applicant.getNric() + " did not submit this enquiry.");
            return false;
        }
         // Assume only deletable before reply? Let's assume before reply (DRAFT/SUBMITTED).
        if (enquiry.getStatus() != DocumentStatus.DRAFT && enquiry.getStatus() != DocumentStatus.SUBMITTED) {
             System.err.println("Enquiry Delete Error: Enquiry " + enquiryId + " cannot be deleted as it has been replied to or closed.");
             return false;
        }

        boolean deleted = enquiry.delete(applicant); // Assumes Enquiry.delete updates status
         if(deleted) {
            // Also remove from repository
            boolean repoDeleted = Database.getDocumentsRepository().deleteDocumentById(enquiryId);
             if (repoDeleted) {
                 System.out.println("Enquiry " + enquiryId + " deleted successfully.");
                 return true;
             } else {
                  System.err.println("Enquiry Delete Error: Failed to remove enquiry " + enquiryId + " from repository.");
                  // Might need to revert status change if delete fails? Complex transaction...
                  return false;
             }
         } else {
              System.err.println("Enquiry Delete Error: Failed to mark enquiry " + enquiryId + " as deleted.");
              return false;
         }
     }

    // --- Helper Methods ---

    public boolean checkEligibility(Applicant applicant, FlatType flatType) {
        int age = applicant.getAge();
        MaritalStatus status = applicant.getMaritalStatus();
        if (status == MaritalStatus.MARRIED) return age >= 21;
        else if (status == MaritalStatus.SINGLE) return age >= 35 && flatType == FlatType.TWO_ROOM;
        else return false;
    }

}