package entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import entities.project.Project;

import entities.user.User;
// Import User model classes when created

/**
 * Simple static in-memory database simulation.
 * Holds lists of users, projects, etc.
 * NOTE: In a real application, avoid static for testability.
 * This is used here for simplicity as requested.
 */
public class Database {
    // Use ConcurrentHashMap if multi-threading might be involved later
    private static final List<User> users = new ArrayList<>();
    private static final List<Project> projects = new ArrayList<>();
    private static final List<Document> documents = new ArrayList<>();

    // private static final List<Project> projects = new ArrayList<>();
    // private static final List<Application> applications = new ArrayList<>();

    // Static initializer block to load initial data (e.g., from file)
    static {
        System.out.println("Initializing Database...");
        // TODO: Load initial user data from file (CSV/TXT) as per requirements[cite: 8, 36]
        // Example: Assume loadUsersFromFile() populates the list
        loadUsersFromFile(); // Placeholder for actual file loading
        System.out.println("Loaded " + users.size() + " users.");

        // TODO: Load initial project data if applicable
    }

    private static void loadUsersFromFile() {}

    public static Optional<User> findUserByNric(String nric) {
        return users.stream()
                    .filter(user -> user.getNric().equalsIgnoreCase(nric))
                    .findFirst();
    }
}