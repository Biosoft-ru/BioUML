package biouml.plugins.wdl.parser.validator;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.wdl.parser.AstAlias;
import biouml.plugins.wdl.parser.AstAs;
import biouml.plugins.wdl.parser.AstBashString;
import biouml.plugins.wdl.parser.AstCall;
import biouml.plugins.wdl.parser.AstCommand;
import biouml.plugins.wdl.parser.AstConditional;
import biouml.plugins.wdl.parser.AstDeclaration;
import biouml.plugins.wdl.parser.AstExpression;
import biouml.plugins.wdl.parser.AstHints;
import biouml.plugins.wdl.parser.AstImport;
import biouml.plugins.wdl.parser.AstInput;
import biouml.plugins.wdl.parser.AstOutput;
import biouml.plugins.wdl.parser.AstRuntime;
import biouml.plugins.wdl.parser.AstScatter;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.AstStruct;
import biouml.plugins.wdl.parser.AstSymbol;
import biouml.plugins.wdl.parser.AstTask;
import biouml.plugins.wdl.parser.AstText;
import biouml.plugins.wdl.parser.AstWorkflow;
import biouml.plugins.wdl.parser.Node;
import biouml.plugins.wdl.parser.SimpleNode;
import biouml.plugins.wdl.parser.Token;


public class TypeChecker
{
    protected Logger log = Logger.getLogger(TypeChecker.class.getName());
    private VersionValidator validator;
    private DocumentPrototype doc;

    public TypeChecker(String name)
    {
        this("1.0", name);
    }

