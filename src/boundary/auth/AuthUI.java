package boundary.auth;

import boundary.user.ApplicantMainPage;
import controller.AccountController;
import entity.repository.AccountRepository;
import entity.user.User;
import utils.ValidateNRIC;
import utils.io.StringScanner;

public class AuthUI {
    /**
     * Prompts user for NRIC and password, then authenticates via AccountController.
     * @return Authenticated User object or null if failed.
     */
    public static void login() {
        System.out.println("\n[Login]");
        String nric = promptValidNric();
        String password = StringScanner.scan("Enter Password: ");
        
        User user = AccountController.login(nric, password);

        if (user != null) {
            System.out.println("\nLogin successful! Welcome, " + user.getName() + "!");
            System.out.println("\nRedirecting you to the " + user.getRole() + " homepage...");
            redirectBasedOnRole(user); // redirect on successful login
        } else {
            System.out.println("\nInvalid NRIC or password. Please try again.");
        }
    }

    /**
     * Prompts for NRIC until valid format is entered.
     * @return Valid NRIC in uppercase.
     */
    private static String promptValidNric() {
        while(true) {
            String input = StringScanner.scan("Enter NRIC (e.g., S1234567A): ")
                .trim().toUpperCase();
            
            if(ValidateNRIC.isValidNric(input)) return input;
            System.out.println("Invalid NRIC! Please follow the format: [S/T]XXXXXXX[A-Z]");
        }
    }

    /**
     * Redirects user to appropriate main page based on role.
     */
    private static void redirectBasedOnRole(User user) {
        switch(user.getRole()) {
            case APPLICANT -> ApplicantMainPage.start(user);
            case HDB_OFFICER -> login(); // HDBOfficerMainPage.start(user);
            case HDB_MANAGER -> login(); // HDBManagerMainPage.start(user);
            default -> System.out.println("No valid dashboard for this role.");
        }
    }
}