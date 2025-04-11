package boundary;

import controller.AccountController;
import entity.repository.AccountRepository;

public class UIEntry {
    /**
     * Check if application is executing for the first time.
     * 
     * @return true if the application is executing for the first time, 
     * false otherwise.
     */
    private static boolean firstStart() {
        return true;
    }

    /**
     * Entry to the main application.
     * If the application is being run for the first time, initialise it.
     * Then, display the main menu.
     */
    public static void start() {
        if (firstStart()) {
            System.out.println("First start! Performing first time initialisation...");

            // Initialize AccountController and load applicants.csv data
            AccountController accountController = new AccountController();
            // Load all user types
            accountController.loadApplicantsFromCSV("data/applicants.csv");
            accountController.loadOfficersFromCSV("data/hdb_officers.csv");
            accountController.loadManagersFromCSV("data/hdb_managers.csv");

            // Check account initialisation
            AccountRepository accountRepo = AccountRepository.getInstance();
            accountRepo.getAllUsers().forEach(System.out::println);;
        }
        MainMenu.start();
    }
}