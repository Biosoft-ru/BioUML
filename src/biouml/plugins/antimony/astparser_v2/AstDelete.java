/* Generated By:JJTree: Do not edit this line. AstDelete.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=true,NODE_PREFIX=Ast,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package biouml.plugins.antimony.astparser_v2;

public class AstDelete extends SimpleNode
{
    public AstDelete(int id)
    {
        super(id);
    }

    public AstDelete(AntimonyNotationParser p, int id)
    {
        super(p, id);
    }

    public AstSymbol getAstSymbol()
    {
        Node astNode = this.jjtGetChild(0);
        if( astNode != null && astNode instanceof AstSymbol )
            return (AstSymbol)astNode;
        return null;
    }

    @Override
    public String toString()
    {
        return "delete";
    }
}
/* JavaCC - OriginalChecksum=1b7640266ce0be4a63b3ff767fec1d8e (do not edit this line) */