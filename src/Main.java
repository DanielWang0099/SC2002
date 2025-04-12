import boundary.MainCLI;
import entities.database.Database;

public class Main {
    public static void main(String[] args) {
        // Optional: Add a try-catch block here for any unhandled exceptions during startup
        try {
             System.out.println("Application starting...");
             // The Database static block will run when MainCLI constructor accesses it,
             // ensuring data is loaded before the application starts fully.
            MainCLI application = new MainCLI(); // Create an instance of your main CLI class
            application.start(); // Start the application logic (shows menu, handles login, etc.)
        } catch (Exception e) {
             System.err.println("An unexpected error occurred during application startup or execution: " + e.getMessage());
             e.printStackTrace(); // Print stack trace for debugging
             // Optionally attempt a final save before exiting on error
             try {
                 System.err.println("Attempting emergency data save...");
                 Database.saveAllData(); // Ensure Database class is imported
             } catch (Exception saveEx) {
                  System.err.println("Emergency save failed: " + saveEx.getMessage());
             }
             System.exit(1); // Exit with error code
        }
    }
}
