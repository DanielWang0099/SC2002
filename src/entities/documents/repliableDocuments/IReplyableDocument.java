package entities.documents.repliableDocuments;

import entities.documents.IBaseSubmittableDocument;
import entities.user.User;

public interface IReplyableDocument extends IBaseSubmittableDocument {

    /**
     * Adds a reply to the document.
     * Typically changes status to REPLIED or CLOSED.
     * Requires appropriate user role (e.g., HDB Officer/Manager).
     * @param replier The user providing the reply.
     * @param replyContent The content of the reply.
     * @return true if replying was successful, false otherwise.
     */
    boolean reply(User replier, String replyContent);

     /**
      * Gets the reply content, if any.
      * @return The reply string, or null if not replied.
      */
     String getReplyContent();

     /**
      * Gets the user who replied, if any.
      * @return The replying User object, or null if not replied.
      */
     User getReplier();
}