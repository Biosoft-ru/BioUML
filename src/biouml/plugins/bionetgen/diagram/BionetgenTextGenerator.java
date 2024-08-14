package biouml.plugins.bionetgen.diagram;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.bionetgen.bnglparser.BNGList;
import biouml.plugins.bionetgen.bnglparser.BNGStart;
import biouml.plugins.bionetgen.bnglparser.Node;
import biouml.plugins.bionetgen.bnglparser.SimpleNode;
import biouml.plugins.bionetgen.bnglparser.Token;

public class BionetgenTextGenerator
{
    public static final String HIGHLIGHT_START = "BIONETGEN_HIGHLIGHT_START";
    public static final String HIGHLIGHT_END = "BIONETGEN_HIGHLIGHT_END";

    protected Logger log = Logger.getLogger(BionetgenTextGenerator.class.getName());

    protected BNGStart astStart;
    protected StringBuilder sb;

    public BionetgenTextGenerator(BNGStart astStart)
    {
        this.astStart = astStart;
    }

    public String generateText()
    {
        sb = new StringBuilder();
        for( int i = 0; i < astStart.jjtGetNumChildren(); i++ )
        {
            addElement(astStart.jjtGetChild(i));
        }
        return moveHighlighterStart(sb.toString());
    }

    private void addElement(Node currentNode)
    {
        Token firstToken = ( (SimpleNode)currentNode ).jjtGetFirstToken();
        if( !isIgnored((SimpleNode)currentNode) && firstToken != null && firstToken.specialToken != null )
            sb.append(getSpecialTokens(firstToken.specialToken));

        if( currentNode.isHighlight() )
            sb.append(HIGHLIGHT_START);
        sb.append(currentNode.toBNGString());
        for( int i = 0; i < currentNode.jjtGetNumChildren(); i++ )
        {
            try
            {
                addElement(currentNode.jjtGetChild(i));
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can't add AST node (" + currentNode.jjtGetChild(i) + ") to text: " + t.getMessage(), t);
            }
        }
        if( currentNode.isHighlight() )
            sb.append(HIGHLIGHT_END);
    }

    private String moveHighlighterStart(String source)
    {
        String str = source;
        String toBeReplaced = HIGHLIGHT_START + " ";
        String toReplaceBy = " " + HIGHLIGHT_START;
        while( str.contains(toBeReplaced) )
        {
            str = str.replaceAll(toBeReplaced, toReplaceBy);
        }
        return str;
    }

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

    private static boolean isIgnored(SimpleNode astNode)
    {
        return ( astNode.toBNGString().isEmpty() || astNode instanceof BNGList );
    }
}
