package boundary;

import java.util.Scanner;
import controller.MainController;
import entities.user.User;

public class HdbOfficerBoundary extends BaseBoundary {
    public HdbOfficerBoundary(Scanner scanner, MainController mainController, User currentUser) {
        super(scanner, mainController, currentUser);
    }

    @Override
    protected void displayMenu() {
        System.out.println("\n--- HDB Officer Menu (" + currentUser.getNric() + ") ---");
        // TODO: Add HDB Officer specific options + Applicant options[cite: 9, 18-23]
        System.out.println("1. View Project Details (Handled/All)");
        System.out.println("2. Register for Project Team");
        System.out.println("3. Manage Flat Selection for Applicant");
        System.out.println("4. Generate Flat Selection Receipt");
        System.out.println("5. View/Reply Project Enquiries");
        System.out.println("--- Applicant Functions ---");
        System.out.println("6. View Available BTO Projects (as Applicant)");
        // ... other applicant options ...
        System.out.println("10. Change Password");
        System.out.println("11. Logout");
    }

    @Override
    protected boolean processCommandOption(int choice) {
         boolean continueLoop = true;
        // TODO: Implement cases using mainController.getHdbOfficerController()
        // Remember to handle applicant functions too, potentially calling applicant controller methods
        switch (choice) {
             case 1: System.out.println("Action: View Project Details..."); break;
             case 2: System.out.println("Action: Register for Project Team..."); break;
             case 3: System.out.println("Action: Manage Flat Selection..."); break;
             case 4: System.out.println("Action: Generate Receipt..."); break;
             case 5: System.out.println("Action: Manage Enquiries..."); break;
             case 6: System.out.println("Action: View BTO Projects (as Applicant)..."); break;
             // ... other cases ...
             case 10:
                 System.out.println("Action: Change Password...");
                 promptChangePassword();
                 break;
             case 11:
                System.out.println("Logging out...");
                continueLoop = false;
                break;
            default:
                System.out.println("Invalid choice.");
        }
        return continueLoop;
    }

     // Helper method for password change
    private void promptChangePassword() {
         System.out.println("\n--- Change Password ---");
         String oldPassword = getStringInput("Enter Old Password: ");
         String newPassword1 = getStringInput("Enter New Password: ");
         String newPassword2 = getStringInput("Confirm New Password: ");

         if (!newPassword1.equals(newPassword2)) {
             System.out.println("New passwords do not match.");
             return;
         }
         boolean success = mainController.getAuthController().changePassword(currentUser, oldPassword, newPassword1);
         // Handle success/failure message
     }
}

/**
 * Boundary for HDB Manager users.
 */