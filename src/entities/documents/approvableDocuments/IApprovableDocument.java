package entities.documents.approvableDocuments;

import entities.user.User;
import entities.documents.IBaseSubmittableDocument;

public interface IApprovableDocument extends IBaseSubmittableDocument {

    /**
     * Approves the document.
     * Typically changes status to APPROVED.
     * Requires appropriate user role (e.g., HDB Manager).
     * @param approver The user approving the document.
     * @return true if approval was successful, false otherwise.
     */
    boolean approve(User approver);

    /**
     * Rejects the document.
     * Typically changes status to REJECTED.
     * Requires appropriate user role (e.g., HDB Manager).
     * @param rejector The user rejecting the document.
     * @param reason Optional reason for rejection.
     * @return true if rejection was successful, false otherwise.
     */
    boolean reject(User rejector, String reason);

}

//---

/**
 * Interface for documents that can be replied to, like enquiries.
 * Extends basic submittable actions.
 */