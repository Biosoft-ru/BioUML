package biouml.workbench;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.gui.GUI;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.standard.type.Base;

import com.developmentontheedge.application.Application;

@SuppressWarnings ( "serial" )
public class RemoveDataElementAction extends AbstractAction
{
    protected Logger log = Logger.getLogger(RemoveDataElementAction.class.getName());

    public static final String KEY = "Remove Data Element";

    public static final String DATA_ELEMENT = "Data element";

    public RemoveDataElementAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        DataElement de = (DataElement)getValue(DATA_ELEMENT);
        DataCollection<?> dc = DataElementPath.create(de).getParentCollection();

        if( !dc.isMutable() )
        {
            String message = BioUMLApplication.getMessageBundle().getResourceString("ERROR_CANNOT_REMOVE");
            message = MessageFormat.format(message, new Object[] {de.getName(), dc.getCompletePath()});
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), message);
            return;
        }

        if( de instanceof Diagram )
        {
            String message = BioUMLApplication.getMessageBundle().getResourceString("CONFIRM_REMOVE_DIAGRAM");
            message = MessageFormat.format(message, new Object[] {de.getName()});
            int res = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message);
            if( res != JOptionPane.YES_OPTION )
                return;
        }
        else
        {
            String message = BioUMLApplication.getMessageBundle().getResourceString("CONFIRM_REMOVE_ELEMENT");
            message = MessageFormat.format(message, new Object[] {de.getName(), dc.getCompletePath()});
            int res = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message);
            if( res != JOptionPane.YES_OPTION )
                return;
        }
        try
        {
            dc.remove(de.getName());
            GUI.getManager().getRepositoryTabs().selectElement( dc.getCompletePath(), true );
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, ExceptionRegistry.log( t ));
        }
    }

    protected boolean checkContainKernel(Compartment compartment, Base kernel) throws Exception
    {
        return compartment.recursiveStream().select( Compartment.class ).anyMatch( c -> c.containsKernel( kernel ) );
    }

}
