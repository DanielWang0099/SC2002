package boundary.usersBoundary;

import java.util.Scanner;
import controller.MainController;
import controller.ProjectController;
import entities.user.*;
import entities.project.*; // Project, FlatType, User, HdbManager, HdbOfficer etc.
import entities.documents.approvableDocuments.*; // Needed for checking related docs on delete
import entities.documents.repliableDocuments.*;
import entities.database.Database;
import entities.documents.*;
import java.util.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.*;

public class HdbManagerBoundary extends BaseBoundary {
    private static final String[] menuOptions = {
        "#Project Management",
        "Create New BTO Project",
        "Edit Existing BTO Project",
        "Delete Existing BTO Project",
        "Toggle Project Visibility",
        "View All Projects",
        "View My Managed Projects",
        "#Registration Management",
        "View Pending Officer Registrations",
        "Process Officer Registration (Approve/Reject)",
        "#Application Management",
        "View Pending BTO Applications",
        "Process BTO Application (Approve/Reject)",
        "View Pending Withdrawal Requests",
        "Process Withdrawal Request (Approve/Reject)",
        "#Enquiry Management",
        "View All Enquiries",
        "View Enquiries for My Projects",
        "Reply to Enquiry",
        "#Reporting",
        "Generate Booking Report",
        "#General",
        "Change Password",
    };

    public HdbManagerBoundary(Scanner scanner, MainController mainController, User currentUser) {
        super(scanner, mainController, (HdbManager) currentUser);
    }

     private HdbManager currentManager() {
        return (HdbManager) currentUser;
    }

    @Override
    protected String[] getMenuOptions() {
        return menuOptions;
    }

    @Override
    protected boolean processCommandOption(int choice) {
         boolean continueLoop = true;
         switch (choice) {
            case 0 -> { System.out.println("Logging out..."); continueLoop = false; }
            // Project Management
            case 1 -> handleCreateProject();
            case 2 -> handleEditProject();
            case 3 -> handleDeleteProject();
            case 4 -> handleToggleVisibility();
            case 5 -> handleViewAllProjects();
            case 6 -> handleViewMyProjects();
            // Registration Management
            case 7 -> handleViewPendingOfficerRegs();
            case 8 -> handleProcessOfficerReg();
            // Application Management
            case 9 -> handleViewPendingBtoApps();
            case 10 -> handleProcessBtoApp();
            case 11 -> handleViewPendingWithdrawals();
            case 12 -> handleProcessWithdrawal();
            // Enquiry Management
            case 13 -> handleViewAllEnquiries();
            case 14 -> handleViewManagedEnquiries();
            case 15 -> handleReplyToEnquiry();
            // Reporting
            case 16 -> handleGenerateReport();
            // General
            case 17 -> {
                boolean changed = handleChangePassword(); // Use inherited helper method
                if (changed){
                    System.out.println("Password change process completed. For security, please log in again.");
                    continueLoop = false; // <-- Set to false to exit menu loop and force re-login
                }
            }
            default -> System.out.println("Invalid choice. Please try again.");
        }
        return continueLoop;
    }

    // --- Action Handlers ---

    private void handleCreateProject() {
        System.out.println("--- Create New BTO Project ---");
        String name = getStringInput("Enter Project Name: ");
        // Check uniqueness early
        if (Database.getProjectsRepository().findById(name).isPresent()){
             System.out.println("Error: Project name '" + name + "' already exists.");
             return;
        }
        String neighbourhood = getStringInput("Enter Neighbourhood: ");

        Map<FlatType, Integer> units = new HashMap<>();
        Map<FlatType, Double> prices = new HashMap<>();
        // Get units/prices for each flat type
        for (FlatType ft : FlatType.values()) {
             if (getYesNoInput("Include " + ft + " flats?")) {
                 int count = -1;
                 while(count < 0) { count = getUserChoice("Enter number of units for " + ft + ": "); if(count < 0) System.out.println("Units cannot be negative.");}
                 units.put(ft, count);

                 double price = -1.0;
                 while (price < 0) {
                      System.out.print("Enter selling price for " + ft + ": ");
                      try { price = Double.parseDouble(scanner.nextLine()); if(price < 0) System.out.println("Price cannot be negative.");}
                      catch (NumberFormatException e) { System.out.println("Invalid price format."); price = -1.0; }
                 }
                 prices.put(ft, price);
             }
        }
        if (units.isEmpty()) { System.out.println("Error: Project must have at least one flat type."); return; }

        Date openDate = getDateInput("Enter Application Open Date");
        if(openDate == null) return; // User cancelled
        Date closeDate = getDateInput("Enter Application Close Date");
        if(closeDate == null) return; // User cancelled

        Project createdProject = mainController.getHdbManagerController().createProject(
            currentManager(), name, neighbourhood, units, prices, openDate, closeDate
        );

        if (createdProject != null) {
            System.out.println("Project created successfully. Remember to toggle visibility when ready.");
        } else {
            System.out.println("Failed to create project."); // Error from controller
        }
    }


