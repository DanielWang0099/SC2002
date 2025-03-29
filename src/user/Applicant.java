package user;

public class Applicant extends User {
    /**
     * Constructs an Applicant object.
     * @param name          the name of the user
     * @param nric          the NRIC of the user
     * @param age           the age of the user
     * @param maritalStatus the marital status of the user, SINGLE or MARRIED
     * @param password      the password of the user
     */
    public Applicant(String name, String nric, int age, MaritalStatus maritalStatus, String password) {
        super(name, nric, age, maritalStatus, password);
    }

    /**
     * Default constructor for Applicant class.
     */
    public Applicant() {
        this(null, null, 0, MaritalStatus.SINGLE, null);
    }
}
