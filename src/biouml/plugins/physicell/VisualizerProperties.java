package biouml.plugins.physicell;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class VisualizerProperties extends Option
{
    private MulticellEModel emodel;
    private CellDefinitionVisualizerProperties[] properties = new CellDefinitionVisualizerProperties[0];

    public void VisualizerProperties()
    {
        properties = new CellDefinitionVisualizerProperties[1];
        properties[0] = new CellDefinitionVisualizerProperties();
    }
    
    public void setEModel(MulticellEModel emodel)
    {
        this.emodel = emodel;
        for (CellDefinitionVisualizerProperties prop: properties)
        {
            prop.setModel( emodel );
        }
    }
    
    @PropertyName("Cell Type Properties")
    public CellDefinitionVisualizerProperties[] getProperties()
    {
        return properties;
    }

    public void setProperties(CellDefinitionVisualizerProperties[] properties)
    {
        Object oldValue = this.properties;
        this.properties = properties;
        for (CellDefinitionVisualizerProperties prop: properties)
        {
            prop.setModel( emodel );
        }
        firePropertyChange( "properties", oldValue, properties );
    }
    
    /**
     * Add single visualizer
     */
    public void addVisualizer()
    {
        int l = properties.length;
        CellDefinitionVisualizerProperties[] newVisualizer = new CellDefinitionVisualizerProperties[l + 1];
        newVisualizer[l] = new CellDefinitionVisualizerProperties();
        System.arraycopy( properties, 0, newVisualizer, 0, l );
        this.setProperties( newVisualizer );
    }
    
    /**
     * Remove single visualizer
     */
    public void removeVisualizer(int index)
    {
        int l = properties.length;
        CellDefinitionVisualizerProperties[] newVisualizers = new CellDefinitionVisualizerProperties[l - 1];
        if( index == 0 )
            System.arraycopy( properties, 1, newVisualizers, 0, l - 1 );
        else if( index == l - 1 )
            System.arraycopy( properties, 0, newVisualizers, 0, l - 1 );
        else
        {
            System.arraycopy( properties, 0, newVisualizers, 0, index );
            System.arraycopy( properties, index + 1, newVisualizers, index, l - index - 1 );
        }
        this.setProperties( newVisualizers );
    }

}
