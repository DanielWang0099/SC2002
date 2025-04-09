package entities.documents;

public enum DocumentStatus {
    DRAFT,       // Initial state, can be edited/deleted
    SUBMITTED,   // Submitted for review/action
    PENDING_APPROVAL, // Awaiting approval/rejection
    APPROVED,
    REJECTED,
    WITHDRAWN,   // If applicable
    REPLIED,     // For enquiries
    CLOSED       // Final state
}