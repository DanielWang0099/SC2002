package entities.documents.repliableDocuments;

import entities.documents.DocumentStatus;
import entities.user.User;
import java.time.LocalDateTime;
import java.util.UUID;
import entities.documents.DocumentType;

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


     public Enquiry(User submitter, /*Project project,*/ String enquiryContent) {
        this.documentID = "ENQ-" + UUID.randomUUID().toString().substring(0, 8);
        this.submitter = submitter;
        // this.project = project;
        this.enquiryContent = enquiryContent;
        this.status = DocumentStatus.DRAFT; // Starts as draft
        this.submissionDate = null;
        this.lastModifiedDate = LocalDateTime.now();
        this.lastModifiedBy = submitter;
        this.documentType = DocumentType.APPLICATION;
         System.out.println("Created Draft Enquiry: " + documentID);
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
         // Applicant can edit their enquiry if it's still in DRAFT
         if (this.status == DocumentStatus.DRAFT && editor.equals(this.submitter) && newContent instanceof String) {
             this.enquiryContent = (String) newContent;
             this.lastModifiedDate = LocalDateTime.now();
             this.lastModifiedBy = editor;
              System.out.println("Enquiry " + documentID + " edited by " + editor.getNric());
             return true;
         }
          System.out.println("Editing failed for " + documentID + ". Invalid status ("+this.status+"), editor, or content type.");
         return false;
    }

    @Override
    public boolean delete(User deleter) {
       // Applicant can delete their enquiry if it's still in DRAFT
        if (this.status == DocumentStatus.DRAFT && deleter.equals(this.submitter)) {
            this.status = DocumentStatus.CLOSED; // Or a specific DELETED status
             System.out.println("Enquiry " + documentID + " deleted by " + deleter.getNric());
            // TODO: Remove from data store
            return true;
        }
         System.out.println("Deletion failed for " + documentID + ". Invalid status ("+this.status+") or deleter.");
        return false;
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
     // Add getters for project, dates etc.
}