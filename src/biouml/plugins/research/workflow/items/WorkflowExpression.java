package biouml.plugins.research.workflow.items;

import java.awt.Point;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

public class WorkflowExpression extends WorkflowVariable implements InitialElementProperties
{
    public static class CalculationException extends Exception
    {
        private static final long serialVersionUID = 1L;

        public CalculationException(String message, Throwable e)
        {
            super(message+": "+e.getMessage(), e);
        }

        public CalculationException(String message)
        {
            super(message);
        }
    }

    protected static enum LexemeType
    {
        STRING, VARIABLE
    }

    protected static class Lexeme
    {
        LexemeType type;
        String data;
        WorkflowVariable var;

        public Lexeme(LexemeType type, String data)
        {
            super();
            this.type = type;
            this.data = data;
        }

        public Lexeme(LexemeType type, WorkflowVariable var, String data)
        {
            super();
            this.type = type;
            this.var = var;
            this.data = data;
        }

        @Override
        public String toString()
        {
            switch(type)
            {
            case VARIABLE:
                String expr = this.var.getName()+((this.data != null && !this.data.isEmpty())?"/"+this.data:"");
                return "$" + escape(expr) + "$";
            default:
                return escape(this.data);
            }
        }
    }

    public static String escape(String str)
    {
        return str.replace( "\\", "\\\\" ).replace( "$", "\\$" );
    }

    public static final String PARAMETER_EXPRESSION = "parameter-expression";

    protected static final PropertyDescriptor PARAMETER_EXPRESSION_PD = StaticDescriptor.create(PARAMETER_EXPRESSION);

    public WorkflowExpression(Node node, Boolean canSetName)
    {
        super(node, canSetName);
    }

    public void setExpression(String value)
    {
        startTransaction("Set expression");
        getNode().getAttributes().add(new DynamicProperty(PARAMETER_EXPRESSION_PD, String.class, value));
        completeTransaction();
        firePropertyChange("*", null, null);
    }

    public String getExpression()
    {
        Object valueObj = getNode().getAttributes().getValue(PARAMETER_EXPRESSION);
        return valueObj == null ? null : valueObj.toString();
    }

    /**
     * Update references to the specified variable when it's renamed.
     * Does nothing if specified variable is not used in the expression or expression is not valid
     */
    private final Map<String,String> renamedVariables = new HashMap<>();
    public void updateReference(String oldName, String newName)
    {
        Lexeme[] lexemes = null;
        renamedVariables.clear();
        renamedVariables.put(oldName, newName);
        try
        {
            lexemes = getLexemes(getExpression());
        }
        catch( CalculationException e )
        {
        }
        renamedVariables.clear();
        if(lexemes == null) return;
        setExpression(StreamEx.of(lexemes).joining());
    }

    protected WorkflowVariable getVariable(String name) throws CalculationException
    {
        Diagram workflow = Diagram.getDiagram(getNode());
        DataElement de = null;
        try
        {
            DataCollection origin = getNode().getOrigin();
            while(de == null && origin != null)
            {
                de = origin.get(name);
                if(origin == workflow) break;
                origin = origin.getOrigin();
            }
        }
        catch( Exception e )
        {
            throw new CalculationException("Unable to get variable '" + name + "'", e);
        }
        if( de == null || ! ( de instanceof Node ) )
        {
            WorkflowVariable variable = SystemVariable.getVariable(name, workflow);
            if(variable != null) return variable;
            variable = getCustomVariable(name, workflow);
            if(variable != null) return variable;
            throw new CalculationException("Variable '" + name + "' not found");
        }
        WorkflowItem item = WorkflowItemFactory.getWorkflowItem((Node)de);
        if( item == null || ! ( item instanceof WorkflowVariable ) )
            throw new CalculationException("Item '" + name + "' is not a variable");
        return (WorkflowVariable)item;
    }

    protected WorkflowVariable getCustomVariable(String name, Diagram workflow)
    {
        return null;
    }

