/* Generated By:JJTree: Do not edit this line. AstImport.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=true,NODE_PREFIX=Ast,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package biouml.plugins.antimony.astparser;

public class AstImport extends SimpleNode
{
    public AstImport(int id)
    {
        super(id);
    }

    public AstImport(AntimonyParser p, int id)
    {
        super(p, id);
    }


    public String getPath()
    {
        for( int i = 0; i < this.jjtGetNumChildren(); i++ )
            if( this.jjtGetChild(i) instanceof AstText )
                return ( ( (AstText)this.jjtGetChild(i) ).getText() ).replaceAll("\"", "");
        return null;
    }

    public String toString()
    {
        return "import";
    }

    public void setPath(String path)
    {
        for( int i = 0; i < this.jjtGetNumChildren(); i++ )
            if( this.jjtGetChild(i) instanceof AstText )
                ( (AstText)this.jjtGetChild(i) ).setText("\"" + path + "\"");
    }
}
/* JavaCC - OriginalChecksum=b3fdfd5f2c4253f71140d1206d054806 (do not edit this line) */