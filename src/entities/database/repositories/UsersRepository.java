package entities.database.repositories;

import entities.user.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import entities.database.repositories.usersRepositories.*;

public class UsersRepository {

    private final ApplicantRepository applicantRepository;
    private final HdbOfficerRepository hdbOfficerRepository;
    private final HdbManagerRepository hdbManagerRepository;

    // Package-private constructor, managed by Database facade
    public UsersRepository() {
        System.out.println("Initializing UsersRepository Facade...");
        this.applicantRepository = new ApplicantRepository();
        this.hdbOfficerRepository = new HdbOfficerRepository();
        this.hdbManagerRepository = new HdbManagerRepository();
        // Load users assuming new CSV format
        loadInitialUsersFromFile("users.csv");
        System.out.println("UsersRepository Facade initialized.");
        System.out.println("Counts: Applicants=" + applicantRepository.count() +
                           ", Officers=" + hdbOfficerRepository.count() +
                           ", Managers=" + hdbManagerRepository.count());
    }

    /**
     * Loads user data from a specified file (CSV assumed).
     * !! ASSUMED FORMAT: Name,NRIC,Password,Role,Age,MaritalStatus !!
     * (e.g., John Doe,S1234567A,password,APPLICANT,25,SINGLE)
     * @param filename The path to the user data file.
     */
    private void loadInitialUsersFromFile(String filename) {
        System.out.println("Loading users from file: " + filename + " (Expecting Name,NRIC,Password,Role,Age,MaritalStatus format)");
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            // boolean headerSkipped = false; // Optional header skip
            while ((line = reader.readLine()) != null) {
                 // if (!headerSkipped) { headerSkipped = true; continue; }

                String[] data = line.split(",");
                if (data.length == 6) { // Expecting 6 columns now
                    String name = data[0].trim();
                    String nric = data[1].trim();
                    String password = data[2].trim();
                    String roleStr = data[3].trim().toUpperCase();
                    int age = Integer.parseInt(data[4].trim());
                    String maritalStatusStr = data[5].trim().toUpperCase();

                    User user = null;
                    MaritalStatus maritalStatusEnum;
                    Role roleEnum; // Variable for Role enum

                    // NRIC format validation
                    if (!nric.matches("^[ST]\\d{7}[A-Z]$")) {
                         System.err.println("Skipping invalid NRIC format in file: " + nric);
                         continue;
                    }

                     // --- Convert MaritalStatus String to Enum ---
                    try {
                        maritalStatusEnum = MaritalStatus.valueOf(maritalStatusStr);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Skipping unknown MaritalStatus '" + maritalStatusStr + "' for NRIC " + nric);
                        continue;
                    }
                     // --- ---

                     // --- Convert Role String to Enum ---
                     try {
                         // Map potential variations if needed
                         if (roleStr.equals("OFFICER")) roleStr = "HDB_OFFICER";
                         if (roleStr.equals("MANAGER")) roleStr = "HDB_MANAGER";
                         roleEnum = Role.valueOf(roleStr);
                     } catch (IllegalArgumentException e) {
                         System.err.println("Skipping unknown Role '" + roleStr + "' for NRIC " + nric);
                         continue;
                     }
                     // --- ---

                    // --- Instantiate user based on role using NEW constructor ---
                    switch (roleEnum) {
                        case APPLICANT:
                            // Uses the new Applicant constructor
                            user = new Applicant(name, nric, age, maritalStatusEnum, password, Role.APPLICANT);
                            break;
                        case HDB_OFFICER:
                            // Uses the new HdbOfficer constructor
                            user = new HdbOfficer(name, nric, age, maritalStatusEnum, password, Role.HDB_OFFICER);
                            break;
                        case HDB_MANAGER:
                             // Uses the new HdbManager constructor
                            user = new HdbManager(name, nric, age, maritalStatusEnum, password, Role.HDB_MANAGER);
                            break;
                        // No default needed as we validated roleEnum
                    }

                    if (user != null) {
                        save(user); // Use the facade's save method
                    }
                } else {
                     System.err.println("Skipping malformed line in user file (expected 6 columns): " + line);
                }
            }
             System.out.println("Finished loading users from " + filename);
        } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) { // Catch potential errors
            System.err.println("Error loading users from file '" + filename + "': " + e.getMessage());
            if (applicantRepository.count() == 0 && hdbOfficerRepository.count() == 0 && hdbManagerRepository.count() == 0) {
                 System.err.println("File loading failed. Adding fallback sample users.");
                 addFallbackSampleUsers(); // Use corrected fallback
             }
        }
    }

     // Corrected fallback method using new constructor signature
     private void addFallbackSampleUsers() {
         save(new Applicant("Alice Applicant", "S1234567A", 25, MaritalStatus.SINGLE, "password", Role.APPLICANT));
         save(new HdbOfficer("Oliver Officer", "S2222222D", 32, MaritalStatus.MARRIED, "password", Role.HDB_OFFICER));
         save(new HdbManager("Mary Manager", "T3333333E", 45, MaritalStatus.MARRIED, "password", Role.HDB_MANAGER));
         System.out.println("Added fallback sample users.");
     }


    // --- Other methods (findUserByNric, save, delete, getters) remain the same ---
    public Optional<User> findUserByNric(String nric) {
         if (nric == null) return Optional.empty();
         String upperNric = nric.toUpperCase();
         return applicantRepository.findById(upperNric)
                 .<User>map(applicant -> applicant)
                 .or(() -> hdbOfficerRepository.findById(upperNric).map(officer -> officer))
                 .or(() -> hdbManagerRepository.findById(upperNric).map(manager -> manager));
    }

    public void save(User user) {
        if (user instanceof Applicant) {
            applicantRepository.save((Applicant) user);
        } else if (user instanceof HdbOfficer) {
            hdbOfficerRepository.save((HdbOfficer) user);
        } else if (user instanceof HdbManager) {
            hdbManagerRepository.save((HdbManager) user);
        } else if (user != null) {
             System.err.println("Attempted to save user with unknown role type: " + user.getClass().getName());
        } else {
            System.err.println("Attempted to save a null user.");
        }
    }

    public boolean delete(User user) {
         if (user == null) return false;
         boolean deleted = false;
         if (user instanceof Applicant) {
            deleted = applicantRepository.delete((Applicant) user);
        } else if (user instanceof HdbOfficer) {
            deleted = hdbOfficerRepository.delete((HdbOfficer) user);
        } else if (user instanceof HdbManager) {
            deleted = hdbManagerRepository.delete((HdbManager) user);
        } else {
             System.err.println("Attempted to delete user with unknown role type: " + user.getClass().getName());
        }
         if (deleted) {
             System.out.println("User deleted via Facade: " + user.getNric());
         }
         return deleted;
    }

     public boolean deleteByNric(String nric) {
         if (nric == null) return false;
         Optional<User> userOpt = findUserByNric(nric);
         if (userOpt.isPresent()) {
             return delete(userOpt.get());
         }
         return false;
     }

    public List<User> findAllUsers() {
        return Stream.of(
                applicantRepository.findAll().stream(),
                hdbOfficerRepository.findAll().stream(),
                hdbManagerRepository.findAll().stream()
            )
            .flatMap(userStream -> userStream)
            .collect(Collectors.toList());
    }

    public ApplicantRepository getApplicantRepository() { return applicantRepository; }
    public HdbOfficerRepository getHdbOfficerRepository() { return hdbOfficerRepository; }
    public HdbManagerRepository getHdbManagerRepository() { return hdbManagerRepository; }
}