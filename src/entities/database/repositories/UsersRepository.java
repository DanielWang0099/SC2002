package entities.database.repositories;

import entities.user.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import entities.database.repositories.usersRepositories.*;

/**
 * This entity class is a repository for managing User entities.
 */
public class UsersRepository {

    private final ApplicantRepository applicantRepository;
    private final HdbOfficerRepository hdbOfficerRepository;
    private final HdbManagerRepository hdbManagerRepository;

    // Package-private constructor, managed by Database facade
/*     public UsersRepository() {
        System.out.println("Initializing UsersRepository Facade...");
        this.applicantRepository = new ApplicantRepository(); // Constructor now calls loadFromFile()
        this.hdbOfficerRepository = new HdbOfficerRepository(); // Constructor now calls loadFromFile()
        this.hdbManagerRepository = new HdbManagerRepository(); // Constructor now calls loadFromFile()
        // loadInitialUsersFromFile method is removed from here
        System.out.println("UsersRepository Facade initialized.");
        System.out.println("Counts: Applicants=" + applicantRepository.count() +
                           ", Officers=" + hdbOfficerRepository.count() +
                           ", Managers=" + hdbManagerRepository.count());
    } */

    public UsersRepository() {
        System.out.println("Initializing UsersRepository Facade instances...");
        this.applicantRepository = new ApplicantRepository();
        this.hdbOfficerRepository = new HdbOfficerRepository();
        this.hdbManagerRepository = new HdbManagerRepository();
        // DO NOT call loading here
        System.out.println("UsersRepository Facade instances created.");
    }

    /**
     * Loads user data from a specified file (CSV assumed).
     * !! ASSUMED FORMAT: Name,NRIC,Password,Role,Age,MaritalStatus !!
     * (e.g., John Doe,S1234567A,password,APPLICANT,25,SINGLE)
     * @param filename The path to the user data file.
     */
    public void saveAllUsers() {
        System.out.println("Saving all user types to CSV...");
        applicantRepository.saveToFile();
        hdbOfficerRepository.saveToFile();
        hdbManagerRepository.saveToFile();
        System.out.println("Finished saving user data.");
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