    private void handleEditProject() {
        System.out.println("--- Edit Existing BTO Project ---");
        System.out.println("Projects you manage:");
        // Use the controller to get projects managed by the current manager
        List<Project> myProjects = mainController.getHdbManagerController().viewMyProjects(currentManager());

        // Use the display helper (ensure it's implemented or copied)
        if(!displayProjectsList(myProjects, true)) {
            System.out.println("You do not manage any projects.");
            return;
        }

        String projectName = getStringInput("Enter name of project to edit (or 'cancel'): ");
        if(projectName.equalsIgnoreCase("cancel")) {
            System.out.println("Edit cancelled.");
            return;
        }

        // Find the selected project from the list of manageable projects
        Optional<Project> projectOpt = myProjects.stream()
                                        .filter(p -> p.getName().equalsIgnoreCase(projectName))
                                        .findFirst();

         if (projectOpt.isEmpty()) {
             // Check if it exists at all, maybe they typed it wrong but manage it
             projectOpt = Database.getProjectsRepository().findById(projectName);
             if(projectOpt.isEmpty() || !projectOpt.get().getManager().equals(currentManager())){
                 System.out.println("Project '" + projectName + "' not found or you do not manage it.");
                 return;
             }
         }

         Project project = projectOpt.get();
         System.out.println("\nEditing Project: " + project.getName() + ". Leave input blank or enter 'skip' to keep current value.");

         // --- Edit Neighbourhood ---
         String currentNeigh = project.getNeighbourhood();
         String newNeighInput = getStringInput("New Neighbourhood [" + currentNeigh + "]: ");
         String newNeigh = null; // Pass null if not changing
         if (!newNeighInput.isBlank() && !newNeighInput.equalsIgnoreCase("skip")) {
             newNeigh = newNeighInput;
         }

        // --- Edit Units and Prices ---
        Map<FlatType, Integer> newCounts = new HashMap<>();
        Map<FlatType, Double> newPrices = new HashMap<>();
        System.out.println("--- Edit Flat Units & Prices ---");
        for (FlatType ft : FlatType.values()) {
            int currentCount = project.getInitialUnitCount(ft);
            double currentPrice = project.getUnitPrice(ft);
            boolean currentlyExists = currentCount > 0 || project.getInitialFlatUnitCounts().containsKey(ft); // Check if type was defined

            System.out.println("\nFlat Type: " + ft);
            if (currentlyExists) {
                 System.out.println("  Current Units: " + currentCount + " | Current Price: " + String.format("%.2f", currentPrice));
                 if (!getYesNoInput("  Update details for " + ft + "?")) {
                     continue; // Skip updates for this flat type
                 }
            } else {
                 if (!getYesNoInput("  Add " + ft + " to this project?")) {
                     continue; // Skip adding this flat type
                 }
            }

            // Get new count
             int newCount = -1;
             while(newCount < 0) {
                  String countInput = getStringInput("  Enter new number of units for " + ft + " ['skip']: ");
                  if(countInput.equalsIgnoreCase("skip")) { newCount = currentCount; break;} // Keep current if skipping
                  try { newCount = Integer.parseInt(countInput); if(newCount < 0) System.out.println("Units cannot be negative."); }
                  catch (NumberFormatException e) { System.out.println("Invalid number."); newCount = -1;}
             }
             if(newCount != currentCount || !currentlyExists) { // Store if changed or newly added
                newCounts.put(ft, newCount);
             }


            // Get new price
            double newPrice = -1.0;
            while(newPrice < 0) {
                 String priceInput = getStringInput("  Enter new selling price for " + ft + " ['skip']: ");
                 if(priceInput.equalsIgnoreCase("skip")) { newPrice = currentPrice; break;} // Keep current if skipping
                  try { newPrice = Double.parseDouble(priceInput); if(newPrice < 0) System.out.println("Price cannot be negative."); }
                  catch (NumberFormatException e) { System.out.println("Invalid price format."); newPrice = -1.0;}
            }
             if(newPrice != currentPrice || !currentlyExists) { // Store if changed or newly added
                 newPrices.put(ft, newPrice);
             }
        }

        // --- Edit Dates ---
         System.out.println("--- Edit Application Dates ---");
         System.out.println("Current Open Date: " + (project.getApplicationOpenDate() != null ? INPUT_DATE_FORMAT.format(project.getApplicationOpenDate()) : "N/A"));
         Date newOpen = null;
         if(getYesNoInput("Update Open Date?")) {
             newOpen = getDateInput("Enter new Application Open Date");
             if (newOpen == null) System.out.println("Keeping original Open Date."); // User cancelled date input
         }

         System.out.println("Current Close Date: " + (project.getApplicationCloseDate() != null ? INPUT_DATE_FORMAT.format(project.getApplicationCloseDate()) : "N/A"));
         Date newClose = null;
          if(getYesNoInput("Update Close Date?")) {
             newClose = getDateInput("Enter new Application Close Date");
              if (newClose == null) System.out.println("Keeping original Close Date."); // User cancelled date input
         }

        // Pass null to controller if maps are empty (no changes made)
        Map<FlatType, Integer> countsToSend = newCounts.isEmpty() ? null : newCounts;
        Map<FlatType, Double> pricesToSend = newPrices.isEmpty() ? null : newPrices;


        // --- Call Controller ---
         System.out.println("\nSubmitting changes...");
        boolean success = mainController.getProjectController().editProject( // Use ProjectController directly
            currentManager(), projectName,
            newNeigh, // Pass null if not changed
            countsToSend, // Pass null if not changed
            pricesToSend, // Pass null if not changed
            newOpen, // Pass null if not changed
            newClose // Pass null if not changed
        );

         if (success) {
             System.out.println("Project updated successfully.");
         } else {
             System.out.println("Project update failed. Check logs or input values.");
             // Controller should print specific error messages
         }
    }

