package controller;

import controller.usersController.ApplicantController;
import controller.usersController.HdbManagerController;
import controller.usersController.HdbOfficerController;

public class MainController {
    private final UserAuthenticationController authController;
    private final ApplicantController applicantController;
    private final HdbOfficerController hdbOfficerController;
    private final HdbManagerController hdbManagerController;
    private final ProjectController projectController; // Added ProjectController

    public MainController() {
        System.out.println("Initializing MainController...");
        // Initialize all specific controllers
        // The Database facade (and its data loading) is implicitly initialized
        // when the controllers access it for the first time, or when accessed earlier.
        this.authController = new UserAuthenticationController();
        this.projectController = new ProjectController(); // Initialize ProjectController
        this.applicantController = new ApplicantController(projectController); // Pass ProjectController if needed
        this.hdbOfficerController = new HdbOfficerController(projectController, applicantController); // Pass dependencies
        this.hdbManagerController = new HdbManagerController(projectController); // Pass ProjectController
        System.out.println("MainController initialized.");
    }

    // Provide getters for boundaries to access specific controllers
    public UserAuthenticationController getAuthController() { return authController; }
    public ApplicantController getApplicantController() { return applicantController; }
    public HdbOfficerController getHdbOfficerController() { return hdbOfficerController; }
    public HdbManagerController getHdbManagerController() { return hdbManagerController; }
    public ProjectController getProjectController() { return projectController; } // Getter for ProjectController
}