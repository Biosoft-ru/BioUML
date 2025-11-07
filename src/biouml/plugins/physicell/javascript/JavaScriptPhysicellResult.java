package biouml.plugins.physicell.javascript;

import java.awt.image.BufferedImage;

import biouml.plugins.physicell.document.PhysicellSimulationResult;
import biouml.plugins.physicell.document.StateVisualizer;
import biouml.plugins.physicell.document.StateVisualizer2D;
import biouml.plugins.physicell.document.StateVisualizer3D;
import biouml.plugins.physicell.document.ViewOptions;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.plugins.javascript.Global;

public class JavaScriptPhysicellResult
{
    PhysicellSimulationResult result;
    StateVisualizer visualizer;

    public JavaScriptPhysicellResult(PhysicellSimulationResult result)
    {
        this.result = result;
        result.init();
        if( result.getOptions().is2D() )
        {
            visualizer = new StateVisualizer2D();
        }
        else
        {
            visualizer = new StateVisualizer3D();
        }
        visualizer.setResult( result );
    }

    public ViewOptions getOptions()
    {
        return visualizer.getOptions();
    }

    public BufferedImage createImage() throws Exception
    {
        TextDataElement tde = result.getPoint( result.getOptions().getTime() );
        visualizer.readAgents( tde.getContent(), tde.getName() );
        visualizer.setDensityState( result.getDensity( result.getOptions().getTime(), result.getOptions().getSubstrate() ) );
        return visualizer.draw();

    }

    public void showImage() throws Exception
    {
        BufferedImage image = createImage();
        ScriptEnvironment environment = Global.getEnvironment();
        if( environment != null )
            environment.showGraphics( image );
    }

}
