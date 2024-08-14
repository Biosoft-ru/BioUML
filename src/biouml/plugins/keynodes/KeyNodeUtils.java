package biouml.plugins.keynodes;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.SubFunctionJobControl;

import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.LayoutJobControl;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.ColorUtils;

import java.awt.Color;

import biouml.model.Diagram;
import biouml.model.DiagramElementStyleDeclaration;
import biouml.model.DiagramViewOptions;
import biouml.plugins.sbgn.SbgnDiagramViewOptions;
import biouml.workbench.graph.DiagramToGraphTransformer;

public class KeyNodeUtils
{
    public static void defineHighlightStyles(DiagramViewOptions viewOptions)
    {
        ColorFont defaultFont = viewOptions instanceof SbgnDiagramViewOptions ? ( (SbgnDiagramViewOptions)viewOptions ).getCustomTitleFont()
                : viewOptions.getDefaultFont();

        DiagramElementStyleDeclaration[] styles = new DiagramElementStyleDeclaration[6];
        styles[0] = new DiagramElementStyleDeclaration( KeyNodeConstants.HIT_KEY_HIGHLIGHT );
        styles[0].getStyle().setBrush( new Brush( ColorUtils.parseColor( "#FFCEFF" ) ) );
        styles[0].getStyle().setFont( defaultFont );
        styles[1] = new DiagramElementStyleDeclaration( KeyNodeConstants.KEY_HIGHLIGHT );
        styles[1].getStyle().setBrush( new Brush( ColorUtils.parseColor( "#FFD0D0" ) ) );
        styles[1].getStyle().setFont( defaultFont );
        styles[2] = new DiagramElementStyleDeclaration( KeyNodeConstants.HIT_HIGHLIGHT );
        styles[2].getStyle().setBrush( new Brush( ColorUtils.parseColor( "#CCF" ) ) );
        styles[2].getStyle().setFont( defaultFont );
        styles[3] = new DiagramElementStyleDeclaration( KeyNodeConstants.ACTIVATION_STYLE );
        styles[3].getStyle().setPen( new Pen( 1, new Color( 155, 0, 0 ) ) );
        styles[3].getStyle().setFont( defaultFont );
        styles[4] = new DiagramElementStyleDeclaration( KeyNodeConstants.INHIBITION_STYLE );
        styles[4].getStyle().setPen( new Pen( 1, new Color( 0, 85, 175 ) ) );
        styles[4].getStyle().setFont( defaultFont );
        styles[5] = new DiagramElementStyleDeclaration( KeyNodeConstants.USER_REACTION_STYLE );
        styles[5].getStyle().setPen( new Pen( 1, new Color( 0, 0, 255 ) ) );
        styles[5].getStyle().setFont( defaultFont );
        viewOptions.setStyles( styles );
    }

    public static void layoutDiagram(Diagram diagram, AbstractJobControl jc)
    {
        layoutDiagram( diagram, 40, 40, jc );
    }

    public static void layoutDiagram(Diagram diagram, int nodeDistanceX, int nodeDistanceY, AbstractJobControl jc)
    {
        LayoutSubJob lj = new LayoutSubJob( jc );
        HierarchicLayouter layouter = new HierarchicLayouter();
        layouter.setVerticalOrientation(true);
        layouter.setLayerDeltaX( nodeDistanceX );
        layouter.setLayerDeltaY( nodeDistanceY );
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
        int numberOfEstimatedOperations = pathwayLayouter.estimate(graph, 0);
        lj.setNumberOfEstimatedOperations(numberOfEstimatedOperations);
        pathwayLayouter.doLayout(graph, lj);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
        boolean notificationEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled( false );
        diagram.setPathLayouter( layouter );
        diagram.setNotificationEnabled( notificationEnabled );
    }

    public static class LayoutSubJob extends SubFunctionJobControl implements LayoutJobControl
    {
        private int numberOfEstimatedOperations;

        public LayoutSubJob(AbstractJobControl jobControl, int from, int to)
        {
            super( jobControl, from, to );
        }

        public LayoutSubJob(AbstractJobControl jobControl)
        {
            super(jobControl);
        }

        @Override
        public void done(int operationsDone)
        {
            double progressStep = 100d / numberOfEstimatedOperations;
            if( progressStep * operationsDone < 100 )
            {
                this.setPreparedness((int) ( progressStep * operationsDone + 0.5 ));
            }
            else
            {
                this.setPreparedness(100);
            }
        }

        @Override
        public int getNumberOfEstimatedOperations()
        {
            return numberOfEstimatedOperations;
        }

        @Override
        public void setNumberOfEstimatedOperations(int estimatedOperations)
        {
            numberOfEstimatedOperations = estimatedOperations;
        }

    }

}
