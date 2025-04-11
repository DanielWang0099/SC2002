package controller;

import entity.user.Applicant;
import entity.user.HdbManager;
import entity.user.HdbOfficer;
import entity.user.User;
import enums.UserRole;
import enums.MaritalStatus;
import entity.repository.AccountRepository;

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
     * @return true if password changed successfully
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
     * Loads applicants from CSV
     */
    public void loadApplicantsFromCSV(String filePath) {
        loadUsersFromCSV(filePath, UserRole.APPLICANT);
    }

    /**
     * Loads HDB officers from CSV
     */
    public void loadOfficersFromCSV(String filePath) {
        loadUsersFromCSV(filePath, UserRole.HDB_OFFICER);
    }

    /**
     * Loads HDB managers from CSV
     */
    public void loadManagersFromCSV(String filePath) {
        loadUsersFromCSV(filePath, UserRole.HDB_MANAGER);
    }

    /**
     * Generic CSV loader with user type differentiation
     */
    private void loadUsersFromCSV(String filePath, UserRole UserRole) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                processCSVLine(line, UserRole);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processes individual CSV line based on user type
     */
    private void processCSVLine(String line, UserRole UserRole) {
        String[] data = line.split(",", -1);
        if (data.length != 5) {
            System.err.println("Invalid data format in line: " + line);
            return;
        }

        try {
            User user = createUserFromData(data, UserRole);
            accountRepo.addUser(user);
        } catch (NumberFormatException e) {
            System.err.println("Invalid age in line: " + line);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid data in line: " + line);
        }
    }

    /**
     * User creation by role
     */
    private User createUserFromData(String[] data, UserRole UserRole) 
        throws IllegalArgumentException {
        String name = data[0].trim();
        String nric = data[1].trim();
        String password = data[2].trim();
        int age = Integer.parseInt(data[3].trim());
        MaritalStatus maritalStatus = MaritalStatus.valueOf(data[4].trim().toUpperCase());

        return switch (UserRole) {
            case APPLICANT -> new Applicant(name, nric, age, maritalStatus, password);
            case HDB_OFFICER -> new HdbOfficer(name, nric, age, maritalStatus, password);
            case HDB_MANAGER -> new HdbManager(name, nric, age, maritalStatus, password);
        };
    }
}