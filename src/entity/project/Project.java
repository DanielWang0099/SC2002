package entity.project;

import entity.user.HdbManager;
import entity.user.HdbOfficer;
import enums.FlatType;
import java.util.Date;
import java.util.List;

public class Project {
    private String name;
    private String neighbourhood;
    private List<FlatType> flatType;
    private Date applicationOpenDate;
    private Date applicationCloseDate;
    private HdbManager manager;
    private HdbOfficer[] officers;
    private boolean visibility;

    /**
     * Constructs a Project object.
     * 
     * @param name                  the name of the project
     * @param neighbourhood         the neighbourhood of the project
     * @param flatType              the types of flats in the project
     * @param applicationOpenDate   the opening date of the applications
     * @param applicationCloseDate  the closing date of the applications
     */
    public Project(String name, String neighbourhood, List<FlatType> flatType, 
        Date applicationOpenDate, Date applicationCloseDate) {
        this.name = name;
        this.neighbourhood = neighbourhood;
        this.flatType = flatType;
        this.applicationOpenDate = applicationOpenDate;
        this.applicationCloseDate = applicationCloseDate;
        this.officers = new HdbOfficer[10];
        this.visibility = true;
    }

    @Override
    public String toString() {
        return String.format("Project %s:\n Neighbourhood: %s\n Flat Types: %s\n Application Open Date: %s\n Application Close Date: %s\n Visibility: %s", 
        name, neighbourhood, flatType, applicationOpenDate, applicationCloseDate, visibility);
    }


    // Getter Methods

    /**
     * Returns the name of the project.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the neighbourhood of the project.
     * @return neighbourhood
     */
    public String getNeighbourhood() {
        return neighbourhood;
    }

    /**
     * Returns the type of flats for the project.
     * @return flatType
     */
    public List<FlatType> getFlatType() {
        return flatType;
    }

    /**
     * Returns the opening date of applications for the project.
     * @return applicationOpenDate
     */
    public Date getApplicationOpenDate() {
        return applicationOpenDate;
    }

    /**
     * Returns the closing date of applications for the project.
     * @return applicationCloseDate
     */
    public Date getApplicationCloseDate() {
        return applicationCloseDate;
    }

    /**
     * Returns the HDB Manager in charge for the project.
     * @return manager
     */
    public HdbManager getManager() {
        return manager;
    }

    /**
     * Returns the HDB Officers for the project.
     * @return officers
     */
    public HdbOfficer[] getOfficers() {
        return officers;
    }

    /**
     * Returns the visibility of the project.
     * @return visibility
     */
    public boolean getVisibility() {
        return visibility;
    }


    // Setter Methods

    /**
     * Sets the name of the project.
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the neighbourhood of the project.
     * @param neighbourhood
     */
    public void setNeighbourhood(String neighbourhood) {
        this.neighbourhood = neighbourhood;
    }

    /**
     * Sets the type of flats of the project.
     * @param flatType
     */
    public void setFlatType(List<FlatType> flatType) {
        this.flatType = flatType;
    }

    /**
     * Sets the opening date of applications for the project.
     * @param applicationOpenDate
     */
    public void setApplicationOpenDate(Date applicationOpenDate) {
        this.applicationOpenDate = applicationOpenDate;
    }

    /**
     * Sets the closing date of applications for the project.
     * @param applicationCloseDate
     */
    public void setApplicationCloseDate(Date applicationCloseDate) {
        this.applicationCloseDate = applicationCloseDate;
    }

    /**
     * Sets the HDB Manager in charge for the project.
     * @param manager
     */
    public void setManager(HdbManager manager) {
        this.manager = manager;
    }

    /**
     * Sets the HDB Officers for the project.
     * @param officers
     */
    public void getOfficers(HdbOfficer[] officers) {
        this.officers = officers;
    }

    /**
     * Sets the visibility of the project.
     * @param visibility
     */
    public void isVisible(boolean visibility) {
        this.visibility = visibility;
    }
}