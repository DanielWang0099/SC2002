package boundary;

import java.util.Scanner;
import controller.MainController;
import entities.user.User;

public class HdbManagerBoundary extends BaseBoundary {
    public HdbManagerBoundary(Scanner scanner, MainController mainController, User currentUser) {
        super(scanner, mainController, currentUser);
    }

    @Override
    protected void displayMenu() {
        System.out.println("\n--- HDB Manager Menu (" + currentUser.getNric() + ") ---");
        // TODO: Add HDB Manager specific options[cite: 9, 25-33]
        System.out.println("1. Create BTO Project");
        System.out.println("2. Edit/Delete BTO Project");
        System.out.println("3. Toggle Project Visibility");
        System.out.println("4. View All Projects / Filter Own");
        System.out.println("5. Manage HDB Officer Registrations");
        System.out.println("6. Approve/Reject BTO Applications");
        System.out.println("7. Approve/Reject Application Withdrawals");
        System.out.println("8. Generate Reports");
        System.out.println("9. View/Reply Enquiries (Handled/All)");
        System.out.println("10. Change Password");
        System.out.println("11. Logout");

    }

    @Override
    protected boolean processCommandOption(int choice) {
        boolean continueLoop = true;
        // TODO: Implement cases using mainController.getHdbManagerController()
         switch (choice) {
             case 1: System.out.println("Action: Create Project..."); break;
             case 2: System.out.println("Action: Edit/Delete Project..."); break;
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