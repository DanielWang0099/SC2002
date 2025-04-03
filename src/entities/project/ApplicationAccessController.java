package entities.project;

import entities.user.Applicant;
import entities.user.MaritalStatus;

public class ApplicationAccessController {

    /**
     * Handles the checking of the applicant's eligibility to apply for the entities.project.
     * @param   project     the entities.project for which the applicant wants to apply for.
     * @param   applicant   the applicant who is looking to apply.
     * @return  true if applicant is elibible, false otherwise.
     */
    public boolean checkEligible(Project project, Applicant applicant) {
        return project.getVisibility() && (applicant.getMaritalStatus() == MaritalStatus.SINGLE
            ? applicant.getAge() >= 35 && eligibleForSingle(project)
            : applicant.getAge() >= 21
        );
    }

    /**
     * Checks if the applicant, who is single, meets the elibility criteria
     */
    private boolean eligibleForSingle(Project project) {
        return project.getFlatType().contains(FlatType.TWO_ROOM);
    }

}