    public TypeChecker(String version, String name)
    {
        try
        {
            validator = VersionValidator.getValidator(version);
            doc = new DocumentPrototype(name, version);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    public void check(AstStart astStart)
    {
        for( int i = 0; i < astStart.jjtGetNumChildren(); i++ )
        {
            try
            {
                addTopElement(astStart.jjtGetChild(i));
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, t.getMessage());
            }
        }

    }

    private void addTopElement(Node currentNode) throws Exception
    {
        if( currentNode instanceof AstImport )
        {
            addImport((AstImport)currentNode);
        }
        else if( currentNode instanceof AstWorkflow )
        {
            addWorkflow((AstWorkflow)currentNode);
        }
        else if( currentNode instanceof AstTask )
        {
            addTask((AstTask)currentNode);
        }
        else if( currentNode instanceof AstStruct )
        {
            addStruct((AstStruct)currentNode);
        }

    }

    private void addHints(AstRuntime currentNode)
    {
        // TODO Auto-generated method stub

    }

    private void addRuntime(AstRuntime currentNode) throws Exception
    {

        validator.checkRuntimeAttributes(null, null);

    }

    private void addStruct(AstStruct currentNode) throws Exception
    {
        Scope struct = new Scope(currentNode.getStructName(), doc);

        for( Node child : currentNode.getChildren() )
        {
            if( child instanceof AstDeclaration )
            {
                AstDeclaration decl = (AstDeclaration)child;
                struct.addField(new Field(decl.getName(), decl.getType(), struct));
            }
        }

        doc.addStruct(struct);

    }

    private void addConditional(AstConditional currentNode)
    {
        // TODO Auto-generated method stub

    }

    private void addScatter(AstScatter currentNode)
    {
        // TODO Auto-generated method stub

    }

    private void addTask(AstTask tsk) throws Exception
    {
        TaskPrototype tskPrototype = new TaskPrototype(tsk.getName());
        for( Node child : tsk.getChildren() )
        {
            if( child instanceof AstCommand )
            {
                addCommand((AstCommand)child, tskPrototype);
            }
            else if( child instanceof AstRuntime )
            {
                addRuntime((AstRuntime)child);
            }
            else if( child instanceof AstHints )
            {
                addHints((AstRuntime)child);
            }
            else if( child instanceof AstDeclaration )
            {
                AstDeclaration decl = (AstDeclaration)child;
                tskPrototype.addField(new Field(decl.getName(), decl.getType(), tskPrototype));
            }
            else if( child instanceof AstInput )
            {
                addInputs((AstInput)child, tskPrototype);
            }
            else if( child instanceof AstOutput )
            {
                addOutputs((AstOutput)child, tskPrototype);
            }
        }
        doc.addTask(tskPrototype);

    }

    private void addCommand(AstCommand child, TaskPrototype tsk)
    {
        if( child.getBashString() != null )
        {
            AstBashString str = child.getBashString();
            tsk.setCommand(generateText(str, true).substring(2).replaceFirst("^\\s*", "")); // remove first new line
        }
    }

    private void addWorkflow(AstWorkflow wf) throws Exception
    {
        WorkflowPrototype wfPrototype = new WorkflowPrototype(wf.getName());
        for( Node child : wf.getChildren() )
        {
            if( child instanceof AstCall )
            {
                addCall((AstCall)child, wfPrototype);
            }
            else if( child instanceof AstScatter )
            {
                addScatter((AstScatter)child);
            }
            else if( child instanceof AstConditional )
            {
                addConditional((AstConditional)child);
            }
            else if( child instanceof AstRuntime )
            {
                addRuntime((AstRuntime)child);
            }
            else if( child instanceof AstHints )
            {
                addHints((AstRuntime)child);
            }
            else if( child instanceof AstDeclaration )
            {
                AstDeclaration decl = (AstDeclaration)child;
                wfPrototype.addField(new Field(decl.getName(), decl.getType(), wfPrototype));
            }
            else if( child instanceof AstInput )
            {
                addInputs((AstInput)child, wfPrototype);
            }
            else if( child instanceof AstOutput )
            {
                addOutputs((AstOutput)child, wfPrototype);
            }
        }
        doc.setWorkflow(wfPrototype);

    }

    private void addCall(AstCall call, WorkflowPrototype wfPrototype)
    {
        CallPrototype callPrototype = new CallPrototype(call.getName(), wfPrototype);
        wfPrototype.addCall(callPrototype);
    }

    private void addOutputs(AstOutput output, TaskPrototype tsk) throws Exception
    {
        for( Node child : output.getChildren() )
        {
            if( child instanceof AstDeclaration )
            {
                AstDeclaration decl = (AstDeclaration)child;
                Field field = new Field(decl.getName(), decl.getType(), tsk);
                AstExpression expr = decl.getExpression();

                if( expr != null )
                    field.setValue(generateText(expr, false));
                tsk.addOutput(field);
            }
        }

    }

    private void addInputs(AstInput child, TaskPrototype wfPrototype)
    {
        // TODO Auto-generated method stub

    }

    private void addImport(AstImport astNode) throws Exception
    {
        ImportPrototype imp = null;

        for( Node child : astNode.getChildren() )
        {
            if( child instanceof AstText )
                imp = new ImportPrototype( ( (AstText)child ).getText());
            else if( imp != null )
            {
                if( child instanceof AstAs )
                    imp.setAliasName( ( (AstAs)child ).getAlias());
                else if( child instanceof AstAlias )
                {
                    Field impField = null;
                    for( Node n : ( (AstAlias)child ).getChildren() )
                    {
                        if( n instanceof AstSymbol )
                        {
                            impField = new Field( ( (AstSymbol)n ).getName(), imp);
                        }
                        else if( n instanceof AstAs && impField != null )
                        {
                            impField.setAlias( ( (AstAs)n ).getAlias());
                            imp.addField(impField);
                        }
                    }
                }
            }
        }

        if( imp != null )
            doc.addImport(imp);


    }

    public DocumentPrototype getPrototype(AstStart astStart)
    {
        check(astStart);
        return doc;
    }

    private String generateText(SimpleNode node, boolean specialTokensNeeded)
    {
        StringBuilder sb = new StringBuilder();
        addElement(node, sb, specialTokensNeeded);
        return sb.toString();
    }

    private void addElement(Node currentNode, StringBuilder sb, boolean specialTokensNeeded)
    {
        if( specialTokensNeeded )
        {
            biouml.plugins.wdl.parser.Token firstToken = ( (SimpleNode)currentNode ).jjtGetFirstToken();
            if( !currentNode.toString().isEmpty() )
            {
                if( firstToken != null && firstToken.specialToken != null )
                    sb.append(getSpecialTokens(firstToken.specialToken));
            }
        }

        sb.append(currentNode.toString());

        for( int i = 0; i < currentNode.jjtGetNumChildren(); i++ )
        {
            try
            {
                addElement(currentNode.jjtGetChild(i), sb, specialTokensNeeded);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can't add element(" + currentNode.jjtGetChild(i) + "): " + t);
            }
        }
    }

    private String getSpecialTokens(Token token)
    {
        StringBuilder result = new StringBuilder("");
        if( token != null )
        {
            result.append(getSpecialTokens(token.specialToken));
            result.append(token);
        }
        return result.toString();
    }

}