package biouml.plugins.expression;

import java.awt.Color;
import java.awt.Component;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.ShapeView;
import ru.biosoft.graphics.View;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanUtil;
import biouml.model.AbstractFilter;
import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.DiagramFilter;
import biouml.model.Edge;
import biouml.model.EquivalentNodeGroup;
import biouml.model.Node;
import biouml.standard.diagram.Util;
import biouml.standard.filter.CompositeHighlightAction;
import biouml.standard.filter.HighlightAction;
import biouml.standard.filter.InteriorHighlightAction;
import biouml.standard.filter.ShapedHighlightAction;
import biouml.standard.filter.TextMarkAction;


/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class ExpressionFilter extends AbstractFilter
{
    private static final Pen OUTSIDE_FILL_CONTOUR = new Pen(1.0f, new Color(100, 100, 100));
    private String name;

    //set loading=true to avoid table min max recalculation when filter properties are restored from saved
    //empty constructor is usually called during properties as bean initialization
    //call setLoading(false) after object creation to get working ExpressionFilter
    public ExpressionFilter()
    {
        setLoading( true );
    }

    /**
     * @param name
     */
    public ExpressionFilter(String name)
    {
        super();
        setName( name );
        setLoading( false );
    }

    @Override
    public Component getOptionsControl()
    {
        // TODO (not implemented as it goes through getProperties)
        return null;
    }

    /**
     * Hides or highlights diagram elements according to filter settings.
     * Prerequisite: diagram view should be generated by DiagramViewBuilder.
     */
    @Override
    public void apply(Compartment diagram)
    {
        if( !isEnabled() )
            return;

        if(properties.getTable() == null || !(properties.getTable().optDataElement() instanceof TableDataCollection))
            return;

        for(DiagramElement de: diagram)
        {
            // de.getKernel() instanceof biouml.standard.type.Compartment
            if(de instanceof Node && de.getKernel() != null)
                processNode((Node)de);
            if( de instanceof Compartment && ! ( de instanceof EquivalentNodeGroup ) )
                apply((Compartment)de);
        }
    }

    @Override
    protected void processNode(Node node)
    {
        if(node.getKernel() == null) return;
        TableDataCollection table = properties.getTable().getDataElement(TableDataCollection.class);
        String name = node.getKernel().getName();
        RowDataElement row = null;
        try
        {
            row = table.get(name);
        }
        catch( Exception e )
        {
        }
        if(row == null) return;
        if(properties.isUseInsideFill())
        {
            processInsideFill(node, row, properties.getInsideOptions());
        }
        if(properties.isUseOutsideFill())
        {
            processOutsideFill(node, row, properties.getOutsideOptions());
        }
        if(properties.isUsePval())
        {
            processPvalFill(node, row, properties.getPvalOptions());
        }
        if(properties.isUseFlux())
        {
            processFlux(node, row, properties.getFluxOptions());
        }
    }

    private void processOutsideFill(Node node, RowDataElement row, OutsideFillProperties properties)
    {
        CompositeHighlightAction compositeHighlightAction = new ShapedHighlightAction( properties.getFillWidth(),
                properties.isUseGradientFill() );
        if(properties.getColumns() == null || properties.getColumns().length == 0)
        {
            try
            {
                compositeHighlightAction.add(
                        new HighlightAction( new Brush( properties.getColor2() ), OUTSIDE_FILL_CONTOUR, null, properties.getFillWidth() ) );
            }
            catch( Exception e )
            {
            }
        } else
        {
            for(String column: properties.getColumns())
            {
                try
                {
                    double value = ((Number)row.getValue(column)).doubleValue();
                    String valueStr = String.format("%.4g", value);
                    Color color = properties.getColor(value);
                    compositeHighlightAction
                            .add( new HighlightAction( new Brush( color ), OUTSIDE_FILL_CONTOUR, valueStr, properties.getFillWidth() ) );
                }
                catch( Exception e )
                {
                }
            }
        }
        compositeHighlightAction.apply(node);
    }

    private void processInsideFill(Node node, RowDataElement row, InsideFillProperties properties)
    {
        if(properties.getColumn() == null || properties.getColumn().equals(ColumnNameSelector.NONE_COLUMN))
        {
            try
            {
                new InteriorHighlightAction(new Brush(properties.getColor2()), null).apply(node);
            }
            catch( Exception e )
            {
            }
        } else
        {
            try
            {
                double value = ((Number)row.getValue(properties.getColumn())).doubleValue();
                String valueStr = String.format("%.4g", value);
                Color color = properties.getColor(value);
                new InteriorHighlightAction(new Brush(color), valueStr).apply(node);
            }
            catch( Exception e )
            {
            }
        }
    }

    private void processPvalFill(Node node, RowDataElement row, PvalProperties properties)
    {
        if(properties.getColumn() == null || properties.getColumn().equals(ColumnNameSelector.NONE_COLUMN))
        {
        } else
        {
            try
            {
                double value = ((Number)row.getValue(properties.getColumn())).doubleValue();
                String valueStr = String.format("P-value=%.3g", value);
                String stars = properties.getStars(value);
                new TextMarkAction(stars, valueStr).apply(node);
            }
            catch( Exception e )
            {
            }
        }
    }

    private void processFlux(Node node, RowDataElement row, FluxProperties properties)
    {
        if(!Util.isReaction( node ))
            return;
        Object value = row.getValue( properties.getColumn() );
        if(!(value instanceof Number))
            return;
        double val = ((Number)value).doubleValue();
        val = (val - properties.getMin())/(properties.getMax()-properties.getMin());
        if(val < 0) val = 0;
        if(val > 1) val = 1;
        float thickness = (float) ( val*properties.getMaxWidth()+1.0 );
        for(Edge edge : node.getEdges())
        {
            updateFluxView( (CompositeView)edge.getView(), thickness );
        }
    }
    private void updateFluxView(CompositeView cView, float thickness)
    {
        for( View v : cView )
        {
            if( v instanceof CompositeView )
                updateFluxView( (CompositeView)v, thickness );
            else if( v instanceof ShapeView )
                ( (ShapeView)v ).setPen( ( (ShapeView)v ).getPen().withWidth( thickness ) );
        }
    }

    private ExpressionFilterProperties properties = new ExpressionFilterProperties();

    @Override
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        if( this.name == null )
            this.name = name;
    }

    public void setProperties(ExpressionFilterProperties properties)
    {
        if( properties == null )
            return;
        this.properties = properties;
    }
    @Override
    public ExpressionFilterProperties getProperties()
    {
        return properties;
    }

    @Override
    public DiagramFilter clone()
    {
        ExpressionFilter newFilter = (ExpressionFilter)super.clone();
        newFilter.properties = new ExpressionFilterProperties();
        BeanUtil.copyBean(getProperties(), newFilter.getProperties());
        return newFilter;
    }

    @Override
    public void setLoading(boolean loading)
    {
        super.setLoading( loading );
        properties.setLoading( loading );
    }
}