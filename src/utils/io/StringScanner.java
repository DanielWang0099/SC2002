package utils.io;

import java.util.Scanner;

public class StringScanner {
    private static final Scanner scanner = new Scanner(System.in);

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
