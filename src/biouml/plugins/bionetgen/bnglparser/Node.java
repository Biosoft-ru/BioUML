/* Generated By:JJTree: Do not edit this line. Node.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=true,NODE_PREFIX=BNG,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package biouml.plugins.bionetgen.bnglparser;

/* All AST nodes must implement this interface.  It provides basic
   machinery for constructing the parent and child relationships
   between nodes. */

public interface Node
{
    public String getName();
    
    public boolean isHighlight();

    /**
     * Creates string representation of AST node with all special tokens.
     * @return string representation of AST node with special tokens.
     */
    public String toBNGString();

    /**
     * Returns string representation of AST node removing all special tokens.
     * @return name of the ASTNode without spaces.
     */
    public String getFullName();

    /**
     * Removes node from the parent's children array.
     * @return index in parent's children array.
     */
    public int remove();

    /**
     * Returns the index of the first occurrence of the specified node
     * in the children array, or -1 if it does not contain the node.
     * @param node child to be found in the children array.
     * @return the index of the first occurrence of the specified node in
     *         the children array, or -1 if it does not contain the node.
     */
    public int indexOf(Node node);

    /**
     * Inserts the new child at the specified position in the children array.
     * Shifts the child currently at that position (if any)
     * and any subsequent children to the right.
     * @param newChild child to be inserted.
     * @param i index at which the new child is to be inserted.
     */
    public void addChild(Node newChild, int i);

    /**
     * Adds the specified child as last element in the array of the children.
     * @param child child to be inserted.
     */
    public void addAsLast(Node child);

    /**
     * Removes child at the specified position and shifts
     * subsequent children (if any) to the right.
     * @param i index at which the child is to be removed. 
     */
    public void removeChild(int i);

    /** This method is called after the node has been made the current
      node.  It indicates that child nodes can now be added to it. */
    public void jjtOpen();

    /** This method is called after all the child nodes have been
      added. */
    public void jjtClose();

    /** This pair of methods are used to inform the node of its
      parent. */
    public void jjtSetParent(Node n);
    public Node jjtGetParent();

    /** This method tells the node to add its argument to the node's
      list of children.  */
    public void jjtAddChild(Node n, int i);

    /** This method returns a child node.  The children are numbered
       from zero, left to right. */
    public Node jjtGetChild(int i);

    /** Return the number of children the node has. */
    public int jjtGetNumChildren();
}
/* JavaCC - OriginalChecksum=1b2b2a101650b641cdbdbf37931fb8b0 (do not edit this line) */