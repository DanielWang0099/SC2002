package entities.project;

import java.util.Date;
import java.util.List;

import entities.user.HDBManager;
import entities.user.HDBOfficer;

public class Project {
    private String name;
    private String neighbourhood;
    private List<FlatType> flatType;
    private Date applicationOpenDate;
    private Date applicationCloseDate;
    private HDBManager manager;
    private HDBOfficer[] officers;
    private boolean visibility;

    /**
     * Constructs a Project object.
     * 
     * @param name                  the name of the entities.project
     * @param neighbourhood         the neighbourhood of the entities.project
     * @param flatType              the types of flats in the entities.project
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
        this.officers = new HDBOfficer[10];
        this.visibility = true;
    }

    @Override
    public String toString() {
        return String.format("Project %s:\n Neighbourhood: %s\n Flat Types: %s\n Application Open Date: %s\n Application Close Date: %s\n Visibility: %s", 
        name, neighbourhood, flatType, applicationOpenDate, applicationCloseDate, visibility);
    }


    // Getter Methods

    /**
     * Returns the name of the entities.project.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the neighbourhood of the entities.project.
     * @return neighbourhood
     */
    public String getNeighbourhood() {
        return neighbourhood;
    }

    /**
     * Returns the type of flats for the entities.project.
     * @return flatType
     */
    public List<FlatType> getFlatType() {
        return flatType;
    }

    /**
     * Returns the opening date of applications for the entities.project.
     * @return applicationOpenDate
     */
    public Date getApplicationOpenDate() {
        return applicationOpenDate;
    }

    /**
     * Returns the closing date of applications for the entities.project.
     * @return applicationCloseDate
     */
    public Date getApplicationCloseDate() {
        return applicationCloseDate;
    }

    /**
     * Returns the HDB Manager in charge for the entities.project.
     * @return manager
     */
    public HDBManager getManager() {
        return manager;
    }

    /**
     * Returns the HDB Officers for the entities.project.
     * @return officers
     */
    public HDBOfficer[] getOfficers() {
        return officers;
    }

    /**
     * Returns the visibility of the entities.project.
     * @return visibility
     */
    public boolean getVisibility() {
        return visibility;
    }


    // Setter Methods

    /**
     * Sets the name of the entities.project.
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the neighbourhood of the entities.project.
     * @param neighbourhood
     */
    public void setNeighbourhood(String neighbourhood) {
        this.neighbourhood = neighbourhood;
    }

    /**
     * Sets the type of flats of the entities.project.
     * @param flatType
     */
    public void setFlatType(List<FlatType> flatType) {
        this.flatType = flatType;
    }

    /**
     * Sets the opening date of applications for the entities.project.
     * @param applicationOpenDate
     */
    public void setApplicationOpenDate(Date applicationOpenDate) {
        this.applicationOpenDate = applicationOpenDate;
    }

    /**
     * Sets the closing date of applications for the entities.project.
     * @param applicationCloseDate
     */
    public void setApplicationCloseDate(Date applicationCloseDate) {
        this.applicationCloseDate = applicationCloseDate;
    }

    /**
     * Sets the HDB Manager in charge for the entities.project.
     * @param manager
     */
    public void setManager(HDBManager manager) {
        this.manager = manager;
    }

    /**
     * Sets the HDB Officers for the entities.project.
     * @param officers
     */
    public void getOfficers(HDBOfficer[] officers) {
        this.officers = officers;
    }

    /**
     * Sets the visibility of the entities.project.
     * @param visibility
     */
    public void isVisible(boolean visibility) {
        this.visibility = visibility;
    }
}
