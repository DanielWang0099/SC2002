package entity.repository;

import entity.user.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages persistent storage and retrieval of user account data.
 * Implements singleton pattern to ensure single instance of user account storage.
 */
public class AccountRepository {
    private static AccountRepository instance;
    private List<User> users;

    private AccountRepository() {
        this.users = new ArrayList<>();
    }

    /**
     * Retrieves the singleton user account repository instance.
     * 
     * @return The shared AccountRepository instance.
     */
    public static AccountRepository getInstance() {
        if (instance == null) {
            instance = new AccountRepository();
        }
        return instance;
    }

     /**
     * Adds a new user to the repository.
     * 
     * @param user  The user object to store.
     */
    public void addUser(User user) {
        users.add(user);
    }

    /**
     * Retrieves all stored user accounts.
     * 
     * @return A copy of the user list.
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    /**
     * Finds a user by their NRIC identifier.
     * 
     * @param nric  The NRIC to search for
     * @return Matching User object or null if not found
     */
    public User findUserByNric(String nric) {
        return users.stream()
            .filter(user -> user.getNric().equals(nric))
            .findFirst()
            .orElse(null);
    }
}