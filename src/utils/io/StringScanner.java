package utils.io;

import java.util.Scanner;

/**
 * Provides safe string input handling with validation and error recovery.
 */
public class StringScanner {
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Prompts for and validates string input with error handling.
     * 
     * @param prompt    The message displayed to the user.
     * @return Validated string input.
     */
    public static String scan(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return scanner.nextLine().trim();
            } catch (Exception e) {
                System.out.println("Input Error: Please enter a valid input!");
            }
        }
    }
}
