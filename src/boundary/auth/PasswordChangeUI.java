package boundary.auth;

import controller.AccountController;
import utils.io.StringScanner;

public class PasswordChangeUI {
    /**
     * Handles password change UI flow
     * @param nric User's NRIC
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

        boolean success = AccountController.changePassword(nric, oldPass, newPass);
        if (success) {
            System.out.println("Password changed successfully!\nPlease login again...");
        } else {
            System.out.println("Failed to change password. Check your current password.");
        }
        return success;
    }
}