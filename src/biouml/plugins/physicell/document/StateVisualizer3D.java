package biouml.plugins.physicell.document;

import java.awt.Point;
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
        scene = Util.readScene( content, options3D.getQualityInt() );
    }

    public void setResult(PhysicellSimulationResult result)
    {
        super.setResult( result );
        ModelData modelData = result.getModelData();
        options3D = result.getOptions().getOptions3D();
        renderer = new Renderer3D( modelData, 0, 0 );

        try
        {
            renderer.setDensityState( result.getDensity( 0 ) );
            renderer.setDrawDensity( true );
        }
        catch( Exception ex )
        {
        }
    }

    @Override
    public BufferedImage draw()
    {
        renderer.setAngle( options3D.getHead(), options3D.getPitch() );
        renderer.setCutOff( options3D.getCutOff() );
        renderer.setAxes( options.isAxes() );
        renderer.setAgents(options.isCells());
        renderer.setStatistics( options.isStatistics() );
        renderer.setStatisticsLOcation( new Point( options.getStatisticsX(), options.getStatisticsY() ) );
        SceneHelper.addDisks( scene, options3D.getXCutOff(), SceneHelper.PLANE_YZ );
        SceneHelper.addDisks( scene, options3D.getYCutOff(), SceneHelper.PLANE_XZ );
        SceneHelper.addDisks( scene, options3D.getZCutOff(), SceneHelper.PLANE_XY );
        return renderer.render( scene, options.getTime() );
    }
}