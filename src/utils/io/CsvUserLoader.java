package utils.io;

import entity.user.*;
import entity.repository.AccountRepository;
import enums.UserRole;
import enums.MaritalStatus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Handles loading of user data from CSV files into the system.
 */
public final class CsvUserLoader {
    private static AccountRepository accountRepo;

    /**
     * Constructor for CSVLoadUser.
     * @param accountRepo   The repository where loaded users will be stored.
     */
    public CsvUserLoader(AccountRepository accountRepo) {
        this.accountRepo = accountRepo;
    }

    /**
     * Generic CSV loader for different user types
     * @param filePath  Path to CSV file
     * @param role      User role to create
     * @throws IOException If file operations fail
     */
    public void loadUsersFromCSV(String filePath, UserRole UserRole) {
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
     * Processes individual CSV line.
     * @param line  CSV line content.
     * @param role  User role to create.
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
     * Creates user entity from parsed data.
     * @param data  Parsed CSV values.
     * @param role  User role to create.
     * @return Created user entity.
     * @throws IllegalArgumentException For invalid enum values.
     * @throws NumberFormatException For invalid numeric values.
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