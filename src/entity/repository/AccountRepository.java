package entity.repository;

import entity.user.User;
import java.util.ArrayList;
import java.util.List;

public class AccountRepository {
    private static AccountRepository instance;
    private List<User> users;

    private AccountRepository() {
        this.users = new ArrayList<>();
    }

    public static AccountRepository getInstance() {
        if (instance == null) {
            instance = new AccountRepository();
        }
        return instance;
    }

    public void addUser(User user) {
        users.add(user);
    }

    // Additional methods for finding users, etc.
    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public User findUserByNric(String nric) {
        return users.stream()
                .filter(user -> user.getNric().equals(nric))
                .findFirst()
                .orElse(null);
    }
}