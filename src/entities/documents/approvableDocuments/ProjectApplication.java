package entities.documents.approvableDocuments;

import entities.user.User;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

import entities.documents.DocumentStatus;
import entities.documents.DocumentType;
import entities.project.*;


public class ProjectApplication implements IApprovableDocument {

    private final String documentID;
    private User applicant;
    private DocumentStatus status;
    private LocalDateTime submissionDate;
    private LocalDateTime lastModifiedDate;
    private User lastModifiedBy; // Keep User reference for runtime logic
    private String lastModifiedByNric; // ADDED: Store NRIC for persistence consistency
    private String rejectionReason;
    private DocumentType documentType; // Should always be APPLICATION
    private String projectName; // Store project name
    private FlatType bookedFlatType; // <-- ADDED FIELD for booked flat
    
    /**
     * Constructor suitable for loading data or full initialization.
     * Generates ID if null/empty is passed. Sets default status if null.
     */
    public ProjectApplication(String documentID,
                              User applicant,
                              String projectName, // Store projectName directly
                              DocumentStatus status,
                              LocalDateTime submissionDate,
                              LocalDateTime lastModifiedDate,
                              User lastModifiedBy, // Can be null during loading if only NRIC known initially
                              String lastModifiedByNric, // Added NRIC
                              String rejectionReason,
                              FlatType bookedFlatType // Added booked type
                              /* Project project object removed from constructor for looser coupling */
                              ) {
        // Validate essential fields
        Objects.requireNonNull(applicant, "Applicant cannot be null");
        Objects.requireNonNull(projectName, "Project Name cannot be null");

        this.documentID = (documentID == null || documentID.trim().isEmpty())
                            ? "APP-" + UUID.randomUUID().toString().substring(0, 8) // Generate ID if needed
                            : documentID;
        this.documentType = DocumentType.APPLICATION; // Always set type
        this.applicant = applicant;
        this.projectName = projectName;
        this.status = (status == null) ? DocumentStatus.DRAFT : status; // Default to DRAFT if null
        this.submissionDate = submissionDate;
        this.lastModifiedDate = (lastModifiedDate == null && status == DocumentStatus.DRAFT) ? LocalDateTime.now() : lastModifiedDate; // Sensible default?
        this.lastModifiedBy = lastModifiedBy; // Can be null initially if loaded from NRIC
        this.lastModifiedByNric = (lastModifiedBy != null) ? lastModifiedBy.getNric() : lastModifiedByNric; // Use NRIC from User if available
        this.rejectionReason = rejectionReason;
        this.bookedFlatType = bookedFlatType; // Set booked type

        // Ensure NRIC derived if user object passed
        if (this.lastModifiedBy != null && this.lastModifiedByNric == null) {
             this.lastModifiedByNric = this.lastModifiedBy.getNric();
        }

        // System.out.println("Loaded/Created Project Application: " + this.documentID); // Adjusted log
    }

    /**
     * Static factory method to create a brand new application in DRAFT state.
     * @param applicant The applicant user.
     * @param project The project being applied for.
     * @return A new ProjectApplication instance.
     */
    public static ProjectApplication createNewProjectApplication(User applicant, Project project) {
        Objects.requireNonNull(project, "Project cannot be null for new application");
        LocalDateTime now = LocalDateTime.now();
        // Sets status to DRAFT, submissionDate to null, lastModified to now/applicant, others null/default.
        // Calls the main constructor. Booked type is null initially.
        return new ProjectApplication(null, applicant, project.getName(), DocumentStatus.DRAFT, null, now, applicant, applicant.getNric(), null, null);
    }

    // --- Getters (including new ones) ---

    @Override public String getDocumentID() { return documentID; }
    @Override public DocumentType getDocumentType() { return documentType; }
    @Override public User getSubmitter() { return applicant; } // Applicant is the submitter
    @Override public DocumentStatus getStatus() { return status; }
    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public LocalDateTime getLastModifiedDate() { return lastModifiedDate; }
    public User getLastModifiedBy() { // Maybe lookup user from NRIC if needed and lastModifiedBy is null?
        // if (lastModifiedBy == null && lastModifiedByNric != null) {
        //     // Optional: Lookup user from repository - creates dependency
        //     // lastModifiedBy = Database.getUsersRepository().findUserByNric(lastModifiedByNric).orElse(null);
        // }
         return lastModifiedBy;
    }
    public String getLastModifiedByNric() { return lastModifiedByNric; } // Getter for NRIC
    public String getRejectionReason() { return rejectionReason; }
    public String getProjectName() { return projectName; }
    public FlatType getBookedFlatType() { return bookedFlatType; } // <-- ADDED GETTER
    public User getApplicant() { return applicant; } // Alias

