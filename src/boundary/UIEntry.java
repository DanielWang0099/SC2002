package boundary;

import controller.AccountController;
import entity.repository.AccountRepository;

/**
 * Handles the initial entry point and system initialisation for the BTO Management System.
 * Manages first-time setup procedures and transition to the main application flow.
 */
public class UIEntry {
    /**
     * Determines if the application is running for the first time.
     * 
     * @return {@code true} if this is the initial execution, 
     *         {@code false} for subsequent runs
     */
    private static boolean isFirstStart() {
        return true;    // temporary value
    }

    /**
     * Initializes the application environment and transitions to main menu.
     * Performs first-time setup if required, including data loading and system checks.
     */
    public static void start() {
        if (isFirstStart()) {
            System.out.println("First start! Performing first time initialisation...");

            // Initialize AccountController and load user data from csv
            AccountController accountController = new AccountController();
            accountController.loadApplicantsFromCSV("data/applicants.csv");
            accountController.loadOfficersFromCSV("data/hdb_officers.csv");
            accountController.loadManagersFromCSV("data/hdb_managers.csv");

            // Check account initialisation
            AccountRepository accountRepo = AccountRepository.getInstance();
            accountRepo.getAllUsers().forEach(System.out::println);;
            System.out.println("System initialisation complete!");
        }
        MainMenu.start();
    }
}