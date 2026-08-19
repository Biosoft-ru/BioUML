package biouml.plugins.wdl.parser;

public class ParserUtil
{
    public static void replaceChild(SimpleNode parent, int index, Node newChild)
    {
        if( newChild == null )
        {
            throw new IllegalArgumentException( "New child must not be null" );
        }

        if( parent.children == null || index < 0 || index >= parent.children.length )
        {
            throw new IndexOutOfBoundsException( "Child index: " + index + ", number of children: " + parent.jjtGetNumChildren() );
        }

        parent.children[index] = newChild;
        newChild.jjtSetParent( parent );
    }

    public static void replaceChild(SimpleNode parent, Node oldChild, Node newChild)
    {
        if( parent == null )
        {
            throw new IllegalArgumentException( "Parent must not be null" );
        }

        if( oldChild == null )
        {
            throw new IllegalArgumentException( "Old child must not be null" );
        }

        if( newChild == null )
        {
            throw new IllegalArgumentException( "New child must not be null" );
        }

        if( parent.children == null )
        {
            throw new IllegalArgumentException( "Parent has no children" );
        }

        for( int i = 0; i < parent.children.length; i++ )
        {
            if( parent.children[i] == oldChild )
            {
                parent.children[i] = newChild;
                newChild.jjtSetParent( parent );

                /*
                 * Detach the old node only if it still points to this parent.
                 */
                if( oldChild.jjtGetParent() == parent )
                {
                    oldChild.jjtSetParent( null );
                }

                return;
            }
        }

        throw new IllegalArgumentException( "The specified node is not a child of the given parent" );
    }
}
