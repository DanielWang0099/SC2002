package controller;

import entity.user.Applicant;
import entity.user.HdbManager;
import entity.user.HdbOfficer;
import entity.user.User;
import entity.repository.AccountRepository;
import enums.UserRole;
import enums.MaritalStatus;
import utils.io.CsvUserLoader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class AccountController {
    private static final AccountRepository accountRepo = AccountRepository.getInstance();

    // AUTHENTICATION METHODS

    /**
     * Authenticates a user with NRIC and password
     * @param nric      User's NRIC
     * @param password  Input password
     * 
     * @return Authenticated User or null
     */
    public static User login(String nric, String password) {
        User user = accountRepo.findUserByNric(nric);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * Changes user password after verification
     * @param nric          User's NRIC
     * @param oldPassword   Current password
     * @param newPassword   New password
     * 
     * @return  {@code true} if password changed successfully,
     *          {@code false} otherwise.
     */
    public static boolean changePassword(String nric, String oldPassword, String newPassword) {
        User user = accountRepo.findUserByNric(nric);
        if (user != null && user.getPassword().equals(oldPassword)) {
            user.setPassword(newPassword);
            return true;
        }
        return false;
    }


    // LOADING METHODS

    /**
     * Loads all initial user data during program startup.
     */
    public static void initializeUserData() {
        try {
            CsvUserLoader loader = new CsvUserLoader(accountRepo);
            loader.loadUsersFromCSV("data/applicants.csv", UserRole.APPLICANT);
            loader.loadUsersFromCSV("data/hdb_officers.csv", UserRole.HDB_OFFICER);
            loader.loadUsersFromCSV("data/hdb_managers.csv", UserRole.HDB_MANAGER);
        } catch (Exception e) {
            System.err.println("Failed to initialise user data: " + e.getMessage());
        }
        System.out.println("[AccountController] User data initialisation completed!");
    }
}