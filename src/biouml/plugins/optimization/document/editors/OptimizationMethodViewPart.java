package biouml.plugins.optimization.document.editors;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.beans.Option;

import biouml.model.Diagram;
import biouml.plugins.optimization.MessageBundle;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationConstraintCalculator;
import biouml.plugins.optimization.OptimizationMethodRegistry;
import biouml.plugins.optimization.OptimizationParameters;
import biouml.plugins.optimization.OptimizationUtils;
import biouml.plugins.optimization.ParameterEstimationProblem;
import biouml.plugins.optimization.document.OptimizationDocument;
import biouml.plugins.optimization.document.OptimizationDocumentListener;
import biouml.plugins.simulation.plot.PlotDialog;
import biouml.standard.simulation.SimulationResult;
import biouml.workbench.diagram.DiagramDocument;
import biouml.workbench.diagram.SetInitialValuesAction;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.DataElementPathDialog;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationMethodInfo;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.ExplorerPane;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.ViewPartSupport;
import ru.biosoft.jobcontrol.ClassJobControl;

@SuppressWarnings ( "serial" )
public class OptimizationMethodViewPart extends ViewPartSupport
{
    protected Logger log = Logger.getLogger(OptimizationMethodViewPart.class.getName());

    protected OptimizationMethodViewPane viewPane;
    public OptimizationMethodViewPart()
    {
        viewPane = new OptimizationMethodViewPane();
        initListeners();
        add(viewPane);
    }

    private boolean isMethodSelected;
    private OptimizationMethodInfo info;

    @Override
    public void explore(Object model, Document document)
    {
        this.model = model;
        this.document = document;

        if( model instanceof Optimization )
        {
            ( (OptimizationDocument)document ).removeOptimizationDocumentListener(documentListener);
            ( (OptimizationDocument)document ).addOptimizationDocumentListener(documentListener);
            ( (Optimization)model ).removePropertyChangeListener(optListener);
            ( (Optimization)model ).addPropertyChangeListener(optListener);

            method = ( (Optimization)model ).getOptimizationMethod();
            viewPane.setMethodName(method.getName());

            info = method.getOptimizationMethodInfo();
            info.removePropertyChangeListener(methodInfoListener);
            info.addPropertyChangeListener(methodInfoListener);
        }
    }

    @Override
    public boolean canExplore(Object model)
    {
        return ( model instanceof Optimization );
    }

    private OptimizationDocumentListener documentListener;
    private PropertyChangeListener optListener;
    private PropertyChangeListener methodInfoListener;

    private void initListeners()
    {
        documentListener = e -> plotAction.setEnabled(false);

        optListener = evt -> {
            if( evt.getPropertyName().equals(Optimization.OPTIMIZATION_METHOD) )
            {
                if( !isMethodSelected )
                {
                    Option mParams = method.getParameters();

                    mParams.removePropertyChangeListener((OptimizationDocument)document);
//                        DataElementPath diagramPath = ( (OptimizationMethodParameters)mParams ).getDiagramPath();

                    method = ( (Optimization)model ).getOptimizationMethod();
                    mParams = method.getParameters();

                    info = method.getOptimizationMethodInfo();
                    info.removePropertyChangeListener(methodInfoListener);
                    info.addPropertyChangeListener(methodInfoListener);

//                        ( (OptimizationMethodParameters)mParams ).setDiagramPath(diagramPath);

                    mParams.addPropertyChangeListener((OptimizationDocument)document);

                    viewPane.setMethodName( ( (Optimization)model ).getOptimizationMethod().getName());
                }
            }
        };

        methodInfoListener = evt -> {
            if( evt.getPropertyName().equals(OptimizationMethodInfo.DEVIATION) )
            {
                viewPane.setObjectiveFunctionValue(info.getDeviation());
            }
            else if( evt.getPropertyName().equals(OptimizationMethodInfo.PENALTY) )
            {
                viewPane.setPenaltyFunctionValue(info.getPenalty());
            }
            else if( evt.getPropertyName().equals(OptimizationMethodInfo.EVALUATIONS) )
            {
                viewPane.setEvaluationsNumber(info.getEvaluations());
            }
        };

        viewPane.getMethodNamesBox().addActionListener(ae -> initOptimizationMethod());
        viewPane.getMethodNamesBox().addPropertyChangeListener((OptimizationDocument)document);
    }

