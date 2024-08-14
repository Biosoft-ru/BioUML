
package biouml.plugins.research.workflow.engine;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataElementPath;
import biouml.model.Node;

/**
 * @author anna
 *
 */
public class PlotElementParameters
{
    private Node node;
    
    public PlotElementParameters(Node n)
    {
        node = n;
    }
    
    public DataElementPath getPlotPath ()
    {
        return DataElementPath.create((String)node.getAttributes().getValue(PlotElement.PLOT_PATH));
    }
    
    public void setPlotPath(DataElementPath path)
    {
        if(node.getAttributes().getProperty(PlotElement.PLOT_PATH) == null)
            node.getAttributes().add(new DynamicProperty(PlotElement.PLOT_PATH, String.class, ""));

        node.getAttributes().setValue(PlotElement.PLOT_PATH, path.toString());
    }
    
    public boolean isAutoOpen()
    {
        Object val = node.getAttributes().getValue(PlotElement.AUTO_OPEN);
        return val == null ? false : (Boolean)val;
    }
    
    public void setAutoOpen(boolean autoOpen)
    {
        if(node.getAttributes().getProperty(PlotElement.AUTO_OPEN) == null)
            node.getAttributes().add(new DynamicProperty(PlotElement.AUTO_OPEN, Boolean.class, ""));
            
        node.getAttributes().setValue(PlotElement.AUTO_OPEN, autoOpen);
    }
    
}
