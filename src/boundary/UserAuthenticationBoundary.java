package boundary;

import java.util.Scanner;
import controller.MainController;
import entities.user.User;
import java.util.Optional;

public class UserAuthenticationBoundary { // Doesn't need BaseBoundary if only used once
    private Scanner scanner;
    private MainController mainController;

    public UserAuthenticationBoundary(Scanner scanner, MainController mainController) {
        this.scanner = scanner;
        this.mainController = mainController;
    }

     /**
     * Prompts for credentials and attempts login.
     * @return Optional<User> containing the logged-in user if successful, empty otherwise.
     */
    public Optional<User> promptLogin() {
        System.out.println("\n--- User Login ---");
        String nric = getStringInput("Enter NRIC: ");
        // Consider Console.readPassword() for better security if possible
        String password = getStringInput("Enter Password: ");

        Optional<User> userOpt = mainController.getAuthController().login(nric, password);

        if (userOpt != null) {
            System.out.println("Login Successful!");
        } else {
            System.out.println("Login Failed: Invalid NRIC or password.");
        }
        return userOpt;
    }

     // Method to handle password change might go here or in specific user boundaries
     public void promptChangePassword(User user) {
         System.out.println("\n--- Change Password ---");
         String oldPassword = getStringInput("Enter Old Password: ");
         String newPassword1 = getStringInput("Enter New Password: ");
         String newPassword2 = getStringInput("Confirm New Password: ");

         if (!newPassword1.equals(newPassword2)) {
             System.out.println("New passwords do not match.");
             return;
         }

         boolean success = mainController.getAuthController().changePassword(user, oldPassword, newPassword1);
         if (success) {
             System.out.println("Password change successful.");
         } else {
             System.out.println("Password change failed.");
         }
     }

    // Utility method moved from BaseBoundary as this boundary might not need the full loop
     protected String getStringInput(String prompt) {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print(prompt);
            input = scanner.nextLine();
            if (input.trim().isEmpty()) {
                System.out.println("Input cannot be empty. Please try again.");
            }
        }
        return input.trim();
    }
}