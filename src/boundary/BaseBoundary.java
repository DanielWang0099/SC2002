package boundary;

import java.util.Scanner;
import controller.MainController;
import entities.user.User;

public abstract class BaseBoundary {
    protected Scanner scanner;
    protected MainController mainController;
    protected User currentUser; // The user interacting with this boundary

    public BaseBoundary(Scanner scanner, MainController mainController, User currentUser) {
        this.scanner = scanner;
        this.mainController = mainController;
        this.currentUser = currentUser;
    }
    /**
     * Displays the menu specific to this boundary.
     */
    protected abstract void displayMenu();

    /**
     * Processes the user's menu choice.
     * @param choice The integer choice entered by the user.
     * @return true if the menu loop should continue, false to exit the loop.
     */
    protected abstract boolean processCommandOption(int choice);

     /**
     * Gets integer input from the user safely.
     * @param prompt The message to display to the user.
     * @return The integer entered, or -1 if input is invalid.
     */
    protected int getUserChoice(String prompt) {
        System.out.print(prompt);
        int choice = -1;
        try {
            String line = scanner.nextLine();
            choice = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
        }
        return choice;
    }

     /**
     * Gets non-empty string input from the user.
     * @param prompt The message to display to the user.
     * @return The string entered by the user.
     */
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


    /**
     * Runs the main interaction loop for this boundary.
     * Displays the menu, gets choice, processes command until exit condition.
     */
    public void runMenuLoop() {
        boolean keepRunning = true;
        while (keepRunning) {
            displayMenu();
            int choice = getUserChoice("Enter your choice: ");
            if (choice != -1) { // Only process valid integer inputs
                 keepRunning = processCommandOption(choice);
            }
            // Pause slightly or wait for Enter press for better UX
            if (keepRunning) {
                 System.out.println("\nPress Enter to continue...");
                 scanner.nextLine(); // Consume the leftover newline
            }
        }
    }
}