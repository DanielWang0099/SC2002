package utils;

public class ValidateNRIC {
    /**
     * Checks if a given National Registry Identification Card (NRIC) number is valid.
     * 
     * @param nric  NRIC to validate
     * @return  {@code true} if valid format (S/T followed by 7 digits and ending with a letter), 
     *          {@code false} otherwise.
     */
    public static boolean isValidNric(String nric) {
        return nric.matches("^[ST]\\d{7}[A-Z]$");
    }
}