     private void handleDeleteProject() {
        System.out.println("--- Delete Existing Project ---");
        System.out.println("Projects you manage:");
        List<Project> myProjects = mainController.getHdbManagerController().viewMyProjects(currentManager());
        if(!displayProjectsList(myProjects, false)) return;

        String projectName = getStringInput("Enter name of project to DELETE (or 'cancel'): ");
        if(projectName.equalsIgnoreCase("cancel")) return;

         if(!myProjects.stream().anyMatch(p -> p.getName().equals(projectName))){
              System.out.println("Project not found or you do not manage it.");
             return;
         }

         if(getYesNoInput("!! ARE YOU SURE you want to permanently delete project '" + projectName + "'? This cannot be undone.")) {
            boolean success = mainController.getHdbManagerController().deleteProject(currentManager(), projectName);
             if (success) System.out.println("Project deleted."); else System.out.println("Project deletion failed."); // Error from controller
         } else {
              System.out.println("Deletion cancelled.");
         }
     }

/*     private void handleToggleVisibility() {
         System.out.println("--- Toggle Project Visibility ---");
         System.out.println("Projects you manage:");
         List<Project> myProjects = mainController.getHdbManagerController().viewMyProjects(currentManager());
         if(!displayProjectsList(myProjects, true)) return; // Show visibility status

         String projectName = getStringInput("Enter name of project to toggle visibility for (or 'cancel'): ");
         if(projectName.equalsIgnoreCase("cancel")) return;

         Optional<Project> projectOpt = myProjects.stream().filter(p -> p.getName().equals(projectName)).findFirst();
         if (projectOpt.isEmpty()) {
             System.out.println("Project not found or you do not manage it.");
             return;
         }
         Project project = projectOpt.get();

         boolean currentVisibility = project.isVisible();
         boolean makeVisible = getYesNoInput("Project is currently " + (currentVisibility ? "Visible" : "Hidden") + ". Set visibility to " + (!currentVisibility ? "Visible" : "Hidden") + "?");

         if (makeVisible = true) { // Only proceed if toggling
             boolean success = mainController.getHdbManagerController().toggleProjectVisibility(currentManager(), projectName, makeVisible);
             if (success) System.out.println("Visibility updated."); else System.out.println("Failed to update visibility.");
         } else {
            
              System.out.println("Visibility not changed.");
         }
    } */

