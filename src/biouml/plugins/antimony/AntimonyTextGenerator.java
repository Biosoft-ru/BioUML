package biouml.plugins.antimony;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.antimony.astparser_v2.AstEqualZero;
import biouml.plugins.antimony.astparser_v2.AstFunction;
import biouml.plugins.antimony.astparser_v2.AstModel;
import biouml.plugins.antimony.astparser_v2.AstSingleProperty;
import biouml.plugins.antimony.astparser_v2.AstStart;
import biouml.plugins.antimony.astparser_v2.AstStoichiometry;
import biouml.plugins.antimony.astparser_v2.Node;
import biouml.plugins.antimony.astparser_v2.SimpleNode;
import biouml.plugins.antimony.astparser_v2.Token;

public class AntimonyTextGenerator
{
    protected Logger log = Logger.getLogger(AntimonyTextGenerator.class.getName());

    protected AstStart astStart;
    protected StringBuilder sb = new StringBuilder();

    public AntimonyTextGenerator(AstStart astStart)
    {
        this.astStart = astStart;
    }

    public String generateText()
    {
        addElement(astStart);
        return sb.toString();
    }

    private void addElement(Node currentNode)
    {
        Token firstToken = ( (SimpleNode)currentNode ).jjtGetFirstToken();
        if( ( !currentNode.toAntimonyString().isEmpty() || currentNode instanceof AstStoichiometry || currentNode instanceof AstEqualZero )
                && ! ( currentNode instanceof AstSingleProperty ) )
        {
            if( !AntimonyUtility.isIgnoreInText(currentNode) && firstToken != null && firstToken.specialToken != null )
                sb.append(getSpecialTokens(firstToken.specialToken));
        }
        if( currentNode.isHighlight() )
            sb.append(AntimonyConstants.HIGHLIGHT_START);
        sb.append(currentNode.toAntimonyString());

        for( int i = 0; i < currentNode.jjtGetNumChildren(); i++ )
        {
            try
            {
                addElement(currentNode.jjtGetChild(i));
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can't add element(" + currentNode.jjtGetChild(i) + "): " + t);
            }
        }
        if( isBlock(currentNode) )
            sb.append("end");

        if( currentNode.isHighlight() )
            sb.append(AntimonyConstants.HIGHLIGHT_END);
    }

    /**
     * returns string with all special tokens
     * @param token
     * @return
     */
    private String getSpecialTokens(Token token)
    {
        StringBuilder result = new StringBuilder("");
        if( token != null )
        {
            result.append(getSpecialTokens(token.specialToken));
            result.append(token);
        }
        return result.toString();
    }

    private boolean isBlock(Node node)
    {
        return ( node instanceof AstModel && ! ( (AstModel)node ).isOutsideModel() ) || node instanceof AstFunction;
    }
}
