package biouml.plugins.sedml;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jlibsedml.SedML;

import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.type.Type;

public abstract class WorkflowParser
{
    protected Diagram workflow;
    protected SedML sedml;
    
    public WorkflowParser(Diagram workflow, SedML sedml)
    {
        this.workflow = workflow;
        this.sedml = sedml;
    }
    
    public abstract void parse() throws Exception;
    
    
    protected List<Compartment> findAnalyses(Class<? extends AnalysisMethod> analysisClass)
    {
        return findAnalyses( analysisClass, workflow, true );
    }
    
    public static List<Compartment> findAnalyses(Class<? extends AnalysisMethod> analysisClass, Compartment parent, boolean deep)
    {
        String name = AnalysisMethodRegistry.getAnalysisMethod( analysisClass ).getName();
        List<Compartment> result = new ArrayList<>();
        for( Compartment child : parent.stream( Compartment.class ) )
        {
            if( deep )
                result.addAll( findAnalyses( analysisClass, child, deep ) );
            String analysisFullName = (String)child.getAttributes().getValue( AnalysisDPSUtils.PARAMETER_ANALYSIS_FULLNAME );
            if( analysisFullName == null )
                continue;
            AnalysisMethod analysis = AnalysisMethodRegistry.getAnalysisMethod( analysisFullName );
            if( analysis == null )
                continue;
            String analysisName = analysis.getName();
            if( name.equals( analysisName ) )
                result.add( child );
        }
        return result;
    }

    protected static boolean isAnalysisNode(Compartment node, Class<? extends AnalysisMethod> analysisClass)
    {
        String fullAnalysisName = node.getAttributes().getValueAsString( AnalysisDPSUtils.PARAMETER_ANALYSIS_FULLNAME );
        if(fullAnalysisName == null)
            return false;
        AnalysisMethod analysisMethod = AnalysisMethodRegistry.getAnalysisMethod( fullAnalysisName );
        if(analysisMethod == null)
            return false;
        AnalysisMethod targetAnalysis = AnalysisMethodRegistry.getAnalysisMethod( analysisClass );
        return targetAnalysis.getName().equals( analysisMethod.getName() );
    }
    
    protected static boolean isDependsOnCycleVariable(Node node)
    {
        if(node.getKernel().getType().equals( Type.ANALYSIS_CYCLE_VARIABLE ))
            return true;
        return node.edges()
                .filter( e->e.getOutput() == node ).map( Edge::getInput )
                .anyMatch( WorkflowParser::isDependsOnCycleVariable );
    }
    
    private static final Pattern TITLE_PATTERN = Pattern.compile( "^(.*) [(](.*)[)]$" );
    protected static IdName parseTitle(String title)
    {
        Matcher matcher = TITLE_PATTERN.matcher( title );
        if(matcher.matches())
            return new IdName( matcher.group( 2 ), matcher.group(1) );
        return new IdName( title, null );
    }
    
    protected static class IdName
    {
        public final String id, name;
        public IdName(String id, String name)
        {
            this.id = id;
            this.name = name;
        }
    }
    
    protected static boolean isWorkflowVariable(Node node)
    {
        String kernelType = node.getKernel().getType();
        return kernelType.equals( Type.ANALYSIS_EXPRESSION ) || kernelType.equalsIgnoreCase( Type.ANALYSIS_PARAMETER );
    }
    
    protected static boolean isWorkflowExpression(Node node)
    {
        String kernelType = node.getKernel().getType();
        return kernelType.equals( Type.ANALYSIS_EXPRESSION );
    }
    
    protected static boolean isCycleVariable(Node node)
    {
        String kernelType = node.getKernel().getType();
        return kernelType.equals( Type.ANALYSIS_CYCLE_VARIABLE );
        
    }
}
