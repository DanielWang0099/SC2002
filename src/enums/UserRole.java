package enums;

/**
 * This enum represents the various user roles, 
 * facilitates implementation of role-specific capabilities.
 */
public enum UserRole {
    /** UserRole with project application capabilities. */
    APPLICANT,
    /** UserRole with flat allocation and enquiry management privileges. */
    HDB_OFFICER,
    /** UserRole with highest permission and access capabiltiies. */
    HDB_MANAGER
}