    protected Lexeme[] getLexemes(String expression) throws CalculationException
    {
        if( expression == null )
            throw new CalculationException( "Expression is empty" );

        List<Lexeme> lexems = new ArrayList<>();
        boolean stringLexem = true;
        StringBuilder curToken = new StringBuilder();
        for( int i = 0; i < expression.length(); i++ )
        {
            char c = expression.charAt( i );
            if( c == '$' )
            {
                Lexeme l = stringLexem ? new Lexeme( LexemeType.STRING, curToken.toString() ) : parseVariableLexeme( curToken.toString() );
                lexems.add( l );
                curToken.setLength( 0 );
                stringLexem = !stringLexem;
                continue;
            }
            if( c == '\\' && i + 1 < expression.length() )
                c = expression.charAt( ++i );
            curToken.append( c );
        }
        if( !stringLexem )
            throw new CalculationException( "Truncated expression: variable should ends with $" );
        lexems.add( new Lexeme( LexemeType.STRING, curToken.toString() ) );

        return lexems.toArray( new Lexeme[lexems.size()] );
    }

    private Lexeme parseVariableLexeme(String str) throws CalculationException
    {
        String varName = str;
        String property = null;
        int slashPos = varName.indexOf( "/" );
        if( slashPos > -1 )
        {
            property = varName.substring( slashPos + 1 );
            varName = varName.substring( 0, slashPos );
        }
        if( renamedVariables.containsKey( varName ) )
            varName = renamedVariables.get( varName );
        return new Lexeme( LexemeType.VARIABLE, getVariable( varName ), property );
    }

    protected String calculateExpression(String expression, WorkflowVariable[] dependentVariables) throws CalculationException
    {
        Lexeme[] lexemes = getLexemes(expression);
        StringBuilder result = new StringBuilder();
        if( dependentVariables == null )
            dependentVariables = new WorkflowVariable[0];
        WorkflowVariable[] newDependentVariables = new WorkflowVariable[dependentVariables.length + 1];
        System.arraycopy(dependentVariables, 0, newDependentVariables, 0, dependentVariables.length);
        for( Lexeme lexeme : lexemes )
        {
            switch( lexeme.type )
            {
                case STRING:
                    result.append(lexeme.data);
                    break;
                case VARIABLE:
                    Object value = null;
                    for( WorkflowVariable var : dependentVariables )
                    {
                        if( var == lexeme.var )
                            throw new CalculationException("Cyclic dependency on variable '" + var.getName() + "'");
                    }
                    if( lexeme.var instanceof WorkflowExpression )
                    {
                        newDependentVariables[newDependentVariables.length - 1] = lexeme.var;
                        try
                        {
                            value = ( (WorkflowExpression)lexeme.var ).getValueDependent(newDependentVariables);
                        }
                        catch(Exception e)
                        {
                            throw new CalculationException("Cannot fetch value of '" + lexeme.var.getName() + "'", e);
                        }
                    }
                    else
                    {
                        try
                        {
                            value = lexeme.var.getValue();
                        }
                        catch( Exception e )
                        {
                            throw new CalculationException("Cannot fetch value of '" + lexeme.var.getName() + "'", e);
                        }
                    }
                    if(value != null && lexeme.data != null)
                    {
                        ComponentModel model = ComponentFactory.getModel(value, Policy.DEFAULT, true);
                        Property property = model.findProperty(lexeme.data);
                        if(property == null)
                            throw new CalculationException("No such property '"+lexeme.data+"' of '"+lexeme.var.getName()+"'");
                        try
                        {
                            value = property.getValue();
                        }
                        catch(Exception e)
                        {
                            throw new CalculationException("Cannot fetch property '"+lexeme.data+"' of '"+lexeme.var.getName()+"'");
                        }
                    }
                    result.append( value );
            }
        }
        return result.toString();
    }

    protected Object getValueDependent(WorkflowVariable[] dependentVariables) throws CalculationException
    {
        return getType().fromString(calculateExpression(getExpression(), dependentVariables));
    }

    @Override
    public Object getValue() throws Exception
    {
        return getValueDependent(new WorkflowVariable[] {this});
    }

    @Override
    public DiagramElementGroup createElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( Diagram.getDiagram(c).getType().getSemanticController().canAccept(c, getNode()) )
        {
            viewPane.add(getNode(), location);
        }
        return new DiagramElementGroup( getNode() );
    }
}
