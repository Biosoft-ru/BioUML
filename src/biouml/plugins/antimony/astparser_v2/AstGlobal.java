/* Generated By:JJTree: Do not edit this line. AstGlobal.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=true,NODE_PREFIX=Ast,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package biouml.plugins.antimony.astparser_v2;

public class AstGlobal extends SimpleNode
{
    public AstGlobal(int id)
    {
        super(id);
    }

    public AstGlobal(AntimonyNotationParser p, int id)
    {
        super(p, id);
    }


    String format;
    public void setFormat(String format)
    {
        this.format = format;
    }

    @Override
    public String toString()
    {
        return format;
    }
}
/* JavaCC - OriginalChecksum=e166cf6cd085427715fe0b08dd2c0f29 (do not edit this line) */
