package boundary.user;

import boundary.auth.PasswordChangeUI;
import entity.user.User;
import utils.io.IntScanner;
import utils.ui.MenuBuilder;

public class ApplicantMainPage {
    public static void start(User user) {
        boolean logout = false;
        while(!logout) {
            int choice = MenuBuilder.create()
                .setHeader("BTOMS", "Applicant Dashboard")
                .setOptions(
                    "View Available Projects",
                    "Apply for Project",
                    "View Application Status",
                    "Withdraw Application",
                    "Manage Enquiries",
                    "Change Password")
                .setFooter("Logout")
                .render();
            
            switch(choice) {
                case 0 -> {
                    logout = true;
                    System.out.println("Logging out...");
                }
                case 1 -> {
                    // ProjectController.viewAvailableProjects(user);
                }
                case 2 -> {
                    // ApplicationController.applyForProject(user);
                }
                case 3 -> {
                    // ApplicationController.viewStatus(user);
                }
                case 4 -> {
                    // ApplicationController.withdrawApplication(user);
                }
                case 5 -> {
                    // EnquiryController.manageEnquiries(user);
                }
                case 6 -> {
                    boolean passwordChanged = PasswordChangeUI.changePassword(user.getNric());
                    if (passwordChanged) logout = true;
                }
                default -> System.out.println("Invalid option! Please try again.");
            }
        }
    }
}