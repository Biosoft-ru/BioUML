/* Generated By:JJTree: Do not edit this line. AstStoichiometry.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=true,NODE_PREFIX=Ast,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package biouml.plugins.antimony.astparser;

public class AstStoichiometry extends SimpleNode
{
    public AstStoichiometry(int id)
    {
        super(id);
    }

    public AstStoichiometry(AntimonyParser p, int id)
    {
        super(p, id);
    }

    private String stoichiometry = "1";

    public String getStoichiometry()
    {
        return stoichiometry;
    }

    public void setStoichiometry(String stoiciometry)
    {
        this.stoichiometry = stoiciometry;
    }

    public String toString()
    {
        String num = stoichiometry;
        if( stoichiometry.equals("1") )
            num = "";
        return num;
    }
}
/* JavaCC - OriginalChecksum=3f2f7f0c7e5ecb20860ad27f6bd6df73 (do not edit this line) */
