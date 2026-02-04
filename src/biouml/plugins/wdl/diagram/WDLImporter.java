package biouml.plugins.wdl.diagram;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.plugins.wdl.model.CallInfo;
import biouml.plugins.wdl.model.CommandInfo;
import biouml.plugins.wdl.model.ConditionalInfo;
import biouml.plugins.wdl.model.ExpressionInfo;
import biouml.plugins.wdl.model.ImportInfo;
import biouml.plugins.wdl.model.InputInfo;
import biouml.plugins.wdl.model.MetaInfo;
import biouml.plugins.wdl.model.OutputInfo;
import biouml.plugins.wdl.model.ScatterInfo;
import biouml.plugins.wdl.model.ScriptInfo;
import biouml.plugins.wdl.model.StructInfo;
import biouml.plugins.wdl.model.TaskInfo;
import biouml.plugins.wdl.model.WorkflowInfo;
import biouml.plugins.wdl.parser.AstCall;
import biouml.plugins.wdl.parser.AstConditional;
import biouml.plugins.wdl.parser.AstDeclaration;
import biouml.plugins.wdl.parser.AstExpression;
import biouml.plugins.wdl.parser.AstImport;
import biouml.plugins.wdl.parser.AstInput;
import biouml.plugins.wdl.parser.AstMeta;
import biouml.plugins.wdl.parser.AstOutput;
import biouml.plugins.wdl.parser.AstScatter;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.AstStruct;
import biouml.plugins.wdl.parser.AstSymbol;
import biouml.plugins.wdl.parser.AstTask;
import biouml.plugins.wdl.parser.AstWorkflow;
import biouml.plugins.wdl.parser.WDLParser;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.bean.BeanInfoEx2;

public class WDLImporter implements DataElementImporter
{
    private WDLImportProperties properties = null;
    protected static final Logger log = Logger.getLogger(WDLImporter.class.getName());

    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if( parent.isAcceptable(Diagram.class) )
            if( file == null )
                return ACCEPT_HIGHEST_PRIORITY;
            else
            {
                String lcname = file.getName().toLowerCase();
                if( lcname.endsWith(".wdl") )
                    return ACCEPT_HIGHEST_PRIORITY;
            }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(DataCollection<?> parent, File file, String diagramName, FunctionJobControl jobControl, Logger log)
            throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();

