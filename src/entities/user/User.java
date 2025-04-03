package entities.user;

abstract class User {
    private String name;
    private String nric;
    private int age;
    private MaritalStatus maritalStatus;
    private String password;


    /**
     * Constructs a User object.
     * @param name          the name of the entities.user
     * @param nric          the NRIC of the entities.user
     * @param age           the age of the entities.user
     * @param maritalStatus the marital status of the entities.user, SINGLE or MARRIED
     * @param password      the password of the entities.user
     */
    public User(String name, String nric, Integer age, MaritalStatus maritalStatus, String password) {
        this.name = name;
        this.nric = nric;
        this.age = age;
        this.maritalStatus = maritalStatus;
        this.password = password;
    }

    /**
     * Default constructor for User class.
     */
    public User() {
        this(null, null, 0, MaritalStatus.SINGLE, null);
    }


    // Getter Methods

    /**
     * Returns the name of the entities.user.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the nric of the entities.user.
     * @return nric
     */
    public String getNric() {
        return nric;
    }

    /**
     * Returns the age of the entities.user.
     * @return age
     */
    public int getAge() {
        return age;
    }

    /**
     * Returns the marital status of the entities.user.
     * @return marital status
     */
    public MaritalStatus getMaritalStatus() {
        return maritalStatus;
    }

    /**
     * Returns the password of the entities.user.
     * @return password
     */
    public String getPassword() {
        return password;
    }


    // Setter Methods

    /**
     * Updates the name of the entities.user.
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Updates the nric of the entities.user.
     * @param nric
     */
    public void setNric(String nric) {
        this.nric = nric;
    }

    /**
     * Updates the age of the entities.user.
     * @param age
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * Updates the marital status of the entities.user.
     * @param maritalStatus
     */
    public void setMaritalStatus(MaritalStatus maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    /**
     * Updates the password of the entities.user.
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }


    // Other Methods

    /**
     * To login and authenticate the entities.user.
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
    public String toString() {
        return String.format("User %s:\n NRIC: %s\n Age: %s\n Marital Status: %s\n Password: %s", 
        name, nric, age, maritalStatus, password);
    }
}