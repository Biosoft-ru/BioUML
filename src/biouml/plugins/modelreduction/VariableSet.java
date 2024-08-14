package biouml.plugins.modelreduction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.support.SerializableAsText;

/**
 * Group of variables corresponding to particular subdiagram or top level of composite or flat diagram
 * It can be used by user to specify variables (e.g. steadyState, input, output) for analysis
 * Variables of different subdiagrams may be not unique, thus VariableSet is needed.
 * It also allow user to select special subgroups of variables like all constant parameters, ODE variables, etc..
 * @author Ilya
 */
public class VariableSet extends Option implements SerializableAsText
{
    private static final String TOP_LEVEL = "(Top level)";
    private static final String NO_DIAGRAM_SELECTED = "(No diagram selected)";
    
    public static final String CONSTANT_PARAMETERS = "(Constant parameters)";
    public static final String RATE_VARIABLES = "(ODE variables)";
    public static final String ALL_VARIABLES = "(All variables)";
    public static final String TIME_VARIABLE = "time";

    private Diagram diagram;
    
    /**Engine provides information about which variable types are in the diagram and how to process that types
     * For now all of it is handled in this class
     * TODO: move this functionality to engines
     * */
    private SimulationEngine engine;
    private String[] variableNames;
    private Diagram variableSource;

    private String subdiagramName = TOP_LEVEL;

    public VariableSet(Diagram diagram, String[] variableNames)
    {
        setDiagram(diagram);
        this.variableNames = variableNames;
    }
    
    public VariableSet()
    {
        
    }
    
    public VariableSet(Diagram diagram, String subDiagramName, String[] variableNames)
    {
        setDiagram(diagram);
        setSubdiagramName(subDiagramName);
        this.variableNames = variableNames;
    }
    
    public void setDiagram(Diagram diagram)
    {
        if (this.diagram != null && this.diagram.equals(diagram))
            return;
        this.diagram = diagram;
        setSubdiagramName(TOP_LEVEL);
    }
    
    public Diagram getDiagram()
    {
        return diagram;
    }

    public void setEngine(SimulationEngine engine)
    {
        this.engine = engine;
        setDiagram(engine.getDiagram());
    }

    @PropertyName ( "Module" )
    public String getSubdiagramName()
    {
        return subdiagramName;
    }
    public void setSubdiagramName(String subdiagramName)
    {
        String oldValue = this.subdiagramName;
        this.subdiagramName = subdiagramName;
        variableSource = findVariableSource();
        setVariableNames(new String[0]);
        this.firePropertyChange("subdiagramName", oldValue, subdiagramName);
    }
    
    @PropertyName ( "Name" )
    public String[] getVariableNames()
    {
        return variableNames;
    }
    public void setVariableNames(String[] variableNames)
    {
        String[] oldValue = this.variableNames;
        this.variableNames = variableNames;
        this.firePropertyChange("variableNames", oldValue, variableNames);
    }
    
    public Diagram findVariableSource()
    {
        if( subdiagramName.equals(NO_DIAGRAM_SELECTED) )
            return null;
        
        if( subdiagramName.equals(TOP_LEVEL) )
            return diagram;

        DiagramElement de = diagram.get(subdiagramName);
        if( ! ( de instanceof SubDiagram ) )
            return null;
        return ( (SubDiagram)de ).getDiagram();
    }

    public StreamEx<String> getAvailableModules()
    {    	
        if (!DiagramUtility.isComposite(diagram))
            return StreamEx.of(TOP_LEVEL);
        return diagram == null ? StreamEx.of(NO_DIAGRAM_SELECTED)
                : StreamEx.of(Util.getSubDiagrams(diagram)).map(s -> s.getName()).prepend(TOP_LEVEL);
    }

