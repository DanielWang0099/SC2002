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
            System.out.println("[BTOMS] Performing first time initialisation...");
            AccountController.initializeUserData();
        }
        MainMenu.start();
    }
}