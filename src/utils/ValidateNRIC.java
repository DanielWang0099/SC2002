package utils;

public class ValidateNRIC {
    /**
     * Validates NRIC format
     * @param nric  NRIC to validate
     * @return true if valid format (S/T followed by 7 digits and ending with a letter)
     */
    public static boolean isValidNric(String nric) {
        return nric.matches("^[ST]\\d{7}[A-Z]$");
    }
}
