package controller.usersController;

import controller.ProjectController;

public abstract class BaseController {
    protected final ProjectController projectController;

    public BaseController(ProjectController projectController) {
        this.projectController = projectController;
    }
}
