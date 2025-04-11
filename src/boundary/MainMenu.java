package boundary;

import boundary.auth.AuthUI;
import utils.io.IntScanner;
import utils.ui.MenuBuilder;

public class MainMenu {
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
                        System.out.println("Program terminating...");
                        System.exit(0);
                    }
                    case 1 -> {
                        AuthUI.login();
                    }
                    default -> System.out.println("Invalid option! Please try again.");
                }
            }
        } catch (Exception e) {
            start();
        }
    }
}
