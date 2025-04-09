package entities.request;

public class Request {
    private final String requestID;
    private String title;
    private final String senderID;
    private final String projectID;
    private final RequestType requestType;
    private RequestStatus requestStatus;

    /**
     * Constructs a Request object.
     *
     * @param requestID     Identifier of the request
     * @param title         Title of the request
     * @param senderID      Identifier of the sender
     * @param projectID     Identifier of the associated project
     * @param requestType   Type of request
     * @param requestStatus Status of request
     */
    public Request(String requestID, String title, String senderID,
    String projectID, RequestType requestType, RequestStatus requestStatus) {
        this.requestID = requestID;
        this.title = title;
        this.senderID = senderID;
        this.projectID = projectID;
        this.requestType = requestType;
        this.requestStatus = requestStatus;
    }


    // Getter Methods

    /**
     * Returns the identifier of the request.
     * @return requestID
     */
    public String getRequestID() {
        return requestID;
    }

    /**
     * Returns the title of the request.
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the request sender's identifier.
     * @return senderID
     */
    public String getSenderID() {
        return senderID;
    }

    /**
     * Returns the identifier of the project associated with the request.
     * @return projectID
     */
    public String getProjectID() {
        return projectID;
    }

    /**
     * Returns the type of the request.
     * @return requestType
     */
    public RequestType getRequestType() {
        return requestType;
    }

    /**
     * Returns the status of the request.
     * @return requestStatus
     */
    public RequestStatus getRequestStatus() {
        return requestStatus;
    }


    // Setter Methods

    /**
     * Updates the title of the request.
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Updates the status of this request (e.g., SUCCESSFUL, PENDING).
     * @param requestStatus
     */
    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }
}
