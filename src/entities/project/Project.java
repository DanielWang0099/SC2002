package entities.project;

import java.util.Date; // Consider using java.time.LocalDate for better date handling
import java.util.Map;
import java.util.HashMap; // Or other Map implementation
import java.util.List;
import java.util.Objects;
import java.util.Arrays;
import java.util.stream.Collectors;
import entities.user.*;

/**
 * This entity class represents a Build-To-Order (BTO) project.
 */
public class Project {
    private String name;
    private String neighbourhood;
    private Map<FlatType, Integer> flatUnitCounts;
    private Map<FlatType, Integer> remainingFlatUnits;
    private Map<FlatType, Double> flatUnitPrices;
    private Date applicationOpenDate;
    private Date applicationCloseDate;
    private HdbManager manager;
    private HdbOfficer[] officers;
    private int assignedOfficerCount;
    private final int MAX_OFFICER_SLOTS = 10;
    private boolean visibility;

    /**
     * Constructs a new Project object.
     *
     * @param name                 The unique name of the project.
     * @param neighbourhood        The neighbourhood of the project.
     * @param initialFlatUnitCounts A map containing initial counts for each available FlatType.
     * @param flatUnitPrices       A map containing the selling price for each available FlatType.
     * @param applicationOpenDate  The opening date of applications.
     * @param applicationCloseDate The closing date of applications.
     * @param creatingManager      The HDB Manager creating this project listing.
     */
    public Project(String name, String neighbourhood,
            Map<FlatType, Integer> initialFlatUnitCounts, Map<FlatType, Double> flatUnitPrices,
            Date applicationOpenDate, Date applicationCloseDate, HdbManager creatingManager) {

        // --- Constructor parameter validation (as provided by user) ---
        Objects.requireNonNull(name, "Project name cannot be null");
        // ... other Objects.requireNonNull ...
        if(applicationOpenDate.after(applicationCloseDate)){ throw new IllegalArgumentException("Application open date cannot be after close date."); }
        if (initialFlatUnitCounts.entrySet().stream().anyMatch(entry -> entry.getValue() < 0) ||
        flatUnitPrices.entrySet().stream().anyMatch(entry -> entry.getValue() < 0)) { throw new IllegalArgumentException("Unit counts and prices cannot be negative."); }
        for (FlatType ft : initialFlatUnitCounts.keySet()) { if (!flatUnitPrices.containsKey(ft)) { System.err.println("Warning: Price not provided for flat type " + ft + " in project " + name + ". Defaulting to 0.0"); } }
        // --- End Validation ---

        this.name = name;
        this.neighbourhood = neighbourhood;
        this.flatUnitCounts = new HashMap<>(initialFlatUnitCounts);
        this.remainingFlatUnits = new HashMap<>(initialFlatUnitCounts);
        this.flatUnitPrices = new HashMap<>(flatUnitPrices);
        this.applicationOpenDate = applicationOpenDate;
        this.applicationCloseDate = applicationCloseDate;
        this.manager = creatingManager;
        // --- Officer Initialization ---
        this.officers = new HdbOfficer[MAX_OFFICER_SLOTS]; // Create array
        this.assignedOfficerCount = 0; // Initialize count
        // --- -------------------- ---
        this.visibility = false;
    }

    public void setName(String name) { this.name = name; }
    public void setNeighbourhood(String neighbourhood) { this.neighbourhood = neighbourhood; }
    public void setApplicationOpenDate(Date applicationOpenDate) { this.applicationOpenDate = applicationOpenDate; }
    public void setApplicationCloseDate(Date applicationCloseDate) { this.applicationCloseDate = applicationCloseDate; }
    public void setManager(HdbManager manager) { this.manager = manager; }
    public void setVisibility(boolean visible) { this.visibility = visible; }

     // --- Getters (include price) ---
    public String getName() { return name; }
    public String getNeighbourhood() { return neighbourhood; }
    public Date getApplicationOpenDate() { return applicationOpenDate; }
    public Date getApplicationCloseDate() { return applicationCloseDate; }
    public HdbManager getManager() { return manager; }
    public boolean isVisible() { return visibility; } // Preferred boolean getter name
    // public boolean getVisibility() {return visibility; } // Redundant with isVisible()
    public int getInitialUnitCount(FlatType type) { return flatUnitCounts.getOrDefault(type, 0); }
    public Map<FlatType, Integer> getInitialFlatUnitCounts() { return new HashMap<>(flatUnitCounts); }
    public int getRemainingUnitCount(FlatType type) { return remainingFlatUnits.getOrDefault(type, 0); }
    public Map<FlatType, Integer> getRemainingFlatUnits() { return new HashMap<>(remainingFlatUnits); }
    public int getAvailableOfficerSlots() { return MAX_OFFICER_SLOTS - assignedOfficerCount; }
    public double getUnitPrice(FlatType type) { return flatUnitPrices.getOrDefault(type, 0.0); }
    public Map<FlatType, Double> getFlatUnitPrices() { return new HashMap<>(flatUnitPrices); }

