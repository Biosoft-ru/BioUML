/* Generated By:JJTree: Do not edit this line. AstListOfValues.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=true,NODE_PREFIX=Ast,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package biouml.plugins.antimony.astparser_v2;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.antimony.AntimonyAstCreator;

public class AstList extends SimpleNode
{
    public AstList(int id)
    {
        super(id);
    }

    public AstList(AntimonyNotationParser p, int id)
    {
        super(p, id);
    }

    void setValue(List<String> values)
    {
        AstRegularFormulaElement lb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
        lb.setElement("[");
        addAsLast(lb);
        for( int i = 0; i < values.size(); i++ )
        {
            AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
            symbol.setName(values.get(i));
            AntimonyAstCreator.createSpace(symbol);
            addAsLast(symbol);

            if( i < values.size() - 1 )
                addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
        }
        AstRegularFormulaElement rb = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
        rb.setElement("]");
        addAsLast(rb);
    }

    public List<String> getValue()
    {
        List<String> symbolsName = new ArrayList<String>();
        for( int i = 0; i < this.jjtGetNumChildren(); i++ )
            if( this.jjtGetChild(i) instanceof AstSymbol )
                symbolsName.add( ( (AstSymbol)this.jjtGetChild(i) ).getName());

        return symbolsName;
    }

    @Override
    public String toAntimonyString()
    {
        return "";
    }

}
/* JavaCC - OriginalChecksum=01b46c7bdc1c80d09690450d0e49be24 (do not edit this line) */