    private void handleToggleVisibility() {
        System.out.println("--- Toggle Project Visibility ---");
        System.out.println("Projects you manage:");
        // Ensure controller method signature matches if updated
        List<Project> myProjects = mainController.getHdbManagerController()
                                        .viewAllProjects(currentManager()); // Get unfiltered, sorted list

       // Ensure display method shows current visibility
       if(!displayProjectsList(myProjects, true)) { // Assuming this helper shows visibility
            System.out.println("You do not manage any projects.");
            return;
       }

        String projectName = getStringInput("Enter name of project to toggle visibility for (or 'cancel'): ");
        if(projectName.equalsIgnoreCase("cancel")) {
             System.out.println("Operation cancelled.");
             return;
        }

        // Find the project reliably
        Optional<Project> projectOpt = Database.getProjectsRepository().findById(projectName);

        // Verify manager owns this project
        if (projectOpt.isEmpty() || !projectOpt.get().getManager().equals(currentManager())) {
            System.out.println("Project '" + projectName + "' not found or you do not manage it.");
            return;
        }
        Project project = projectOpt.get();

        // --- Corrected Logic ---
        boolean currentVisibility = project.isVisible();
        // Ask clearly if they want to change to the opposite state
        boolean userWantsToToggle = getYesNoInput("Project '" + projectName + "' is currently " +
                                                  (currentVisibility ? "Visible" : "Hidden") +
                                                  ". Do you want to change it to " +
                                                  (!currentVisibility ? "Visible" : "Hidden") + "?");

        if (userWantsToToggle) {
            // If user said YES, the desired new state is the opposite of the current one
            boolean newState = !currentVisibility;
            // Call the controller with the actual desired state
            boolean success = mainController.getProjectController() // Use ProjectController for this action
                                .toggleProjectVisibility(currentManager(), projectName, newState);

            if (success) {
                System.out.println("Project visibility successfully set to " + (newState ? "Visible" : "Hidden") + ".");
            } else {
                System.out.println("Failed to update project visibility."); // Error from controller
            }
        } else {
            // If user said NO, do nothing
            System.out.println("Visibility not changed.");
        }
        // --- End Corrected Logic ---
   }
    private void handleViewAllProjects() {
        System.out.println("--- View All Projects ---");
        // --- Optional Filtering ---
        FlatType flatTypeFilter = null;
        String neighFilter = null;
        String managerNricFilter = null;
        Date[] dateRangeFilter = null;
        if (getYesNoInput("Apply filters?")) {
            flatTypeFilter = promptForFlatTypeFilter();
            neighFilter = promptForNeighbourhoodFilter();
            managerNricFilter = promptForManagerNricFilter();
            dateRangeFilter = promptForDateRangeFilter();
        }

        // --- Optional Sorting ---
        String sortBy = null;
        boolean sortAsc = true;
        if (getYesNoInput("Apply custom sorting? (Default: Project Name Ascending)")) {
            sortBy = promptForSortFieldManager(); // Use specific helper for manager options
            if (sortBy != null) sortAsc = getYesNoInput("Sort Ascending?");
        }

        System.out.println("Fetching all projects (applying filters/sort)...");

        // Call controller with all filters and sorting
        List<Project> projects = mainController.getProjectController()
            .getFilteredProjects(currentManager(), neighFilter, flatTypeFilter, managerNricFilter, dateRangeFilter,
                                 sortBy, sortAsc);

        displayProjectsList(projects, true); // Show manager info
    }

