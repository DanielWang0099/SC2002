package controller;

import entities.Database;
import entities.user.User;
import java.util.Optional;

public class UserAuthenticationController {

    public Optional<User> login(String nric, String password) {
        Optional<User> userOpt = Database.findUserByNric(nric);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // IMPORTANT: Compare passwords securely in a real app (hashing)
            if (user.getPassword().equals(password)) {
                 // NRIC format check based on requirements[cite: 7]
                 if (!nric.matches("^[ST]\\d{7}[A-Z]$")) {
                     System.out.println("Warning: NRIC format is invalid, but login allowed (debug).");
                     // return Optional.empty(); // Enforce format check strictly if needed
                 }
                return Optional.of(user); // Login successful
            }
        }
        return Optional.empty(); // Login failed
    }

     public boolean changePassword(User user, String oldPassword, String newPassword) {
        // Verify old password first
        if (!user.getPassword().equals(oldPassword)) {
            System.out.println("Incorrect old password.");
            return false;
        }
        // TODO: Add validation for new password complexity if needed
        user.setPassword(newPassword);
        // In a real DB scenario, you'd save the user object here.
        // Since it's static in-memory, the change persists on the object.
        System.out.println("Password changed successfully for " + user.getNric());
        return true;
    }
}