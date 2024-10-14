package biouml.plugins.physicell;

import java.util.Arrays;

import javax.swing.JTable;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.table.RowModel;

import biouml.plugins.physicell.RulesTab.ListRowModel;
import ru.biosoft.gui.TabularPropertiesEditor;

public class VisualizerTab extends TabularPropertiesEditor
{
    private VisualizerProperties properties = new VisualizerProperties();
    private Object template;

    public void explore(VisualizerProperties properties)
    {
        this.properties = properties;
        update();
    }

    public void addVisualizer()
    {
        properties.addVisualizer();
        update();
    }
    
    public void removeSelectedVisualizer()
    {
        int index = getTable().getSelectedRow();
        if( index < 0 )
            return;
        properties.removeVisualizer( index );
        update();
    }
    
    public void update()
    {
        explore( getRowModel(), getTemplate(), PropertyInspector.SHOW_USUAL );
        getTable().setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
    }

    protected RowModel getRowModel()
    {
        return new ListRowModel( Arrays.asList( properties.getProperties() ), CellDefinitionVisualizerProperties.class );
    }

    protected Object createTemplate()
    {
        return new CellDefinitionVisualizerProperties();
    }

    protected Object getTemplate()
    {
        if( template == null )
            template = createTemplate();
        return template;
    }
}