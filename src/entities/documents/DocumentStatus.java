package entities.documents;

/**
 * This enum represents the different states of a document.
 */
public enum DocumentStatus {
    /** Document's initial state, can be edited/deleted */
    DRAFT,
    /** Document has been submitted for processing */
    SUBMITTED,
    /** Document awaiting approval/rejection */
    PENDING_APPROVAL,
    /** Document officially approved */
    APPROVED,
    /** Document officially rejected */
    REJECTED,
    /** Document has been withdrawn, if applicable */
    WITHDRAWN,
    /** Document (Enquiry) has received a response */
    REPLIED,
    /** Document reached end of lifecycle, final state */
    CLOSED,
    /** Document approved and flat booking completed */
    BOOKED
}