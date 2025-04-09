package entities.user;

public interface Model {
    /** 
     * since user use NRIC as ID, so save it for now and 
     * consider if it is a good interface for request and project
    */
    String getID();
}
