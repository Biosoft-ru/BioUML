package biouml.plugins.chemoinformatics.access;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import biouml.standard.type.Structure;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.gui.GUI;
import ru.biosoft.table.document.TableDocument;

@SuppressWarnings ( "serial" )
public class OpenStructuresAction extends AbstractAction
{
    public static final String KEY = "Open structures";
    public static final String DATA_ELEMENT = "structures";

    public OpenStructuresAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        @SuppressWarnings ( "unchecked" )
        DataCollection<Structure> structures = (DataCollection<Structure>)getValue(DATA_ELEMENT);
        TableStructureWrapper structureTable = new TableStructureWrapper(structures);
        try
        {
            GUI.getManager().addDocument( new TableDocument(structureTable) );
        }
        catch( Exception t )
        {
            ExceptionRegistry.log(t);
        }
    }
}
