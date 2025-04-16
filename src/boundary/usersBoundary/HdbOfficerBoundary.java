package boundary.usersBoundary;

import java.util.Scanner;
import controller.MainController;
import controller.ProjectController;
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
    private static final String[] menuOptions = {
        "#Officer Functions",
        "View Projects Assigned/Handled",
        "Register for Project Team",
        "View My Registration Status",
        "Process Flat Booking for Applicant",
        "Generate Booking Receipt",
        "View Enquiries for Handled Projects",
        "Reply to Enquiry",
        "#Applicant Functions",
        "View Available BTO Projects (Applicant View)",
        "Apply for Project (as Applicant)",
        "View My Application Status (as Applicant)",
        "Request Application Withdrawal (as Applicant)",
        "Create Enquiry (as Applicant)",
        "View My Enquiries (as Applicant)",
        "Edit My Enquiry (as Applicant)",
        "Delete My Enquiry (as Applicant)",
        "#General",
        "Change Password",
    };

    public HdbOfficerBoundary(Scanner scanner, MainController mainController, User currentUser) {
        super(scanner, mainController, (HdbOfficer) currentUser);
    }

    private HdbOfficer currentOfficer() {
        return (HdbOfficer) currentUser;
    }

    @Override
    protected String[] getMenuOptions() {
        return menuOptions;
    }

    @Override
    protected boolean processCommandOption(int choice) {
        switch (choice) {
            case 0 -> { System.out.println("Logging out..."); return false; }
            // Officer Functions
            case 1 -> handleViewHandledProjects();
            case 2 -> handleRegisterForProject();
            case 3 -> handleViewMyRegistrations();
            case 4 -> handleProcessFlatBooking();
            case 5 -> handleGenerateReceipt();
            case 6 -> handleViewHandledEnquiries();
            case 7 -> handleReplyToEnquiry();
            // Applicant Functions (Delegate to HdbOfficerController which calls ApplicantController)
            case 8 -> handleViewAvailableProjectsAsApplicant();
            case 9 -> handleApplyForProjectAsApplicant();
            case 10 -> handleViewMyApplicationsAsApplicant();
            case 11 -> handleRequestWithdrawalAsApplicant();
            case 12 -> handleCreateEnquiryAsApplicant();
            case 13 -> handleViewMySubmittedEnquiries();
            case 14 -> handleEditMyEnquiryAsApplicant();
            case 15 -> handleDeleteMyEnquiryAsApplicant();
            // General
            case 16 -> {              
                boolean changed = handleChangePassword(); // Use inherited helper method
                if (changed){
                    System.out.println("Password change process completed. For security, please log in again.");
                    return false;
                }
            }
            default -> System.out.println("Invalid choice. Please try again.");
        }
        return true;
    }

    // --- Officer Action Handlers ---

    private void handleViewHandledProjects() {
        System.out.println("Fetching projects you are handling...");
        List<Project> projects = mainController.getHdbOfficerController().viewHandledProjects(currentOfficer());
        displayProjectsList(projects, true); // Show more detail for handled projects
    }

    private void handleRegisterForProject() {

        FlatType flatTypeFilter = null;
        String neighFilter = null;
         if (getYesNoInput("Apply filters?")) {
            flatTypeFilter = promptForFlatTypeFilter();
            neighFilter = promptForNeighbourhoodFilter();
        }
        String sortBy = null;
        boolean sortAsc = true;
         if (getYesNoInput("Apply custom sorting? (Default: Project Name Ascending)")) {
           sortBy = promptForSortField();
           if (sortBy != null) sortAsc = getYesNoInput("Sort Ascending?");
        }

         System.out.println("Fetching projects open for registration...");
         // Show projects the officer could potentially register for (visible or not?)
         // Let's show all projects for now, controller will check eligibility
        List<Project> allProjects = mainController.getProjectController()
                                     .getFilteredProjects(currentOfficer(), neighFilter, flatTypeFilter, null, null, sortBy, sortAsc);// Use officer view logic maybe?
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

         List<Project> handledProjects = mainController.getHdbOfficerController().viewHandledProjects(currentOfficer());
         Project projectToManage = null;
 
         if (handledProjects.isEmpty()) {
             System.out.println("You are not currently assigned to handle any projects.");
             return;
         } else if (handledProjects.size() == 1) {
             projectToManage = handledProjects.get(0);
             System.out.println("Processing bookings for Project: " + projectToManage.getName());
         } else {
             System.out.println("Projects you are handling:");
             displayProjectsList(handledProjects, false); // Show simplified list for selection
             String projectNameChoice = getStringInput("Enter the Project Name you are processing bookings for: ");
             Optional<Project> projectOpt = handledProjects.stream()
                                                .filter(p -> p.getName().equalsIgnoreCase(projectNameChoice))
                                                .findFirst();
             if (projectOpt.isEmpty()) {
                 System.out.println("Invalid project selection.");
                 return;
             }
             projectToManage = projectOpt.get();
         }
 
         // 2. Fetch and Filter Applications for the Selected Project
         System.out.println("\nFetching APPROVED applications for project '" + projectToManage.getName() + "'...");
         List<ProjectApplication> allAppsForProject = Database.getDocumentsRepository()
                                                         .getApplicationRepository()
                                                         .findByProjectId(projectToManage.getName()); // Use existing repo method
 
         List<ProjectApplication> approvedApps = allAppsForProject.stream()
                 .filter(app -> app.getStatus() == DocumentStatus.APPROVED)
                 .collect(Collectors.toList());
 
         // 3. Display Approved Applications
         if (!displayApplicationsList(approvedApps)) { // Use the existing display helper
             System.out.println("There are no applications currently approved and awaiting booking for this project.");
             return;
         }
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
                  // New: Ask user whether to generate receipt for the booking
                  if (getYesNoInput("Generate receipt for this booking?")) {
                      String receipt = mainController.getHdbOfficerController().generateBookingReceipt(currentOfficer(), applicationToBook);
                      if (receipt != null) {
                          System.out.println(receipt);
                      } else {
                          System.out.println("Receipt generation failed.");
                      }
                  }
             } else {
                   System.out.println("Flat booking failed."); // Error from controller
             }
         } else {
              System.out.println("Booking cancelled.");
         }
    }

    private void handleGenerateReceipt() {
        System.out.println("--- Generate Booking Receipt ---");

        List<Project> handledProjects = mainController.getHdbOfficerController().viewHandledProjects(currentOfficer());
        Project projectToManage = null;

        if (handledProjects.isEmpty()) {
            System.out.println("You are not currently assigned to handle any projects.");
            return;
        } else if (handledProjects.size() == 1) {
            projectToManage = handledProjects.get(0);
            System.out.println("Processing bookings for Project: " + projectToManage.getName());
        } else {
            System.out.println("Projects you are handling:");
            displayProjectsList(handledProjects, false);
            String projectNameChoice = getStringInput("Enter the Project Name you are processing bookings for: ");
            Optional<Project> projectOpt = handledProjects.stream()
                                               .filter(p -> p.getName().equalsIgnoreCase(projectNameChoice))
                                               .findFirst();
            if (projectOpt.isEmpty()) {
                System.out.println("Invalid project selection.");
                return;
            }
            projectToManage = projectOpt.get();
        }

        System.out.println("\nFetching BOOKED applications for project '" + projectToManage.getName() + "'...");
        List<ProjectApplication> allAppsForProject = Database.getDocumentsRepository()
                                                        .getApplicationRepository()
                                                        .findByProjectId(projectToManage.getName());
        List<ProjectApplication> bookedApps = allAppsForProject.stream()
                .filter(app -> app.getStatus() == DocumentStatus.BOOKED)
                .collect(Collectors.toList());

        if (bookedApps.isEmpty()) {
             System.out.println("There are no applications booked for this project.");
             return;
        }

        // New: Prompt officer for receipt generation option
        boolean generateAll = getYesNoInput("Generate receipt for all bookings in this project? (Otherwise, single booking receipt)");

        if (generateAll) {
            System.out.println("Generating receipts for all booked applications in project '" + projectToManage.getName() + "'...");
            for (ProjectApplication app : bookedApps) {
                String receipt = mainController.getHdbOfficerController().generateBookingReceipt(currentOfficer(), app);
                if (receipt != null) {
                    System.out.println(receipt);
                } else {
                    System.out.println("Failed to generate receipt for Application ID: " + app.getDocumentID());
                }
                System.out.println("-------------------------------------");
            }
        } else {
            if (!displayApplicationsList(bookedApps)) {
                System.out.println("There are no applications booked available for receipt.");
                return;
            }
            String appId = getStringInput("Enter Application ID of the booked flat: ");
            Optional<ProjectApplication> appOpt = Database.getDocumentsRepository().getApplicationRepository().findById(appId);
            if (appOpt.isPresent() && appOpt.get().getStatus() == DocumentStatus.BOOKED) {
                String receipt = mainController.getHdbOfficerController().generateBookingReceipt(currentOfficer(), appOpt.get());
                if (receipt != null) {
                    System.out.println(receipt);
                } else {
                    System.out.println("Failed to generate receipt.");
                }
            } else {
                System.out.println("Application ID not found or application is not in BOOKED state.");
            }
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

        // --- Optional Filtering ---
        FlatType flatTypeFilter = null;
        String neighFilter = null;
        if (getYesNoInput("Apply filters?")) {
            flatTypeFilter = promptForFlatTypeFilter();
            neighFilter = promptForNeighbourhoodFilter();
        }

        // --- Optional Sorting ---
        String sortBy = null;
        boolean sortAsc = true; // Default ascending
        if (getYesNoInput("Apply custom sorting? (Default: Project Name Ascending)")) {
            sortBy = promptForSortField(); // Use helper method below
            if (sortBy != null) { // Only ask for order if a field was chosen
                 sortAsc = getYesNoInput("Sort Ascending?");
            }
        }

        System.out.println("Fetching available projects...");

        // Call controller with filters and sorting
        List<Project> projects = mainController.getProjectController()
            .getFilteredProjects(currentOfficer(), neighFilter, flatTypeFilter, null, null, // No manager/date filter for applicants
                                 sortBy, sortAsc); // Pass sorting info

        displayProjectsList(projects, false);
    }

    private void handleApplyForProjectAsApplicant() {
         System.out.println("Available Projects (Applicant View):");

         FlatType flatTypeFilter = null;
         String neighFilter = null;
          if (getYesNoInput("Apply filters?")) {
             flatTypeFilter = promptForFlatTypeFilter();
             neighFilter = promptForNeighbourhoodFilter();
         }
         String sortBy = null;
         boolean sortAsc = true;
          if (getYesNoInput("Apply custom sorting? (Default: Project Name Ascending)")) {
            sortBy = promptForSortField();
            if (sortBy != null) sortAsc = getYesNoInput("Sort Ascending?");
         }

         List<Project> projects = mainController.getProjectController()
                                     .getFilteredProjects(currentOfficer(), neighFilter, flatTypeFilter, null, null, sortBy, sortAsc);

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

    
    private String promptForSortField() {
        System.out.println("Available Sort Fields:");
        System.out.println(" 1. Project Name (Default)");
        System.out.println(" 2. Neighbourhood");
        // Add others here if implemented in Controller (e.g., Open Date)
        System.out.println(" 3. Application Open Date");
        int choice = getUserChoice("Sort by field number (or press Enter for default): ");
        switch (choice) {
            case 1: return ProjectController.SORT_BY_NAME; // Use constants
            case 2: return ProjectController.SORT_BY_NEIGHBOURHOOD;
            case 3: return ProjectController.SORT_BY_OPEN_DATE;
            default:
                System.out.println("Using default sort by Project Name.");
                return ProjectController.SORT_BY_NAME; // Default
        }
    }
}