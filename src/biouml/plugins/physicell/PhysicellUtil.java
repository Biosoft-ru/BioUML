package biouml.plugins.physicell;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.Model;
import ru.biosoft.physicell.ui.AgentColorer;
import ru.biosoft.physicell.ui.AgentVisualizer;
import ru.biosoft.util.DPSUtils;

public class PhysicellUtil
{
    /**
     * Find node connected to edge with given role class or null if there is no one
     */
    public static <T extends Role> T findNode(Edge e, Class<T> role)
    {
        return e.nodes().map( n -> n.getRole() ).select( role ).findAny().orElse( null );
    }

    /**
     * Retrieve role from attribute
     */
    public static <T extends Role> T validateRole(DiagramElement de, Class<T> c, String name) throws Exception
    {
        DynamicProperty dp = de.getAttributes().getProperty( name );
        if( dp == null )
        {
            dp = DPSUtils.createHiddenReadOnly( name, c, c.getConstructor( DiagramElement.class ).newInstance( de ) );
            de.getAttributes().add( dp );
        }
        T role = c.cast( dp.getValue() );
        de.setRole( role );
        return role;
    }

    public static CellDefinitionProperties findCellDefinition(String name, MulticellEModel emodel)
    {
        for( CellDefinitionProperties cdp : emodel.getCellDefinitions() )
        {
            if( cdp.getName().equals( name ) )
                return cdp;
        }
        return null;
    }

    public static BufferedImage generateLegend(Model model, ColorFont font, AgentColorer colorer)
    {
        int yOffset = 10;
        int xOffset = 10;
        int radius = 10;
        int nuclearRadius = 5;
        AgentVisualizer agentVisualizer = new AgentVisualizer();
        agentVisualizer.setAgentColorer( colorer );
        BufferedImage temp = new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB );
        Graphics gTemp = temp.getGraphics();
        List<String> names = new ArrayList<>();
        for( CellDefinition cd : model.getCellDefinitions() )
            names.add( cd.name );

        FontMetrics fm = gTemp.getFontMetrics( font.getFont() );

        int width = StreamEx.of( names ).mapToInt( s -> fm.stringWidth( s ) ).max().orElse( 0 );
        int lineHeight = (int)Math.round(fm.getStringBounds("G", gTemp).getHeight());
        int height = ( lineHeight + yOffset ) * names.size();

        BufferedImage img = new BufferedImage( width + xOffset + radius*2, height, BufferedImage.TYPE_INT_ARGB );
        Graphics g = img.getGraphics();
        int x = 0;
        int y = 0;
        for( CellDefinition cd : model.getCellDefinitions() )
        {
            String name = cd.name;
            Cell c = new Cell( cd, model , false);
            agentVisualizer.drawAgent( c, x + radius, y + radius, radius, nuclearRadius, g );
            g.setFont( font.getFont() );
            g.setColor( Color.black );
            g.drawString( name, x + 2 * radius + xOffset, y + radius  + lineHeight/2 );
            y += ( fm.getHeight() + yOffset );
        }
        return img;
    }
}