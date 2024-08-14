package ru.biosoft.access.repository;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.ExceptionRegistry;

/**
 * @author lan
 *
 */
public abstract class AbstractElementAction extends AbstractAction
{
    protected static final Logger log = Logger.getLogger( AbstractElementAction.class.getName() );
    public static final int PRIORITY_NOT_SUPPORTED = Integer.MIN_VALUE;
    /**
     * Minimal priority for action to be executed by double-click
     * (if best action for given item has lower priority, nothing is executed)
     */
    public static final int PRIORITY_AUTO_ACTION = 40;
    private DataElement de;
    private int priority;

    void setDataElement(DataElement de)
    {
        this.de = de;
    }

    void setPriority(int priority)
    {
        this.priority = priority;
    }

    protected int getActionPriority(DataElement de)
    {
        return isApplicable(de)?priority:PRIORITY_NOT_SUPPORTED;
    }

    protected abstract void performAction(DataElement de) throws Exception;

    protected abstract boolean isApplicable(DataElement de);

    @Override
    public void actionPerformed(ActionEvent e)
    {
        try
        {
            performAction(de);
        }
        catch( Throwable ex )
        {
            log.log(Level.SEVERE, "Unable to perform action "+getValue(NAME)+": " + ExceptionRegistry.log(ex));
            JOptionPane.showMessageDialog(Application.getActiveApplicationFrame(), ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName()+" ("+getValue(NAME)+")";
    }
}
