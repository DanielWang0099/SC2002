package boundary;

import java.util.Optional;
import java.util.Scanner;

// Import boundaries, controllers, models, utilities
import boundary.usersBoundary.ApplicantBoundary;
import boundary.usersBoundary.BaseBoundary;
import boundary.usersBoundary.HdbManagerBoundary;
import boundary.usersBoundary.HdbOfficerBoundary;
import controller.*;
import entities.user.User;
import entities.database.*;
import utilities.io.IntScanner;
import utilities.io.StringScanner;
import utilities.ui.MenuBuilder;

/**
 * Main entry point for the BTO Management System CLI application.
 * Initializes the system and handles the main login/dispatch loop.
 */
public class MainCLI {

    private Scanner scanner;
    private MainController mainController;
    private UserAuthenticationBoundary authBoundary;

    public MainCLI() {
        this.scanner = new Scanner(System.in);
        // Initialize the main controller, which initializes others and loads data via Database static block
        this.mainController = new MainController();
        this.authBoundary = new UserAuthenticationBoundary(scanner, mainController);
         // Trigger Database initialization explicitly if needed (usually happens on first access)
         Database.getUsersRepository(); // Example access
         System.out.println("MainCLI Initialized. Ready to start.");
    }

    /**
     * Starts the application. Presents the initial menu and handles user choices.
     */
    public void start() {
        boolean running = true;
        while (running) {
            int choice = MenuBuilder.create()
                .setHeader("Welcome to the", "Build-To-Order (BTO)", "Management System")
                .setOptions("Login")
                .setFooter("Exit Application")
                .render();

            switch (choice) {
                case 0 -> { // Exit Application
                    running = false; // Set flag to exit the loop
                }
                case 1 -> { // Login
                    Optional<User> userOpt = authBoundary.promptLogin();
                    if (userOpt.isPresent()) {
                        dispatchToUserBoundary(userOpt.get());
                        // After the user boundary loop finishes (logout),
                        // the main loop continues, showing the initial menu again.
                        System.out.println("\nYou have been logged out.");
                    } else {
                        // Login failed, message already shown by authBoundary.
                        // Loop continues to show initial menu.
                        System.out.println("Please try logging in again or exit.");
                    }
                    // Pause for user to read messages before showing menu again
                    System.out.println("\nPress Enter to continue...");
                    // Consume potential leftover newline from previous input
                    scanner.nextLine();
                }
                default -> {
                    System.out.println("Invalid choice. Please enter 1 or 2.");
                    // Pause for user to read messages
                    System.out.println("\nPress Enter to continue...");
                    // Consume potential leftover newline
                    scanner.nextLine();
                }
            }
        }
        exit(); // Call exit when the loop terminates
    }

    /**
     * Dispatches control to the appropriate boundary based on user role.
     * @param user The logged-in user.
     */
    private void dispatchToUserBoundary(User user) {
        System.out.println("\n========================================");
        System.out.println(" User Role Dispatch ");
        System.out.println("========================================");

        BaseBoundary userBoundary = null;

        // Check user object and role are not null before switching
        if (user == null || user.getRole() == null) {
            System.err.println("Error: Cannot dispatch boundary for null user or role.");
            return;
        }

        switch (user.getRole()) {
            case APPLICANT -> userBoundary = new ApplicantBoundary(scanner, mainController, user);
            case HDB_OFFICER -> userBoundary = new HdbOfficerBoundary(scanner, mainController, user);
            case HDB_MANAGER -> userBoundary = new HdbManagerBoundary(scanner, mainController, user);
            default -> {
                // This case should ideally not be reached if Role enum is used correctly
                System.out.println("Error: Unknown user role encountered [" + user.getRole() + "]. Logging out.");
                return; // Exit dispatch method
            }
        }

        if (userBoundary != null) {
            System.out.println("\nRedirecting to " + user.getRole() + " menu...");
            userBoundary.runMenuLoop();
            System.out.println("\n========================================");
            System.out.println(" End of " + user.getRole() + " Session ");
            System.out.println("========================================");
        }
    }

    /**
     * Cleans up resources, saves data, and exits the application.
     */
    public void exit() {
        System.out.println("\nExiting BTO Management System...");

        // --- SAVE ALL DATA ---
        // Calls the static method in the Database facade to trigger saves in repositories
        System.out.println("Saving data to files...");
        Database.saveAllData();
        System.out.println("Data saving complete.");
        // --- --------------- ---

        System.out.println("Goodbye!");
        if (scanner != null) {
            // Close scanner to release system resources
            scanner.close();
            System.out.println("Scanner closed.");
            IntScanner.close();
            StringScanner.close();
        }
        System.exit(0); // Terminate the application
    }

    /**
     * Main method to launch the application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Optional: Add a try-catch block here for any unhandled exceptions during startup
        try {
            System.out.println("Application starting...");
            // The Database static block will run when MainCLI constructor accesses it,
            // ensuring data is loaded before the application starts fully.
            MainCLI application = new MainCLI();
            application.start();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during application startup or execution: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
            // Optionally attempt a final save before exiting on error
            try {
                System.err.println("Attempting emergency data save...");
                Database.saveAllData();
            } catch (Exception saveEx) {
                System.err.println("Emergency save failed: " + saveEx.getMessage());
            }
            System.exit(1); // Exit with error code
        }
    }
}