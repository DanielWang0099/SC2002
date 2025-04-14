package boundary;

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

public class ApplicantBoundary extends BaseBoundary {

    public ApplicantBoundary(Scanner scanner, MainController mainController, User currentUser) {
        // Ensure the currentUser is indeed an Applicant
        super(scanner, mainController, currentUser);
    }

    // Cast currentUser to Applicant for convenience
    private Applicant currentApplicant() {
        return (Applicant) currentUser;
    }

    @Override
    protected void displayMenu() {
        System.out.println("\n--- Applicant Menu (" + currentUser.getName() + " | " + currentUser.getNric() + ") ---");
        System.out.println("1. View Available BTO Projects");
        System.out.println("2. Apply for Project");
        System.out.println("3. View My Application Status");
        System.out.println("4. Request Application Withdrawal");
        System.out.println("5. Create Enquiry");
        System.out.println("6. View My Enquiries");
        System.out.println("7. Edit My Enquiry");
        System.out.println("8. Delete My Enquiry");
        System.out.println("9. Change Password");
        System.out.println("10. Logout");
    }

    @Override
    protected boolean processCommandOption(int choice) {
        boolean continueLoop = true;
        switch (choice) {
            case 1:
                handleViewAvailableProjects();
                break;
            case 2:
                handleApplyForProject();
                break;
            case 3:
                handleViewMyApplications();
                break;
            case 4:
                 handleRequestWithdrawal();
                 break;
            case 5:
                 handleCreateEnquiry();
                 break;
            case 6:
                 handleViewMyEnquiries();
                 break;
            case 7:
                 handleEditMyEnquiry();
                 break;
            case 8:
                 handleDeleteMyEnquiry();
                 break;
            case 9:
                handleChangePassword(); // Use inherited helper method
                break;
            case 10:
                System.out.println("Logging out...");
                continueLoop = false; // Signal to exit the loop
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
        return continueLoop;
    }

    // --- Action Handlers ---

    private void handleViewAvailableProjects() {
        System.out.println("--- View Available BTO Projects ---");

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
            .getFilteredProjects(currentApplicant(), neighFilter, flatTypeFilter, null, null, // No manager/date filter for applicants
                                 sortBy, sortAsc); // Pass sorting info

        displayProjectsList(projects);
    }

    private void handleApplyForProject() {
        System.out.println("--- Apply for Project ---");
        System.out.println("View Available Projects (Apply Filters/Sort Optional):");

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
                                    .getFilteredProjects(currentApplicant(), neighFilter, flatTypeFilter, null, null, sortBy, sortAsc);

        if (!displayProjectsList(projects)) return;

        String projectName = getStringInput("Enter the Project Name to apply for (or type 'cancel'): ");
        if (projectName.equalsIgnoreCase("cancel")) return;

        ProjectApplication application = mainController.getApplicantController().applyForProject(currentApplicant(), projectName);
        if (application != null) {
            System.out.println("Successfully submitted application for '" + projectName + "'.");
            System.out.println("Your Application ID: " + application.getDocumentID());
        } else {
            System.out.println("Failed to submit application for '" + projectName + "'. Please check eligibility and project status.");
            // Specific error printed by controller
        }
    }

     private void handleViewMyApplications() {
        System.out.println("Fetching your applications...");
        List<ProjectApplication> applications = mainController.getApplicantController().viewMyApplications(currentApplicant());
        displayApplicationsList(applications);
    }

     private void handleRequestWithdrawal() {
         System.out.println("Your submitted/approved applications:");
         List<ProjectApplication> applications = mainController.getApplicantController().viewMyApplications(currentApplicant()).stream()
                 // Filter for states where withdrawal might be possible
                 .filter(app -> app.getStatus() != DocumentStatus.REJECTED &&
                                app.getStatus() != DocumentStatus.WITHDRAWN &&
                                app.getStatus() != DocumentStatus.CLOSED) // Adjust filter as needed
                 .collect(Collectors.toList());

         if (!displayApplicationsList(applications)) {
             System.out.println("You have no applications eligible for withdrawal request.");
             return;
         }

         String appId = getStringInput("Enter the Application ID to request withdrawal for (or type 'cancel'): ");
         if (appId.equalsIgnoreCase("cancel")) return;

         Withdrawal withdrawal = mainController.getApplicantController().requestWithdrawal(currentApplicant(), appId);
         if(withdrawal != null) {
              System.out.println("Withdrawal request submitted. Request ID: " + withdrawal.getDocumentID());
         } else {
              System.out.println("Failed to submit withdrawal request.");
              // Error message from controller
         }
     }

     private void handleCreateEnquiry() {
         System.out.println("You can create an enquiry about a specific project or a general enquiry.");
         String projectName = getStringInput("Enter Project Name to ask about (leave blank for general enquiry, or type 'cancel'): ");
         if (projectName.equalsIgnoreCase("cancel")) return;
         if (projectName.isBlank()) projectName = null; // Handle general enquiry

         String content = getStringInput("Enter your enquiry: ");

         Enquiry enquiry = mainController.getApplicantController().createEnquiry(currentApplicant(), projectName, content);
          if (enquiry != null) {
             System.out.println("Enquiry submitted successfully. Enquiry ID: " + enquiry.getDocumentID());
         } else {
             System.out.println("Failed to submit enquiry.");
             // Error message from controller
         }
     }

     private void handleViewMyEnquiries() {
         System.out.println("Fetching your enquiries...");
         List<Enquiry> enquiries = mainController.getApplicantController().viewMyEnquiries(currentApplicant());
         displayEnquiriesList(enquiries, true); // Show details for own enquiries
     }

    private void handleEditMyEnquiry() {
        System.out.println("Your editable enquiries (Not REPLIED or CLOSED):"); // Updated description
        // Filter for enquiries that are NOT REPLIED and NOT CLOSED
        List<Enquiry> enquiries = mainController.getApplicantController().viewMyEnquiries(currentApplicant()).stream()
               .filter(e -> e.getStatus() != DocumentStatus.REPLIED && e.getStatus() != DocumentStatus.CLOSED)
               .collect(Collectors.toList());

       if (!displayEnquiriesList(enquiries, true)) {
           System.out.println("You have no enquiries that can currently be edited.");
           return;
       }

        String enquiryId = getStringInput("Enter the Enquiry ID to edit (or type 'cancel'): ");
        if (enquiryId.equalsIgnoreCase("cancel")) return;

        // Verify the selected ID is actually in the editable list shown
        if (enquiries.stream().noneMatch(e -> e.getDocumentID().equals(enquiryId))) {
             System.out.println("Invalid Enquiry ID selected from the editable list.");
             return;
        }


        String newContent = getStringInput("Enter the new enquiry content: ");

        boolean success = mainController.getApplicantController().editEnquiry(currentApplicant(), enquiryId, newContent);
        // Success/failure messages are now primarily handled within the controller/model edit methods.
        if(success) {
            // System.out.println("Enquiry update attempt finished."); // Optional confirmation
        } else {
            // System.out.println("Failed to update enquiry.");
        }
    }

    private void handleDeleteMyEnquiry() {
         System.out.println("Your deletable enquiries (DRAFT or SUBMITTED):");
         List<Enquiry> enquiries = mainController.getApplicantController().viewMyEnquiries(currentApplicant()).stream()
                .filter(e -> e.getStatus() == DocumentStatus.DRAFT || e.getStatus() == DocumentStatus.SUBMITTED)
                .collect(Collectors.toList());

        if (!displayEnquiriesList(enquiries, false)) { // Don't show full details for deletion list
             System.out.println("You have no enquiries that can be deleted.");
            return;
        }

        String enquiryId = getStringInput("Enter the Enquiry ID to delete (or type 'cancel'): ");
        if (enquiryId.equalsIgnoreCase("cancel")) return;

        if (getYesNoInput("Are you sure you want to delete Enquiry " + enquiryId + "?")) {
             boolean success = mainController.getApplicantController().deleteEnquiry(currentApplicant(), enquiryId);
             if(success) {
                 System.out.println("Enquiry deleted successfully.");
             } else {
                 System.out.println("Failed to delete enquiry.");
                 // Error message from controller
             }
        } else {
             System.out.println("Deletion cancelled.");
        }
    }

    // --- Display Helpers ---

    private boolean displayProjectsList(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            System.out.println("No projects found.");
            return false;
        }
        System.out.println("\n--- Projects List ---");
        // Header
        System.out.printf("%-25s | %-20s | %-15s | %-15s | %-15s | %s%n",
                          "Project Name", "Neighbourhood", "2-Room Units", "3-Room Units", "Open Date", "Close Date");
        System.out.println("----------------------------------------------------------------------------------------------------------");
        for (Project p : projects) {
             // Ensure dates are not null before formatting
            String openDateStr = p.getApplicationOpenDate() != null ? INPUT_DATE_FORMAT.format(p.getApplicationOpenDate()) : "N/A";
            String closeDateStr = p.getApplicationCloseDate() != null ? INPUT_DATE_FORMAT.format(p.getApplicationCloseDate()) : "N/A";
            System.out.printf("%-25s | %-20s | %-15d | %-15d | %-15s | %s%n",
                              p.getName(),
                              p.getNeighbourhood(),
                              p.getInitialUnitCount(FlatType.TWO_ROOM),
                              p.getInitialUnitCount(FlatType.THREE_ROOM),
                              openDateStr,
                              closeDateStr);
        }
         System.out.println("----------------------------------------------------------------------------------------------------------");
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