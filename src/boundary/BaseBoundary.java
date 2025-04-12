package boundary;

import java.util.Scanner;
import controller.MainController;
import entities.user.User;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;


public abstract class BaseBoundary {
    protected Scanner scanner;
    protected MainController mainController;
    protected User currentUser; // The user interacting with this boundary
    // Consistent date format for user input if needed
    protected static final SimpleDateFormat INPUT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    static { INPUT_DATE_FORMAT.setLenient(false); } // Make date parsing strict


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
     * @return true if the menu loop should continue, false to exit the loop (logout).
     */
    protected abstract boolean processCommandOption(int choice);

     /**
     * Gets integer input from the user safely.
     * @param prompt The message to display to the user.
     * @return The user's integer choice, or -1 if input is invalid or empty.
     */
    protected int getUserChoice(String prompt) {
        System.out.print(prompt);
        int choice = -1;
        try {
            String line = scanner.nextLine();
            if (line != null && !line.trim().isEmpty()) {
               choice = Integer.parseInt(line.trim());
            } else {
                 System.out.println("Invalid input. Please enter a number."); // Handle empty input here
            }
        } catch (NumberFormatException e) {
           System.out.println("Invalid input. Please enter a valid number.");
        }
        return choice;
    }

     /**
     * Gets non-empty string input from the user.
     * @param prompt The message to display to the user.
     * @return The non-empty string entered by the user (trimmed).
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
     * Gets yes/no confirmation from the user.
     * @param prompt The question to ask.
     * @return true for 'yes', false for 'no'.
     */
    protected boolean getYesNoInput(String prompt) {
        String input = "";
        while (true) {
            System.out.print(prompt + " (yes/no): ");
            input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("yes") || input.equals("y")) {
                return true;
            } else if (input.equals("no") || input.equals("n")) {
                return false;
            } else {
                System.out.println("Invalid input. Please enter 'yes' or 'no'.");
            }
        }
    }

     /**
     * Gets a date input from the user in yyyy-MM-dd format.
     * @param prompt The message to display.
     * @return The parsed Date object, or null if input is invalid.
     */
     protected Date getDateInput(String prompt) {
        Date date = null;
        while (date == null) {
            System.out.print(prompt + " (yyyy-MM-dd): ");
            String dateStr = scanner.nextLine().trim();
            if (dateStr.equalsIgnoreCase("cancel")) { // Allow cancellation
                 System.out.println("Date input cancelled.");
                 return null;
            }
            try {
                date = INPUT_DATE_FORMAT.parse(dateStr);
            } catch (ParseException e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd or type 'cancel'.");
            }
        }
        return date;
     }


    /**
     * Runs the main interaction loop for this boundary.
     * Displays the menu, gets choice, processes command until exit condition.
     */
    public void runMenuLoop() {
        boolean keepRunning = true;
        while (keepRunning) {
            System.out.println(); // Add a blank line for spacing
            displayMenu();
            int choice = getUserChoice("Enter your choice: ");
            if (choice != -1) { // Only process valid integer inputs
                 // Add a separator line before processing output
                 System.out.println("------------------------------------------");
                 keepRunning = processCommandOption(choice);
            }
            // Pause slightly or wait for Enter press for better UX, unless logging out
            if (keepRunning) {
                 System.out.println("------------------------------------------");
                 System.out.print("Press Enter to return to menu...");
                 scanner.nextLine(); // Consume the leftover newline/wait for Enter
            }
        }
    }

     /**
     * Helper method within boundaries to handle the change password interaction.
     */
     protected void handleChangePassword() {
         System.out.println("\n--- Change Password ---");
         String oldPassword = getStringInput("Enter Current Password: ");
         String newPassword1 = getStringInput("Enter New Password: ");
         String newPassword2 = getStringInput("Confirm New Password: ");

         if (!newPassword1.equals(newPassword2)) {
             System.out.println("New passwords do not match. Password change cancelled.");
             return;
         }

         // Call the Authentication Controller via MainController
         boolean success = mainController.getAuthController().changePassword(currentUser, oldPassword, newPassword1);

         if (success) {
             System.out.println("Password change successful.");
             // Note: User object in currentUser is updated, data file saved on exit.
         } else {
             System.out.println("Password change failed. Please check current password and try again.");
             // Specific error messages (e.g., too short, same as old) handled by Controller.
         }
     }
}