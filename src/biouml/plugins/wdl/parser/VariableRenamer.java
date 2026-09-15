package biouml.plugins.wdl.parser;

public class VariableRenamer
{
    private String oldName;
    private String newName;

    public String rename(String expression, String oldName, String newName) throws ParseException
    {
        AstExpression astExpression = new ExpressionParser().parseExpression( expression );
        rename( astExpression, oldName, newName );
        return new ExpressionFormatter().format( astExpression );
    }

    public void rename(Node start, String oldName, String newName)
    {
        this.oldName = oldName;
        this.newName = newName;
        if( start != null )
        {
            int n = start.jjtGetNumChildren();
            for( int i = 0; i < n; i++ )
                visit( start.jjtGetChild( i ) );
        }
    }

    protected void visit(Node node)
    {
        if( node instanceof AstSymbol )
        {
            visit( (AstSymbol)node );
        }
        else if (node instanceof AstRegularFormulaElement)
        {
            visit( (AstRegularFormulaElement)node );
        }
        for( int i = 0; i < node.jjtGetNumChildren(); i++ )
        {
            visit( node.jjtGetChild( i ) );
        }
    }

    private void visit(AstSymbol node)
    {
        if( ! ( oldName.equals( node.getName() ) ) )
            return;
        SimpleNode parent = (SimpleNode)node.jjtGetParent();
        int index = ParserUtil.findIndex( parent, node );
        AstSymbol newChild = new AstSymbol( WDLParserTreeConstants.JJTSYMBOL );
        newChild.setName( newName );
        parent.children[index] = newChild;
    }
    
    private void visit(AstRegularFormulaElement node)
    {
        if( !node.isVariable || ! oldName.equals(node.toString()) )
            return;
        SimpleNode parent = (SimpleNode)node.jjtGetParent();
        int index = ParserUtil.findIndex( parent, node );
        AstRegularFormulaElement newChild = new AstRegularFormulaElement( WDLParserTreeConstants.JJTREGULARFORMULAELEMENT );
        newChild.setElement( newName );
        parent.children[index] = newChild;
    }

    
    public static void main(String ... args)
    {
        try
        {
//            String expression = "(ii + i + ii)*kji + i";
//            String expression = "i.k";
            String expression = "calls( x, y)";
            String result = new VariableRenamer().rename( expression, "x", "oo" );
            System.out.println( expression +" -> "+result );

        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
}