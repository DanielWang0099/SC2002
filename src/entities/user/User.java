package entities.user;

import java.util.Objects;

public class User {
    private String name;
    private String nric;
    private int age;
    private MaritalStatus maritalStatus;
    private String password;
    private Role role;


    /**
     * Constructs a User object.
     * @param name          the name of the user
     * @param nric          the NRIC of the user
     * @param age           the age of the user
     * @param maritalStatus the marital status of the user, SINGLE or MARRIED
     * @param password      the password of the user
     * @param role
     */
    public User(String name, String nric, Integer age, MaritalStatus maritalStatus, String password, Role role) {
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

    // Getters (and potentially Setters if needed, e.g., change password)
    public String getName() {return name;}
    public String getNric() { return nric; }
    public String getPassword() { return password; } // Used for login check
    public Role getRole() { return role; }
    public int getAge() { return age; }
    public MaritalStatus getMaritalStatus() { return maritalStatus; }

    public void setPassword(String newPassword) {
        // Add validation for new password if needed
        this.password = newPassword;
    }


    // Other Methods

    /**
     * To login and authenticate the user.
     */
    public void login() {};

    /**
     * Change password functionality.
     */
    public void changePassword() {};


    /**
     * Override toString() method
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(nric, user.nric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nric);
    }

    @Override
    public String toString() {
        return "User{" +
               "nric='" + nric + '\'' +
               ", role=" + role +
               ", age=" + age +
               ", maritalStatus='" + maritalStatus + '\'' +
               '}';
    }
}