    /**
     * Gets the selling price for a specific flat type.
     * @param type The FlatType.
     * @return The price, or 0.0 if the type isn't offered or price not set.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(name, project.name); // Equality based on unique project name
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }


    /*
     * Updates the initial and remaining counts for a specific flat type.
     * Used when editing a project listing (before applications open).
     * @param type The FlatType to update.
     * @param newCount The new total count for this flat type.
     * @return true if successful, false otherwise (e.g., negative count).
     */
    public boolean updateFlatUnitCount(FlatType type, int newCount) {
        if (newCount < 0) return false;
        flatUnitCounts.put(type, newCount);
        remainingFlatUnits.put(type, newCount);
        // Also ensure price exists, maybe set default if adding a type?
        flatUnitPrices.putIfAbsent(type, 0.0); // Add price entry if new type
        return true;
    }

    /**
     * Updates the price for a specific flat type.
     * @param type The FlatType to update.
     * @param newPrice The new price.
     * @return true if successful, false if price is negative.
     */
    public boolean updateFlatUnitPrice(FlatType type, double newPrice) {
        if (newPrice < 0) {
            return false;
        }
        // Only allow updating price if the flat type exists initially
        if (flatUnitCounts.containsKey(type)) {
            flatUnitPrices.put(type, newPrice);
            return true;
        }
        return false; // Cannot set price for a non-existent flat type
    }


    public boolean decrementRemainingUnit(FlatType type) { /* unchanged */
        int currentRemaining = getRemainingUnitCount(type);
        if (currentRemaining > 0) {
            remainingFlatUnits.put(type, currentRemaining - 1);
            return true;
        }
        return false;
    }
    public boolean incrementRemainingUnit(FlatType type) { /* unchanged */
         int currentRemaining = getRemainingUnitCount(type);
         int initialCount = getInitialUnitCount(type);
         if (currentRemaining < initialCount) {
             remainingFlatUnits.put(type, currentRemaining + 1);
             return true;
         }
         return false;
     }
    public boolean addOfficer(HdbOfficer officer) { /* unchanged */
        if (officer == null) return false;
        if (assignedOfficerCount >= MAX_OFFICER_SLOTS) return false;
        for (int i = 0; i < assignedOfficerCount; i++) {
             if (officers[i] != null && officers[i].equals(officer)) return false;
        }
        officers[assignedOfficerCount] = officer;
        assignedOfficerCount++;
        return true;
    }
    
    public boolean removeOfficer(HdbOfficer officer) {
        if (officer == null || assignedOfficerCount == 0) return false;
        int foundIndex = -1;
        for (int i = 0; i < assignedOfficerCount; i++) { if (officers[i] != null && officers[i].equals(officer)) { foundIndex = i; break; } }
        if (foundIndex != -1) {
            int numMoved = assignedOfficerCount - foundIndex - 1;
            if (numMoved > 0) System.arraycopy(officers, foundIndex + 1, officers, foundIndex, numMoved);
            assignedOfficerCount--;
            officers[assignedOfficerCount] = null; // Clear the now unused slot
            return true;
        } else { return false; } // Officer not found
    }

    // toString, equals, hashCode remain similar, maybe update toString for price
     @Override
    public String toString() {
         return String.format(
            "Project Name: %s\n" +
            "  Neighbourhood: %s\n" +
            "  Initial Units: %s\n" +
            "  Remaining Units: %s\n" +
            "  Prices: %s\n" + // Added Prices
            "  Application Period: %s - %s\n" +
            "  Manager: %s (%s)\n" +
            "  Assigned Officers: %d/%d\n" +
            "  Visibility: %s",
            name, neighbourhood, flatUnitCounts, remainingFlatUnits, flatUnitPrices, // Added Prices
            applicationOpenDate, applicationCloseDate,
            (manager != null ? manager.getName() : "N/A"),
            (manager != null ? manager.getNric() : "N/A"),
            assignedOfficerCount, MAX_OFFICER_SLOTS,
            (visibility ? "Visible to Applicants" : "Hidden from Applicants")
        );
    }

    public List<HdbOfficer> getAssignedOfficers() {
        // Use stream bounded by the count for robustness
        return Arrays.stream(officers, 0, assignedOfficerCount)
                     .filter(Objects::nonNull) // Filter should ideally not be needed if count is correct, but good safeguard
                     .collect(Collectors.toList());
    }

}