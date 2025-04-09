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

        // Instantiate the UsersRepository facade. Its constructor will handle
        // creating the Applicant/Officer/Manager repositories and loading user data.
        usersRepository = new UsersRepository();

        // Instantiate the ProjectsRepository. Its constructor should handle
        // loading project data (if applicable).
        // TODO: Ensure the Project model class exists and is used correctly here.
        projectsRepository = new ProjectsRepository();

        // Instantiate the DocumentsRepository. Its constructor should handle
        // loading any persistent documents (if applicable).
        documentsRepository = new DocumentsRepository();

        System.out.println("Database Facade: All repositories initialized.");
    }

    // Private constructor prevents anyone from creating an instance of the Database class.
    // This enforces the static-only access pattern.
    private Database() {}

    // --- Static "getter" methods ---
    // These methods provide controlled access to the single repository instances.

    /**
     * Gets the singleton instance of the UsersRepository facade.
     * @return The UsersRepository instance.
     */
    public static UsersRepository getUsersRepository() {
        return usersRepository;
    }

    /**
     * Gets the singleton instance of the ProjectsRepository.
     * @return The ProjectsRepository instance.
     */
    public static ProjectsRepository getProjectsRepository() {
        // Optional: Add checks or warnings if the Project model isn't fully implemented yet
        // if (projectsRepository == null /* or Project class check fails */) {
        //     System.err.println("Warning: ProjectsRepository accessed but might depend on unimplemented Project model.");
        // }
        return projectsRepository;
    }

    /**
     * Gets the singleton instance of the DocumentsRepository.
     * @return The DocumentsRepository instance.
     */
    public static DocumentsRepository getDocumentsRepository() {
        return documentsRepository;
    }
}