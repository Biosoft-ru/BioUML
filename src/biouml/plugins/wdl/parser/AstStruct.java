/* Generated By:JJTree: Do not edit this line. AstStruct.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=true,NODE_PREFIX=Ast,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package biouml.plugins.wdl.parser;

public class AstStruct extends AstScope
{
    String name;
    public AstStruct(int id)
    {
        super(id);
    }

    public AstStruct(WDLParser p, int id)
    {
        super(p, id);
    }

    @Override
    public String toString()
    {
        return "struct";
    }

    public void setStructName(String name)
    {
        this.name = name;
    }

    public String getStructName()
    {
        return name;
    }
}
/* JavaCC - OriginalChecksum=1985871cbf3c8201581272ba43e7aa2a (do not edit this line) */
