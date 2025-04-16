package entities.database;

import java.util.List;
import java.util.Optional;

import entities.database.repositories.*;
import entities.database.repositories.documentsRepositories.ApplicationRepository;
import entities.database.repositories.documentsRepositories.EnquiryRepository;
import entities.database.repositories.documentsRepositories.RegistrationRepository;
import entities.database.repositories.documentsRepositories.WithdrawalRepository;
import entities.database.repositories.usersRepositories.ApplicantRepository;
import entities.database.repositories.usersRepositories.HdbManagerRepository;
import entities.database.repositories.usersRepositories.HdbOfficerRepository;
import entities.documents.DocumentStatus;
import entities.documents.approvableDocuments.ProjectApplication;
import entities.project.FlatType;
import entities.project.Project;

// Import User model classes when created

/**
 * Simple static in-memory database simulation.
 * Holds lists of users, projects, etc.
 * NOTE: In a real application, avoid static for testability.
 * This is used here for simplicity as requested.
 */
public final class Database { // final keyword prevents subclassing

    // Static final repository instances (Facades and specific ones if accessed directly)
    // Note: Keep references to specific repos if needed, or just facades
    private static final UsersRepository usersRepository;
    private static final ProjectsRepository projectsRepository;
    private static final DocumentsRepository documentsRepository;

    // Keep specific repo references easily accessible if needed often, avoids extra getter calls
    private static final ApplicantRepository applicantRepository;
    private static final HdbOfficerRepository hdbOfficerRepository;
    private static final HdbManagerRepository hdbManagerRepository;
    private static final ApplicationRepository applicationRepository;
    private static final RegistrationRepository registrationRepository;
    private static final WithdrawalRepository withdrawalRepository;
    private static final EnquiryRepository enquiryRepository;


    // Static initializer block: Phase 1 (Construction), Phase 2 (Loading)
    static {
        System.out.println("Database Facade: Phase 1 - Constructing Repositories...");

        // --- Construct ALL Repositories FIRST ---
        // Their constructors should ONLY initialize maps now.
        usersRepository = new UsersRepository(); // Facade constructor creates sub-repos
        projectsRepository = new ProjectsRepository();
        documentsRepository = new DocumentsRepository(); // Facade constructor creates sub-repos

        // Assign specific repos from facades for easier access later (optional)
        applicantRepository = usersRepository.getApplicantRepository();
        hdbOfficerRepository = usersRepository.getHdbOfficerRepository();
        hdbManagerRepository = usersRepository.getHdbManagerRepository();
        applicationRepository = documentsRepository.getApplicationRepository();
        registrationRepository = documentsRepository.getRegistrationRepository();
        withdrawalRepository = documentsRepository.getWithdrawalRepository();
        enquiryRepository = documentsRepository.getEnquiryRepository();

        System.out.println("Database Facade: Phase 1 Complete. All repository instances created.");

        // --- Phase 2: Load Data in Correct Order ---
        System.out.println("Database Facade: Phase 2 - Loading Data into Repositories...");
        try {
            // 1. Load Users
            applicantRepository.loadFromFile();
            hdbOfficerRepository.loadFromFile();
            hdbManagerRepository.loadFromFile();
            System.out.println("--> Users loaded.");

            // 2. Load Projects (may depend on Managers being loaded)
            projectsRepository.loadFromFile();
            System.out.println("--> Projects loaded.");

            // 3. Load Documents (may depend on Users and Projects)
            // Load Applications first as Withdrawals depend on them
            applicationRepository.loadFromFile();
            System.out.println("--> Applications loaded.");
            registrationRepository.loadFromFile();
            System.out.println("--> Registrations loaded.");
            enquiryRepository.loadFromFile();
            System.out.println("--> Enquiries loaded.");
            withdrawalRepository.loadFromFile(); // Load Withdrawals last
            System.out.println("--> Withdrawals loaded.");

            System.out.println("Database Facade: Phase 2 Complete. All data loading initiated.");

            // --- Phase 3: Reconcile Data (NEW STEP) ---
            System.out.println("Database Facade: Phase 3 - Reconciling Data...");
            reconcileRemainingUnits(); // Call the new reconciliation method
            System.out.println("Database Facade: Phase 3 Complete. Reconciliation finished.");


            System.out.println("Database Facade: All data loading and reconciliation initiated.");

            
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR during data loading phase: " + e.getMessage());
            e.printStackTrace();
            // Application might be in an inconsistent state here.
            // Consider exiting or handling this failure robustly.
        }
    }

    // Static initializer block:
    // This code runs exactly once when the Database class is first loaded by the JVM.
    // It ensures that the repositories are created and initialized (including data loading)
    // before any other part of the application tries to access them.

    // Private constructor prevents anyone from creating an instance of the Database class.
    // This enforces the static-only access pattern.
    private Database() {}

    // --- Static Getters remain the same ---
    public static UsersRepository getUsersRepository() { return usersRepository; }
    public static ProjectsRepository getProjectsRepository() { return projectsRepository; }
    public static DocumentsRepository getDocumentsRepository() { return documentsRepository; }
    // Optional: Add getters for specific repos if frequently needed directly
    // public static ApplicationRepository getApplicationRepo() { return applicationRepository; }

    private static void reconcileRemainingUnits() {
        System.out.println("Reconciling remaining flat units based on booked applications...");
        int adjustedCount = 0;
        // Get all applications directly from its repository
        List<ProjectApplication> allApplications = applicationRepository.findAll(); // Use direct reference

        for (ProjectApplication app : allApplications) {
            if (app.getStatus() == DocumentStatus.BOOKED) {
                FlatType bookedType = app.getBookedFlatType(); // Assumes getter exists
                String projectName = app.getProjectName(); // Assumes getter exists

                if (bookedType != null && projectName != null) {
                    Optional<Project> projectOpt = projectsRepository.findById(projectName); // Use direct reference
                    if (projectOpt.isPresent()) {
                        Project project = projectOpt.get();
                        // Decrement the unit for the loaded booked application
                        // Note: decrementRemainingUnit returns false if already 0, which might
                        // indicate inconsistency, but we proceed anyway for loading state.
                        boolean decremented = project.decrementRemainingUnit(bookedType);
                        if (decremented) {
                             adjustedCount++;
                            // Optional log: System.out.println("Reconciled booking: App " + app.getDocumentID() + " decremented " + bookedType + " for project " + projectName);
                        } else {
                             System.err.println("Reconciliation Warning: Could not decrement unit " + bookedType + " for project " + projectName + " based on booked app " + app.getDocumentID() + ". Remaining count might already be zero.");
                        }
                    } else {
                         System.err.println("Reconciliation Warning: Project '" + projectName + "' not found for booked application " + app.getDocumentID());
                    }
                } else {
                     System.err.println("Reconciliation Warning: Booked application " + app.getDocumentID() + " is missing booked flat type or project name.");
                }
            }
        }
        System.out.println("Finished reconciling units. Decremented units for " + adjustedCount + " booked applications.");
    }

    // --- saveAllData method remains the same ---
    public static void saveAllData() {
        System.out.println("Database Facade: Saving all data...");
        // These call the saveToFile() method on the respective repositories
        usersRepository.saveAllUsers();
        projectsRepository.saveToFile();
        documentsRepository.saveAllDocuments();
        System.out.println("Database Facade: Finished saving all data.");
    }
}