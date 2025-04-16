package entities.documents.repliableDocuments;

import entities.documents.DocumentStatus;
import entities.user.User;
import java.time.LocalDateTime;
import java.util.UUID;
import entities.documents.DocumentType;
import entities.project.*;

public class Enquiry implements IReplyableDocument {

    private final String documentID;
    private User submitter; // User making the enquiry
    // private Project project; // Project the enquiry is about
    private String enquiryContent;
    private String replyContent;
    private User replier; // Officer/Manager who replied
    private DocumentStatus status;
    private LocalDateTime submissionDate;
    private LocalDateTime lastModifiedDate;
    private User lastModifiedBy;
    private LocalDateTime replyDate;
    private DocumentType documentType;
    private String projectName;


    public Enquiry(String documentID,
                   User submitter,
                   Project project,
                   String enquiryContent,
                   DocumentStatus status,
                   LocalDateTime submissionDate,
                   LocalDateTime lastModifiedDate,
                   User lastModifiedBy,
                   String replyContent,
                   User replier,
                   LocalDateTime replyDate) {
        this.documentID = (documentID == null || documentID.trim().isEmpty())
                          ? "ENQ-" + UUID.randomUUID().toString().substring(0, 8)
                          : documentID;
        this.submitter = submitter;
        this.projectName = project.getName();
        this.enquiryContent = enquiryContent;
        this.status = status;
        this.submissionDate = submissionDate;
        this.lastModifiedDate = lastModifiedDate;
        this.lastModifiedBy = lastModifiedBy;
        this.replyContent = replyContent;
        this.replier = replier;
        this.replyDate = replyDate;
        // Optionally, change DocumentType if a dedicated ENQUIRY type exists.
        this.documentType = DocumentType.APPLICATION;
        System.out.println("Created Enquiry: " + this.documentID);
    }

    public static Enquiry createNewEnquiry(User submitter, Project project, String enquiryContent) {
        LocalDateTime now = LocalDateTime.now();
        // Sets status to DRAFT, submissionDate to null, and lastModifiedDate to now.
        return new Enquiry(
            null,
            submitter,
            project,
            enquiryContent,
            DocumentStatus.DRAFT,
            null,
            now,
            submitter,
            null,
            null,
            null
        );
    }

    // --- Interface Methods Implementation (Stubs - adapt for reply logic) ---
    @Override
    public String getDocumentID() { return documentID; }

    @Override
    public DocumentType getDocumentType() { return documentType; }

    @Override
    public User getSubmitter() { return submitter; }

    @Override
    public DocumentStatus getStatus() { return status; }


     @Override
    public boolean submit(User submitter) {
        if (this.status == DocumentStatus.DRAFT && submitter.equals(this.submitter)) {
            this.status = DocumentStatus.SUBMITTED; // Submitted, waiting for reply
            this.submissionDate = LocalDateTime.now();
            this.lastModifiedDate = this.submissionDate;
            this.lastModifiedBy = submitter;
            System.out.println("Enquiry " + documentID + " submitted by " + submitter.getNric());
            return true;
        }
         System.out.println("Submission failed for " + documentID + ". Invalid status ("+this.status+") or submitter.");
        return false;
    }


    @Override
    public boolean edit(User editor, Object newContent) {
        // Check authorization: Only the original submitter can edit.
        if (!editor.equals(this.submitter)) {
            System.err.println("Edit Error: User " + editor.getNric() + " is not the submitter of enquiry " + getDocumentID());
            return false;
        }
    
        // Check status: Cannot edit if already REPLIED or CLOSED.
        if (this.status == DocumentStatus.REPLIED || this.status == DocumentStatus.CLOSED) {
            System.err.println("Edit Error: Enquiry " + getDocumentID() + " cannot be edited because its status is " + this.status);
            return false;
        }
    
        // Validate new content (assuming it should be a non-blank String)
        if (!(newContent instanceof String) || ((String) newContent).isBlank()) {
            System.err.println("Edit Error: New enquiry content must be a non-empty string.");
            return false;
        }
    
        // Perform the edit
        this.enquiryContent = (String) newContent; // Requires enquiryContent setter or direct access
        this.lastModifiedDate = LocalDateTime.now();
        this.lastModifiedBy = editor;
    
        System.out.println("Enquiry " + documentID + " content updated by " + editor.getNric());
        // NOTE: The status might remain SUBMITTED or PENDING, etc. Editing doesn't change the status here.
        return true; // Edit was successful
    }

    @Override
    public boolean delete(User deleter) {
        // Check authorization: Only the original submitter can delete.
        if (!deleter.equals(this.submitter)) {
            System.err.println("Delete Error: User " + deleter.getNric() + " is not the submitter of enquiry " + getDocumentID());
            return false;
        }
    
        // --- UPDATED STATUS CHECK ---
        // Allow deletion as long as it hasn't been REPLIED to or already CLOSED.
        if (this.status == DocumentStatus.REPLIED || this.status == DocumentStatus.CLOSED) {
            System.err.println("Delete Error: Enquiry " + getDocumentID() + " cannot be deleted because its status is " + this.status);
            return false;
        }
        // --- -------------------- ---
    
        // Mark status as closed/deleted conceptually
        // The actual removal from the repository is handled by the Controller/Repository call
        this.status = DocumentStatus.CLOSED;
        this.lastModifiedDate = LocalDateTime.now();
        this.lastModifiedBy = deleter;
        System.out.println("Enquiry " + documentID + " marked as deleted by " + deleter.getNric());
        // Return true indicates the status was successfully updated in the object
        return true;
    }

    @Override
    public boolean reply(User replier, String replyContent) {
        // Officer/Manager replies to a SUBMITTED enquiry
        // TODO: Role check for replier (Officer/Manager)
        if (this.status == DocumentStatus.SUBMITTED /* && replier is Officer/Manager */) {
            this.status = DocumentStatus.REPLIED;
            this.replyContent = replyContent;
            this.replier = replier;
            this.replyDate = LocalDateTime.now();
            this.lastModifiedDate = this.replyDate;
            this.lastModifiedBy = replier;
             System.out.println("Enquiry " + documentID + " replied to by " + replier.getNric());
            return true;
        }
        System.out.println("Reply failed for " + documentID + ". Invalid status ("+this.status+") or replier role.");
        return false;
    }

     @Override
     public String getReplyContent() {
         return replyContent;
     }

     @Override
     public User getReplier() {
         return replier;
     }

     // --- Other Getters ---
     public String getEnquiryContent() { return enquiryContent; }
     public String getProjectName() { return projectName; }

          // New getters for remaining attributes:
    public LocalDateTime getSubmissionDate() {
            return submissionDate;
        }
        
    public LocalDateTime getLastModifiedDate() {
            return lastModifiedDate;
        }
        
    public User getLastModifiedBy() {
            return lastModifiedBy;
        }

    public LocalDateTime getReplyDate() {
        return replyDate;
    }
}