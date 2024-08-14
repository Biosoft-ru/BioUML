package ru.biosoft.math.model;

/**
 * 
 * @author puz
 * 
 * Can be thrown from from Utils.visitAST to
 * pass some information from inside the tree
 * traverse
 *
 */
public class ASTVisitorException extends Exception
{
    Object context;

    public ASTVisitorException(Object context)
    {
        this.context = context;
    }
}
