package ru.biosoft.math.model;

public class HTMLLinearFormatter extends LinearFormatter
{
    private VariableResolver variableResolver;

    public String[] format(AstStart start, VariableResolver resolver, boolean isMinus)
    {
        result = new StringBuilder();
        if( start != null )
        {
            this.variableResolver = resolver;

            if( isMinus )
                changeOperations(start);

            Utils.children( start ).forEach( this::processNode );
        }

        String resultFormula = String.valueOf(result);

        if( isMinus )
        {
            if( resultFormula.startsWith("&minus;") )
                resultFormula = resultFormula.substring(7);
            else
                resultFormula = "&minus; " + resultFormula;
        }

        return new String[] {"", resultFormula};
    }

    protected void changeOperations(SimpleNode start)
    {
        int n = start.jjtGetNumChildren();
        for( int i = 0; i < n; i++ )
        {
            Node node = start.jjtGetChild(i);
            if( node instanceof AstFunNode && !needParenthis((AstFunNode)node) )
            {
                Function function = ( (AstFunNode)node ).getFunction();
                String name = getFunctionName(function.getName());

                if( name.equals("-") )
                {
                    PredefinedFunction newFunction = new PredefinedFunction("+", function.getPriority(), function.getNumberOfParameters());
                    ( (AstFunNode)node ).setFunction(newFunction);
                }
                if( name.equals("+") )
                {
                    PredefinedFunction newFunction = new PredefinedFunction("-", function.getPriority(), function.getNumberOfParameters());
                    ( (AstFunNode)node ).setFunction(newFunction);
                }
                if( name.equals("u-") )
                {
                    PredefinedFunction newFunction = new PredefinedFunction("u+", function.getPriority(), function.getNumberOfParameters());
                    ( (AstFunNode)node ).setFunction(newFunction);
                }

                changeOperations((SimpleNode)node);
            }
        }
    }

    @Override
    protected void processVariable(AstVarNode node)
    {
        String name = node.getName();
        String title = variableResolver.resolveVariable(name);
        if( title == null )
            title = name;

        if( title.indexOf("$") > -1 )
            title = "[" + title + "]";

        title = title.replace("$", "");
        title = title.replace("\"", "");
        title = title.replace("<br>", "");
//        String nodeTitle = node.getTitle();
//        nodeTitle = nodeTitle.replace(name, title);
        result.append("<nobr>" + title + "</nobr>");
    }

    @Override
    protected void processFunction(AstFunNode node)
    {
        Function function = node.getFunction();
        String name = getFunctionName(function.getName());
        if( name.startsWith("\"") )
            name = name.substring(1, name.length() - 1);

        AstFunNode parent = null;
        Function parentFunction = null;
        String parentName = "";
        if( node.jjtGetParent() instanceof AstFunNode )
        {
            parent = (AstFunNode)node.jjtGetParent();
            parentFunction = parent.getFunction();
            parentName = getFunctionName(parentFunction.getName());
        }

        boolean parenthis = needParenthis(node);
        if( parentName != null && parentName.equals("^") )
        {
            if( parent.jjtGetChild(1).equals(node) )
            {
                if( parenthis )
                    result.append("<sup>");
            }
            else
                result.append("<nobr>(");
        }
        else
        {
            if( parenthis )
                result.append("(");
        }

        if( function.getPriority() == Function.FUNCTION_PRIORITY )
        {
            result.append(name);
            result.append("(");

            int n = node.jjtGetNumChildren();
            for( int i = 0; i < n; i++ )
            {
                processNode(node.jjtGetChild(i));
                if( i < n - 1 )
                    result.append(", ");
            }

            result.append(")");
            return;
        }

        // unary operators
        if( function.getNumberOfParameters() == 1 )
        {
            if( name.equals("u-") )
                name = "&minus;";

            if( name.equals("u+") )
                name = "+";

            result.append(name + " ");
            processNode(node.jjtGetChild(0));
        }
        else
        {
            if( name.equals("-") )
                name = "&minus;";

            if( name.equals("&&") )
                name = "<u>and</u>";

            if( name.equals("||") )
                name = "<u>or</u>";

            if( name.equals("!") )
                name = "<u>not</u>";

            if( name.equals("^") && ( node.jjtGetChild(1) instanceof AstVarNode ) )
            {
                String oldTitle = ( (AstVarNode)node.jjtGetChild(1) ).getName();
                String newTitle = "<sup>" + oldTitle + "</sup></nobr>";
                ( (AstVarNode)node.jjtGetChild(1) ).setTitle(newTitle);
            }

            if( name.equals("^") && ( node.jjtGetChild(1) instanceof AstConstant ) )
            {
                String oldName = ( (AstConstant)node.jjtGetChild(1) ).getName();
                String newName;
                if( oldName != null )
                {
                    newName = "<sup>" + oldName + "</sup></nobr>";
                }
                else
                {
                    Object value = ( (AstConstant)node.jjtGetChild(1) ).getValue();
                    newName = "<sup>" + value + "</sup></nobr>";
                }
                ( (AstConstant)node.jjtGetChild(1) ).setName(newName);
            }

            if( name.equals("==") )
                name = "=";

            int n = node.jjtGetNumChildren();
            for( int i = 0; i < n; i++ )
            {
                processNode(node.jjtGetChild(i));

                if( i < n - 1 && !name.equals("^") )
                {
                    result.append(" " + name + " ");
                }
            }
        }

        if( parentName != null && parentName.equals("^") && parent.jjtGetChild(1).equals(node) )
        {
            if( parenthis )
                result.append("</sup></nobr>");
        }
        else
        {
            if( parenthis )
                result.append(")");
        }
    }
}
