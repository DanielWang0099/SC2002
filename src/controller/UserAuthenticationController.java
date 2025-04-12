package controller;

import entities.database.Database;
import entities.user.User;
import java.util.Optional;

public class UserAuthenticationController {

    /**
     * Attempts to log in a user based on NRIC and password.
     * @param nric     The user's NRIC.
     * @param password The user's password.
     * @return Optional containing the logged-in User if credentials are valid, empty otherwise.
     */
    public Optional<User> login(String nric, String password) {
        if (nric == null || password == null || nric.trim().isEmpty()) {
            System.err.println("Login Error: NRIC and password cannot be empty.");
            return Optional.empty();
        }

        // Basic NRIC format check (as per PDF requirements)
        if (!nric.matches("^[ST]\\d{7}[A-Z]$")) {
             System.err.println("Login Error: Invalid NRIC format.");
             return Optional.empty();
        }


        Optional<User> userOpt = Database.getUsersRepository().findUserByNric(nric);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Compare passwords (plain text as required)
            if (user.getPassword().equals(password)) {
                System.out.println("Authentication successful for NRIC: " + nric);
                return Optional.of(user); // Login successful
            } else {
                 System.err.println("Authentication failed: Incorrect password for NRIC: " + nric);
            }
        } else {
             System.err.println("Authentication failed: NRIC not found: " + nric);
        }
        return Optional.empty(); // Login failed
    }

     /**
      * Changes the password for a given user after verifying the old password.
      * @param user        The User object whose password needs changing.
      * @param oldPassword The user's current password for verification.
      * @param newPassword The desired new password.
      * @return true if the password was successfully changed, false otherwise.
      */
     public boolean changePassword(User user, String oldPassword, String newPassword) {
        if (user == null || oldPassword == null || newPassword == null || newPassword.trim().isEmpty()) {
             System.err.println("Password Change Error: User, old password, and new password must be provided.");
             return false;
        }

        // Verify old password [cite: 8 allows changing password]
        if (!user.getPassword().equals(oldPassword)) {
            System.err.println("Password Change Error: Incorrect old password provided for user " + user.getNric());
            return false;
        }

        // Optional: Add validation for new password complexity if desired
        if (newPassword.equals(oldPassword)) {
             System.err.println("Password Change Error: New password cannot be the same as the old password.");
             return false;
        }
        if (newPassword.length() < 6) { // Example minimum length
             System.err.println("Password Change Error: New password must be at least 6 characters long.");
             return false;
        }


        // Update the password on the user object
        user.setPassword(newPassword);

        // Persist the change by saving the user object back to the repository
        // The repository's save method handles updating the existing user
        Database.getUsersRepository().save(user);

        System.out.println("Password changed successfully for user " + user.getNric());
        // Note: Data is saved to CSV only on application exit via Database.saveAllData()
        return true;
    }
}