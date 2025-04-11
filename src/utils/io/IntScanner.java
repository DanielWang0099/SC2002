package utils.io;

import java.util.Scanner;

public class IntScanner {
    private static final Scanner scanner = new Scanner(System.in);

    public static int scan(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return scanner.nextInt();
            } catch (Exception e) {
                System.out.println("Input Error: Please enter a valid integer!");
                scanner.nextLine(); // to clear input buffer
            }
        }
    }
}
