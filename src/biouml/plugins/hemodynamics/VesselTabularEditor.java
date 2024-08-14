package biouml.plugins.hemodynamics;

import javax.swing.JTable;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Diagram;

import java.util.Iterator;

import ru.biosoft.access.support.DataCollectionRowModelAdapter;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.TabularPropertiesEditor;
import biouml.plugins.simulation.SimulatorSupport;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.table.RowModel;

public class VesselTabularEditor extends TabularPropertiesEditor
{
    private static final Logger log = Logger.getLogger( SimulatorSupport.class.getName() );

    @Override
    public boolean canExplore(Object model)
    {
        return model instanceof Diagram  && ((Diagram)model).getType() instanceof HemodynamicsDiagramType;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        try
        {
            explore( getRowModel(), new Vessel( "" ) , PropertyInspector.SHOW_USUAL );
            getTable().setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "Can not explore variables for diagram " + model + ", error: " + e );
            explore( (Iterator<?>)null );
        }
    }

    private RowModel getRowModel()
    {
      return new DataCollectionRowModelAdapter(( (Diagram)model ).getRole(HemodynamicsEModel.class).getVessels());
    }
}
