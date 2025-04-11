package entity.user;

import enums.MaritalStatus;
import enums.UserRole;

public class User {
    private String name;
    private String nric;
    private int age;
    private MaritalStatus maritalStatus;
    private String password;
    private UserRole role;

    /**
     * Constructs a User object.
     * @param name          the name of the user
     * @param nric          the NRIC of the user
     * @param age           the age of the user
     * @param maritalStatus the marital status of the user, SINGLE or MARRIED
     * @param password      the password of the user
     * @param role          the role of the user
     */
    public User(String name, String nric, Integer age, MaritalStatus maritalStatus, String password, UserRole role) {
        this.name = name;
        this.nric = nric;
        this.age = age;
        this.maritalStatus = maritalStatus;
        this.password = password;
        this.role = role;
    }

    /**
     * Default constructor for User class.
     */
    public User() {
        this(null, null, 0, MaritalStatus.SINGLE, null, null);
    }

    // Getter methods

    /**
     * Returns the name of the user.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the nric of the user.
     * @return nric
     */
    public String getNric() {
        return nric;
    }

    /**
     * Returns the age of the user.
     * @return age
     */
    public int getAge() {
        return age;
    }

    /**
     * Returns the marital status of the user.
     * @return marital status
     */
    public MaritalStatus getMaritalStatus() {
        return maritalStatus;
    }

    /**
     * Returns the password of the user.
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the role of the user.
     * @return role
     */
    public UserRole getRole() {
        return role;
    }


    // Setter methods

    /**
     * Sets the password of the user. For change password functionality.
     * @param password
     */
    public void setPassword(String newPassword) {
        // TODO: validation for new password if needed
        this.password = newPassword;
    }


    // Other methods
    /**
     * Override toString() method
     */
    @Override
    public String toString() {
        return String.format("(%s) User: %s\n\tNRIC: %s\n\tAge: %s\n\tMarital Status: %s\n\tPassword: %s\n", 
        role, name, nric, age, maritalStatus, password);
    }
}