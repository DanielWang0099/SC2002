package entities.database.repositories;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import entities.database.repositories.documentsRepositories.*;
import entities.documents.approvableDocuments.*;
import entities.documents.*;
import entities.documents.repliableDocuments.*;
import java.util.stream.Stream;

/**
 * Repository for managing various Document entities.
 * Uses Document ID (String) as the key.
 */
public class DocumentsRepository { // Not implementing IRepository<IBaseSubmittableDocument, String> directly

    private final ApplicationRepository applicationRepository;
    private final RegistrationRepository registrationRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final EnquiryRepository enquiryRepository;

    // Package-private constructor, managed by Database facade
    public DocumentsRepository() {
        System.out.println("Initializing DocumentsRepository Facade...");
        this.applicationRepository = new ApplicationRepository();
        this.registrationRepository = new RegistrationRepository();
        this.withdrawalRepository = new WithdrawalRepository();
        this.enquiryRepository = new EnquiryRepository();
        loadInitialDocuments(); // If applicable
        System.out.println("DocumentsRepository Facade initialized.");
    }

    private void loadInitialDocuments() {
        // TODO: Load any persistent documents from storage if needed
        System.out.println("Document loading placeholder (if needed).");
    }

    /**
     * Saves a document to the appropriate repository based on its type.
     * @param document The document object to save.
     */
    public void saveDocument(IBaseSubmittableDocument document) {
        if (document == null) {
             System.err.println("Attempted to save a null document.");
             return; // Or throw exception
        }

        if (document instanceof ProjectApplication) {
            applicationRepository.save((ProjectApplication) document);
        } else if (document instanceof ProjectRegistration) {
            registrationRepository.save((ProjectRegistration) document);
        } else if (document instanceof Withdrawal) {
            withdrawalRepository.save((Withdrawal) document);
        } else if (document instanceof Enquiry) {
            enquiryRepository.save((Enquiry) document);
        } else {
            System.err.println("Attempted to save document with unknown type: " + document.getClass().getName());
            // Optionally throw an exception
        }
    }

    /**
     * Finds any document by its ID. Uses prefixes (APP-, REG-, WDR-, ENQ-) for efficiency.
     * @param documentId The unique ID of the document.
     * @return Optional containing the document if found, empty otherwise.
     */
    public Optional<IBaseSubmittableDocument> findDocumentById(String documentId) {
        if (documentId == null || documentId.isEmpty()) {
            return Optional.empty();
        }

        // Use ID prefix to route to the correct repository
        if (documentId.startsWith("APP-")) {
            return applicationRepository.findById(documentId).map(doc -> doc); // Cast Optional<Subtype> to Optional<BaseType>
        } else if (documentId.startsWith("REG-")) {
            return registrationRepository.findById(documentId).map(doc -> doc);
        } else if (documentId.startsWith("WDR-")) {
            return withdrawalRepository.findById(documentId).map(doc -> doc);
        } else if (documentId.startsWith("ENQ-")) {
            return enquiryRepository.findById(documentId).map(doc -> doc);
        } else {
            System.err.println("Could not determine document type from ID prefix: " + documentId);
            // Could optionally search all repositories as a fallback, but less efficient
            return Optional.empty();
        }
    }

    /**
     * Deletes a document from the appropriate repository based on its type.
     * @param document The document to delete.
     * @return true if deleted, false otherwise.
     */
    public boolean deleteDocument(IBaseSubmittableDocument document) {
         if (document == null) return false;
         boolean deleted = false;
         if (document instanceof ProjectApplication) {
            deleted = applicationRepository.delete((ProjectApplication) document);
        } else if (document instanceof ProjectRegistration) {
            deleted = registrationRepository.delete((ProjectRegistration) document);
        } else if (document instanceof Withdrawal) {
            deleted = withdrawalRepository.delete((Withdrawal) document);
        } else if (document instanceof Enquiry) {
            deleted = enquiryRepository.delete((Enquiry) document);
        } else {
            System.err.println("Attempted to delete document with unknown type: " + document.getClass().getName());
        }
         if (deleted) {
             System.out.println("Document deleted via Facade: " + document.getDocumentID());
         }
         return deleted;
    }

    /**
     * Deletes a document by its ID from the appropriate repository.
     * Uses prefixes (APP-, REG-, WDR-, ENQ-) for efficiency.
     * @param documentId The ID of the document to delete.
     * @return true if deleted, false otherwise.
     */
     public boolean deleteDocumentById(String documentId) {
         if (documentId == null || documentId.isEmpty()) return false;

         // Route based on prefix
         if (documentId.startsWith("APP-")) {
             return applicationRepository.deleteById(documentId);
         } else if (documentId.startsWith("REG-")) {
             return registrationRepository.deleteById(documentId);
         } else if (documentId.startsWith("WDR-")) {
             return withdrawalRepository.deleteById(documentId);
         } else if (documentId.startsWith("ENQ-")) {
             return enquiryRepository.deleteById(documentId);
         } else {
             System.err.println("Could not determine document type from ID prefix for deletion: " + documentId);
             return false;
         }
     }

    /**
     * Gets all documents from all repositories.
     * @return A combined list of all documents.
     */
    public List<IBaseSubmittableDocument> findAllDocuments() {
        return Stream.of(
                applicationRepository.findAll().stream(),
                registrationRepository.findAll().stream(),
                withdrawalRepository.findAll().stream(),
                enquiryRepository.findAll().stream()
            )
            .flatMap(docStream -> docStream)
            .collect(Collectors.toList());
    }

    // --- Getters for specific repositories ---
    public ApplicationRepository getApplicationRepository() { return applicationRepository; }
    public RegistrationRepository getRegistrationRepository() { return registrationRepository; }
    public WithdrawalRepository getWithdrawalRepository() { return withdrawalRepository; }
    public EnquiryRepository getEnquiryRepository() { return enquiryRepository; }
}