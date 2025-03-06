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

    @Override
    public void readAgents(String content, String name)
    {
        if( currentName.equals( name ) )
            return;
        scene = Util.readScene( content, 3 );
    }

    public void setModelData(ModelData modelData)
    {
        renderer = new Renderer3D( (int)modelData.getXDim().getLength(), (int)modelData.getYDim().getLength(), 0, 0 );
    }

    @Override
    public BufferedImage draw()
    {
        renderer.setAngle( options.getOptions3D().getHead(), options.getOptions3D().getPitch() );
        renderer.setCutOff( options.getOptions3D().getCutOff() );
        renderer.setAxes( options.getOptions3D().isAxes() );
        renderer.setStatistics( options.isStatistics() );
        SceneHelper.addDisks( scene, options.getOptions3D().getXCutOff(), SceneHelper.PLANE_YZ );
        SceneHelper.addDisks( scene, options.getOptions3D().getYCutOff(), SceneHelper.PLANE_XZ );
        SceneHelper.addDisks( scene, options.getOptions3D().getZCutOff(), SceneHelper.PLANE_XY );
        return renderer.render( scene, options.getTime() );
    }

}