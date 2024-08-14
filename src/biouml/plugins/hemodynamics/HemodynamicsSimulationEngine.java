package biouml.plugins.hemodynamics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.jfree.data.general.Series;
import org.jfree.data.xy.XYSeries;

import ru.biosoft.math.model.Formatter;
import ru.biosoft.math.model.JavaFormatter;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.TempFiles;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.access.core.PluginEntry;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.util.EModelHelper;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.Preprocessor;
import biouml.plugins.simulation.ScalarCyclesPreprocessor;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineLogger;
import biouml.plugins.simulation.UniformSpan;
import biouml.standard.simulation.ResultListener;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

@SuppressWarnings ( "serial" )
@PropertyName ( "Hemodynamics simulation engine" )
@PropertyDescription ( "Simulation engine for hemodynamics arterial tree models." )
public class HemodynamicsSimulationEngine extends SimulationEngine
{
    ArterialBinaryTreeModel atm = null;
    protected String outputDir = TempFiles.path("simulation").getAbsolutePath();
    private Template velocityTemplate;
    private static final String templatePath = "resources/arterialModel.vm";
    private boolean resultAllVessels = false;
    private List<HemodynamicsPlotInfo> plotInfos;
    private PlotInfo vesselPlotInfo;
    private Map<String, Integer> varIndexMapping;

    public HemodynamicsSimulationEngine()
    {
        log = new SimulationEngineLogger(HemodynamicsSimulationEngine.class);
        simulator = new HemodynamicsModelSolver();
        simulatorType = "Hemodynamics";
    }

    @Override
    public ArterialBinaryTreeModel createModel() throws Exception
    {
        init();
        varIndexMapping = new HashMap<>();

        int index = 0;
        for( Variable var : this.executableModel.getVariables() )
        {
            if( ! ( var instanceof VariableRole ) )
                varIndexMapping.put(var.getName(), index++);
        }
        Util.numerateVessels(diagram);
        tempVariables = new HashMap<>();
        initPlotInfos();

        Edge e = Util.findRootVessel(diagram);

        atm = (ArterialBinaryTreeModel)compileModel(generateModel(true), true, outputDir)[0];
        atm.init();
        atm.setRoot(Util.getSimpleVessel(e, atm, executableModel));
        convertEdgestoArterialTree(e, atm.root, atm);
        return atm;
    }

    @Override
    protected List<Preprocessor> getDiagramPreprocessors()
    {
        List<Preprocessor> preprocessors = new ArrayList<>();
        preprocessors.add(new ScalarCyclesPreprocessor());
        return preprocessors;
    }


    /** * returns input stream to template  */
    protected InputStream getTemplateInputStream()
    {
        return HemodynamicsSimulationEngine.class.getResourceAsStream(templatePath);
    }


    @Override
    public File[] generateModel(boolean forceRewrite) throws Exception
    {
        diagramModified = false;
        log.info("Model " + diagram.getName() + ": Java code generating...");

        String name = executableModel.getDiagramElement().getName();
        File dir = new File(outputDir);
        dir.mkdirs();
        File modelFile = new File(dir, normalize(name) + ".java");

        if( velocityTemplate == null )
        {
            RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
            SimpleNode node = runtimeServices.parse(new InputStreamReader(getTemplateInputStream()), "ODE template");
            velocityTemplate = new Template();
            velocityTemplate.setRuntimeServices(runtimeServices);
            velocityTemplate.setData(node);
            velocityTemplate.initDocument();
            Velocity.init();
        }
        VelocityContext context = new VelocityContext();

        context.put("engine", this);

        //Creation time
        String pattern = "yyyy.MM.dd HH:mm:ss";
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        String creationTime = format.format(date);

        context.put("creationTime", creationTime);

        try (BufferedWriter bw = com.developmentontheedge.application.ApplicationUtils.utfWriter(modelFile))
        {
            velocityTemplate.merge(context, bw);
        }

        return new File[] {modelFile};
    }

    @Override
    public String normalize(String name)
    {
        return OdeSimulationEngine.escapeWrongSymbols(name);
    }

    @Override
    public String getEngineDescription()
    {
        return "Hemodynamics";
    }

    @Override
    public Object getSolver()
    {
        return simulator;
    }

    @Override
    public void setSolver(Object solver)
    {
        if( ! ( solver instanceof HemodynamicsSolver ) )
            throw new IllegalArgumentException("Only HemodynamicsModelSolver can be used with HemodynamicsSimulationEngine");
        Object oldValue = this.simulator;
        simulator = (HemodynamicsSolver)solver;
        firePropertyChange("solver", oldValue, solver);
    }


