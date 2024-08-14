package biouml.plugins.microarray;

import java.awt.Color;
import java.util.DoubleSummaryStatistics;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.PanelManager;

import biouml.model.AbstractFilter;
import biouml.model.Diagram;
import biouml.model.DiagramFilter;
import biouml.model.Node;
import biouml.standard.filter.Action;
import biouml.standard.filter.CompositeHighlightAction;
import biouml.standard.filter.HighlightAction;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 *  Microarray filter
 */
@SuppressWarnings ( "serial" )
public class MicroarrayFilter extends AbstractFilter
{
    private transient Diagram diagram;
    private transient PanelManager control;
    private transient DataCollection microarrays = null;

    public MicroarrayFilter()
    {
        microarrays = CollectionFactory.getDataCollection("data/microarray");
    }

    public MicroarrayFilter(String name, Diagram diagram)
    {
        this.name = name;
        this.diagram = diagram;
        microarrays = CollectionFactory.getDataCollection("data/microarray");
    }

    @Override
    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;
    }

    /**
     *  Name of filter
     */
    protected String name;
    @Override
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     *  Comment
     */
    protected String comment;
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        this.comment = comment;
    }

    /**
     * List of filter components
     */
    protected ColumnDescription[] elements;
    public ColumnDescription[] getElements()
    {
        if( elements == null )
        {
            elements = new ColumnDescription[0];
        }
        return elements;
    }
    public void setElements(ColumnDescription[] elements)
    {
        this.elements = elements;
    }

    public void addElement(String experimentID, String column, Action action, String comment)
    {
        elements = StreamEx.of( elements ).append( new ColumnDescription( experimentID, column, action, comment ) )
                .toArray( ColumnDescription[]::new );
    }
    public void removeElement(int i)
    {
        elements = StreamEx.of( elements, 0, i ).append( StreamEx.of( elements, i + 1, elements.length ) )
                .toArray( ColumnDescription[]::new );
    }

    /**
     *  Apply filter to node
     */
    @Override
    protected void processNode(Node node)
    {
        if( microarrays == null )
            return;
        CompositeHighlightAction compositeHighlightAction = new CompositeHighlightAction();
        int count = 0;
        MicroarrayLink maLink = (MicroarrayLink)node.getAttributes().getValue("maLink");
        if( maLink != null )
        {
            TableDataCollection me;
            try
            {
                me = (TableDataCollection)microarrays.get(maLink.getMicroarray());
            }
            catch( Throwable e )
            {
                return;
            }
            Object[] attributes = null;
            for( int i = 0; i < maLink.getGenes().size(); i++ )
            {
                try
                {
                    if( elements == null )
                    {
                        attributes = TableDataCollectionUtils.getRowValues(me, maLink.getGenes().get(i));
                    }
                    else
                    {
                        attributes = TableDataCollectionUtils.getRowValues(me, maLink.getGenes().get(i), getFilteredColumnsByMicroarray(me
                                .getName()));
                    }
                    break;
                }
                catch( Exception e )
                {
                }
            }
            if( attributes != null )
            {
                DoubleSummaryStatistics minMaxPair = TableDataCollectionUtils.findMinMax(me);
                double minValue = minMaxPair.getMin();
                double maxValue = minMaxPair.getMax();

                for( Object attribute : attributes )
                {
                    Color color = null;
                    try
                    {
                        double value = Double.parseDouble(String.valueOf(attribute));

                        int intValue = (int) ( ( ( value - minValue ) / ( maxValue - minValue ) ) * 155.0 );
                        if( intValue < 0 )
                            intValue = 0;
                        if( intValue > 155 )
                            intValue = 155;
                        color = new Color(intValue + 100, 255 - intValue, 0);
                    }
                    catch( NumberFormatException e )
                    {
                        color = new Color(255, 255, 255);
                    }
                    compositeHighlightAction.add(new HighlightAction(new Brush(color), new Pen(1.0f, new Color(100, 100, 100))));
                    count++;
                }
            }
            else
            {
                node.setView(diagram.getType().getDiagramViewBuilder().createNodeView(node, diagram.getViewOptions(), ApplicationUtils.getGraphics()));
            }
        }
        if( count > 0 )
        {
            compositeHighlightAction.apply(node);
        }
    }

    private String[] getFilteredColumnsByMicroarray(String microarray)
    {
        return StreamEx.of(elements).filter( cd -> cd.getExperimentID().equals( microarray ) ).toArray( String[]::new );
    }

    @Override
    public java.awt.Component getOptionsControl()
    {
        if( control == null )
        {
            control = new PanelManager();
        }

        return control;
    }

    @Override
    public DiagramFilter clone()
    {
        MicroarrayFilter newFilter = new MicroarrayFilter(name, diagram);
        newFilter.setComment(comment);
        newFilter.setElements( StreamEx.of( elements ).map( ColumnDescription::clone ).toArray( ColumnDescription[]::new ) );
        return newFilter;
    }
}
