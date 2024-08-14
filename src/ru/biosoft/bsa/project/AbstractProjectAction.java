package ru.biosoft.bsa.project;

import javax.swing.AbstractAction;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public abstract class AbstractProjectAction extends AbstractAction
{
    public AbstractProjectAction()
    {
        super();
    }

    public AbstractProjectAction(String name)
    {
        super(name);
    }

    private Project project;

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }
}
