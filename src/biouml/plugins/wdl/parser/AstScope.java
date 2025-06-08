package biouml.plugins.wdl.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AstScope extends SimpleNode
{
    private Map<String, AstType> varMap;

    public AstScope(int id)
    {
        super(id);
    }

    public AstScope(WDLParser p, int id)
    {
        super(p, id);
    }

    public void putVariable(String name, AstType type)
    {
        this.varMap.put(name, type);
    }

    public Map<String, String> getVariables()
    {
        Map<String, String> result = new HashMap<>();
        for (int i=0; i<jjtGetNumChildren(); i++)
        {
            if (jjtGetChild( i ) instanceof AstDeclaration)
            {
                AstDeclaration declaration = (AstDeclaration)jjtGetChild( i );
                AstExpression expr = declaration.getExpression();
                if( expr != null )
                    result.put( declaration.getName(), declaration.getType() + "=" + expr.toString() );
                else
                    result.put( declaration.getName(), declaration.getType() );
            }
        }
        return result;
    }
    
    public List<AstDeclaration> getDeclarations()
    {
        List<AstDeclaration> result = new ArrayList<>();
        for (int i=0; i<jjtGetNumChildren(); i++)
        {
            if (jjtGetChild( i ) instanceof AstDeclaration)
            {
                result.add((AstDeclaration)jjtGetChild( i ));
            }
        }
        return result;
    }
}
