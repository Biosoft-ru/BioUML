package ru.biosoft.math.model;

public class AstVarNode extends SimpleNode
{
    public AstVarNode(int id)
    {
        super(id);
    }

    private String varName;

    /** Returns the name of the variable. */
    public String getName()
    {
        return varName;
    }

    /** Sets the name of the variable. */
    public void setName(String varName_in)
    {
        varName = varName_in;
    }

    protected boolean cSymbol = false;
    public boolean isCSymbol()
    {
        return cSymbol;
    }
    public void setIsCSymbol(boolean cSymbol)
    {
        this.cSymbol = cSymbol;
    }

    protected String title;
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    protected String definitionUrl;
    public String getDefinitionUrl()
    {
        return definitionUrl;
    }
    public void setDefinitionUrl(String definitionUrl)
    {
        this.definitionUrl = definitionUrl;
    }

    /**
     * Creates a string containing the variable's name and value
     */
    @Override
    public String toString()
    {
        return "Variable: \"" + getName() + "\"";
    }

    @Override
    public Node cloneAST()
    {
        AstVarNode node = new AstVarNode(id);
        node.title = title;
        node.cSymbol = cSymbol;
        node.varName = varName;
        node.definitionUrl = definitionUrl;
        return node;
    }
   
    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        final AstVarNode other = (AstVarNode)obj;
        if( cSymbol != other.cSymbol )
            return false;
        if( title == null )
        {
            if( other.title != null )
                return false;
        }
        else if( !title.equals(other.title) )
            return false;
        if( varName == null )
        {
            if( other.varName != null )
                return false;
        }
        else if( !varName.equals(other.varName) )
            return false;
        return true;
    }
}