        try (FileInputStream in = new FileInputStream(file); InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            if( properties.getDiagramName() == null || properties.getDiagramName().isEmpty() )
                throw new Exception("Please specify diagram name.");

            String text = ApplicationUtils.readAsString(file);
            text = text.replace("<<<", "{").replace(">>>", "}");//TODO: fix parsing <<< >>>

            AstStart start = new WDLParser().parse(new StringReader(text));
            ScriptInfo scriptInfo = createScriptInfo(start);
            
            DiagramGenerator generator = new DiagramGenerator();
            Diagram diagram = generator.generateDiagram(scriptInfo, parent, properties.getDiagramName());

            if( jobControl != null )
                jobControl.functionFinished();

            new WDLLayouter().layout(diagram);
            CollectionFactoryUtils.save(diagram);

            return diagram;
        }
        catch( Exception e )
        {
            if( jobControl != null )
                jobControl.functionTerminatedByError(e);
            throw e;
        }
    }
    
    public Diagram generateDiagram(File file, String name, DataCollection parent) throws Exception
    {
        String text = ApplicationUtils.readAsString(file);
        text = text.replace("<<<", "{").replace(">>>", "}");//TODO: fix parsing <<< >>>
        AstStart start = new WDLParser().parse(new StringReader(text));
        ScriptInfo scriptInfo = createScriptInfo(start);
        Diagram diagram = new WDLDiagramType().createDiagram( parent, name, null );
        return new DiagramGenerator().generateDiagram( scriptInfo, diagram );
    }
    
    public Diagram generateDiagram(AstStart start, Diagram diagram) throws Exception
    {
        ScriptInfo scriptInfo = createScriptInfo(start);
        return new DiagramGenerator().generateDiagram( scriptInfo, diagram );
    }
    
    public Diagram generateDiagram(AstStart start, DataCollection dc, String name) throws Exception
    {
        ScriptInfo scriptInfo = createScriptInfo(start);
        Diagram result = new WDLDiagramType().createDiagram( dc, name, null );
        return new DiagramGenerator().generateDiagram( scriptInfo, result );
    }


    @Override
    public Object getProperties(DataCollection<?> parent, File file, String elementName)
    {
        if( properties == null )
            properties = new WDLImportProperties();
        if( elementName != null )
            properties.setDiagramName(elementName);
        else if( file != null )
            properties.setDiagramName(file.getName());
        return properties;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return Diagram.class;
    }

    @Override
    public boolean init(Properties arg0)
    {
        return true;
    }

    public static class WDLImportProperties extends Option
    {
        private String diagramName;

        @PropertyName ( "Diagram name" )
        public String getDiagramName()
        {
            return diagramName;
        }
        public void setDiagramName(String diagramName)
        {
            this.diagramName = diagramName;
        }
    }

    public static class WDLImportPropertiesBeanInfo extends BeanInfoEx2<WDLImportProperties>
    {
        public WDLImportPropertiesBeanInfo()
        {
            super(WDLImportProperties.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            add("diagramName");
        }
    }
    
    private static WorkflowInfo createWorkflowInfo(AstWorkflow astWorkflow)
    {
        WorkflowInfo result = new WorkflowInfo(astWorkflow.getName());
        for( biouml.plugins.wdl.parser.Node child : astWorkflow.getChildren() )
        {
            if( child instanceof AstInput )
            {
                for( AstDeclaration astD : StreamEx.of( ( (AstInput)child ).getChildren()).select(AstDeclaration.class) )
                    result.addInput(createInputInfo(astD));
            }
            else if( child instanceof AstOutput )
            {
                for( AstDeclaration astD : StreamEx.of( ( (AstOutput)child ).getChildren()).select(AstDeclaration.class) )
                    result.addOutput(createOutputInfo(astD));
            }
            else if( child instanceof AstDeclaration )
            {
                result.addObject(createExpressionInfo((AstDeclaration)child));
            }
            else if( child instanceof AstMeta )
            {
                result.setMeta(createMetaInfo(WDLConstants.META_ATTR, (AstMeta)child));
            }
            else if( child instanceof AstCall )
            {
                result.addObject(createCallInfo((AstCall)child));
            }
            else if( child instanceof AstScatter )
            {
                result.addObject(createScatterInfo((AstScatter)child));
            }
            else if( child instanceof AstConditional )
            {
                result.addObject(createConditionalInfo((AstConditional)child));
            }
        }
        return result;
    }

    private static CallInfo createCallInfo(AstCall astCall)
    {
        CallInfo callInfo = new CallInfo();
        callInfo.setTaskName(astCall.getName());
        callInfo.setAlias(astCall.getAlias() == null ? astCall.getName() : astCall.getAlias());

        for( AstSymbol symbol : astCall.getInputs() )
        {
            String expression = symbol.getName();
            AstExpression expr = null;
            if( symbol.getChildren() != null )
            {
                List<AstExpression> exprs = WorkflowUtil.findChild(symbol, AstExpression.class);
                if( !exprs.isEmpty() )
                {
                    expr = exprs.get(0);
                    expression = expr.toString();
                }
            }
            InputInfo input = new InputInfo(null, symbol.getName(), expression);
            
            callInfo.addInputInfo(input);
        }
        return callInfo;
    }

    private static TaskInfo createTaskInfo(AstTask astTask)
    {
        TaskInfo taskInfo = new TaskInfo(astTask.getName());
        for( AstDeclaration astDeclaration : astTask.getBeforeCommand() )
        {
            taskInfo.addBeforeCommand(createExpressionInfo(astDeclaration));
        }
        astTask.getRuntime().entrySet().stream().forEach(e -> taskInfo.setRuntime(e.getKey(), e.getValue()));
        taskInfo.setCommand(new CommandInfo(astTask.getCommand()));
        for( AstDeclaration astDeclaration : astTask.getInput().getDeclarations() )
        {
            taskInfo.addInputInfo(createExpressionInfo(astDeclaration));
        }
        for( AstDeclaration astDeclaration : astTask.getOutput().getDeclarations() )
        {
            taskInfo.addOutputInfo(createExpressionInfo(astDeclaration));
        }
        return taskInfo;
    }

    private static ExpressionInfo createExpressionInfo(AstDeclaration astDeclaration)
    {
        ExpressionInfo expressionInfo = new ExpressionInfo();
        if( astDeclaration.getExpression() != null )
            expressionInfo.setExpression(astDeclaration.getExpression().toString());
        expressionInfo.setName(astDeclaration.getName());
        expressionInfo.setType(astDeclaration.getType());
        return expressionInfo;
    }

    private static InputInfo createInputInfo(AstDeclaration astDeclaration)
    {
        InputInfo inputInfo = new InputInfo();
        if( astDeclaration.getExpression() != null )
            inputInfo.setExpression(astDeclaration.getExpression().toString());
        inputInfo.setName(astDeclaration.getName());
        inputInfo.setType(astDeclaration.getType());
        return inputInfo;
    }

    private static OutputInfo createOutputInfo(AstDeclaration astDeclaration)
    {
        OutputInfo outputInfo = new OutputInfo();
        if( astDeclaration.getExpression() != null )
            outputInfo.setExpression(astDeclaration.getExpression().toString());
        outputInfo.setName(astDeclaration.getName());
        outputInfo.setType(astDeclaration.getType());
        return outputInfo;
    }

    private static ImportInfo createImport(AstImport ast)
    {
        return new ImportInfo(ast.getAlias(), ast.getSource());
    }

    private static MetaInfo createMetaInfo(String name, AstMeta ast)
    {
        MetaInfo meta = new MetaInfo();
        meta.setName(name);
        for( Entry<String, String> entry : meta.getValues().entrySet() )
        {
            meta.setProperty(entry.getKey(), entry.getValue());
        }
        return meta;
    }

    private static ScriptInfo createScriptInfo(AstStart start)
    {
        ScriptInfo scriptInfo = new ScriptInfo();

        for( int i = 0; i < start.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.wdl.parser.Node n = start.jjtGetChild(i);
            if( n instanceof AstWorkflow )
            {
                AstWorkflow astWorkflow = (AstWorkflow)n;
                WorkflowInfo workflowInfo = createWorkflowInfo(astWorkflow);
                scriptInfo.addWorkflow(workflowInfo);
            }
            else if( n instanceof AstTask )
            {
                AstTask task = (AstTask)n;
                TaskInfo taskInfo = createTaskInfo(task);
                scriptInfo.addTask(taskInfo);
            }
            else if( n instanceof AstImport )
            {
                AstImport astImport = (AstImport)n;
                ImportInfo importInfo = createImport(astImport);
                scriptInfo.addImport(importInfo);
            }
            else if( n instanceof AstStruct )
            {
                AstStruct astStruct = (AstStruct)n;
                StructInfo structInfo = createStruct(astStruct);
                scriptInfo.addStruct(structInfo);
            }
        }
        return scriptInfo;
    }

    private static ConditionalInfo createConditionalInfo(AstConditional astConditional)
    {
        ConditionalInfo result = new ConditionalInfo();
        for( biouml.plugins.wdl.parser.Node n : astConditional.getChildren() )
        {
            if( n instanceof AstExpression )
            {
                result.setExpression(n.toString());
            }
        }
        for( biouml.plugins.wdl.parser.Node n : astConditional.getChildren() )
        {
            if( n instanceof AstDeclaration )
            {
                result.addObject(createExpressionInfo((AstDeclaration)n));
            }
            else if( n instanceof AstCall )
            {
                result.addObject(createCallInfo((AstCall)n));
            }
            else if( n instanceof AstScatter )
            {
                result.addObject(createScatterInfo((AstScatter)n));
            }
            else if( n instanceof AstConditional )
            {
                result.addObject(createConditionalInfo((AstConditional)n));
            }
        }
        return result;
    }

    private static ScatterInfo createScatterInfo(AstScatter astScatter)
    {
        ScatterInfo result = new ScatterInfo();
        String variable = astScatter.getVarible();
        AstExpression array = astScatter.getArrayExpression();
        result.setVariable(variable);
        result.setExpression(array.toString());
      
        for( biouml.plugins.wdl.parser.Node n : astScatter.getChildren() )
        {
            if( n instanceof AstDeclaration )
            {
                result.addObject(createExpressionInfo((AstDeclaration)n));
            }
            else if( n instanceof AstCall )
            {
                result.addObject(createCallInfo((AstCall)n));
            }
            else if( n instanceof AstScatter )
            {
                result.addObject(createScatterInfo((AstScatter)n));
            }
            else if( n instanceof AstConditional )
            {
                result.addObject(createConditionalInfo((AstConditional)n));
            }
        }
        return result;
    }

    private static StructInfo createStruct(AstStruct struct)
    {
        StructInfo structInfo = new StructInfo();
        structInfo.setName(struct.getStructName());
        for( AstDeclaration declaration : struct.getDeclarations() )
            structInfo.addExpressions(createExpressionInfo(declaration));
        return structInfo;
    }
}