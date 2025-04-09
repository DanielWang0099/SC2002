package entities.request;

/**
 * This enum represents the different statuses that a request can have.
 */
public enum RequestStatus {
    /** Request is currently pending, awaiting further action */
    PENDING,
    /** Request has been processed, and was successful */
    SUCCESSFUL,
    /** Request has been processed, and was unsuccessful */
    UNSUCCESSFUL,
    /** Request was successful, and completed a flat booking.  */
    BOOKED
}