    @Override
    public String simulate(Model model, ResultListener[] resultListeners) throws Exception
    {
        UniformSpan uniformSpan = new UniformSpan(getInitialTime(), getCompletionTime(), getTimeIncrement());
        try
        {
            jobControl = new FunctionJobControl(log.getLogger());
            simulator.start(model, uniformSpan, resultListeners, jobControl);

            if( jobControl != null && jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST
                    || jobControl.getStatus() == JobControl.TERMINATED_BY_ERROR )
            {
                return simulator.getProfile().getErrorMessage();
            }
        }
        catch( Throwable t )
        {
            throw new Exception("Simulation error: " + t.getMessage());
        }
        finally
        {
            restoreOriginalDiagram();
        }
        return null;
    }

    @Override
    public String getVariableCodeName(String varName)
    {
        return varName;
    }

    @Override
    public String getVariableCodeName(String diagramName, String varName)
    {
        return varName;
    }

    @Override
    public Map<String, Integer> getVarIndexMapping()
    {
        return varIndexMapping;
    }


    @Override
    public Map<Var, List<Series>> getVariablesToPlot(PlotInfo plot)
    {
        if( plot.equals(this.getVesselPlotInfo()) )
        {
            Map<Var, List<Series>> variablesToPlot = new HashMap<>();
            for( HemodynamicsPlotInfo plotInfo : this.plotInfos )
            {
                if( plotInfo.doPlot() && tempVariables.containsKey(plotInfo.getVariable()) )
                {
                    String varName = plotInfo.getVariable();
                    List<Series> list = new ArrayList<>();
                    list.add(new XYSeries(varName));
                    variablesToPlot.put(new Var(varName, 0, tempVariables.get(varName), null), list);
                }
            }
            return variablesToPlot;
        }
        else
        {
            return super.getVariablesToPlot(plot);
        }
    }

    private void convertEdgestoArterialTree(Edge edge, SimpleVessel parent, ArterialBinaryTreeModel atm) throws Exception
    {

        Node node = edge.getOutput();
        List<Edge> vesselEdge = new ArrayList<>();

        for( Edge e : node.getEdges() )
        {
            if( !e.equals(edge) && Util.isVessel(e) )
                vesselEdge.add(e);
        }

        if( vesselEdge.size() == 0 )
            return;

        Edge leftVessel = vesselEdge.get(0);
        atm.addVessel(parent, Util.getSimpleVessel(leftVessel, atm, executableModel), true);

        Edge rightVessel = null;
        if( vesselEdge.size() > 1 )
        {
            rightVessel = vesselEdge.get(1);
            atm.addVessel(parent, Util.getSimpleVessel(rightVessel, atm, executableModel), false);
        }

        convertEdgestoArterialTree(leftVessel, parent.left, atm);

        if( rightVessel != null )
            convertEdgestoArterialTree(rightVessel, parent.right, atm);
    }

    @Override
    public boolean hasVariablesToPlot()
    {
        boolean result = super.hasVariablesToPlot();
        return result ? result : diagram.stream(Edge.class).filter(Util::isVessel).map(Util::getVessel)
                .anyMatch(v -> v.plotArea || v.plotFlow || v.plotPressure || v.plotPulseWaveVelocity || v.plotVelocity);
    }

    public void initPlotInfos() throws Exception
    {
        plotInfos = new ArrayList<>();
        int index = this.varIndexMapping.size();
        for( VariableRole var : diagram.getRole(EModel.class).getVariableRoles() )
        {
            Node node = (Node)var.getDiagramElement();

            if( node != null && node.getKernel() != null && ( node.getKernel().getType().equals("control-point")
                    || node.getKernel().getType().equals(HemodynamicsType.CONTROL_POINT) ) )
            {
                String vesselId = node.getAttributes().getValueAsString("vessel");
                String controlType = node.getAttributes().getValueAsString("type");
                if( controlType == null )
                    controlType = "Pressure";
                int controlSegment;
                try
                {
                    controlSegment = Integer.parseInt(node.getAttributes().getValueAsString("segment"));
                }
                catch( Exception ex )
                {
                    controlSegment = 0;
                }

                Vessel vessel = diagram.stream(Edge.class).filter(e -> Util.isVessel(e)).map(e -> Util.getVessel(e))
                        .filter(v -> v.getTitle().equals(vesselId)).findAny().orElse(null);
                if( vessel == null )
                    continue;

                VariableRole role = node.getRole(VariableRole.class);

                this.varIndexMapping.put(role.getName(), index);
                plotInfos.add(new HemodynamicsPlotInfo(role.getName(), vessel.getTitle(), vessel, controlType, controlSegment, true, null));
                index++;
            }
        }

        for( Vessel vessel : diagram.stream(Edge.class).filter(Util::isVessel).map(Util::getVessel) )
        {
            if( vessel.plotArea || resultAllVessels )
            {
                String name = this.createTempVar(vessel.getTitle() + "_Area", index++);
                plotInfos.add(new HemodynamicsPlotInfo(name, vessel.getTitle() + " Area", vessel, HemodynamicsPlotInfo.AREA,
                        vessel.getSegment(), vessel.plotArea, null));
            }

            if( vessel.plotPressure || resultAllVessels )
            {
                String name = this.createTempVar(vessel.getTitle() + "_Pressure", index++);
                plotInfos.add(new HemodynamicsPlotInfo(name, vessel.getTitle() + " Pressure", vessel, HemodynamicsPlotInfo.PRESSURE,
                        vessel.getSegment(), vessel.plotPressure, null));
            }

            if( vessel.plotFlow || resultAllVessels )
            {
                String name = this.createTempVar(vessel.getTitle() + "_Flow", index++);
                plotInfos.add(new HemodynamicsPlotInfo(name, vessel.getTitle() + " Flow", vessel, HemodynamicsPlotInfo.FLOW,
                        vessel.getSegment(), vessel.plotFlow, null));
            }

            if( vessel.plotVelocity || resultAllVessels )
            {
                String name = this.createTempVar(vessel.getTitle() + "_Velocity", index++);
                plotInfos.add(new HemodynamicsPlotInfo(name, vessel.getTitle() + " Velocity", vessel, HemodynamicsPlotInfo.VELOCITY,
                        vessel.getSegment(), vessel.plotVelocity, null));
            }

            if( vessel.plotPulseWaveVelocity || resultAllVessels )
            {
                String name = this.createTempVar(vessel.getTitle() + "_PWV", index++);
                plotInfos.add(new HemodynamicsPlotInfo(name, vessel.getTitle() + " Pulse Wave Velocity", vessel,
                        HemodynamicsPlotInfo.PULSE_WAVE_VELOCITY, vessel.getSegment(), vessel.plotPulseWaveVelocity, null));
            }
        }
    }

