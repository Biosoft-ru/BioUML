package biouml.plugins.optimization;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Type;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.HtmlDescribedElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisParameters;

@SuppressWarnings ( "serial" )
@ClassIcon ( "resources/optimization.gif" )
public class Optimization extends BaseSupport implements AnalysisMethod, HtmlDescribedElement
{
    protected Logger log = Logger.getLogger(Optimization.class.getName());

    public static final String OPTIMIZATION_METHOD = "optimizationMethod";
    public static final String OPTIMIZATION_DIAGRAM_PATH = "optimizationDiagramPath";

    /**
     * Its i not recommended to use this method, if however you use it be sure to set up diagram to the optimization parameters.<br>
     * <b>TODO:</b> do something about variety of optimization parameters and possible inconsistencies.<br>
     * For example: it is assumed that diagram can not be changed for optimization, however for now it is the parameter and has setter.
     * @param name
     * @param origin
     */
    public Optimization(String name, DataCollection<?> origin)
    {
        super(origin, name, Type.ANALYSIS_METHOD);
        String anName = OptimizationMethodRegistry.getOptimizationMethodNames().findFirst().get();
        optimizationMethod = OptimizationMethodRegistry.getOptimizationMethod( anName );
        methodInfo = OptimizationMethodRegistry.getMethodInfo( anName );
        //        optimizationMethod = optimizationMethod.clone(optimizationMethod.getOrigin(), optimizationMethod.getName());
        description = optimizationMethod.getDescription();

        optimizationParameters = new OptimizationParameters();
        optimizationParameters.setOptimizerParameters(optimizationMethod.getParameters());
        optimizationParameters.setParent(this);
        optimizationDiagramPath = DataElementPath.create(origin, name + "_diagram");
    }

    public static Optimization createOptimization(String name, DataCollection<?> origin, Diagram diagram)
    {
        Optimization optimization = new Optimization(name, origin);
        optimization.getParameters().setDiagram(diagram);
        return optimization;
    }

    public Diagram getDiagram()
    {
        return getParameters().getDiagram();
    }

    //AnalysisMethod implementation
    private String description;
    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public void setDescription(String description)
    {
        this.description = description;
    }

    protected OptimizationParameters optimizationParameters;
    @Override
    public OptimizationParameters getParameters()
    {
        return optimizationParameters;
    }

    @Override
    public void setParameters(AnalysisParameters parameters)
    {
        if( ! ( parameters instanceof OptimizationParameters ) )
            return;
        optimizationParameters = (OptimizationParameters)parameters;
    }

    @Override
    public Map<String, String> generateScripts(AnalysisParameters parameters)
    {
        return Collections.emptyMap();
    }

    @Override
    public AnalysisJobControl getJobControl()
    {
        return null;
    }

    ////////////////////////////////////////

    public Diagram getOptimizationDiagram()
    {
        return (Diagram)optimizationDiagramPath.optDataElement();
    }

    private DataElementPath optimizationDiagramPath = null;
    public DataElementPath getOptimizationDiagramPath()
    {
        return this.optimizationDiagramPath;
    }

    public void setOptimizationDiagramPathQuiet(DataElementPath optDiagramPath)
    {
        this.optimizationDiagramPath = optDiagramPath;
    }

    public void setOptimizationDiagramPath(DataElementPath optDiagramPath)
    {
        DataElementPath oldValue = optimizationDiagramPath;
        this.optimizationDiagramPath = optDiagramPath;
        firePropertyChange(OPTIMIZATION_DIAGRAM_PATH, oldValue, optDiagramPath);
    }

    private OptimizationMethod<?> optimizationMethod;
    private AnalysisMethodInfo methodInfo;
    public OptimizationMethod<?> getOptimizationMethod()
    {
        return this.optimizationMethod;
    }
    public void setOptimizationMethod(OptimizationMethod<?> optMethod)
    {
        OptimizationMethod<?> oldValue = optimizationMethod;
        this.optimizationMethod = optMethod;
        this.methodInfo = OptimizationMethodRegistry.getMethodInfo( optMethod.getName() );
        this.optimizationParameters.setOptimizerParameters(optMethod.getParameters());
        firePropertyChange(OPTIMIZATION_METHOD, oldValue, optMethod);
    }
    public void fireOptimizationMethodChange()
    {
        firePropertyChange(OPTIMIZATION_METHOD, null, null);
    }

    public void initDiagramParameters()
    {
        Diagram diagram = this.getParameters().getDiagram();
        if( diagram != null && diagram.getRole() != null )
        {
            EModel model = diagram.getRole(EModel.class);
            List<Parameter> fParams = getParameters().getFittingParameters();
            StreamEx.of( fParams )
                .mapToEntry( par -> model.getVariable( par.getName() ) )
                .nonNullValues()
                .forKeyValue( (par, var) -> var.setInitialValue( par.getValue() ) );
        }
    }

    @Override
    public Logger getLogger()
    {
        return log;
    }

    @Override
    public void setLogger(Logger log)
    {
        this.log = log;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        //TODO: add parameters validation
    }

    @Override
    public String getDescriptionHTML()
    {
        return methodInfo.getDescriptionHTML();
    }

    @Override
    public URL getBase()
    {
        return methodInfo.getBase();
    }

    @Override
    public String getBaseId()
    {
        return methodInfo.getBaseId();
    }

    @Override
    public double estimateWeight()
    {
        return 1;
    }

    @Override
    public long estimateMemory()
    {
        return 0;
    }

    @Override
    public Optimization clone(DataCollection<?> newOrigin, String newName)
    {
        Optimization clone = (Optimization)super.clone(newOrigin, newName);
        clone.setDescription(getDescription());
        OptimizationMethod<?> method = getOptimizationMethod();
        clone.setOptimizationMethod( method.clone( method.getOrigin(), method.getName() ) );
        clone.setParameters( getParameters().clone() );
        clone.setTitle(getTitle());
        return clone;
    }
}
