package biouml.plugins.physicell.document;

import java.awt.image.BufferedImage;

import ru.biosoft.physicell.ui.ModelData;
import ru.biosoft.physicell.ui.render.Renderer3D;
import ru.biosoft.physicell.ui.render.Scene;
import ru.biosoft.physicell.ui.render.SceneHelper;

public class StateVisualizer3D extends StateVisualizer
{
    private Scene scene;
    private Renderer3D renderer = new Renderer3D( 0, 0, 0, 0 );
    private View3DOptions options3D;

    @Override
    public void readAgents(String content, String name)
    {
        if( currentName.equals( name ) )
            return;
        scene = Util.readScene( content, 3 );
    }

    public void setResult(PhysicellSimulationResult result)
    {
        super.setResult( result );
        ModelData modelData = result.getModelData();
        options3D = result.getOptions().getOptions3D();
        renderer = new Renderer3D( (int)modelData.getXDim().getLength(), (int)modelData.getYDim().getLength(), 0, 0 );
    }

    @Override
    public BufferedImage draw()
    {
        renderer.setAngle( options3D.getHead(), options3D.getPitch() );
        renderer.setCutOff( options3D.getCutOff() );
        renderer.setAxes( options3D.isAxes() );
        renderer.setStatistics( options.isStatistics() );
        SceneHelper.addDisks( scene, options3D.getXCutOff(), SceneHelper.PLANE_YZ );
        SceneHelper.addDisks( scene, options3D.getYCutOff(), SceneHelper.PLANE_XZ );
        SceneHelper.addDisks( scene, options3D.getZCutOff(), SceneHelper.PLANE_XY );
        return renderer.render( scene, options.getTime() );
    }
}