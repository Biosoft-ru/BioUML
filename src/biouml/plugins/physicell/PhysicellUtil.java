package biouml.plugins.physicell;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.TreeMap;

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

    
    private static void hack4CovidModel(CellDefinition cd, Model model, Map<String, Cell> cells)
    {
        if( cd.name.equals( "macrophage" ) )
        {
            Cell c2 = new Cell( cd, model, false );
            c2.customData.set( "activated_immune_cell", 1 );
            cells.put( cd.name + " activated", c2 );

            Cell c3 = new Cell( cd, model, false );
            c3.phenotype.volume.total = c3.customData.get( "threshold_macrophage_volume" ) + 10;
            cells.put( cd.name + " exhausted", c3 );

            Cell c4 = new Cell( cd, model, false );
            c4.customData.set( "ability_to_phagocytose_infected_cell", 1 );
            cells.put( cd.name + " hyperactivated", c4 );
        }
        else if( cd.name.equals( "DC" ) )
        {
            Cell c2 = new Cell( cd, model, false );
            c2.customData.set( "activated_immune_cell", 1 );
            cells.put( "Dendrite cells activated", c2 );
        }
        else if( cd.name.equals( "lung epithelium" ) )
        {
            Cell c2 = new Cell( cd, model, false );
            int color_index = c2.customData.findVariableIndex( c2.getModel().getParameterString( "color_variable" ) );
            c2.customData.set( color_index, 750 );
            cells.put( cd.name + " infected", c2 );
        }
    }
    
    public static BufferedImage generateLegend(Model model, ColorFont font, AgentColorer colorer)
    {
        int yOffset = 10;
        int xOffset = 10;
        int radius = 10;
        AgentVisualizer agentVisualizer = new AgentVisualizer();
        agentVisualizer.setAgentColorer( colorer );
        BufferedImage temp = new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB );
        Graphics gTemp = temp.getGraphics();

        TreeMap<String, Cell> cells = new TreeMap<>();
        for( CellDefinition cd : model.getCellDefinitions() )
        {
            Cell c = new Cell( cd, model, false );
            if( cd.name.equals( "DC" ) )
                cells.put( "Dendrite cells", c );
            else
                cells.put( cd.name, c );
            hack4CovidModel( cd, model, cells );
        }

        FontMetrics fm = gTemp.getFontMetrics( font.getFont() );

        int width = StreamEx.of( cells.keySet() ).mapToInt( s -> fm.stringWidth( s ) ).max().orElse( 0 );
        int lineHeight = (int)Math.round( fm.getStringBounds( "G", gTemp ).getHeight() );
        int height = ( lineHeight + yOffset ) * ( cells.size() + 1 );

        BufferedImage img = new BufferedImage( width + 2*xOffset + radius * 2, height, BufferedImage.TYPE_INT_ARGB );
        Graphics g = img.getGraphics();
        int x = 0;
        int y = 0;


        for( String name : cells.keySet() )
        {
            Cell c = cells.get( name );
            agentVisualizer.drawAgent( c, x + radius, y + radius, (int)c.getRadius(), (int)c.phenotype.geometry.getNuclearRadius(), g );
            g.setFont( font.getFont() );
            g.setColor( Color.black );
            g.drawString( name, x + 2 * radius + xOffset, y + radius + lineHeight / 2 );
            y += ( fm.getHeight() + yOffset );
        }
        return img;
    }
}