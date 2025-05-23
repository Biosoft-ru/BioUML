package biouml.plugins.physicell.document;

import java.awt.Color;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.physicell.ui.DensityState;
import ru.biosoft.physicell.ui.ModelData;
import ru.biosoft.physicell.ui.render.Mesh;
import ru.biosoft.physicell.ui.render.Scene;
import ru.biosoft.physicell.ui.render.SceneHelper;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class Util
{
    public static Scene readScene(String text, int quality)
    {
        Scene scene = new Scene();
        String[] lines = text.split( "\n" );
        for( int i = 1; i < lines.length; i++ )
        {
            String[] parts = lines[i].split( "\t" );
            double x = Double.parseDouble( parts[0] );
            double y = Double.parseDouble( parts[1] );
            double z = Double.parseDouble( parts[2] );
            double outerRadius = Double.parseDouble( parts[3] );
            Color outerColor = null;
            Color innerColor = null;
            if (parts.length == 6)
            {
                outerColor = decodeColor(parts[5]);
                innerColor = outerColor;
            }
            if (parts.length >= 7)
            {
                outerColor = decodeColor(parts[5]);
                innerColor = decodeColor(parts[6]);
            }
            Mesh mesh = SceneHelper.createSphere( x, y, z, outerRadius, outerColor, innerColor, quality );
            scene.addSphere( mesh );
        }
        return scene;
    }

    public static String shorten(String input)
    {
        StringBuffer buffer = new StringBuffer();
        String[] lines = input.split( "\n" );
        buffer.append( lines[0] + "\n" );
        for( int i = 1; i < lines.length; i++ )
        {
            String[] parts = lines[i].split( "\t" );
            double x = Double.parseDouble( parts[0] );
            double y = Double.parseDouble( parts[1] );
            double z = Double.parseDouble( parts[2] );
            double outerRadius = Double.parseDouble( parts[3] );
            double innerRadius = Double.parseDouble( parts[4] );
            Color outerColor = decodeColor( parts[5] );
            Color innerColor = decodeColor( parts[7] );

            x = ( (int) ( x * 10 ) ) / 10.0;
            y = ( (int) ( y * 10 ) ) / 10.0;
            z = ( (int) ( z * 10 ) ) / 10.0;
            outerRadius = ( (int) ( outerRadius * 10 ) ) / 10.0;
            innerRadius = ( (int) ( innerRadius * 10 ) ) / 10.0;
            buffer.append( StreamEx
                    .of( String.valueOf( x ), String.valueOf( y ), String.valueOf( z ), outerRadius, innerRadius, parts[5], parts[7] )
                    .joining( "\t" ) + "\n" );
        }
        return buffer.toString();
    }

    public static Color decodeColor(String s)
    {
        s = s.substring( 1, s.length() - 1 );
        String[] parts = s.split( "," );
        int r = Integer.parseInt( parts[0].trim() );
        int g = Integer.parseInt( parts[1].trim() );
        int b = Integer.parseInt( parts[2].trim() );
        return new Color( r, g, b );
    }

    /**
     * @return Restricts val in circle from -max to max. If val is to become lower then -180 e.g. -180-x (x>0) it becomes 180-x instead.
     */
    public static int restrict(int val, int max)
    {
        if( val < -max || val > max )
            return -(int)Math.signum( val ) * max + val % max;
        return val;
    }

    public static ModelData read(TextDataElement tde)
    {
        ModelData modelData = new ModelData();
        String content = tde.getContent();
        String[] lines = content.split( "\n" );
        for( String line : lines )
        {
            String[] parts = line.split( "\t" );
            switch( parts[0] )
            {
                case "X:":
                    modelData.setXDim( Double.parseDouble( parts[1] ), Double.parseDouble( parts[2] ), Double.parseDouble( parts[3] ) );
                    break;
                case "Y:":
                    modelData.setYDim( Double.parseDouble( parts[1] ), Double.parseDouble( parts[2] ), Double.parseDouble( parts[3] ) );
                    break;
                case "Z:":
                    modelData.setZDim( Double.parseDouble( parts[1] ), Double.parseDouble( parts[2] ), Double.parseDouble( parts[3] ) );
                    break;
                case "2D:":
                    modelData.setUse2D( Boolean.parseBoolean( parts[1] ) );
                    break;
                case "Substrates:":
                    modelData.setSubstrates( StreamEx.of( parts ).without( "Substrates:" ).toArray( String[]::new ) );
            }
        }
        return modelData;
    }
    
    public static DensityState fromTable(TableDataCollection tdc, String substrate)
    {
        DensityState result = new DensityState(tdc.getName());
        int count = tdc.getColumnModel().getColumnCount();
//        for( int i = 3; i < count; i++ )
//        {
//            String name = tdc.getColumnModel().getColumn( i ).getName();
            double[] vals = TableDataCollectionUtils.getColumn( tdc, substrate );
            result.addDensity( substrate, vals );
//        }
        return result;
    }

}