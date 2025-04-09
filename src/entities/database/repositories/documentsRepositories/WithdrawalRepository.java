package entities.database.repositories.documentsRepositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import entities.documents.approvableDocuments.*;
import entities.database.repositories.*;

public class WithdrawalRepository implements IRepository<Withdrawal, String> {
    private final Map<String, Withdrawal> withdrawalMap = new ConcurrentHashMap<>();

     // Package-private constructor
    WithdrawalRepository() {}

    @Override
    public Withdrawal save(Withdrawal withdrawal) {
         if (withdrawal == null || withdrawal.getDocumentID() == null) {
            throw new IllegalArgumentException("Withdrawal and Document ID cannot be null.");
        }
        withdrawalMap.put(withdrawal.getDocumentID(), withdrawal);
        return withdrawal;
    }

    @Override
    public Optional<Withdrawal> findById(String documentId) {
        return Optional.ofNullable(withdrawalMap.get(documentId));
    }

    @Override
    public List<Withdrawal> findAll() {
        return new ArrayList<>(withdrawalMap.values());
    }

     @Override
    public boolean deleteById(String documentId) {
        return withdrawalMap.remove(documentId) != null;
    }

    @Override
    public boolean delete(Withdrawal withdrawal) {
        if (withdrawal == null || withdrawal.getDocumentID() == null) return false;
        return deleteById(withdrawal.getDocumentID());
    }

    @Override
    public long count() {
        return withdrawalMap.size();
    }

     // --- Requirement Specific Methods ---

     /**
      * Find withdrawal requests submitted by a specific applicant.
      * @param applicantNric The NRIC of the applicant.
      * @return List of withdrawal requests by the applicant.
      */
    public List<Withdrawal> findByApplicantNric(String applicantNric) {
         return withdrawalMap.values().stream()
                .filter(w -> w.getSubmitter() != null && w.getSubmitter().getNric().equalsIgnoreCase(applicantNric))
                .collect(Collectors.toList());
    }

    /**
     * Find a withdrawal request associated with a specific ProjectApplication ID.
     * @param originalApplicationId The Document ID of the ProjectApplication being withdrawn.
     * @return Optional containing the Withdrawal request if found.
     */
    public Optional<Withdrawal> findByApplicationId(String originalApplicationId) {
         return withdrawalMap.values().stream()
                .filter(w -> w.getApplicationToWithdraw() != null && w.getApplicationToWithdraw().getDocumentID().equals(originalApplicationId))
                .findFirst(); // Assuming only one withdrawal request per application
    }

}