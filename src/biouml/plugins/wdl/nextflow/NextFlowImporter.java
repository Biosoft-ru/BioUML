package biouml.plugins.wdl.nextflow;


import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.*;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.util.DiagramImageGenerator;
import biouml.plugins.wdl.diagram.DiagramGenerator;
import biouml.plugins.wdl.diagram.WDLDiagramType;
import biouml.plugins.wdl.diagram.WDLLayouter;
import biouml.plugins.wdl.model.CallInfo;
import biouml.plugins.wdl.model.CommandInfo;
import biouml.plugins.wdl.model.ConditionalInfo;
import biouml.plugins.wdl.model.ContainerInfo;
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
    //    static File nextflowFile = new File(
    //            "C:/Users/Damag/eclipse_2024_6/BioUML/src/biouml/plugins/wdl/test_examples/nextflow/simple_if.nf" );
    //        static File nextflowFile = new File( "C:/Users/Damag/nextflow_work/two_steps.nf" );

    static File nextflowFile = new File( "C:/Users/Damag/nextflow_work/ifelse.nf" );

    static File imageFile = new File( workDir, "two_steps.png" );

    private static ScriptInfo scriptInfo;

    public static void main(String[] args) throws Exception
    {
        try
        {

            String code = ApplicationUtils.readAsString( nextflowFile );

            Diagram diagram = new NextFlowImporter().importNextflow( code );
            new WDLLayouter().layout( diagram );

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
        importNextflow( nextflow, diagram );
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
        List<Statement> statements = getStatements( processExpression );
        List<String> currentLabels = null;
        for( Statement step : statements )
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
            else if( isScript( currentLabels ) )
            {
                String command = NextFlowFormatter.format( expression );
                //                String command = ( (GStringExpression)expression ).getText();
                //                List<Expression> variables = ( (GStringExpression)expression ).getValues();
                //                for( Expression variable : variables )
                //                {
                //                    String variableName = ( (VariableExpression)variable ).getName();
                //                    command = command.replace( "$" + variableName, "~{" + variableName + "}" );
                //                }
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
        WorkflowInfo workflowInfo = new WorkflowInfo( name );
        List<Statement> statements = getStatements( workflow );

        for( Statement take : findLabeled( statements, "take" ) )
        {
            parseTake( take, workflowInfo );
        }

        for( Statement emit : findLabeled( statements, "emit" ) )
        {
            parseEmit( emit, workflowInfo );
        }

        for( Statement step : findLabeled( statements, "main" ) )
        {
            workflowInfo.addObject( parseStep( step, workflowInfo ) );
        }
        return workflowInfo;
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


    public List<Statement> findLabeled(List<Statement> statements, String label)
    {
        List<Statement> result = new ArrayList<>();
        boolean labelFound = false;
        for( Statement statement : statements )
        {
            List<String> labels = statement.getStatementLabels();

            boolean hasLabel = hasAny( labels, label );
            boolean noLabels = labels == null;
            boolean otherLabel = !hasLabel && !noLabels;

            if( hasLabel )
                labelFound = true;
            else if( otherLabel && labelFound )
                break;

            if( labelFound && ( hasLabel || noLabels ) )
            {
                result.add( statement );
            }
        }
        return result;
    }

    public Object parseStep(Statement statement, WorkflowInfo workflowInfo) throws Exception
    {
        if( statement instanceof ExpressionStatement )
        {
            Expression expression = ( (ExpressionStatement)statement ).getExpression();
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
                        return callInfo;
                    }
                    else
                    {
                        ExpressionInfo info = parseDeclaration( methodCall );
                        if( left instanceof VariableExpression )
                            info.setName( ( (VariableExpression)left ).getName() );
                        return info;
                    }
                }
            }
            else if( expression instanceof MethodCallExpression )
            {
                return parseDeclaration( (MethodCallExpression)expression );
            }
            else
            {
                throw new Exception( "Unknown statement " + statement );
            }
        }
        else if( statement instanceof IfStatement )
        {
            return parseConditional((IfStatement )statement, workflowInfo);
        }
        else
        {
            throw new Exception( "Unknown statement " + statement );
        }
        return null;
    }
    
    private ConditionalInfo parseConditional(IfStatement ifStatement, WorkflowInfo workflowInfo) throws Exception
    {
        ConditionalInfo conditionalInfo = new ConditionalInfo();
        parseConditionalBlock(ifStatement, conditionalInfo, workflowInfo);
        return conditionalInfo;
    }
    
    private void parseConditionalBlock(IfStatement ifStatement, ConditionalInfo conditionalInfo, WorkflowInfo workflowInfo) throws Exception
    {
        String condition = NextFlowFormatter.format( ifStatement.getBooleanExpression() );
        BlockStatement ifBlock = (BlockStatement)ifStatement.getIfBlock();
        for( Statement inBlockStatement : ifBlock.getStatements() )
        {
            Object step = parseStep( inBlockStatement, workflowInfo );
            conditionalInfo.add( condition, step );
        }
        
        Statement elseStatement = ifStatement.getElseBlock();
        if( elseStatement instanceof BlockStatement )
        {
            for( Statement inBlockStatement : ( (BlockStatement)elseStatement ).getStatements() )
            {
                Object step = parseStep( inBlockStatement, workflowInfo );
                conditionalInfo.addElse( step );
            }
        }
        else if (elseStatement instanceof IfStatement)
        {
            parseConditionalBlock((IfStatement)elseStatement, conditionalInfo, workflowInfo); 
        }
    }
    

    private ExpressionInfo parseDeclaration(MethodCallExpression methodCall)
    {
        ExpressionInfo expressionInfo = new ExpressionInfo();
        String formatted = NextFlowFormatter.format( methodCall );
        expressionInfo.setExpression( formatted );
        expressionInfo.setName( "" );
        return expressionInfo;
    }

    private InputInfo parseTake(Statement statement, WorkflowInfo workflowInfo)
    {
        if( statement instanceof ExpressionStatement )
        {
            Expression expression = ( (ExpressionStatement)statement ).getExpression();
            VariableExpression variableExpression = (VariableExpression)expression;
            String name = variableExpression.getName();
            InputInfo inputInfo = new InputInfo();
            inputInfo.setName( name );
            workflowInfo.addInput( inputInfo );
            return inputInfo;
        }
        return null;
    }

    private OutputInfo parseEmit(Statement statement, WorkflowInfo workflowInfo)
    {
        if( statement instanceof ExpressionStatement )
        {
            Expression expression = ( (ExpressionStatement)statement ).getExpression();
            String variable = null;
            String rhs = null;

            if( expression instanceof BinaryExpression )
            {
                BinaryExpression binaryExpression = (BinaryExpression)expression;
                Expression left = binaryExpression.getLeftExpression();
                Expression right = binaryExpression.getRightExpression();
                if( left instanceof VariableExpression )
                {
                    variable = ( (VariableExpression)left ).getName();
                }
                //            if( right instanceof PropertyExpression )
                //            {
                //                rhs = parsePropertyExpression( (PropertyExpression)right, workflowInfo );
                //            }
                //            else 
                //            {
                rhs = NextFlowFormatter.format( right );
                //            }
            }
            else if (expression instanceof VariableExpression)
            {
                variable = ((VariableExpression)expression).getName();
            }
                
            OutputInfo outputInfo = new OutputInfo();
            outputInfo.setName( variable );
            outputInfo.setExpression( rhs );
            workflowInfo.addOutput( outputInfo );
            return outputInfo;
        }
        return null;
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

    public List<Statement> getStatements(MethodCallExpression workflow)
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
                return getStatements( (MethodCallExpression)first );
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