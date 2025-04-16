package utilities.io;

import java.util.Scanner;

/**
 * Provides safe integer input handling with validation and error recovery.
 * Used generally in our program for menu selections and numerical data entry.
 */
public class IntScanner {
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Prompts for and validates integer input with error handling.
     * 
     * @param prompt    The message displayed to the user.
     * @return Validated integer input.
     */
    public static int scan(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return scanner.nextInt();
            } catch (Exception e) {
                System.out.println("Input Error: Please enter a valid integer!");
                // Pause for user to read messages
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
                // To clear input buffer
                scanner.nextLine(); // to clear input buffer
            }
        }
    }

    /**
     * Properly closes the scanner, to be called when program exits.
     */
    public static void close() {
        scanner.close();
        System.out.println("[IntScanner] Scanner closed.");
    }
}
