package boundary;

import java.util.Optional;
import java.util.Scanner;
import controller.*;
import entities.user.User;
// Import boundaries, controllers, models

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
        // Initialize the main controller, which initializes others and data
        this.mainController = new MainController();
        this.authBoundary = new UserAuthenticationBoundary(scanner, mainController);
    }

    /**
     * Starts the application. Presents the initial menu and handles user choices.
     */
    public void start() {
        System.out.println("\n========================================");
        System.out.println(" Welcome to the BTO Management System");
        System.out.println("========================================");

        boolean running = true;
        while (running) {
            showInitialMenu();
            int choice = getUserChoice("Enter your choice: ");

            switch (choice) {
                case 1: // Login
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
                    scanner.nextLine();
                    break;
                case 2: // Exit
                    running = false; // Set flag to exit the loop
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1 or 2.");
                     // Pause for user to read messages
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        }
        exit(); // Call exit when the loop terminates
    }

    /**
     * Displays the initial main menu for login or exit.
     */
    private void showInitialMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Login");
        System.out.println("2. Exit Application");
    }

     /**
     * Safely reads an integer choice from the user.
     * @param prompt The message to display to the user.
     * @return The user's integer choice, or -1 if input is invalid.
     */
    private int getUserChoice(String prompt) {
        System.out.print(prompt);
        int choice = -1;
        try {
            String line = scanner.nextLine();
            choice = Integer.parseInt(line);
        } catch (NumberFormatException e) {
           // Error message handled in the main loop's default case
        }
        return choice;
    }


    /**
     * Dispatches control to the appropriate boundary based on user role.
     * @param user The logged-in user.
     */
    private void dispatchToUserBoundary(User user) {
        BaseBoundary userBoundary = null;

        switch (user.getRole()) {
            case APPLICANT:
                userBoundary = new ApplicantBoundary(scanner, mainController, user);
                break;
            case HDB_OFFICER:
                userBoundary = new HdbOfficerBoundary(scanner, mainController, user);
                break;
            case HDB_MANAGER:
                userBoundary = new HdbManagerBoundary(scanner, mainController, user);
                break;
            default:
                System.out.println("Error: Unknown user role [" + user.getRole() + "]. Logging out.");
                return; // Exit dispatch method
        }

        if (userBoundary != null) {
            System.out.println("\nRedirecting to " + user.getRole() + " menu...");
            userBoundary.runMenuLoop(); // Start the interaction loop for the user's role
        }
    }

    /**
     * Cleans up resources and exits.
     */
    public void exit() {
        System.out.println("\nExiting BTO Management System. Goodbye!");
        if (scanner != null) {
            scanner.close();
        }
        System.exit(0); // Terminate the application
    }

    /**
     * Main method to launch the application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        MainCLI application = new MainCLI();
        application.start();
    }
}