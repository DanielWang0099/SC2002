package boundary;

import java.util.Scanner;
import controller.MainController;
import entities.user.*;
import entities.project.*; // Project, FlatType, User, HdbManager, HdbOfficer etc.
import entities.documents.approvableDocuments.*; // Needed for checking related docs on delete
import entities.documents.repliableDocuments.*;
import entities.documents.*;
import java.util.List;
import java.util.stream.*;
import entities.database.*;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class HdbOfficerBoundary extends BaseBoundary {

    public HdbOfficerBoundary(Scanner scanner, MainController mainController, User currentUser) {
        super(scanner, mainController, (HdbOfficer) currentUser);
    }

    private HdbOfficer currentOfficer() {
        return (HdbOfficer) currentUser;
    }

    @Override
    protected void displayMenu() {
        System.out.println("\n--- HDB Officer Menu (" + currentUser.getName() + " | " + currentUser.getNric() + ") ---");
        System.out.println("== Officer Functions ==");
        System.out.println(" 1. View Projects Assigned/Handled");
        System.out.println(" 2. Register for Project Team");
        System.out.println(" 3. View My Registration Status");
        System.out.println(" 4. Process Flat Booking for Applicant");
        System.out.println(" 5. Generate Booking Receipt");
        System.out.println(" 6. View Enquiries for Handled Projects");
        System.out.println(" 7. Reply to Enquiry");
        System.out.println("== Applicant Functions ==");
        System.out.println(" 8. View Available BTO Projects (Applicant View)");
        System.out.println(" 9. Apply for Project (as Applicant)");
        System.out.println("10. View My Application Status (as Applicant)");
        System.out.println("11. Request Application Withdrawal (as Applicant)");
        System.out.println("12. Create Enquiry (as Applicant)");
        System.out.println("13. View My Enquiries (as Applicant)");
        System.out.println("14. Edit My Enquiry (as Applicant)");
        System.out.println("15. Delete My Enquiry (as Applicant)");
        System.out.println("== General ==");
        System.out.println("16. Change Password");
        System.out.println("17. Logout");
    }

    @Override
    protected boolean processCommandOption(int choice) {
        boolean continueLoop = true;
        switch (choice) {
            // Officer Functions
            case 1: handleViewHandledProjects(); break;
            case 2: handleRegisterForProject(); break;
            case 3: handleViewMyRegistrations(); break;
            case 4: handleProcessFlatBooking(); break;
            case 5: handleGenerateReceipt(); break;
            case 6: handleViewHandledEnquiries(); break;
            case 7: handleReplyToEnquiry(); break;
            // Applicant Functions (Delegate to HdbOfficerController which calls ApplicantController)
            case 8: handleViewAvailableProjectsAsApplicant(); break;
            case 9: handleApplyForProjectAsApplicant(); break;
            case 10: handleViewMyApplicationsAsApplicant(); break;
            case 11: handleRequestWithdrawalAsApplicant(); break;
            case 12: handleCreateEnquiryAsApplicant(); break;
            case 13: handleViewMySubmittedEnquiries(); break;
            case 14: handleEditMyEnquiryAsApplicant(); break;
            case 15: handleDeleteMyEnquiryAsApplicant(); break;
            // General
            case 16: handleChangePassword(); break;
            case 17: System.out.println("Logging out..."); continueLoop = false; break;
            default: System.out.println("Invalid choice.");
        }
        return continueLoop;
    }

    // --- Officer Action Handlers ---

    private void handleViewHandledProjects() {
        System.out.println("Fetching projects you are handling...");
        List<Project> projects = mainController.getHdbOfficerController().viewHandledProjects(currentOfficer());
        displayProjectsList(projects, true); // Show more detail for handled projects
    }

    private void handleRegisterForProject() {
         System.out.println("Fetching projects open for registration...");
         // Show projects the officer could potentially register for (visible or not?)
         // Let's show all projects for now, controller will check eligibility
         List<Project> allProjects = mainController.getProjectController().getFilteredProjects(currentUser, null, null); // Use officer view logic maybe?
         if (!displayProjectsList(allProjects, false)) return;

         String projectName = getStringInput("Enter Project Name to register for (or 'cancel'): ");
         if (projectName.equalsIgnoreCase("cancel")) return;

         ProjectRegistration registration = mainController.getHdbOfficerController().registerForProjectTeam(currentOfficer(), projectName);
         if(registration != null) {
             System.out.println("Registration request submitted successfully. ID: " + registration.getDocumentID());
         } else {
              System.out.println("Failed to submit registration request."); // Error from controller
         }
    }

    private void handleViewMyRegistrations() {
         System.out.println("Fetching your registration requests...");
         List<ProjectRegistration> registrations = mainController.getHdbOfficerController().viewMyRegistrations(currentOfficer());
         displayRegistrationsList(registrations);
    }

    private void handleProcessFlatBooking() {
         System.out.println("--- Process Flat Booking ---");
         String applicantNric = getStringInput("Enter Applicant's NRIC to process booking for: ");
         // Find the applicant's *approved* application
         List<ProjectApplication> apps = Database.getDocumentsRepository().getApplicationRepository()
                                             .findByApplicantNric(applicantNric).stream()
                                             .filter(app -> app.getStatus() == DocumentStatus.APPROVED)
                                             .collect(Collectors.toList());

         if (apps.isEmpty()) {
             System.out.println("No approved application found for NRIC " + applicantNric);
             return;
         }
         ProjectApplication applicationToBook;
         if (apps.size() == 1) {
             applicationToBook = apps.get(0);
             System.out.println("Found application: " + applicationToBook.getDocumentID() + " for project: " + applicationToBook.getProjectName());
         } else {
             // Should ideally not happen if one active app rule enforced, but handle just in case
             System.out.println("Multiple approved applications found (data inconsistency?). Please specify Application ID:");
             displayApplicationsList(apps);
             String appId = getStringInput("Enter Application ID: ");
             Optional<ProjectApplication> appOpt = apps.stream().filter(a -> a.getDocumentID().equals(appId)).findFirst();
             if (appOpt.isEmpty()){
                  System.out.println("Invalid Application ID selected.");
                  return;
             }
             applicationToBook = appOpt.get();
         }

         // Get available flat types for the project
         Optional<Project> projectOpt = Database.getProjectsRepository().findById(applicationToBook.getProjectName());
         if (projectOpt.isEmpty()) { System.out.println("Error: Project not found."); return; }
         Project project = projectOpt.get();

         System.out.println("Available flat types for project '" + project.getName() + "':");
         List<FlatType> availableTypes = new ArrayList<>();
         for (Map.Entry<FlatType, Integer> entry : project.getRemainingFlatUnits().entrySet()) {
             if (entry.getValue() > 0) {
                  System.out.println("- " + entry.getKey() + " (" + entry.getValue() + " remaining)");
                  availableTypes.add(entry.getKey());
             }
         }
         if (availableTypes.isEmpty()) {
              System.out.println("No units remaining for this project.");
              return;
         }

         FlatType chosenType = null;
         while (chosenType == null) {
            String typeStr = getStringInput("Enter the Flat Type chosen by the applicant (e.g., TWO_ROOM or THREE_ROOM, or 'cancel'): ");
            if (typeStr.equalsIgnoreCase("cancel")) return;
            try {
                 chosenType = FlatType.valueOf(typeStr.toUpperCase());
                 if (!availableTypes.contains(chosenType)) {
                     System.out.println("Invalid or unavailable flat type selected. Please choose from the list.");
                     chosenType = null;
                 }
            } catch (IllegalArgumentException e) {
                 System.out.println("Invalid flat type entered. Please use TWO_ROOM or THREE_ROOM.");
            }
         }


         if (getYesNoInput("Confirm booking of " + chosenType + " for Application " + applicationToBook.getDocumentID() + "?")) {
             boolean success = mainController.getHdbOfficerController().processFlatBooking(currentOfficer(), applicationToBook, chosenType);
             if (success) {
                  System.out.println("Flat booking processed successfully.");
             } else {
                   System.out.println("Flat booking failed."); // Error from controller
             }
         } else {
              System.out.println("Booking cancelled.");
         }
    }

    private void handleGenerateReceipt() {
        System.out.println("--- Generate Booking Receipt ---");
        String appId = getStringInput("Enter Application ID of the booked flat: ");
        Optional<ProjectApplication> appOpt = Database.getDocumentsRepository().getApplicationRepository().findById(appId);

        if (appOpt.isPresent() && appOpt.get().getStatus() == DocumentStatus.BOOKED) {
            String receipt = mainController.getHdbOfficerController().generateBookingReceipt(currentOfficer(), appOpt.get());
            if (receipt != null) {
                System.out.println(receipt);
            } else {
                 System.out.println("Failed to generate receipt."); // Error from controller
            }
        } else {
            System.out.println("Application ID not found or application is not in BOOKED state.");
        }
    }

    private void handleViewHandledEnquiries() {
        System.out.println("Fetching enquiries for projects you handle...");
        List<Enquiry> enquiries = mainController.getHdbOfficerController().getHandledEnquiries(currentOfficer());
        displayEnquiriesList(enquiries, true); // Show details
    }

    private void handleReplyToEnquiry() {
        System.out.println("Unreplied enquiries for projects you handle:");
         List<Enquiry> enquiries = mainController.getHdbOfficerController().getHandledEnquiries(currentOfficer()).stream()
                 .filter(e -> e.getStatus() == DocumentStatus.SUBMITTED)
                 .collect(Collectors.toList());

        if(!displayEnquiriesList(enquiries, true)) {
            System.out.println("No enquiries needing reply found.");
            return;
        }

        String enquiryId = getStringInput("Enter Enquiry ID to reply to (or 'cancel'): ");
        if(enquiryId.equalsIgnoreCase("cancel")) return;

        String replyContent = getStringInput("Enter your reply: ");

        boolean success = mainController.getHdbOfficerController().replyToEnquiry(currentOfficer(), enquiryId, replyContent);
         if(success) {
            System.out.println("Reply submitted successfully.");
        } else {
            System.out.println("Failed to submit reply."); // Error from controller
        }
    }


    // --- Applicant Function Handlers (Delegation) ---

    private void handleViewAvailableProjectsAsApplicant() {
        System.out.println("--- View Available BTO Projects (Officer's View) ---");
         // Get Filters
        FlatType flatTypeFilter = promptForFlatTypeFilter(); // Add this helper method
        String neighFilter = promptForNeighbourhoodFilter(); // Add this helper method
        // Officers viewing general list likely don't filter by manager or date range either
        // String managerNricFilter = null;
        // Date[] dateRangeFilter = null;

        System.out.println("Fetching available projects" +
                           (flatTypeFilter != null ? " offering " + flatTypeFilter : "") +
                           (neighFilter != null ? " in " + neighFilter : "") + "...");

        // Call controller with filters - pass null for manager/date
        List<Project> projects = mainController.getProjectController()
            .getFilteredProjects(currentOfficer(), neighFilter, flatTypeFilter, null, null); // Pass nulls

        displayProjectsList(projects, false); // Use standard applicant view format
    }

    private void handleApplyForProjectAsApplicant() {
         System.out.println("Available Projects (Applicant View):");
         List<Project> projects = mainController.getHdbOfficerController().getAvailableProjectsForViewing(currentOfficer());
         if (!displayProjectsList(projects, false)) return;

         String projectName = getStringInput("Enter the Project Name to apply for (or type 'cancel'): ");
         if (projectName.equalsIgnoreCase("cancel")) return;

         // Use the officer controller's method which includes eligibility checks for officer role
         ProjectApplication application = mainController.getHdbOfficerController().applyForProjectAsApplicant(currentOfficer(), projectName);
         if (application != null) {
             System.out.println("Successfully submitted application for '" + projectName + "'.");
             System.out.println("Your Application ID: " + application.getDocumentID());
         } else {
             System.out.println("Failed to submit application for '" + projectName + "'.");
         }
    }

     private void handleViewMyApplicationsAsApplicant() {
        System.out.println("Fetching your applications submitted as Applicant...");
        List<ProjectApplication> applications = mainController.getHdbOfficerController().viewMyApplicationsAsApplicant(currentOfficer());
        displayApplicationsList(applications);
    }

     private void handleRequestWithdrawalAsApplicant() {
          System.out.println("Your submitted/approved applications:");
         List<ProjectApplication> applications = mainController.getHdbOfficerController().viewMyApplicationsAsApplicant(currentOfficer()).stream()
                 .filter(app -> app.getStatus() != DocumentStatus.REJECTED &&
                                app.getStatus() != DocumentStatus.WITHDRAWN &&
                                app.getStatus() != DocumentStatus.CLOSED)
                 .collect(Collectors.toList());

         if (!displayApplicationsList(applications)) {
             System.out.println("You have no applications eligible for withdrawal request.");
             return;
         }

         String appId = getStringInput("Enter the Application ID to request withdrawal for (or type 'cancel'): ");
         if (appId.equalsIgnoreCase("cancel")) return;

         Withdrawal withdrawal = mainController.getHdbOfficerController().requestWithdrawalAsApplicant(currentOfficer(), appId);
          if(withdrawal != null) {
              System.out.println("Withdrawal request submitted. Request ID: " + withdrawal.getDocumentID());
         } else {
              System.out.println("Failed to submit withdrawal request.");
         }
     }

     private void handleCreateEnquiryAsApplicant() {
         System.out.println("You can create an enquiry about a specific project or a general enquiry.");
         String projectName = getStringInput("Enter Project Name to ask about (leave blank for general, or 'cancel'): ");
         if (projectName.equalsIgnoreCase("cancel")) return;
         if (projectName.isBlank()) projectName = null;

         String content = getStringInput("Enter your enquiry: ");

         Enquiry enquiry = mainController.getHdbOfficerController().createEnquiryAsApplicant(currentOfficer(), projectName, content);
          if (enquiry != null) {
             System.out.println("Enquiry submitted successfully. Enquiry ID: " + enquiry.getDocumentID());
         } else {
             System.out.println("Failed to submit enquiry.");
         }
     }

     private void handleViewMySubmittedEnquiries() {
          System.out.println("Fetching your submitted enquiries...");
         List<Enquiry> enquiries = mainController.getHdbOfficerController().viewMySubmittedEnquiries(currentOfficer());
         displayEnquiriesList(enquiries, true);
     }

     private void handleEditMyEnquiryAsApplicant() {
        System.out.println("Your editable enquiries submitted as Applicant (Not REPLIED or CLOSED):"); // Updated description
        // Filter for enquiries that are NOT REPLIED and NOT CLOSED
       List<Enquiry> enquiries = mainController.getHdbOfficerController().viewMySubmittedEnquiries(currentOfficer()).stream()
              .filter(e -> e.getStatus() != DocumentStatus.REPLIED && e.getStatus() != DocumentStatus.CLOSED)
              .collect(Collectors.toList());

      if (!displayEnquiriesList(enquiries, true)) {
          System.out.println("You have no submitted enquiries that can currently be edited.");
          return;
      }
       String enquiryId = getStringInput("Enter the Enquiry ID to edit (or 'cancel'): ");
        if (enquiryId.equalsIgnoreCase("cancel")) return;

        // Verify the selected ID is actually in the editable list shown
       if (enquiries.stream().noneMatch(e -> e.getDocumentID().equals(enquiryId))) {
            System.out.println("Invalid Enquiry ID selected from the editable list.");
            return;
       }

       String newContent = getStringInput("Enter the new enquiry content: ");

       boolean success = mainController.getHdbOfficerController().editEnquiryAsApplicant(currentOfficer(), enquiryId, newContent);
        if(success) {
           // System.out.println("Enquiry update attempt finished.");
       } else {
           // System.out.println("Failed to update enquiry.");
       }
   }
      private void handleDeleteMyEnquiryAsApplicant() {
           System.out.println("Your deletable enquiries (DRAFT or SUBMITTED):");
         List<Enquiry> enquiries = mainController.getHdbOfficerController().viewMySubmittedEnquiries(currentOfficer()).stream()
                .filter(e -> e.getStatus() == DocumentStatus.DRAFT || e.getStatus() == DocumentStatus.SUBMITTED)
                .collect(Collectors.toList());
        if (!displayEnquiriesList(enquiries, false)) {
             System.out.println("You have no enquiries that can be deleted.");
            return;
        }
        String enquiryId = getStringInput("Enter the Enquiry ID to delete (or 'cancel'): ");
         if (enquiryId.equalsIgnoreCase("cancel")) return;
        if (getYesNoInput("Are you sure you want to delete Enquiry " + enquiryId + "?")) {
             boolean success = mainController.getHdbOfficerController().deleteEnquiryAsApplicant(currentOfficer(), enquiryId);
              if(success) System.out.println("Enquiry deleted."); else System.out.println("Failed to delete enquiry.");
        } else { System.out.println("Deletion cancelled."); }
      }


    // --- Display Helpers (Could be inherited/shared if identical to ApplicantBoundary) ---
    // Using separate copies for now for clarity
     private boolean displayProjectsList(List<Project> projects, boolean showManagerInfo) {
        if (projects == null || projects.isEmpty()) { System.out.println("No projects found."); return false; }
        System.out.println("\n--- Projects List ---");
        System.out.printf("%-25s | %-20s | %-10s | %-10s | %-15s | %-15s | %s%n",
                          "Project Name", "Neighbourhood", "2-Room", "3-Room", "Open Date", "Close Date", "Visibility");
        if (showManagerInfo) System.out.printf("%-25s | %-20s | %-10s | %-10s | %-15s | %-15s | %s | %s (%s) | %s%n",
                          "", "", "Units", "Units", "", "", "", "Manager Name", "NRIC", "Officer Slots");
        System.out.println(String.join("", Collections.nCopies(showManagerInfo ? 150 : 110, "-"))); // Adjust line length

        for (Project p : projects) {
            String openDateStr = p.getApplicationOpenDate() != null ? BaseBoundary.INPUT_DATE_FORMAT.format(p.getApplicationOpenDate()) : "N/A";
            String closeDateStr = p.getApplicationCloseDate() != null ? BaseBoundary.INPUT_DATE_FORMAT.format(p.getApplicationCloseDate()) : "N/A";
            System.out.printf("%-25s | %-20s | %-10d | %-10d | %-15s | %-15s | %s%n",
                              p.getName(), p.getNeighbourhood(),
                              p.getInitialUnitCount(FlatType.TWO_ROOM), p.getInitialUnitCount(FlatType.THREE_ROOM),
                              openDateStr, closeDateStr, p.isVisible() ? "Visible" : "Hidden");
             if (showManagerInfo) {
                 System.out.printf("%-25s | %-20s | %-10s | %-10s | %-15s | %-15s | %s | %s (%s) | %d available%n",
                          "", "", "", "", "", "", "",
                          p.getManager()!=null ? p.getManager().getName() : "N/A",
                          p.getManager()!=null ? p.getManager().getNric() : "N/A",
                          p.getAvailableOfficerSlots());
             }
        }
        System.out.println(String.join("", Collections.nCopies(showManagerInfo ? 150 : 110, "-")));
        return true;
    }

    private boolean displayApplicationsList(List<ProjectApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            System.out.println("No applications found.");
            return false;
        }
        System.out.println("\n--- Applications List ---");
        // Added Booked Flat column header
        System.out.printf("%-12s | %-25s | %-15s | %-10s | %s%n", "App ID", "Project Name", "Status", "Booked Flat", "Submitted Date");
        System.out.println("------------------------------------------------------------------------------------"); // Adjusted length
        for (ProjectApplication app : applications) {
            // Ensure date is not null
            String subDateStr = app.getSubmissionDate() != null ? app.getSubmissionDate().toString().substring(0, 10) : "N/A"; // Simple date part
            // Get booked flat type string (handle null)
            String bookedFlatStr = app.getBookedFlatType() != null ? app.getBookedFlatType().name() : "---";

            System.out.printf("%-12s | %-25s | %-15s | %-10s | %s%n",
                              app.getDocumentID(),
                              app.getProjectName(),
                              app.getStatus(),
                              bookedFlatStr, // Display booked flat type
                              subDateStr);

            // Display rejection reason if applicable
            if (app.getStatus() == DocumentStatus.REJECTED && app.getRejectionReason() != null) {
                 System.out.println("  -> Rejection Reason: " + app.getRejectionReason());
            }
        }
         System.out.println("------------------------------------------------------------------------------------");
        return true;
    }

     private boolean displayEnquiriesList(List<Enquiry> enquiries, boolean showFullDetail) {
         if (enquiries == null || enquiries.isEmpty()) {
            System.out.println("No enquiries found.");
            return false;
        }
        System.out.println("\n--- Enquiries List ---");
        System.out.printf("%-12s | %-25s | %-15s | %-15s | %s%n",
                          "Enquiry ID", "Project Name", "Status", "Submitted Date", "Content Snippet");
         System.out.println("-------------------------------------------------------------------------------------------");
        for (Enquiry e : enquiries) {
            String projectName = e.getProjectName() != null ? e.getProjectName() : "[General]";
            String subDateStr = e.getSubmissionDate() != null ? e.getSubmissionDate().toString().substring(0, 10) : "N/A";
             String snippet = e.getEnquiryContent() != null ?
                              (e.getEnquiryContent().length() > 30 ? e.getEnquiryContent().substring(0, 27) + "..." : e.getEnquiryContent())
                              : "N/A";
            System.out.printf("%-12s | %-25s | %-15s | %-15s | %s%n",
                              e.getDocumentID(), projectName, e.getStatus(), subDateStr, snippet);
            if (showFullDetail) {
                System.out.println("  Full Enquiry: " + e.getEnquiryContent());
                if (e.getStatus() == DocumentStatus.REPLIED && e.getReplyContent() != null) {
                    System.out.println("  Reply (" + (e.getReplier() != null ? e.getReplier().getNric() : "System") + "): " + e.getReplyContent());
                }
            }
        }
         System.out.println("-------------------------------------------------------------------------------------------");
        return true;
    }

      private boolean displayRegistrationsList(List<ProjectRegistration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            System.out.println("No registration requests found.");
            return false;
        }
        System.out.println("\n--- Registration Requests List ---");
        System.out.printf("%-12s | %-25s | %-15s | %s%n", "Reg ID", "Project Name", "Status", "Submitted Date");
        System.out.println("-----------------------------------------------------------------------");
        for (ProjectRegistration reg : registrations) {
            String subDateStr = reg.getSubmissionDate() != null ? reg.getSubmissionDate().toString().substring(0, 10) : "N/A";
            System.out.printf("%-12s | %-25s | %-15s | %s%n",
                              reg.getDocumentID(),
                              reg.getProjectName(),
                              reg.getStatus(),
                              subDateStr);
             if (reg.getStatus() == DocumentStatus.REJECTED && reg.getRejectionReason() != null) { // Requires getter
                 System.out.println("  -> Rejection Reason: " + reg.getRejectionReason());
             }
        }
         System.out.println("-----------------------------------------------------------------------");
        return true;
    }

}