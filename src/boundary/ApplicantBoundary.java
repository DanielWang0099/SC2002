package boundary;

import java.util.Scanner;
import controller.MainController;
import entities.user.User;

public class ApplicantBoundary extends BaseBoundary {
    public ApplicantBoundary(Scanner scanner, MainController mainController, User currentUser) {
        super(scanner, mainController, currentUser);
    }

    @Override
    protected void displayMenu() {
        System.out.println("\n--- Applicant Menu (" + currentUser.getNric() + ") ---");
        System.out.println("1. View Available BTO Projects");
        System.out.println("2. Apply for Project");
        System.out.println("3. View My Application Status");
        System.out.println("4. Withdraw Application");
        System.out.println("5. Manage Enquiries");
        System.out.println("6. Change Password");
        System.out.println("7. Logout");
        // Add more options based on requirements[cite: 10, 11, 13, 16, 17, 8]
    }

    @Override
    protected boolean processCommandOption(int choice) {
        boolean continueLoop = true;
        switch (choice) {
            case 1:
                System.out.println("Action: View BTO Projects...");
                // mainController.getApplicantController().viewProjects(currentUser);
                break;
            case 2:
                 System.out.println("Action: Apply for Project...");
                // mainController.getApplicantController().applyForProject(currentUser);
                break;
            // TODO: Implement cases 3, 4, 5 based on requirements
            case 6:
                 System.out.println("Action: Change Password...");
                 // Delegate password change prompt (could be in BaseBoundary or here)
                 promptChangePassword(); // Helper method in this class
                 break;
            case 7:
                System.out.println("Logging out...");
                continueLoop = false; // Signal to exit the loop
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
        return continueLoop;
    }

    // Helper method for password change within this boundary
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
         if (success) {
             System.out.println("Password change successful.");
         } else {
             // Error message printed by controller
             System.out.println("Password change failed.");
         }
     }
}

/**
 * Boundary for HDB Officer users.
 * NOTE: Renamed from HDBOfficcerBoundary to HdbOfficerBoundary for consistency.
 */