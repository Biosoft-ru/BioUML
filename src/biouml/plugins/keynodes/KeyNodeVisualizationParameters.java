package biouml.plugins.keynodes;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author anna
 *
 */
@SuppressWarnings ( "serial" )
@PropertyName ( "Visualization parameters" )
public class KeyNodeVisualizationParameters extends KeyNodeTableActionParameters
{
    private boolean layoutDiagram = true;
    private boolean visualizeAllPaths = false;
    private boolean addParticipants = false;

    private int layoutXDist = 40;
    private int layoutYDist = 40;

    public KeyNodeVisualizationParameters()
    {
        super();
    }

    @PropertyName ( "Visualize all paths" )
    @PropertyDescription ( "Flag to show if all paths should be visualized" )
    public boolean isVisualizeAllPaths()
    {
        return visualizeAllPaths;
    }
    public void setVisualizeAllPaths(boolean visualizeAllPaths)
    {
        if( !addParticipants && visualizeAllPaths )
            return;
        boolean oldValue = this.visualizeAllPaths;
        this.visualizeAllPaths = visualizeAllPaths;
        firePropertyChange( "visualizeAllPaths", oldValue, visualizeAllPaths );
    }
    public boolean hideVisualizeAllPaths()
    {
        return !addParticipants;
    }

    public String getSuffix()
    {
        return visualizeAllPaths ? " with all paths" : "";
    }

    @PropertyName("Layout diagram")
    @PropertyDescription("Layout resulting diagram.")
    public boolean isLayoutDiagram()
    {
        return layoutDiagram;
    }
    public void setLayoutDiagram(boolean layoutDiagram)
    {
        boolean oldValue = this.layoutDiagram;
        this.layoutDiagram = layoutDiagram;
        firePropertyChange( "layoutDiagram", oldValue, layoutDiagram );
    }

    @PropertyName("Additional reaction participants")
    @PropertyDescription("Add all reaction paricipants from hub.")
    public boolean isAddParticipants()
    {
        return addParticipants;
    }
    public void setAddParticipants(boolean addParticipants)
    {
        boolean oldValue = this.addParticipants;
        this.addParticipants = addParticipants;
        if( !addParticipants && visualizeAllPaths )
            setVisualizeAllPaths( false );
        firePropertyChange( "*", oldValue, addParticipants );
    }

    public int getLayoutXDist()
    {
        return layoutXDist;
    }
    public void setLayoutXDist(int layoutXDist)
    {
        int oldValue = this.layoutXDist;
        this.layoutXDist = layoutXDist;
        firePropertyChange( "layoutXDist", oldValue, layoutXDist );
    }

    public int getLayoutYDist()
    {
        return layoutYDist;
    }
    public void setLayoutYDist(int layoutYDist)
    {
        int oldValue = this.layoutYDist;
        this.layoutYDist = layoutYDist;
        firePropertyChange( "layoutYDist", oldValue, layoutYDist );
    }


}
