package controller;

import entities.database.Database;

public class MainController {
    private final UserAuthenticationController authController;
    private final ApplicantController applicantController;
    private final HdbOfficerController hdbOfficerController;
    private final HdbManagerController hdbManagerController;

    public MainController() {
        // Initialize all specific controllers
        this.authController = new UserAuthenticationController();
        this.applicantController = new ApplicantController();
        this.hdbOfficerController = new HdbOfficerController();
        this.hdbManagerController = new HdbManagerController();
        // Trigger Database initialization if not already done by accessing it
    }

    // Provide getters for boundaries to access specific controllers
    public UserAuthenticationController getAuthController() { return authController; }
    public ApplicantController getApplicantController() { return applicantController; }
    public HdbOfficerController getHdbOfficerController() { return hdbOfficerController; }
    public HdbManagerController getHdbManagerController() { return hdbManagerController; }
}