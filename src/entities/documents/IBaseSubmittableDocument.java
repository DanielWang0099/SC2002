package entities.documents;

import entities.user.User;

/**
 * This interface defines common operations for documents that follow a
 * common submission lifecycle.
 */
public interface IBaseSubmittableDocument {

    /**
     * Gets the unique identifier for this document.
     * @return A unique String ID.
     */
    String getDocumentID();


    DocumentType getDocumentType();
    
    /**
     * Gets the user who submitted this document.
     * @return The submitting User object.
     */
    User getSubmitter();

    /**
     * Gets the current status of the document.
     * @return The DocumentStatus enum value.
     */
    DocumentStatus getStatus();


    /**
     * Allows editing the content of the document.
     * Specific implementation depends on the document type.
     * Typically only allowed in DRAFT status.
     * @param editor The user performing the edit.
     * @param newContent Object representing the new content (e.g., String, Map).
     * @return true if editing was successful, false otherwise.
     */
    boolean edit(User editor, Object newContent);

    /**
     * Allows deleting the document.
     * Typically only allowed in DRAFT status or by specific roles.
     * @param deleter The user performing the deletion.
     * @return true if deletion was successful, false otherwise.
     */
    boolean delete(User deleter);

     /**
     * Submits the document for processing (approval, reply, etc.).
     * Typically changes status from DRAFT to SUBMITTED or PENDING_APPROVAL.
     * @param submitter The user submitting the document.
     * @return true if submission was successful, false otherwise.
     */
    boolean submit(User submitter);

}