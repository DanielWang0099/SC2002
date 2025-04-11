package entity.user;

import enums.MaritalStatus;
import enums.UserRole;

public class HdbManager extends User {
    /**
     * Constructs a HdbManager object.
     * @param name          the name of the user
     * @param nric          the NRIC of the user
     * @param age           the age of the user
     * @param maritalStatus the marital status of the user, SINGLE or MARRIED
     * @param password      the password of the user
     * @param role
     */
    public HdbManager(String name, String nric, int age, MaritalStatus maritalStatus, String password) {
        super(name, nric, age, maritalStatus, password, UserRole.HDB_MANAGER);
    }

    /**
     * Default constructor for HdbManager class.
     */
    public HdbManager() {
        this(null, null, 0, MaritalStatus.SINGLE, null);
    }
}
