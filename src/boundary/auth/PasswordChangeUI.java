package boundary.auth;

import controller.AccountController;
import utils.io.StringScanner;

/**
 * Handles the password change workflow for authenticated users.
 */
public class PasswordChangeUI {
    /**
     * Guides user through password change process with validation.
     * @param nric NRIC of the user requesting password change.
     * @return  {@code true} if password was successfully changed, 
     *          {@code false} otherwise.
     */
    public static boolean changePassword(String nric) {
        System.out.println("\n[Change Password]");
        String oldPass = StringScanner.scan("Enter current password: ");
        String newPass = StringScanner.scan("Enter new password: ");
        String confirmPass = StringScanner.scan("Confirm new password: ");
        System.out.println();

        if (!newPass.equals(confirmPass)) {
            System.out.println("New passwords do not match!");
            return false;
        }

        if (newPass.equals(oldPass)) {
            System.out.println("New password cannot be the same as old password!");
            return false;
        }

        boolean success = AccountController.changePassword(nric, oldPass, newPass);
        if (success) {
            System.out.println("Password changed successfully!\n" +
                "Please login with your new credentials...");
        } else {
            System.out.println("Failed to change password. Check your current password.");
        }
        return success;
    }
}