package biouml.plugins.physicell;

import java.util.Arrays;

import javax.swing.JTable;

import com.developmentontheedge.beans.swing.PropertyInspector;
import com.developmentontheedge.beans.swing.table.RowModel;

import biouml.plugins.physicell.RulesTab.ListRowModel;
import ru.biosoft.gui.TabularPropertiesEditor;

public class ColorSchemesTab extends TabularPropertiesEditor
{
//    private ColorScheme[] schemes;
    private MulticellEModel model;
    private Object template;

    public ColorSchemesTab(MulticellEModel emodel)
    {
        this.model = emodel;
    }

    public void explore(VisualizerProperties properties)
    {
        update();
    }

    public void addColorScheme()
    {
        ColorScheme[] schemes = model.getColorSchemes();
        int l = schemes.length;
        ColorScheme[] newSchemes = new ColorScheme[l + 1];
        newSchemes[l] = new ColorScheme();
        System.arraycopy( schemes, 0, newSchemes, 0, l );
       model.setColorSchemes(newSchemes);
        update();
    }

    public void removeSelectedVisualizer()
    {
        int index = getTable().getSelectedRow();
        if( index < 0 )
            return;
        update();
    }

    public void update()
    {
        explore( getRowModel(), getTemplate(), PropertyInspector.SHOW_USUAL );
        getTable().setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
    }

    protected RowModel getRowModel()
    {
        return new ListRowModel( Arrays.asList( model.getColorSchemes()), ColorScheme.class );
    }

    protected Object createTemplate()
    {
        return new ColorScheme();
    }

    protected Object getTemplate()
    {
        if( template == null )
            template = new ColorScheme();
        return template;
    }
}