    protected StreamEx<String> getAvailableVariables()
    {
        List<String> result = new ArrayList<>();
        result.add(ALL_VARIABLES);
        result.add(RATE_VARIABLES);
        result.add(CONSTANT_PARAMETERS);
        
        if (variableSource == null)
            findVariableSource();
        
        if( variableSource != null && variableSource.getRole() instanceof EModel )
            result.addAll(
                    variableSource.getRole( EModel.class ).getVariables().stream().map( v -> v.getName() ).collect( Collectors.toSet() ) );
        return StreamEx.of(result).sorted();
    }
    
    public String[] getVariables()
    {
        List<String> result = new ArrayList<>();
        boolean allConstant = ArrayUtils.contains(variableNames, CONSTANT_PARAMETERS);
        boolean allRate = ArrayUtils.contains(variableNames, RATE_VARIABLES);
        boolean all = ArrayUtils.contains(variableNames, ALL_VARIABLES);

        if( allConstant || all )
            result.addAll(getConstantParameteres().toSet());
        if( allRate || all )
            result.addAll(getRateVariables().toSet());

        result.addAll(StreamEx.of(variableNames).without(CONSTANT_PARAMETERS).without(RATE_VARIABLES).without(ALL_VARIABLES).without(TIME_VARIABLE).toList());

        return StreamEx.of(result).toArray(String[]::new);
    }

    public static String[] getVariablePaths(VariableSet[] variableSets)
    {
        List<String> targetVariableList = new ArrayList<>();
        for( VariableSet varSet : variableSets )
            targetVariableList.addAll(StreamEx.of(varSet.getVariablePaths()).toList());

        return StreamEx.of(targetVariableList).toArray(String[]::new);
    }

    public String[] getVariablePaths()
    {
        String path = DiagramUtility.generatPath(variableSource);
        return StreamEx.of(getVariables()).map(v -> path.isEmpty() ? v : path + SimulationEngine.VAR_PATH_DELIMITER + v)
                .toArray(String[]::new);
    }

    private StreamEx<String> getConstantParameteres()
    {
        if( variableSource == null || ! ( variableSource.getRole() instanceof EModel ) )
            return StreamEx.empty();

        //TODO exclude initial equations
        //TFCS
        return StreamEx.of( variableSource.getRole( EModel.class ).getParameters().stream() )
                .filter( v -> Variable.TYPE_PARAMETER.equals( v.getType() ) )
                .map(p -> p.getName());
    }

    //TODO: process variable types through Diagram and EModel
    private StreamEx<String> getRateVariables()
    {
//            if (!(engine instanceof JavaSimulationEngine))
//                return StreamEx.empty();
//            Diagram diagram = engine.getDiagram();
//            if( diagram == null || ! ( diagram.getRole() instanceof EModel ) || ! ( engine instanceof JavaSimulationEngine ) )
//                return StreamEx.empty();
    //
//            return StreamEx.of( ( (JavaSimulationEngine)engine ).getSpeciesNames());
//TFCS
return StreamEx.of( variableSource.getRole( EModel.class ).getVariables().stream() )
        .filter( v -> Variable.TYPE_DIFFERENTIAL.equals( v.getType() ) )
                    .map(p -> p.getName());
    }

    public VariableSet(String descr)
    {
        String[] arr = descr.split("\t");
        String path = arr[0];
        Diagram diagram = DataElementPath.create(path).getDataElement(Diagram.class);
        String subdiagramName = arr[1];
        String[] variableNames = arr[2].split(",");
        
        this.setDiagram(diagram);
        this.setSubdiagramName(subdiagramName);
        this.setVariableNames(variableNames);
    }

    @Override
    public String getAsText()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(DataElementPath.create(diagram));
        buffer.append("\t" + subdiagramName);
        buffer.append("\t" + StreamEx.of(variableNames).joining(","));
        return buffer.toString();
    }
    
    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("\t" + subdiagramName);
        buffer.append("\t" + StreamEx.of(variableNames).joining(","));
        return buffer.toString();
    }
}
