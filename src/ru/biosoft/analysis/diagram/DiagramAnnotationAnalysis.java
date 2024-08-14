package ru.biosoft.analysis.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Stub;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.graphics.GraphicsUtils;
import ru.biosoft.graphics.HtmlView;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.TextUtil;

@ClassIcon("resources/AnnotateDiagram.gif")
public class DiagramAnnotationAnalysis extends AnalysisMethodSupport<DiagramAnnotationAnalysisParameters>
{
    public DiagramAnnotationAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new DiagramAnnotationAnalysisParameters() );
    }

    @Override
    public Diagram justAnalyzeAndPut() throws Exception
    {
        Diagram input = parameters.getInputDiagram().getDataElement( Diagram.class );
        Diagram result = input.clone( parameters.getOutputDiagram().getParentCollection(), parameters.getOutputDiagram().getName() );
        TableDataCollection table = parameters.getTable().getDataElement( TableDataCollection.class );
        int columnIndex = table.getColumnModel().getColumnIndex( parameters.getColumn() );
        Compartment compartment = result;
        log.info( "Annotating..." );
        boolean annotationAdded = false;
        for(Node node : result.getNodes())
        {
            RowDataElement row = table.get( node.getName() );
            if(row != null)
            {
                Stub.Note kernel = new Stub.Note( compartment, "note_"+node.getName() );
                String title = TextUtil.toString( row.getValues()[columnIndex] );
                kernel.setTitle( title );
                Node note = new Node(compartment, kernel);
                Rectangle bounds = GraphicsUtils.getAsComplexTextView(new HtmlView( title, result.getViewOptions().getDefaultFont(), new Point(0,0), new Dimension(200, 10))).getBounds();
                Dimension dim = new Dimension(bounds.width+10, bounds.height+10);
                note.setShapeSize( dim );
                compartment.put( note );
                Edge edge = new Edge( new Stub.NoteLink( compartment, note.getName()+" -> "+node.getName() ), note, node );
                compartment.put( edge );
                annotationAdded = true;
            }
        }
        jobControl.setPreparedness( 50 );
        if(annotationAdded)
        {
            log.info( "Layouting..." );
            HierarchicLayouter layouter = new HierarchicLayouter();
            layouter.setVerticalOrientation(true);
            Graph graph = DiagramToGraphTransformer.generateGraph(result, null);
            PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
            pathwayLayouter.doLayout(graph, null);
            DiagramToGraphTransformer.applyLayout(graph, result);
        } else
        {
            log.warning( "No annotation found: check whether input table has proper data" );
        }
        CollectionFactoryUtils.save( result );
        jobControl.setPreparedness( 100 );
        return result;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        if(getParameters().getColumn() == null || ColumnNameSelector.NONE_COLUMN.equals( getParameters().getColumn()))
            throw new IllegalArgumentException( "Please specify column" );
    }
}
