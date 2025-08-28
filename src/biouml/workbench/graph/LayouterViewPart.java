package biouml.workbench.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.Action;
import javax.swing.JComponent;

import biouml.model.Diagram;
import biouml.model.util.DiagramImageGenerator;
import biouml.workbench.diagram.DiagramDocument;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.application.action.SeparatorAction;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.jobcontrol.JobProgressBar;

import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.LayoutJobControl;
import ru.biosoft.graphics.View;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.plugins.graph.LayouterOptionsListener;
import ru.biosoft.plugins.graph.LayouterPane;

@SuppressWarnings ( "serial" )
public class LayouterViewPart extends ViewPartSupport implements LayouterOptionsListener, JobControlListener
{
    private final LayouterPane layoterPane;

    private Diagram diagram;
    private Layouter currentLayouter;
    private Graph currentGraph;
    private Diagram currentDiagram;
    private LayoutJobControl jobControl;

    public LayouterViewPart()
    {
        layoterPane = new LayouterPane();
        layoterPane.addListener(this);
        add(layoterPane, BorderLayout.CENTER);
    }

    @Override
    public JComponent getView()
    {
        return this;
    }

    public Layouter getCurrentLayouter()
    {
        return currentLayouter;
    }

    public void setCurrentGraph(Graph graph)
    {
        this.currentGraph = graph;
    }

    public Graph getCurrentGraph()
    {
        return currentGraph;
    }

    public void setCurrentDiagram(Diagram diagram)
    {
        this.currentDiagram = diagram;
    }

    public LayoutJobControl getJobControl()
    {
        return jobControl;
    }
    public void setJobControl(LayoutJobControl jobControl)
    {
        this.jobControl = jobControl;
    }

    protected PartialLayoutAction partialLayoutAction;
    protected PrepareLayoutAction prepareLayoutAction;
    protected ExpertLayoutAction expertLayoutAction;
    protected ApplyLayoutAction applyLayoutAction;
    protected SaveLayoutAction saveLayoutAction;
    protected StopLayoutAction stopLayoutAction;

    @Override
    public Action[] getActions()
    {
        ActionManager actionManager = Application.getActionManager();
        if( prepareLayoutAction == null )
        {
            prepareLayoutAction = new PrepareLayoutAction();
            prepareLayoutAction.setEnabled(false);
            actionManager.addAction(PartialLayoutAction.KEY, prepareLayoutAction);

            new ActionInitializer(MessageBundle.class).initAction(prepareLayoutAction, PrepareLayoutAction.KEY);
            prepareLayoutAction.setEnabled(true);
        }
        if( partialLayoutAction == null )
        {
            partialLayoutAction = new PartialLayoutAction();
            partialLayoutAction.setEnabled(false);
            actionManager.addAction(PrepareLayoutAction.KEY, partialLayoutAction);

            new ActionInitializer(MessageBundle.class).initAction(partialLayoutAction, PartialLayoutAction.KEY);
            partialLayoutAction.setEnabled(true);
        }
        if( applyLayoutAction == null )
        {
            applyLayoutAction = new ApplyLayoutAction();
            applyLayoutAction.setEnabled(false);
            actionManager.addAction(ApplyLayoutAction.KEY, applyLayoutAction);

            new ActionInitializer(MessageBundle.class).initAction(applyLayoutAction, ApplyLayoutAction.KEY);
        }

        if( expertLayoutAction == null )
        {
            expertLayoutAction = new ExpertLayoutAction();
            expertLayoutAction.setEnabled(true);
            actionManager.addAction(ExpertLayoutAction.KEY, expertLayoutAction);

            new ActionInitializer(MessageBundle.class).initAction(expertLayoutAction, ExpertLayoutAction.KEY);
        }

        if( saveLayoutAction == null )
        {
            saveLayoutAction = new SaveLayoutAction();
            actionManager.addAction(SaveLayoutAction.KEY, saveLayoutAction);

            new ActionInitializer(MessageBundle.class).initAction(saveLayoutAction, SaveLayoutAction.KEY);
        }

        if( stopLayoutAction == null )
        {
            stopLayoutAction = new StopLayoutAction();
            actionManager.addAction(StopLayoutAction.KEY, stopLayoutAction);

            new ActionInitializer(MessageBundle.class).initAction(stopLayoutAction, StopLayoutAction.KEY);
        }

        return new Action[] {prepareLayoutAction/*, partialLayoutAction*/, stopLayoutAction, applyLayoutAction, new SeparatorAction(), saveLayoutAction,
                new SeparatorAction(), expertLayoutAction};
    }