    private void handleViewMyProjects() {
        System.out.println("--- View My Managed Projects ---");
       // --- Optional Filtering ---
       FlatType flatTypeFilter = null;
       String neighFilter = null;
       // Manager filter is implicit
       Date[] dateRangeFilter = null;
       if (getYesNoInput("Apply filters (Neighbourhood, Flat Type, Date Range)?")) {
           flatTypeFilter = promptForFlatTypeFilter();
           neighFilter = promptForNeighbourhoodFilter();
           dateRangeFilter = promptForDateRangeFilter();
       }

       // --- Optional Sorting ---
       String sortBy = null;
       boolean sortAsc = true;
       if (getYesNoInput("Apply custom sorting? (Default: Project Name Ascending)")) {
           sortBy = promptForSortFieldManager(); // Use manager sort options
           if (sortBy != null) sortAsc = getYesNoInput("Sort Ascending?");
       }

       System.out.println("Fetching projects you manage (applying filters/sort)...");

       // Call controller with filters and sorting (manager passed implicitly)
       List<Project> projects = mainController.getProjectController().getFilteredProjects(currentManager(), neighFilter, flatTypeFilter, currentManager().getNric(), dateRangeFilter,
                                 sortBy, sortAsc);

       displayProjectsList(projects, true); // Show manager info
   }
    private void handleViewPendingOfficerRegs() {
        System.out.println("Fetching pending officer registrations for your projects...");
        List<ProjectRegistration> regs = mainController.getHdbManagerController().viewPendingOfficerRegistrations(currentManager());
        displayRegistrationsList(regs);
    }

    private void handleProcessOfficerReg() {
         System.out.println("--- Process Officer Registration ---");
         System.out.println("Pending officer registrations for your projects:");
         List<ProjectRegistration> regs = mainController.getHdbManagerController().viewPendingOfficerRegistrations(currentManager());
         if(!displayRegistrationsList(regs)) return;

         String regId = getStringInput("Enter Registration ID to process (or 'cancel'): ");
         if (regId.equalsIgnoreCase("cancel")) return;

         // Verify selected ID is in the pending list
         if (regs.stream().noneMatch(r -> r.getDocumentID().equals(regId))) {
              System.out.println("Invalid Registration ID selected from the pending list.");
              return;
         }

         boolean approve = getYesNoInput("Approve this registration?");
         String reason = "";
         if (!approve) {
             reason = getStringInput("Enter reason for rejection: ");
         }

         boolean success = mainController.getHdbManagerController().processOfficerRegistration(currentManager(), regId, approve, reason);
         if(success) System.out.println("Registration processed."); else System.out.println("Failed to process registration.");

    }

    private void handleViewPendingBtoApps() {
        System.out.println("Fetching pending BTO applications for your projects...");
        List<ProjectApplication> apps = mainController.getHdbManagerController().viewPendingBtoApplications(currentManager());
        displayApplicationsList(apps);
    }

    private void handleProcessBtoApp() {
         System.out.println("--- Process BTO Application ---");
         System.out.println("Pending BTO applications for your projects:");
          List<ProjectApplication> apps = mainController.getHdbManagerController().viewPendingBtoApplications(currentManager());
          if(!displayApplicationsList(apps)) return;

         String appId = getStringInput("Enter Application ID to process (or 'cancel'): ");
          if (appId.equalsIgnoreCase("cancel")) return;

          // Verify selected ID is in the pending list
          if (apps.stream().noneMatch(a -> a.getDocumentID().equals(appId))) {
              System.out.println("Invalid Application ID selected from the pending list.");
              return;
         }


         boolean approve = getYesNoInput("Approve this application (grant successful ballot)?");
         String reason = "";
         if (!approve) {
             reason = getStringInput("Enter reason for rejection: ");
         }

         boolean success = mainController.getHdbManagerController().processBtoApplication(currentManager(), appId, approve, reason);
          if(success) System.out.println("Application processed."); else System.out.println("Failed to process application.");
    }


