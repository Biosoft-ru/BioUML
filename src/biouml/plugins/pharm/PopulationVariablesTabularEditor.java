package biouml.plugins.pharm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.JTable;

import ru.biosoft.access.support.DataCollectionRowModelAdapter;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.TabularPropertiesEditor;
import biouml.model.Diagram;
import biouml.model.Role;


import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.table.RowModel;

public class PopulationVariablesTabularEditor extends TabularPropertiesEditor implements PropertyChangeListener
{
    protected Object template;

    protected PopulationEModel executableModel;
    public PopulationEModel getExecutableModel()
    {
        return executableModel;
    }

    @Override
    public boolean canExplore(Object model)
    {
        if( model instanceof biouml.model.Diagram )
        {
            Role role = ( (Diagram)model ).getRole();
            return role instanceof PopulationEModel;
        }

        return false;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        try
        {
            if( executableModel != null )
                executableModel.removePropertyChangeListener(this);

            executableModel = (PopulationEModel) ( (Diagram)model ).getRole();
            executableModel.addPropertyChangeListener(this);

            explore(getRowModel(), template, PropertyInspector.SHOW_USUAL);
            this.getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        }
        catch( Exception e )
        {
            System.out.println("Can not explore variables for diagram " + model + ", error: " + e);
            explore((Iterator)null);
        }
    }

    protected RowModel getRowModel()
    {
        if( template == null )
            template = new PopulationVariable("template");

        return new DataCollectionRowModelAdapter(executableModel.getPopulationVariables());
    }

    @Override
    public Action[] getActions()
    {
        return null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // TODO Auto-generated method stub
        
    }
}
