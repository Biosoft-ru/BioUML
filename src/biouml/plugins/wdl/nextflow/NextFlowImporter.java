package biouml.plugins.wdl.nextflow;

//
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.*;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.util.DiagramImageGenerator;
import biouml.plugins.wdl.diagram.DiagramGenerator;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.plugins.wdl.model.CallInfo;
import biouml.plugins.wdl.model.CommandInfo;
import biouml.plugins.wdl.model.ExpressionInfo;
import biouml.plugins.wdl.model.InputInfo;
import biouml.plugins.wdl.model.OutputInfo;
import biouml.plugins.wdl.model.ScriptInfo;
import biouml.plugins.wdl.model.TaskInfo;
import biouml.plugins.wdl.model.WorkflowInfo;
import one.util.streamex.StreamEx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class NextFlowImporter
{
    static File workDir = new File( "C:/Users/Damag/nextflow_work" );
    static File imageFile = new File( workDir, "two_steps.png" );

    private static ScriptInfo scriptInfo;

    public static void main(String[] args) throws Exception
    {
        try
        {
            File workDir = new File( "C:/Users/Damag/nextflow_work/two_steps.nf" );
            String code = ApplicationUtils.readAsString( workDir );

            Diagram diagram = new NextFlowImporter().importNextflow( code );

            BufferedImage image = DiagramImageGenerator.generateDiagramImage( diagram );
            ImageWriter writer = ImageIO.getImageWritersBySuffix( "png" ).next();

            imageFile.delete();
            try (ImageOutputStream stream = ImageIO.createImageOutputStream( imageFile ))
            {
                writer.setOutput( stream );
                writer.write( image );
            }
            writer.dispose();

            System.out.println( "Done" );
            System.out.println( "========================================" );
            NextFlowGenerator nextflowGenerator = new NextFlowGenerator();
            String regenerated = nextflowGenerator.generate( diagram );
            System.out.println( regenerated );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    public Diagram importNextflow(String nextflow, Diagram diagram) throws Exception
    {
        parseNextflow( nextflow );
        new DiagramGenerator().generateDiagram( scriptInfo, diagram );
        return diagram;
    }
    
    public Diagram importNextflow(String nextflow) throws Exception
    {
        Diagram diagram = new WDLDiagramType().createDiagram( null, "test" );
        importNextflow(nextflow, diagram );
        return diagram;
    }

    public ScriptInfo parseNextflow(String nextflow) throws Exception
    {
        scriptInfo = new ScriptInfo();
        AstBuilder builder = new AstBuilder();
        List<ASTNode> nodes = builder.buildFromString( CompilePhase.SEMANTIC_ANALYSIS, false, nextflow );

        for( BinaryExpression param : getParams( nodes ) )
        {

        }
        for( MethodCallExpression process : getProcesses( nodes ) )
        {
            TaskInfo taskInfo = parseProcess( process );
            scriptInfo.addTask( taskInfo );
        }
        for( MethodCallExpression workflow : getWorkflows( nodes ) )
        {
            WorkflowInfo workflowInfo = parseWorkflow( workflow );
            scriptInfo.addWorkflow( workflowInfo );
        }
        return scriptInfo;
    }

    public TaskInfo parseProcess(MethodCallExpression processExpression) throws Exception
    {
        String name = getWorkflowName( processExpression );
        TaskInfo taskInfo = new TaskInfo( name );
        List<Statement> steps = getSteps( processExpression );
        List<String> currentLabels = null;
        for( Statement step : steps )
        {
            List<String> labels = step.getStatementLabels();
            if( labels != null )
                currentLabels = labels;

            if( ! ( step instanceof ExpressionStatement ) )
                throw new Exception( "Unknown process part " + step );

            Expression expression = ( (ExpressionStatement)step ).getExpression();

            if( expression instanceof MethodCallExpression )
            {
                MethodCallExpression methodCall = (MethodCallExpression)expression;
                String methodName = getMethodName( methodCall );
                boolean inout = methodName.equals( "val" ) || methodName.equals( "path" );
                if( inout && isInput( currentLabels ) )
                    parseInput( methodCall, taskInfo );
                else if( inout && isOutput( currentLabels ) )
                    parseOutput( methodCall, taskInfo );
                else if( !inout )
                    parseDirective( methodCall, taskInfo );
            }
            else if( isScript( currentLabels ) && expression instanceof GStringExpression )
            {
                String command = ( (GStringExpression)expression ).getText();
                List<Expression> variables = ( (GStringExpression)expression ).getValues();
                for( Expression variable : variables )
                {
                    String variableName = ( (VariableExpression)variable ).getName();
                    command = command.replace( "$" + variableName, "~{" + variableName + "}" );
                }
                CommandInfo commandInfo = new CommandInfo( command );
                taskInfo.setCommand( commandInfo );
            }
        }
        return taskInfo;
    }

    private OutputInfo parseOutput(MethodCallExpression expression, TaskInfo taskInfo)
    {
        String outputName = null;
        String outputExpression = null;
        String outputType = getMethodName( expression );
        Expression arguments = expression.getArguments();
        for( Expression argumentExpression : ( (ArgumentListExpression)arguments ).getExpressions() )
        {
            if( argumentExpression instanceof ConstantExpression )
            {
                outputExpression = ( (ConstantExpression)argumentExpression ).getValue().toString();
            }
            else if( argumentExpression instanceof MapExpression )
            {
                MapExpression mapping = ( (MapExpression)argumentExpression );
                for( MapEntryExpression entry : mapping.getMapEntryExpressions() )
                {
                    Expression key = entry.getKeyExpression();
                    Expression value = entry.getValueExpression();
                    if( key instanceof ConstantExpression && ( (ConstantExpression)key ).getValue().equals( "emit" ) )
                    {
                        outputName = value.getText();
                    }
                }
            }
        }
        OutputInfo outputInfo = new OutputInfo();
        outputInfo.setName( outputName );
        outputInfo.setExpression( outputExpression );
        outputInfo.setType( outputType );
        taskInfo.addOutputInfo( outputInfo );
        return outputInfo;
    }

    private void parseDirective(MethodCallExpression methodExpression, TaskInfo taskInfo)
    {
        String key = null;
        String value = null;

        Expression methodNameExpression = methodExpression.getMethod();
        if( methodNameExpression instanceof ConstantExpression )
        {
            key = ( (ConstantExpression)methodNameExpression ).getValue().toString();
        }
        Expression argumentExpressions = methodExpression.getArguments();
        for( Expression argumentExpression : ( (ArgumentListExpression)argumentExpressions ).getExpressions() )
        {
            if( argumentExpression instanceof ConstantExpression )
            {
                String variableName = ( (ConstantExpression)argumentExpression ).getValue().toString();
                value = variableName;
            }
        }
        if( key != null && value != null )
            taskInfo.setMetaProperty( key, value );
    }

    private InputInfo parseInput(MethodCallExpression expression, TaskInfo taskInfo)
    {
        String inputName = null;
        String inputType = getMethodName( expression );
        Expression argumentExpressions = expression.getArguments();
        for( Expression argumentExpression : ( (ArgumentListExpression)argumentExpressions ).getExpressions() )
        {
            if( argumentExpression instanceof VariableExpression )
            {
                inputName = ( (VariableExpression)argumentExpression ).getName();
            }
        }
        InputInfo inputInfo = new InputInfo();
        inputInfo.setName( inputName );
        inputInfo.setType( inputType );
        taskInfo.addInputInfo( inputInfo );
        return inputInfo;
    }

    public WorkflowInfo parseWorkflow(MethodCallExpression workflow) throws Exception
    {
        String name = getWorkflowName( workflow );
        WorkflowInfo info = new WorkflowInfo( name );
        List<Statement> steps = getSteps( workflow );

        List<String> currentLabels = null;
        for( Statement step : steps )
        {
            List<String> labels = step.getStatementLabels();
            if( labels != null )
                currentLabels = labels;
            parseStep( step, currentLabels, info );
        }
        return info;
    }

    private static boolean hasAny(List<String> labels, String ... toFind)
    {
        if( labels == null )
            return false;

        Set<String> labelSet = StreamEx.of( labels ).toSet();
        for( String s : toFind )
        {
            if( labelSet.contains( s ) )
                return true;
        }
        return false;
    }

    private static boolean isInput(List<String> labels)
    {
        return hasAny( labels, "input" );
    }

    private static boolean isOutput(List<String> labels)
    {
        return hasAny( labels, "output" );
    }

    private static boolean isScript(List<String> labels)
    {
        return hasAny( labels, "script" );
    }

    private static boolean isTake(List<String> labels)
    {
        return hasAny( labels, "take" );
    }

    private static boolean isMain(List<String> labels)
    {
        return hasAny( labels, "main" );
    }

    private static boolean isEmit(List<String> labels)
    {
        return hasAny( labels, "emit" );
    }

    public void parseStep(Statement statement, List<String> labels, WorkflowInfo workflowInfo) throws Exception
    {
        if( ! ( statement instanceof ExpressionStatement ) )
            return;

        Expression expression = ( (ExpressionStatement)statement ).getExpression();
        if( isMain( labels ) )
        {
            if( expression instanceof BinaryExpression )
            {
                Expression left = ( (BinaryExpression)expression ).getLeftExpression();
                Expression right = ( (BinaryExpression)expression ).getRightExpression();
                if( right instanceof MethodCallExpression )
                {
                    MethodCallExpression methodCall = (MethodCallExpression)right;
                    if( isTaskCall( methodCall ) )
                    {
                        CallInfo callInfo = createCallInfo( (MethodCallExpression)right, workflowInfo );
                        if( left instanceof VariableExpression )
                            callInfo.setAttribute( "NextflowResult", ( (VariableExpression)left ).getName() );
                        workflowInfo.addObject( callInfo );
                    }
                    else
                    {
                        ExpressionInfo info = parseDeclaration( methodCall, workflowInfo );
                        if( left instanceof VariableExpression )
                            info.setName( ( (VariableExpression)left ).getName() );
                    }
                }
            }
            else if( expression instanceof MethodCallExpression )
            {
                MethodCallExpression methodCall = (MethodCallExpression)expression;
                ExpressionInfo info = parseDeclaration( methodCall, workflowInfo );
            }
        }
        else if( isTake( labels ) && expression instanceof VariableExpression )
        {
            parseTake( (VariableExpression)expression, workflowInfo );
        }
        else if( isEmit( labels ) && expression instanceof BinaryExpression )
        {
            parseEmit( (BinaryExpression)expression, workflowInfo );
        }
    }

    private ExpressionInfo parseDeclaration(MethodCallExpression methodCall, WorkflowInfo workflowInfo)
    {
        ExpressionInfo expressionInfo = new ExpressionInfo();
        String formatted = NextFlowFormatter.format( methodCall );
        expressionInfo.setExpression( formatted );
        expressionInfo.setName( "" );
        workflowInfo.addObject( expressionInfo );
        return expressionInfo;
    }

    private InputInfo parseTake(VariableExpression variableExpression, WorkflowInfo workflowInfo)
    {
        String name = variableExpression.getName();
        InputInfo inputInfo = new InputInfo();
        inputInfo.setName( name );
        workflowInfo.addInput( inputInfo );
        return inputInfo;
    }

    private OutputInfo parseEmit(BinaryExpression binaryExpression, WorkflowInfo workflowInfo)
    {
        Expression left = binaryExpression.getLeftExpression();
        Expression right = binaryExpression.getRightExpression();
        String variable = null;
        String rhs = null;
        if( left instanceof VariableExpression )
        {
            variable = ( (VariableExpression)left ).getName();
        }
        if( right instanceof PropertyExpression )
        {
            rhs = parsePropertyExpression( (PropertyExpression)right, workflowInfo );
        }
        OutputInfo outputInfo = new OutputInfo();
        outputInfo.setName( variable );
        outputInfo.setExpression( rhs );
        workflowInfo.addOutput( outputInfo );
        return outputInfo;
    }

    private String parsePropertyExpression(PropertyExpression propertyExpression, WorkflowInfo workflowInfo)
    {
        Expression objectExpression = propertyExpression.getObjectExpression();
        Expression property = propertyExpression.getProperty();
        String source = null;
        String propertyString = null;
        if( objectExpression instanceof VariableExpression )
            source = ( (VariableExpression)objectExpression ).getName();
        if( property instanceof ConstantExpression )
            propertyString = ( (ConstantExpression)property ).getValue().toString();

        CallInfo callInfo = findCallByResult( workflowInfo, source );
        String callName = callInfo.getAlias();
        return callName + "." + propertyString;
    }

    public CallInfo findCallByResult(WorkflowInfo workflow, String resultName)
    {
        for( Object object : workflow.getObjects() )
        {
            if( object instanceof CallInfo )
            {
                String callResult = ( (CallInfo)object ).getAttribute( "NextflowResult" ).toString();
                if( resultName.equals( callResult ) )
                    return (CallInfo)object;
            }
        }
        return null;
    }

    public boolean isTaskCall(MethodCallExpression methodCall)
    {
        Expression method = methodCall.getMethod();
        Expression object = methodCall.getObjectExpression();
        return isThis( object ) && method instanceof ConstantExpression;
    }

    public CallInfo createCallInfo(MethodCallExpression methodCall, WorkflowInfo workflowInfo)
    {
        CallInfo callInfo = new CallInfo();
        Expression method = methodCall.getMethod();
        Expression object = methodCall.getObjectExpression();

        if( isThis( object ) && method instanceof ConstantExpression ) //this is process call
        {
            String taskName = ( (ConstantExpression)method ).getValue().toString();

            TaskInfo taskInfo = scriptInfo.getTask( taskName );
            List<ExpressionInfo> taskInputs = taskInfo.getInputs();

            callInfo.setTaskName( taskName );
            callInfo.setAlias( taskName );

            Expression arguments = methodCall.getArguments();
            List<Expression> argumentLst = ( (ArgumentListExpression)arguments ).getExpressions();
            int index = 0;
            for( Expression argument : argumentLst )
            {
                ExpressionInfo taskInput = taskInputs.get( index );
                if( argument instanceof VariableExpression )
                {
                    String input = ( (VariableExpression)argument ).getName();
                    InputInfo inputInfo = new InputInfo();
                    inputInfo.setExpression( input );
                    inputInfo.setName( taskInput.getName() );
                    callInfo.addInputInfo( inputInfo );

                    index++;
                }
                else if( argument instanceof PropertyExpression )
                {
                    String rhs = parsePropertyExpression( (PropertyExpression)argument, workflowInfo );
                    InputInfo inputInfo = new InputInfo();
                    inputInfo.setName( taskInput.getName() );
                    inputInfo.setExpression( rhs );
                    callInfo.addInputInfo( inputInfo );

                    index++;
                }
            }
        }
        return callInfo;
    }

    public static boolean isThis(Expression expression)
    {
        if( expression instanceof VariableExpression )
        {
            return "this".equals( ( (VariableExpression)expression ).getName() );
        }
        return false;
    }

    public List<Statement> getSteps(MethodCallExpression workflow)
    {
        Expression arguments = workflow.getArguments();
        if( arguments instanceof ArgumentListExpression )
        {
            List<Expression> expressions = ( (ArgumentListExpression)arguments ).getExpressions();
            Expression first = expressions.get( 0 );
            if( first instanceof ClosureExpression )
            {
                Statement code = ( (ClosureExpression)first ).getCode();
                if( code instanceof BlockStatement )
                    return ( (BlockStatement)code ).getStatements();
            }
            else if( first instanceof MethodCallExpression )
            {
                return getSteps( (MethodCallExpression)first );
            }
        }
        return null;
    }

    public String getWorkflowName(MethodCallExpression workflow)
    {
        Expression arguments = workflow.getArguments();
        if( arguments instanceof ArgumentListExpression )
        {
            List<Expression> expressions = ( (ArgumentListExpression)arguments ).getExpressions();
            if( expressions.size() > 0 )
            {
                Expression firstArgument = expressions.get( 0 );
                if( firstArgument instanceof MethodCallExpression )
                    return getMethodName( (MethodCallExpression)firstArgument );
            }
        }
        return "";
    }

    public String getMethodName(MethodCallExpression expression)
    {
        Expression method = expression.getMethod();
        if( method instanceof ConstantExpression )
            return ( (ConstantExpression)method ).getValue().toString();
        return "";
    }

    private static List<Expression> getExpressions(List<ASTNode> nodes)
    {
        List<Expression> result = new ArrayList<>();
        for( ASTNode node : nodes )
        {
            if( node instanceof BlockStatement )
            {
                for( Statement statement : ( (BlockStatement)node ).getStatements() )
                {
                    if( statement instanceof ExpressionStatement )
                    {
                        Expression expression = ( (ExpressionStatement)statement ).getExpression();
                        result.add( expression );
                    }
                }
            }
        }
        return result;
    }


    public List<BinaryExpression> getParams(List<ASTNode> nodes)
    {
        List<BinaryExpression> result = new ArrayList<>();
        for( ASTNode node : nodes )
        {
            if( node instanceof BlockStatement )
            {
                for( Statement statement : ( (BlockStatement)node ).getStatements() )
                {
                    if( statement instanceof ExpressionStatement )
                    {
                        Expression expression = ( (ExpressionStatement)statement ).getExpression();
                        if( expression instanceof BinaryExpression )
                        {
                            Expression left = ( (BinaryExpression)expression ).getLeftExpression();
                            if( left instanceof PropertyExpression )
                            {
                                Expression objectExpression = ( (PropertyExpression)left ).getObjectExpression();
                                if( objectExpression instanceof VariableExpression )
                                {
                                    if( "params".equals( ( (VariableExpression)objectExpression ).getName() ) )
                                        result.add( (BinaryExpression)expression );
                                }
                            }

                        }
                    }
                }
            }
        }
        return result;
    }

    public List<Expression> getDeclarations(List<ASTNode> nodes)
    {
        List<Expression> expressions = getExpressions( nodes );
        for( Expression expression : expressions )
        {
            //            System.out.println( expression );
        }
        return expressions;
    }

    public List<MethodCallExpression> getProcesses(List<ASTNode> nodes)
    {
        List<MethodCallExpression> result = new ArrayList<>();
        for( ASTNode node : nodes )
        {
            if( node instanceof BlockStatement )
            {
                for( Statement statement : ( (BlockStatement)node ).getStatements() )
                {
                    if( statement instanceof ExpressionStatement )
                    {
                        Expression expression = ( (ExpressionStatement)statement ).getExpression();
                        if( expression instanceof MethodCallExpression )
                        {
                            String methodName = getMethodName( (MethodCallExpression)expression );
                            if( methodName.equals( "process" ) )
                            {
                                result.add( (MethodCallExpression)expression );
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public List<MethodCallExpression> getWorkflows(List<ASTNode> nodes)
    {
        List<MethodCallExpression> result = new ArrayList<>();
        for( ASTNode node : nodes )
        {
            if( node instanceof BlockStatement )
            {
                for( Statement statement : ( (BlockStatement)node ).getStatements() )
                {
                    if( statement instanceof ExpressionStatement )
                    {
                        Expression expression = ( (ExpressionStatement)statement ).getExpression();
                        if( expression instanceof MethodCallExpression )
                        {
                            String methodName = getMethodName( (MethodCallExpression)expression );
                            if( methodName.equals( "workflow" ) )
                            {
                                result.add( (MethodCallExpression)expression );
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}