    private void handleViewPendingWithdrawals() {
        System.out.println("Fetching pending withdrawal requests for your projects...");
        List<Withdrawal> withdrawals = mainController.getHdbManagerController().viewPendingWithdrawals(currentManager());
        displayWithdrawalsList(withdrawals);
    }

     private void handleProcessWithdrawal() {
         System.out.println("--- Process Withdrawal Request ---");
         System.out.println("Pending withdrawal requests for your projects:");
         List<Withdrawal> withdrawals = mainController.getHdbManagerController().viewPendingWithdrawals(currentManager());
         if(!displayWithdrawalsList(withdrawals)) return;

         String wdId = getStringInput("Enter Withdrawal ID to process (or 'cancel'): ");
          if (wdId.equalsIgnoreCase("cancel")) return;

         // Verify ID
         if (withdrawals.stream().noneMatch(w -> w.getDocumentID().equals(wdId))) {
              System.out.println("Invalid Withdrawal ID selected from the pending list.");
              return;
         }


         boolean approve = getYesNoInput("Approve this withdrawal request?");
         String reason = "";
         if (!approve) {
             reason = getStringInput("Enter reason for rejection: ");
         }

         boolean success = mainController.getHdbManagerController().processWithdrawalRequest(currentManager(), wdId, approve, reason);
          if(success) System.out.println("Withdrawal request processed."); else System.out.println("Failed to process withdrawal request.");
     }

    private void handleViewAllEnquiries() {
        System.out.println("Fetching ALL enquiries...");
        List<Enquiry> enquiries = mainController.getHdbManagerController().viewAllEnquiries(currentManager());
        displayEnquiriesList(enquiries, true); // Show details
    }

    private void handleViewManagedEnquiries() {
        System.out.println("Fetching enquiries for projects you manage...");
        List<Enquiry> enquiries = mainController.getHdbManagerController().viewManagedEnquiries(currentManager());
        displayEnquiriesList(enquiries, true); // Show details
    }

     private void handleReplyToEnquiry() {
        System.out.println("Unreplied enquiries for projects you handle:");
        List<Enquiry> enquiries = mainController.getHdbManagerController().viewManagedEnquiries(currentManager()).stream()
                 .filter(e -> e.getStatus() == DocumentStatus.SUBMITTED)
                 .collect(Collectors.toList());

        // Option to view *all* unreplied if needed? For now, just managed ones.
        // List<Enquiry> allUnreplied = mainController.getHdbManagerController().viewAllEnquiries(currentManager()).stream()...

        if(!displayEnquiriesList(enquiries, true)) {
            System.out.println("No enquiries needing reply found for your projects.");
            return;
        }

        String enquiryId = getStringInput("Enter Enquiry ID to reply to (or 'cancel'): ");
        if(enquiryId.equalsIgnoreCase("cancel")) return;

        // Verify ID is in the displayed list
        if (enquiries.stream().noneMatch(e -> e.getDocumentID().equals(enquiryId))) {
             System.out.println("Invalid Enquiry ID selected from the list.");
             return;
        }


        String replyContent = getStringInput("Enter your reply: ");

        boolean success = mainController.getHdbManagerController().replyToEnquiry(currentManager(), enquiryId, replyContent);
         if(success) {
            System.out.println("Reply submitted successfully.");
        } else {
            System.out.println("Failed to submit reply."); // Error from controller
        }
     }

