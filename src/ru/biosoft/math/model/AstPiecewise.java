/* Generated By:JJTree: Do not edit this line. AstVarNode.java */
package ru.biosoft.math.model;

public class AstPiecewise extends SimpleNode
{
    public AstPiecewise(int id)
    {
        super(id);
    }

    /** Creates a string containing the variable's name and value */
    @Override
    public String toString()
    {
        return "Piecewise: " + jjtGetNumChildren();
    }

    @Override
    public Node cloneAST()
    {
        return new AstPiecewise(id);
    }
    
    @Override
    public boolean equals(Object node)
    {
        return node instanceof AstPiecewise && ((AstPiecewise)node).id == id;
    }
}