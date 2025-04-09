package entities.documents.approvableDocuments;

import entities.user.User;
import java.time.LocalDateTime;
import java.util.UUID;
import entities.documents.DocumentStatus;
import entities.documents.DocumentType;


public class Withdrawal implements IApprovableDocument {

    private final String documentID;
    private User applicant; // Applicant requesting withdrawal
    private ProjectApplication applicationToWithdraw; // The application being withdrawn
    private DocumentStatus status;
    private LocalDateTime submissionDate; // Date withdrawal was requested
    private LocalDateTime lastModifiedDate;
    private User lastModifiedBy;
    private String rejectionReason; // Reason withdrawal rejected
    private DocumentType documentType;


     public Withdrawal(User applicant, ProjectApplication applicationToWithdraw) {
        if (applicationToWithdraw == null) {
             throw new IllegalArgumentException("Application to withdraw cannot be null.");
        }
        this.documentID = "WDR-" + UUID.randomUUID().toString().substring(0, 8);
        this.applicant = applicant;
        this.applicationToWithdraw = applicationToWithdraw;
        this.status = DocumentStatus.DRAFT; // Request starts as draft
        this.submissionDate = null;
        this.lastModifiedDate = LocalDateTime.now();
        this.lastModifiedBy = applicant;
        this.documentType = DocumentType.APPLICATION;
         System.out.println("Created Draft Withdrawal Request: " + documentID + " for Application " + applicationToWithdraw.getDocumentID());
    }

    // --- Interface Methods Implementation (Stubs - adapt logic for withdrawal) ---

    @Override
    public String getDocumentID() { return documentID; }

    @Override
    public DocumentType getDocumentType() { return documentType; }

    @Override
    public User getSubmitter() { return applicant; }

    @Override
    public DocumentStatus getStatus() { return status; }

    @Override
    public boolean submit(User submitter) {
        // Check if the applicant owns the application and is submitting
        if (this.status == DocumentStatus.DRAFT && submitter.equals(this.applicant) && applicationToWithdraw.getSubmitter().equals(applicant)) {
             this.status = DocumentStatus.PENDING_APPROVAL; // Manager needs to approve withdrawal
             this.submissionDate = LocalDateTime.now();
             this.lastModifiedDate = this.submissionDate;
             this.lastModifiedBy = submitter;
              System.out.println("Withdrawal " + documentID + " submitted by " + submitter.getNric());
             return true;
        }
        System.out.println("Submission failed for " + documentID + ". Invalid status ("+this.status+"), submitter, or application ownership.");
        return false;
    }

    @Override
    public boolean edit(User editor, Object newContent) {
        // Withdrawals likely not editable, only submittable/deletable in draft
         System.out.println("Editing not supported for Withdrawal " + documentID);
        return false;
    }

    @Override
    public boolean delete(User deleter) {
        // Applicant can delete the withdrawal request if it's still in DRAFT
        if (this.status == DocumentStatus.DRAFT && deleter.equals(this.applicant)) {
             this.status = DocumentStatus.CLOSED;
              System.out.println("Withdrawal Request " + documentID + " deleted by " + deleter.getNric());
             // TODO: Remove from data store
             return true;
        }
        System.out.println("Deletion failed for " + documentID + ". Invalid status ("+this.status+") or deleter.");
        return false;
    }

    @Override
    public boolean approve(User approver) {
        // Manager approves the withdrawal
        // TODO: Role check (Manager)
         if (this.status == DocumentStatus.PENDING_APPROVAL /* && approver.getRole() == Role.HDB_MANAGER */) {
             this.status = DocumentStatus.APPROVED;
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = approver;
             this.rejectionReason = null;
             // TODO: Update the status of the original applicationToWithdraw
             // applicationToWithdraw.setStatus(DocumentStatus.WITHDRAWN); // Or similar
             // TODO: Potentially release flat unit if already booked
             System.out.println("Withdrawal " + documentID + " approved by " + approver.getNric());
             return true;
         }
        System.out.println("Approval failed for " + documentID + ". Invalid status ("+this.status+") or approver role.");
        return false;
    }

    @Override
    public boolean reject(User rejector, String reason) {
        // Manager rejects the withdrawal request
         // TODO: Role check (Manager)
         if (this.status == DocumentStatus.PENDING_APPROVAL /* && rejector.getRole() == Role.HDB_MANAGER */) {
             this.status = DocumentStatus.REJECTED; // Withdrawal request rejected, original application stands
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = rejector;
             this.rejectionReason = reason;
              System.out.println("Withdrawal " + documentID + " rejected by " + rejector.getNric() + ". Reason: " + reason);
             return true;
         }
        System.out.println("Rejection failed for " + documentID + ". Invalid status ("+this.status+") or rejector role.");
        return false;
    }
     // Add specific getters (e.g., getApplicationToWithdraw)
      public ProjectApplication getApplicationToWithdraw() {
          return applicationToWithdraw;
      }
}


//---