    private PlotInfo getVesselPlotInfo()
    {
        if( vesselPlotInfo == null )
        {
            vesselPlotInfo = new PlotInfo();
            vesselPlotInfo.setEModel(executableModel);
            vesselPlotInfo.setTitle("Vessel properties");
        }
        return vesselPlotInfo;
    }

    @Override
    public PlotInfo[] getPlots()
    {
        PlotInfo[] plots = super.getPlots();
        PlotInfo[] result = new PlotInfo[plots.length + 1];
        System.arraycopy(plots, 0, result, 0, plots.length);
        result[result.length - 1] = getVesselPlotInfo();
        return result;
    }

    public List<HemodynamicsPlotInfo> getPlotInfos()
    {
        if( plotInfos == null )
            plotInfos = new ArrayList<>();
        return plotInfos;
    }

    @Override
    protected List<PluginEntry> getClassPathEntries()
    {
        try
        {
            return Arrays.asList(ApplicationUtils.resolvePluginPath("biouml.plugins.hemodynamics:src.jar"));
        }
        catch( Exception e )
        {
            return Collections.emptyList();
        }
    }

    @Override
    public String getOutputDir()
    {
        return this.outputDir;
    }

    public boolean isCustomVariable(Variable variable)
    {
        return !ArterialBinaryTreeModel.basicVariables.keySet().contains(variable.getName());
    }

    public Map<String, Integer> getCustomVariables()
    {
        return ArterialBinaryTreeModel.basicVariables;
    }

    Formatter formatter = new JavaFormatter(new HashMap<String, Integer>());
    public String[] formatEquation(Equation eq)
    {
        return formatter.format(eq.getMath());
    }

    HashMap<String, Integer> tempVariables;
    private String createTempVar(String baseName, int index)
    {
        baseName = normalize(baseName);
        String name = EModelHelper.generateUniqueVariableName(executableModel, baseName);
        tempVariables.put(name, index);
        varIndexMapping.put(name, index);
        return name;
    }

    public Map<String, Integer> getTempVars()
    {
        return this.tempVariables;
    }


    private void init() throws Exception
    {
        diagram = originalDiagram.clone( originalDiagram.getOrigin(), originalDiagram.getName() );
        preprocess(diagram);

        String inputCondition = ( (HemodynamicsOptions)simulator.getOptions() ).getInputCondition();
        if( !HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING.equals(inputCondition) )
            executableModel.getVariable("inputPressure").setConstant(true);

        if( !HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING.equals(inputCondition) )
            executableModel.getVariable("inputFlow").setConstant(true);

        String outputCondition = ( (HemodynamicsOptions)simulator.getOptions() ).getOutputCondition();

        if( !HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING.equals(outputCondition) )
            executableModel.getVariable("outputPressure").setConstant(true);

        if( !HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING.equals(outputCondition) )
            executableModel.getVariable("outputFlow").setConstant(true);
    }

    @PropertyName ( "Save results for all vessels" )
    @PropertyDescription ( "If true then variables for P,Q and A for all vessels will be created." )
    public boolean isResultAllVessels()
    {
        return resultAllVessels;
    }

    public void setResultAllVessels(boolean resultForAllVessels)
    {
        this.resultAllVessels = resultForAllVessels;
    }

    @Override
    public void setOutputDir(String outputDir)
    {
        this.outputDir = outputDir;
    }

    @Override
    public Map<String, Integer> getVarPathIndexMapping()
    {
        return getVarIndexMapping();
    }
}