    private OptimizationMethod<?> method;
    private void initOptimizationMethod()
    {
        try
        {
            String methodName = viewPane.getMethodName();
            Option mParams = method.getParameters();

            if( !methodName.equals(method.getName()) )
            {
                mParams.removePropertyChangeListener((OptimizationDocument)document);
//                DataElementPath diagramPath = ( (OptimizationMethodParameters)mParams ).getDiagramPath();

                method = OptimizationMethodRegistry.getOptimizationMethod(methodName);
                method = method.clone(method.getOrigin(), method.getName());
                mParams = method.getParameters();

                info = method.getOptimizationMethodInfo();
                info.removePropertyChangeListener(methodInfoListener);
                info.addPropertyChangeListener(methodInfoListener);

//                ( (OptimizationMethodParameters)mParams ).setDiagramPath(diagramPath);

                mParams.addPropertyChangeListener((OptimizationDocument)document);

                isMethodSelected = true;
                ( (Optimization)model ).setOptimizationMethod(method);
                isMethodSelected = false;
            }

            ( (Optimization)model ).setDescription(method.getDescription());

            ExplorerPane explorerPane = (ExplorerPane)Application.getApplicationFrame().getPanelManager().getPanel(ApplicationFrame.EXPLORER_PANE_NAME);
            if( explorerPane != null )
                explorerPane.updateTab();
//            explorerPane.explore(info, null);
//            explorerPane.getPropertiesEditor().explore(method);

            viewPane.explore(mParams);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, MessageBundle.getMessage("ERROR_OPT_METHOD_INITIALIZATION"), ex);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Actions
    //

    public static final String RUN = "run optimization";
    public static final String STOP = "stop optimization";
    public static final String PLOT = "plot optimization result";
    public static final String OPEN_DIAGRAM = "open optimization diagram";
    public static final String SAVE_INTERMEDIATE_RESULTS = "save intermediate results";

    protected Action runAction = new RunAction(RUN);
    protected Action stopAction = new StopAction(STOP);
    protected Action plotAction = new PlotAction(PLOT);
    protected Action openDiagramAction = new OpenDiagramAction(OPEN_DIAGRAM);
    protected Action saveIntermediateResultsAction = new SaveIntermediateResultsAction(SAVE_INTERMEDIATE_RESULTS);
    protected Action setInitialValuesAction = new SetInitialValuesAction(log)
    {
        @Override
        protected void setValue(DataElement de, double value)
        {
            ((Parameter)de).setValue( value );
        }
        
        @Override
        protected Iterator<DataElement> getElementsIterator()
        {
            return StreamEx.of(( (Optimization)model ).getParameters().getFittingParameters()).map(p -> (DataElement) p).iterator();
        }
    };

    private boolean firstInit = true;

    @Override
    public Action[] getActions()
    {
        ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
        initializer.initAction(runAction, RUN);
        initializer.initAction(plotAction, PLOT);
        initializer.initAction(stopAction, STOP);
        initializer.initAction(openDiagramAction, OPEN_DIAGRAM);
        initializer.initAction(saveIntermediateResultsAction, SAVE_INTERMEDIATE_RESULTS);

        initializer = new ActionInitializer(biouml.workbench.diagram.MessageBundle.class);
        initializer.initAction(setInitialValuesAction, SetInitialValuesAction.KEY);

        if( firstInit )
        {
            plotAction.setEnabled(false);
            stopAction.setEnabled(false);
            saveIntermediateResultsAction.setEnabled(false);

            firstInit = false;
        }

        return new Action[] {runAction, stopAction, plotAction, openDiagramAction, saveIntermediateResultsAction, setInitialValuesAction};
    }

    class RunAction extends AbstractAction
    {
        public RunAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            run();
        }
    }

