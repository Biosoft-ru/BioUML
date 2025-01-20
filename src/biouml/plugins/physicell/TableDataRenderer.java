package biouml.plugins.physicell;

import java.awt.Color;
import java.io.File;

import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.VideoFileImporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.physicell.ui.render.Renderer3D;
import ru.biosoft.physicell.ui.render.Scene;
import ru.biosoft.physicell.ui.render.SceneHelper;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.TempFiles;

public class TableDataRenderer
{

    public void renderFolder(DataCollection dc, DataCollection images, int width, int height) throws Exception
    {
        for( Object obj : dc.getNameList() )
        {
            DataElement de = dc.get( obj.toString() );
            if( de instanceof TableDataCollection )
            {
                render( (TableDataCollection)de, images, width, height );
            }
        }
    }

    public void createVideo(DataCollection dc, String name) throws Exception
    {
        File result = TempFiles.file( name + ".mp4" );
        SeekableByteChannel out = NIOUtils.writableFileChannel( result.getAbsolutePath() );
        AWTSequenceEncoder encoder = new AWTSequenceEncoder( out, Rational.R( 10, 1 ) );
        try
        {
            for( Object obj : dc.getNameList() )
            {
                DataElement de = dc.get( obj.toString() );
                if( de instanceof ImageDataElement )
                {
                    encoder.encodeImage( ( (ImageDataElement)de ).getImage() );
                }
            }

            VideoFileImporter importer = new VideoFileImporter();
            importer.getProperties( dc, result, name ).setResolution( "1280 x 720 (High definition)" );
            importer.doImport( dc, result, name, null, null );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        finally
        {
            encoder.finish();
            NIOUtils.closeQuietly( out );
        }
    }

    public void render(TableDataCollection tdc, DataCollection images, int width, int height) throws Exception
    {
        Scene scene = createScene( tdc );
        double p = -0.3839;
        double h = -0.7679;
        Renderer3D renderer = new Renderer3D( width, height, h, p );
        images.put( new ImageDataElement( tdc.getName(), images, renderer.render( scene ) ) );
    }

    public Scene createScene(TableDataCollection tdc)
    {
        Scene scene = new Scene();
        for( RowDataElement row : tdc )
        {
            Object[] objects = row.getValues();
            double x = Double.parseDouble( objects[0].toString() );
            double y = Double.parseDouble( objects[1].toString() );
            double z = Double.parseDouble( objects[2].toString() );
            double radius = Double.parseDouble( objects[3].toString() );
            Color color = decode( objects[5].toString() );
            scene.add( SceneHelper.createSphere( x, y, z, radius, color ) );
        }
        return scene;
    }

    public Color decode(String text)
    {
        text = text.substring( 1, text.length() - 1 );
        String[] parts = text.split( "," );
        int red = Integer.parseInt( parts[0].trim() );
        int green = Integer.parseInt( parts[1].trim() );
        int blue = Integer.parseInt( parts[2].trim() );
        return new Color( red, green, blue );
    }
}
