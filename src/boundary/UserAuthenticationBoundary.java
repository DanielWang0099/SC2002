package boundary;

import java.util.Scanner;
import controller.MainController;
import entities.user.User;
import java.util.Optional;

public class UserAuthenticationBoundary {
    private Scanner scanner;
    private MainController mainController;

    public UserAuthenticationBoundary(Scanner scanner, MainController mainController) {
        this.scanner = scanner;
        this.mainController = mainController;
    }

     /**
     * Prompts for credentials and attempts login using the Auth Controller.
     * @return Optional<User> containing the logged-in user if successful, empty otherwise.
     */
    public Optional<User> promptLogin() {
        System.out.println("\n========================================");
        System.out.println("              User Login                ");
        System.out.println("========================================");

        String nric = getStringInput("Enter NRIC: ");
        String password = getStringInput("Enter Password: ");

        Optional<User> userOpt = mainController.getAuthController().login(nric, password);

        System.out.println("----------------------------------------");
        if (userOpt.isPresent()) {
            System.out.println("Login Successful! Welcome " + userOpt.get().getName());
        } else {
            System.out.println("Login Failed.");
        }
        System.out.println("========================================");
        return userOpt;
    }

    // Helper moved from BaseBoundary as this boundary might not need the full loop logic
    protected String getStringInput(String prompt) {
        String input = "";
        while (input.trim().isEmpty()) {
            System.out.print(prompt);
            input = scanner.nextLine();
            if (input.trim().isEmpty()) {
                System.out.println("Input cannot be empty. Please try again.");
            }
        }
        return input.trim();
    }
}