    class StopAction extends AbstractAction
    {
        public StopAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            stop();
        }
    }

    class PlotAction extends AbstractAction
    {
        public PlotAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            plot();
        }
    }

    class OpenDiagramAction extends AbstractAction
    {
        public OpenDiagramAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            openDiagram();
        }
    }

    class SaveIntermediateResultsAction extends AbstractAction
    {
        public SaveIntermediateResultsAction(String name)
        {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            saveIntermediateResults();
        }
    }

    private ClassJobControl jobControl;
    protected void run()
    {
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                Optimization optimization = (Optimization)model;
                method = optimization.getOptimizationMethod();
                method.setOwnerPath(optimization.getCompletePath());

                OptimizationConstraintCalculator calculator = new OptimizationConstraintCalculator();

                try
                {
                    OptimizationUtils.checkOptimization((Optimization)model, calculator);
                    runAction.setEnabled(false);
                    stopAction.setEnabled(true);
                    plotAction.setEnabled(false);
                    saveIntermediateResultsAction.setEnabled(true);
                    setInitialValuesAction.setEnabled(false);

                    OptimizationParameters params = optimization.getParameters();
                    OptimizationProblem problem = new ParameterEstimationProblem(params, calculator);

                    method.setOptimizationProblem(problem);

                    jobControl = method.getJobControl();
                    jobControl.addListener(viewPane.getJobControlListener());
                    jobControl.run();

                    Object[] optResults = method.getAnalysisResults();
                    toPlot = OptimizationUtils.refreshOptimizationDiagram(optResults, params);
                    if( toPlot != null )
                        plotAction.setEnabled(true);

                    runAction.setEnabled(true);
                    stopAction.setEnabled(false);
                    saveIntermediateResultsAction.setEnabled(false);
                    setInitialValuesAction.setEnabled(true);
                }
                catch( IllegalArgumentException e )
                {
                    log.log(Level.SEVERE, e.getMessage());
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, MessageBundle.getMessage("ERROR_OPTIMIZATION_SIMULATION"), e);
                }
            }
        };

        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void stop()
    {
        if( jobControl != null )
            jobControl.terminate();

        viewPane.stopJobControl();

        runAction.setEnabled(true);
        stopAction.setEnabled(false);
        saveIntermediateResultsAction.setEnabled(false);
        setInitialValuesAction.setEnabled(true);

        log.info(MessageBundle.getMessage("INFO_OPTIMIZATION_TERMINATION"));
    }

    private SimulationResult toPlot;
    private void plot()
    {
        try
        {
            PlotDialog plotDialog = new PlotDialog(toPlot);
            plotDialog.doModal();
        }
        catch( Exception exc )
        {
            log.log(Level.SEVERE, MessageBundle.getMessage("ERROR_OPTIMIZATION_PLOT"), exc);
        }
    }

    private void openDiagram()
    {
        try
        {
            OptimizationDocument optDocument = (OptimizationDocument)getDocument();
            Diagram diagram = ( (Optimization)model ).getOptimizationDiagram();
            DiagramDocument diagramDocument = optDocument.getDiagramDocument(diagram);

            GUI.getManager().addDocument(diagramDocument);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not open optimization diagram", e);
        }
    }

    private void saveIntermediateResults()
    {
        try
        {
            OptimizationMethod<?> method = ( (Optimization)model ).getOptimizationMethod();

            double[] values = method.getLastDisplayedSolution();
            double deviation = method.getLastDisplayedDeviation();
            double penalty = method.getLastDisplayedPenalty();

            if(values == null)
            {
                values = method.getIntermediateSolution();
                deviation = method.getDeviation();
                penalty = method.getPenalty();
            }

            int evaluations = method.getOptimizationProblem().getEvaluationsNumber();

            DataElementPathDialog dialog = new DataElementPathDialog();
            dialog.setValue(method.getParameters().getResultPath());
            if( dialog.doModal() )
            {
                method.saveResults(dialog.getValue(), values, deviation, penalty, evaluations, true, true);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not get intermediate results", e);
        }
    }
}
