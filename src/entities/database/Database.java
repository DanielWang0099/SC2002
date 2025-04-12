package entities.database;

import entities.database.repositories.*;

// Import User model classes when created

/**
 * Simple static in-memory database simulation.
 * Holds lists of users, projects, etc.
 * NOTE: In a real application, avoid static for testability.
 * This is used here for simplicity as requested.
 */
public final class Database { // final keyword prevents subclassing

    // Static final instances of each repository. They are initialized once.
    private static final UsersRepository usersRepository;
    private static final ProjectsRepository projectsRepository;
    private static final DocumentsRepository documentsRepository;

    // Static initializer block:
    // This code runs exactly once when the Database class is first loaded by the JVM.
    // It ensures that the repositories are created and initialized (including data loading)
    // before any other part of the application tries to access them.
    static {
        System.out.println("Database Facade: Initializing repositories...");
        // Init order matters if repos depend on each other during load (e.g., Project load needs Users)
        usersRepository = new UsersRepository();
        projectsRepository = new ProjectsRepository();
        documentsRepository = new DocumentsRepository(); // Initialize this too
        System.out.println("Database Facade: All repositories initialized.");
    }

    // Private constructor prevents anyone from creating an instance of the Database class.
    // This enforces the static-only access pattern.
    private Database() {}

    // --- Getters ---
    public static UsersRepository getUsersRepository() { return usersRepository; }
    public static ProjectsRepository getProjectsRepository() { return projectsRepository; }
    public static DocumentsRepository getDocumentsRepository() { return documentsRepository; } // Getter for docs

    /**
     * Saves all persistent data (Users, Projects) to their respective files.
     * Call this before application shutdown.
     */
    public static void saveAllData() {
        System.out.println("Database Facade: Saving all data...");
        usersRepository.saveAllUsers();         // Saves applicants.csv, hdb_officers.csv, hdb_managers.csv
        projectsRepository.saveToFile();        // Saves projects.csv
        documentsRepository.saveAllDocuments(); // Saves applications.csv, enquiries.csv, etc.
        System.out.println("Database Facade: Finished saving all data.");
    }
}