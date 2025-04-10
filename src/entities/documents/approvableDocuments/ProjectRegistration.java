package entities.documents.approvableDocuments;

import entities.user.User;
import java.time.LocalDateTime;
import java.util.UUID;
import entities.documents.DocumentStatus;
import entities.documents.DocumentType;


public class ProjectRegistration implements IApprovableDocument {

    private final String documentID;
    private User officer; // Officer requesting registration
    // private Project project; // Project to register for
    private DocumentStatus status;
    private LocalDateTime submissionDate;
    private LocalDateTime lastModifiedDate;
    private User lastModifiedBy;
    private String rejectionReason;
    private DocumentType documentType;

     public ProjectRegistration(User officer /*, Project project*/) {
        this.documentID = "REG-" + UUID.randomUUID().toString().substring(0, 8);
        this.officer = officer;
        // this.project = project;
        this.status = DocumentStatus.DRAFT;
        this.submissionDate = null;
        this.lastModifiedDate = LocalDateTime.now();
        this.lastModifiedBy = officer;
        this.documentType = DocumentType.APPLICATION;
         System.out.println("Created Draft Project Registration: " + documentID);
    }

    // --- Interface Methods Implementation (Stubs - similar to ProjectApplication) ---

    @Override
    public String getDocumentID() { return documentID; }

    @Override
    public DocumentType getDocumentType() { return documentType; }
    
    @Override
    public User getSubmitter() { return officer; }

    @Override
    public DocumentStatus getStatus() { return status; }

    @Override
    public boolean submit(User submitter) {
         if (this.status == DocumentStatus.DRAFT && submitter.equals(this.officer)) {
            this.status = DocumentStatus.PENDING_APPROVAL;
            this.submissionDate = LocalDateTime.now();
            this.lastModifiedDate = this.submissionDate;
            this.lastModifiedBy = submitter;
            System.out.println("Registration " + documentID + " submitted by " + submitter.getNric());
            return true;
        }
        System.out.println("Submission failed for " + documentID + ". Invalid status ("+this.status+") or submitter.");
        return false;
    }

    @Override
    public boolean edit(User editor, Object newContent) {
        // Typically registrations might not be editable after creation, maybe only deletable in draft.
         if (this.status == DocumentStatus.DRAFT && editor.equals(this.officer)) {
             // If editing is allowed, implement here
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = editor;
             System.out.println("Registration " + documentID + " edited by " + editor.getNric() + " (if supported).");
             return true; // Change if editing not allowed
         }
         System.out.println("Editing failed/not supported for " + documentID);
         return false;
    }

    @Override
    public boolean delete(User deleter) {
        if (this.status == DocumentStatus.DRAFT && deleter.equals(this.officer)) {
            this.status = DocumentStatus.CLOSED;
             System.out.println("Registration " + documentID + " deleted by " + deleter.getNric());
            // TODO: Remove from data store
            return true;
        }
         System.out.println("Deletion failed for " + documentID + ". Invalid status ("+this.status+") or deleter.");
        return false;
    }

    @Override
    public boolean approve(User approver) {
        // Approved by HDB Manager in charge of the project
        // TODO: Role check for approver (Manager) and check if they manage the project
         if (this.status == DocumentStatus.PENDING_APPROVAL /* && approver manages project */ ) {
             this.status = DocumentStatus.APPROVED;
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = approver;
             this.rejectionReason = null;
              System.out.println("Registration " + documentID + " approved by " + approver.getNric());
             // TODO: Update officer's profile, project's officer list, officer slots
             return true;
         }
         System.out.println("Approval failed for " + documentID + ". Invalid status ("+this.status+") or approver role/project assignment.");
         return false;
    }

    @Override
    public boolean reject(User rejector, String reason) {
         // Rejected by HDB Manager in charge of the project
         // TODO: Role check for rejector (Manager) and check if they manage the project
         if (this.status == DocumentStatus.PENDING_APPROVAL /* && rejector manages project */) {
             this.status = DocumentStatus.REJECTED;
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = rejector;
             this.rejectionReason = reason;
              System.out.println("Registration " + documentID + " rejected by " + rejector.getNric() + ". Reason: " + reason);
             return true;
         }
          System.out.println("Rejection failed for " + documentID + ". Invalid status ("+this.status+") or rejector role/project assignment.");
         return false;
    }
     // Add specific getters as needed
}


//---