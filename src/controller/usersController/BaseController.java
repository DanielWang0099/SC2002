package controller.usersController;

import controller.ProjectController;

/**
 * Abstract base class for user controllers.
 */
public abstract class BaseController {
    protected final ProjectController projectController;

    /**
     * Creates a new BaseController.
     * @param projectController the ProjectController instance.
     */
    public BaseController(ProjectController projectController) {
        this.projectController = projectController;
    }
}
