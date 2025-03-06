package biouml.plugins.physicell.document;

import java.awt.image.BufferedImage;

import ru.biosoft.physicell.ui.ModelData;

public abstract class StateVisualizer
{
    protected String currentName = "";
    protected ViewOptions options = new ViewOptions();
    protected ModelData modelData;
    
    public ViewOptions getOptions()
    {
        return options;
    }
    public void setOptions(ViewOptions options)
    {
        this.options = options;
    }
    
    public void setModelData(ModelData modelData)
    {
        this.modelData = modelData;
    }
    
    public abstract void readAgents(String content, String name);    
    public abstract BufferedImage draw();
}