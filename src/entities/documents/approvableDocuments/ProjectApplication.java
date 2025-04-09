package entities.documents;

import entities.user.User;
import java.time.LocalDateTime;
import java.util.UUID;


public class ProjectApplication implements IApprovableDocument {

    private final String documentID;
    private User applicant; // The user applying
    // private Project project; // The project being applied for
    private DocumentStatus status;
    private LocalDateTime submissionDate;
    private LocalDateTime lastModifiedDate;
    private User lastModifiedBy;
    private String rejectionReason; // Store reason if rejected

    public ProjectApplication(User applicant /*, Project project*/) {
        this.documentID = "APP-" + UUID.randomUUID().toString().substring(0, 8);
        this.applicant = applicant;
        // this.project = project;
        this.status = DocumentStatus.DRAFT; // Start as draft
        this.submissionDate = null;
        this.lastModifiedDate = LocalDateTime.now();
        this.lastModifiedBy = applicant;
        System.out.println("Created Draft Project Application: " + documentID);
    }

    // --- Interface Methods Implementation (Stubs) ---

    @Override
    public String getDocumentID() {
        return documentID;
    }

     @Override
     public User getSubmitter() {
         return applicant;
     }

    @Override
    public DocumentStatus getStatus() {
        return status;
    }

    @Override
    public boolean submit(User submitter) {
        if (this.status == DocumentStatus.DRAFT && submitter.equals(this.applicant)) {
            this.status = DocumentStatus.PENDING_APPROVAL; // Or SUBMITTED
            this.submissionDate = LocalDateTime.now();
            this.lastModifiedDate = this.submissionDate;
            this.lastModifiedBy = submitter;
            System.out.println("Application " + documentID + " submitted by " + submitter.getNric());
            return true;
        }
        System.out.println("Submission failed for " + documentID + ". Invalid status ("+this.status+") or submitter.");
        return false;
    }


    @Override
    public boolean edit(User editor, Object newContent) {
        // Can only edit in DRAFT status, potentially by the applicant
         if (this.status == DocumentStatus.DRAFT && editor.equals(this.applicant)) {
             // TODO: Implement actual editing logic based on 'newContent'
             // e.g., update specific fields of the application
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = editor;
             System.out.println("Application " + documentID + " edited by " + editor.getNric());
             return true;
         }
         System.out.println("Editing failed for " + documentID + ". Invalid status ("+this.status+") or editor.");
         return false;
    }

    @Override
    public boolean delete(User deleter) {
       // Can only delete in DRAFT status, potentially by the applicant
        if (this.status == DocumentStatus.DRAFT && deleter.equals(this.applicant)) {
            // TODO: Add logic to remove from any tracking lists (e.g., Database)
            this.status = DocumentStatus.CLOSED; // Mark as closed/deleted conceptually
            System.out.println("Application " + documentID + " deleted by " + deleter.getNric());
            return true;
        }
        System.out.println("Deletion failed for " + documentID + ". Invalid status ("+this.status+") or deleter.");
        return false;
    }

    @Override
    public boolean approve(User approver) {
        // Can only approve if PENDING_APPROVAL, by appropriate role (e.g., Manager)
        // TODO: Add role check for approver
        if (this.status == DocumentStatus.PENDING_APPROVAL /* && approver.getRole() == Role.HDB_MANAGER */) {
             this.status = DocumentStatus.APPROVED;
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = approver;
             this.rejectionReason = null;
             System.out.println("Application " + documentID + " approved by " + approver.getNric());
             return true;
        }
        System.out.println("Approval failed for " + documentID + ". Invalid status ("+this.status+") or approver role.");
        return false;
    }

    @Override
    public boolean reject(User rejector, String reason) {
        // Can only reject if PENDING_APPROVAL, by appropriate role (e.g., Manager)
         // TODO: Add role check for rejector
         if (this.status == DocumentStatus.PENDING_APPROVAL /* && rejector.getRole() == Role.HDB_MANAGER */) {
             this.status = DocumentStatus.REJECTED;
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = rejector;
             this.rejectionReason = reason;
             System.out.println("Application " + documentID + " rejected by " + rejector.getNric() + ". Reason: " + reason);
             return true;
         }
         System.out.println("Rejection failed for " + documentID + ". Invalid status ("+this.status+") or rejector role.");
         return false;
    }

     // --- Getters for specific fields ---
     public String getRejectionReason() { return rejectionReason; }
     // Add getters for project, dates etc. as needed
}


//---