      private void handleGenerateReport() {
          System.out.println("--- Generate Booking Report ---");
          System.out.println("Enter filter criteria (leave blank or type 'all' to ignore filter):");

          String projFilter = getStringInput("Filter by Project Name ['all']: ");
          if (projFilter.equalsIgnoreCase("all")) projFilter = null;

          String maritalStr = getStringInput("Filter by Marital Status (SINGLE/MARRIED) ['all']: ");
          MaritalStatus maritalFilter = null;
          if (!maritalStr.equalsIgnoreCase("all") && !maritalStr.isBlank()) {
              try { maritalFilter = MaritalStatus.valueOf(maritalStr.toUpperCase()); }
              catch (IllegalArgumentException e) { System.out.println("Invalid marital status filter. Ignoring."); }
          }

          String flatStr = getStringInput("Filter by Booked Flat Type (TWO_ROOM/THREE_ROOM) ['all']: ");
          FlatType flatFilter = null;
          if (!flatStr.equalsIgnoreCase("all") && !flatStr.isBlank()) {
                try { flatFilter = FlatType.valueOf(flatStr.toUpperCase()); }
                catch (IllegalArgumentException e) { System.out.println("Invalid flat type filter. Ignoring."); }
          }

          String report = mainController.getHdbManagerController().generateBookingReport(
              currentManager(), projFilter, maritalFilter, flatFilter
          );

          System.out.println("\n" + report);
      }



    // --- Display Helpers (Could be shared/inherited) ---
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

     private boolean displayWithdrawalsList(List<Withdrawal> withdrawals) {
         if (withdrawals == null || withdrawals.isEmpty()) {
            System.out.println("No withdrawal requests found.");
            return false;
        }
        System.out.println("\n--- Withdrawal Requests List ---");
        System.out.printf("%-12s | %-12s | %-12s | %-15s | %s%n", "WithdrawalID", "ApplicantNRIC", "OrigAppID", "Status", "Submitted Date");
        System.out.println("------------------------------------------------------------------------------");
        for (Withdrawal w : withdrawals) {
            String subDateStr = w.getSubmissionDate() != null ? w.getSubmissionDate().toString().substring(0, 10) : "N/A";
            System.out.printf("%-12s | %-12s | %-12s | %-15s | %s%n",
                              w.getDocumentID(),
                              w.getSubmitter().getNric(),
                              w.getApplicationToWithdraw().getDocumentID(),
                              w.getStatus(),
                              subDateStr);
             if (w.getStatus() == DocumentStatus.REJECTED && w.getRejectionReason() != null) { // Requires getter
                 System.out.println("  -> Rejection Reason: " + w.getRejectionReason());
             }
        }
         System.out.println("------------------------------------------------------------------------------");
        return true;
     }

     private String promptForManagerNricFilter() { // New helper
        System.out.print("Filter by Manager NRIC? (Enter NRIC or leave blank for ALL): ");
        String input = scanner.nextLine().trim();
         // Optional: Add NRIC format validation here if desired
        return input.isEmpty() ? null : input;
    }
    private Date[] promptForDateRangeFilter() { // New helper
        System.out.println("Filter by Application Date Range?");
        Date startDate = getDateInput("Enter Start Date (yyyy-MM-dd or leave blank/cancel for none): ");
        if (startDate == null) return null; // No range filter if start date not provided

        Date endDate = null;
        while (endDate == null) {
             endDate = getDateInput("Enter End Date (yyyy-MM-dd or 'cancel'): ");
             if (endDate == null) return null; // No range filter if end date cancelled
             if (startDate.after(endDate)) {
                 System.out.println("End date cannot be before start date. Please re-enter.");
                 endDate = null; // Force re-entry of end date
             }
        }
        return new Date[]{startDate, endDate};
    }

    private String promptForSortFieldManager() {
        System.out.println("Available Sort Fields:");
        System.out.println(" 1. Project Name (Default)");
        System.out.println(" 2. Neighbourhood");
        System.out.println(" 3. Manager NRIC"); // Manager might want this
        System.out.println(" 4. Application Open Date");
        int choice = getUserChoice("Sort by field number (or press Enter for default): ");
        switch (choice) {
            case 1: return ProjectController.SORT_BY_NAME;
            case 2: return ProjectController.SORT_BY_NEIGHBOURHOOD;
            case 3: return ProjectController.SORT_BY_MANAGER;
            case 4: return ProjectController.SORT_BY_OPEN_DATE;
            default:
                System.out.println("Using default sort by Project Name.");
                return ProjectController.SORT_BY_NAME; // Default
        }
    }
}