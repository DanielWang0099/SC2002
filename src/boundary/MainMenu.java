package boundary;

import boundary.auth.AuthUI;
import utils.io.IntScanner;
import utils.ui.MenuBuilder;

/**
 * Handles the main application menu and navigation flow.
 * Serves as the primary user interface hub for system access.
 */
public class MainMenu {
    /**
     * Displays and manages the main application menu.
     * Handles user input and navigation to subsystem features.
     */
    public static void start() {
        try {
            while (true) {
                int choice = MenuBuilder.create()
                    .setHeader("Welcome to the", "Build-To-Order (BTO)", "Management System")
                    .setOptions("Login")
                    .setFooter("Exit Program")
                    .render();

                switch (choice) {
                    case 0 -> {
                        System.out.println("Thank you for using BTO Management System. Program terminating...");
                        System.exit(0);
                    }
                    case 1 -> AuthUI.login();
                    default -> System.out.println("Invalid option! Please enter a valid option from the menu.");
                }
            }
        } catch (Exception e) {
            System.out.println("\nUnexpected error occurred - restarting...");
            start();
        }
    }
}
