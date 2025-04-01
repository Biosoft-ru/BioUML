package biouml.plugins.physicell.document;

import java.awt.image.BufferedImage;

import ru.biosoft.physicell.ui.DensityState;

public abstract class StateVisualizer
{
    protected String currentName = "";
    protected PhysicellSimulationResult result;
    protected DensityState densityState;
    protected ViewOptions options;

    public ViewOptions getOptions()
    {
        return result.getOptions();
    }

    public void setResult(PhysicellSimulationResult result)
    {
        try
        {
            this.result = result;
            this.options = result.getOptions();
        }
        catch( Exception ex )
        {

        }
    }

    public void setDensityState(DensityState density)
    {
        densityState = density;
    }

    public abstract void readAgents(String content, String name);

    public abstract BufferedImage draw();
}