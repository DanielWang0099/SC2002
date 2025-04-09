package entities.request;

/**
 * This enum represents the different types of requests related to
 * Build-To-Order (BTO) projects.
 */
public enum RequestType {
    /** Request to apply for a BTO project */
    APPLICATION,
    /** Request for an HDB Officer to register and join a BTO Project */
    REGISTRATION,
    /** Request to withdraw from an existing BTO application */
    WITHDRAWL,
}
