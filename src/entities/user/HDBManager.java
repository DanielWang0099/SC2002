package entities.user;

public class HDBManager extends User {
    /**
     * Constructs a HDBManager object.
     * @param name          the name of the user
     * @param nric          the NRIC of the user
     * @param age           the age of the user
     * @param maritalStatus the marital status of the user, SINGLE or MARRIED
     * @param password      the password of the user
     * @param role
     */
    public HDBManager(String name, String nric, int age, MaritalStatus maritalStatus, String password, Role role) {
        super(name, nric, age, maritalStatus, password, role);
    }

    /**
     * Default constructor for HDBOfficer class.
     */
    public HDBManager() {
        this(null, null, 0, MaritalStatus.SINGLE, null, null);
    }
}