    @Override
    public boolean canExplore(Object model)
    {
        getActions();
        if( model instanceof Diagram )
            return true;
        return false;
    }

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;
        diagram = ( (DiagramDocument)document ).getDiagram();
        getActions();
        if( applyLayoutAction != null )
        {
            Layouter layouter = diagram.getPathLayouter();
            if(layouter == null)
            {
                layouter = new ForceDirectedLayouter();
            }
            layoterPane.layouterSwitched(layouter);

            prepareLayoutAction.putValue(PrepareLayoutAction.DIAGRAM, diagram);
            prepareLayoutAction.putValue(PrepareLayoutAction.LAYOUTER_VIEW_PART, this);
            
            partialLayoutAction.putValue(PrepareLayoutAction.DIAGRAM, diagram);
            partialLayoutAction.putValue(PrepareLayoutAction.LAYOUTER_VIEW_PART, this);

            saveLayoutAction.putValue(SaveLayoutAction.DIAGRAM, diagram);
            saveLayoutAction.putValue(SaveLayoutAction.LAYOUTER_VIEW_PART, this);

            applyLayoutAction.putValue(ApplyLayoutAction.DIAGRAM, diagram);
            applyLayoutAction.putValue(ApplyLayoutAction.VIEW_PANE, ( (DiagramDocument)document ).getViewPane());
            applyLayoutAction.putValue(ApplyLayoutAction.LAYOUTER_VIEW_PART, this);

            expertLayoutAction.putValue(ExpertLayoutAction.LAYOUTER_VIEW_PART, this);

            stopLayoutAction.putValue(ExpertLayoutAction.LAYOUTER_VIEW_PART, this);
        }
    }

    @Override
    public void layouterSwitched(Layouter layouter)
    {
        if( diagram != null )
        {
            currentLayouter = layouter;
        }
    }

    //
    // Layouter job control listener methods
    //
    @Override
    public void valueChanged(JobControlEvent event)
    {
    }

    @Override
    public void jobStarted(JobControlEvent event)
    {
        applyLayoutAction.setEnabled(false);
        prepareLayoutAction.setEnabled(false);
        stopLayoutAction.setEnabled(true);
    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
        applyLayoutAction.setEnabled(true);
        prepareLayoutAction.setEnabled(true);
        layoterPane.resetPreview();
        stopLayoutAction.setEnabled(false);
    }

    @Override
    public void jobPaused(JobControlEvent event)
    {
    }

    @Override
    public void jobResumed(JobControlEvent event)
    {
    }

    @Override
    public void resultsReady(JobControlEvent event)
    {
        if( currentGraph != null )
        {
            DiagramToGraphTransformer.applyLayout(currentGraph);

            View view = DiagramImageGenerator.generateDiagramView( currentDiagram, ApplicationUtils.getGraphics() );
            Rectangle bounds = view.getBounds();

            int width = bounds.width + 30;
            int height = bounds.height + 30;

            Dimension viewSize = layoterPane.getViewDimension();

            double scale = 1;
            scale = Math.min(scale, (double) ( viewSize.width - 10 ) / width);
            scale = Math.min(scale, (double) ( viewSize.height - 10 ) / height);
            BufferedImage image = new BufferedImage((int) ( width * scale ), (int) ( height * scale ), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            graphics.scale(scale, scale);
            graphics.setClip(0, 0, width, height);
            graphics.setColor(Color.white);
            graphics.fill(new Rectangle(0, 0, width, height));
            view.paint(graphics);

            layoterPane.setPreviewImage(image);
            applyLayoutAction.setEnabled(true);
            prepareLayoutAction.setEnabled(true);
            stopLayoutAction.setEnabled(false);
        }
    }

    public void changeExpertMode()
    {
        layoterPane.changeExpertMode();
    }

    public void setProgresBar(JobProgressBar jpb)
    {
        layoterPane.setPreviewProgressBar(jpb);
        layoterPane.repaint();
    }
}
