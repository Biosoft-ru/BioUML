/* Generated By:JJTree: Do not edit this line. AstVarNode.java */
package ru.biosoft.math.model;

/**
 * The piece element is used to construct the conditionally defined values
 * as part of a piecewise object.
 *
 * It correspond MathML piece and otherwise elements. In last case AstPiece has only one node
 * that specifies the value.
 */
public class AstPiece extends SimpleNode
{
    public AstPiece(int id)
    {
        super(id);
    }

    public Node getCondition()
    {
        // returns first child
        if( jjtGetNumChildren() == 1 )
            return null;
        else
            return jjtGetChild(0);
    }

    public Node getValue()
    {
        // returns last child
        return jjtGetChild(jjtGetNumChildren() - 1);
    }

    @Override
    public String toString()
    {
        if( jjtGetNumChildren() == 1 )
            return "Otherwise";
        else
            return "Piece";
    }

    @Override
    public AstPiece cloneAST()
    {
        return new AstPiece(id);
    }

    @Override
    public boolean equals(Object node)
    {
        return node instanceof AstPiece && ( (AstPiece)node ).id == id;
    }
}