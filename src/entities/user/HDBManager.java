package entities.user;

public class HDBManager extends User {
    /**
     * Constructs a HDBManager object.
     * @param name          the name of the entities.user
     * @param nric          the NRIC of the entities.user
     * @param age           the age of the entities.user
     * @param maritalStatus the marital status of the entities.user, SINGLE or MARRIED
     * @param password      the password of the entities.user
     */
    public HDBManager(String name, String nric, int age, MaritalStatus maritalStatus, String password) {
        super(name, nric, age, maritalStatus, password);
    }

    /**
     * Default constructor for HDBOfficer class.
     */
    public HDBManager() {
        this(null, null, 0, MaritalStatus.SINGLE, null);
    }
}