    // --- Setters (for fields mutable after creation / during loading) ---

    // Applicant might not be settable after creation? Depends on design.
    // public void setApplicant(User applicant) { this.applicant = applicant; }

    public void setStatus(DocumentStatus status) { this.status = status; }
    public void setSubmissionDate(LocalDateTime submissionDate) { this.submissionDate = submissionDate; }
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
    public void setLastModifiedBy(User lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
        // Update NRIC when User object is set
        this.lastModifiedByNric = (lastModifiedBy != null) ? lastModifiedBy.getNric() : null;
    }
     // Setter for NRIC, might be used during loading before User lookup
     public void setLastModifiedByNric(String lastModifiedByNric) { this.lastModifiedByNric = lastModifiedByNric; }

    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    // ProjectName likely shouldn't change after creation?
    // public void setProjectName(String projectName) { this.projectName = projectName; }
    // DocumentType shouldn't change
    // public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public void setBookedFlatType(FlatType bookedFlatType) { this.bookedFlatType = bookedFlatType; } // <-- ADDED SETTER


    // --- Interface Methods Implementation (Updating last modified fields) ---

    @Override
    public boolean submit(User submitter) {
        if (this.status == DocumentStatus.DRAFT && submitter.equals(this.applicant)) {
            this.status = DocumentStatus.PENDING_APPROVAL;
            LocalDateTime now = LocalDateTime.now();
            this.submissionDate = now; // Set submission date
            this.lastModifiedDate = now;
            this.lastModifiedBy = submitter;
            this.lastModifiedByNric = submitter.getNric(); // Set NRIC too
            System.out.println("Application " + documentID + " submitted by " + submitter.getNric());
            return true;
        }
        System.out.println("Submission failed for " + documentID + ". Invalid status ("+this.status+") or submitter.");
        return false;
    }


    @Override
    public boolean edit(User editor, Object newContent) {
         if (this.status == DocumentStatus.DRAFT && editor.equals(this.applicant)) {
             // TODO: Implement actual editing logic based on 'newContent'
             // Example: if (newContent instanceof Map) { updateFields((Map) newContent); }
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = editor;
             this.lastModifiedByNric = editor.getNric(); // Set NRIC
             System.out.println("Application " + documentID + " edited by " + editor.getNric() + " (Logic Pending).");
             return true; // Return true if conceptually editable
         }
         System.out.println("Editing failed for " + documentID + ". Invalid status ("+this.status+") or editor.");
         return false;
    }

    @Override
    public boolean delete(User deleter) {
        if (this.status == DocumentStatus.DRAFT && deleter.equals(this.applicant)) {
            // Logic to physically remove from repository happens in Controller/Repository
            this.status = DocumentStatus.CLOSED; // Mark status as closed/deleted
            this.lastModifiedDate = LocalDateTime.now();
            this.lastModifiedBy = deleter;
            this.lastModifiedByNric = deleter.getNric(); // Set NRIC
            System.out.println("Application " + documentID + " marked as deleted by " + deleter.getNric());
            return true; // Indicates status updated, repo handles removal
        }
        System.out.println("Deletion failed for " + documentID + ". Invalid status ("+this.status+") or deleter.");
        return false;
    }

    @Override
    public boolean approve(User approver) {
        // Role check should happen in Controller before calling this
        if (this.status == DocumentStatus.PENDING_APPROVAL /* && role check done */) {
             this.status = DocumentStatus.APPROVED;
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = approver;
             this.lastModifiedByNric = approver.getNric(); // Set NRIC
             this.rejectionReason = null;
             System.out.println("Application " + documentID + " approved by " + approver.getNric());
             return true;
        }
        System.out.println("Approval failed for " + documentID + ". Invalid status ("+this.status+").");
        return false;
    }

    @Override
    public boolean reject(User rejector, String reason) {
         // Role check should happen in Controller
         if (this.status == DocumentStatus.PENDING_APPROVAL /* && role check done */) {
             this.status = DocumentStatus.REJECTED;
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = rejector;
             this.lastModifiedByNric = rejector.getNric(); // Set NRIC
             this.rejectionReason = reason;
             System.out.println("Application " + documentID + " rejected by " + rejector.getNric() + ". Reason: " + reason);
             return true;
         }
         System.out.println("Rejection failed for " + documentID + ". Invalid status ("+this.status+").");
         return false;
    }

}