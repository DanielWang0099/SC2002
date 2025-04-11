package entity.user;

public class Nric {
    private final String value;

    public Nric(String nric) {
        if (!isValid(nric)) 
            throw new IllegalArgumentException("Invalid NRIC format");
        this.value = nric;
    }

    public String getValue() { return value; }

    /**
     * Function to validate a string to see if it is a valid NRIC number.
     * @param   nric    NRIC number to validate
     * @return          true if NRIC is valid, false otherwise.
     */
    private boolean isValid(String nric) {
        return nric.matches("^[ST]\\d{7}[A-Z]$");
    }
}
