package biouml.plugins.sbgn.title;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lan
 *
 */
public class SyntaxElement
{
    private SyntaxElementType type;
    private String data;
    private List<SyntaxElement> subElements;

    public SyntaxElement(SyntaxElementType type, String data)
    {
        this.type = type;
        this.data = data;
    }

    public SyntaxElement(SyntaxElementType type, List<SyntaxElement> subElements)
    {
        this.type = type;
        this.subElements = new ArrayList<>(subElements);
    }

    public SyntaxElement(String title)
    {
        this(new LexemeList(title));
    }

    public SyntaxElement(List<Lexeme> lexemes)
    {
        type = SyntaxElementType.ROOT;
        subElements = new ArrayList<>();
        for(int i=0; i<lexemes.size(); i++)
        {
            Lexeme lexeme = lexemes.get(i);
            int nBrackets = 0, nParenthesis = 0, nAngleBrackets = 0, nSquareBrackets = 0;
            switch( lexeme.getType() )
            {
                case COLON:
                    subElements.add(new SyntaxElement(SyntaxElementType.COLON, lexeme.getText()));
                    continue;
                case STRING:
                    subElements.add(new SyntaxElement(SyntaxElementType.STRING, lexeme.getText()));
                    continue;
                case INTEGER:
                    subElements.add(new SyntaxElement(SyntaxElementType.INTEGER, lexeme.getText()));
                    continue;
                default:
                    break;
            }
            int j;
            for(j=i; j<lexemes.size(); j++)
            {
                switch(lexemes.get(j).getType())
                {
                    case RIGHT_BRACKET:
                        nBrackets--; break;
                    case LEFT_BRACKET:
                        nBrackets++; break;
                    case RIGHT_PARENTHESIS:
                        nParenthesis--; break;
                    case LEFT_PARENTHESIS:
                        nParenthesis++; break;
                    case ANGLE_BRACKET_LEFT:
                        nAngleBrackets++;
                        break;
                    case ANGLE_BRACKET_RIGHT:
                        nAngleBrackets--;
                        break;
                    case LEFT_SQUARE_BRACKET:
                        nSquareBrackets++;
                        break;
                    case RIGHT_SQUARE_BRACKET:
                        nSquareBrackets--;
                        break;
                    default:
                        break;
                }
                if(nBrackets < 0)
                    throw new IllegalArgumentException("Unexpected }");
                if(nParenthesis < 0)
                    throw new IllegalArgumentException("Unexpected )");
                if( nAngleBrackets < 0 )
                    throw new IllegalArgumentException( "Unexpected >" );
                if( nSquareBrackets < 0 )
                    throw new IllegalArgumentException( "Unexpected ]" );
                if( nBrackets == 0 && nParenthesis == 0 && nAngleBrackets == 0 && nSquareBrackets == 0)
                    break;
            }
            if(nBrackets > 0)
                throw new IllegalArgumentException("Missing }");
            if(nParenthesis > 0)
                throw new IllegalArgumentException("Missing )");
            if( nAngleBrackets > 0 )
                throw new IllegalArgumentException( "Missing >" );
            if( nSquareBrackets > 0 )
                throw new IllegalArgumentException( "Missing ]" );
            SyntaxElement syntaxElement = new SyntaxElement(lexemes.subList(i+1, j));
            syntaxElement.type = lexeme.getType() == LexemeType.LEFT_BRACKET ? SyntaxElementType.BRACKETS
                    : lexeme.getType() == LexemeType.LEFT_PARENTHESIS ? SyntaxElementType.PARENTHESES 
            		: lexeme.getType() == LexemeType.ANGLE_BRACKET_LEFT ? SyntaxElementType.ANGLE_BRACKETS
					: SyntaxElementType.SQUARE_BRACKETS;
            subElements.add(syntaxElement);
            i = j;
        }
    }

    public SyntaxElementType getType()
    {
        return type;
    }

    public String getData()
    {
        return data;
    }

    public List<SyntaxElement> getSubElements()
    {
        return subElements == null ? null : Collections.unmodifiableList(subElements);
    }

    public String getOriginalString()
    {
        if(subElements == null) return data;
        StringBuilder sb = new StringBuilder();
        if(getType() == SyntaxElementType.BRACKETS)
            sb.append('{');
        else if(getType() == SyntaxElementType.PARENTHESES)
            sb.append('(');
        else if( getType() == SyntaxElementType.ANGLE_BRACKETS )
            sb.append( '<' );
        else if( getType() == SyntaxElementType.SQUARE_BRACKETS )
            sb.append( '[' );
        for(SyntaxElement element: subElements)
            sb.append(element.getOriginalString());
        if(getType() == SyntaxElementType.BRACKETS)
            sb.append('}');
        else if(getType() == SyntaxElementType.PARENTHESES)
            sb.append(')');
        else if( getType() == SyntaxElementType.ANGLE_BRACKETS )
            sb.append( '>' );
        else if( getType() == SyntaxElementType.SQUARE_BRACKETS )
            sb.append( ']' );
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return type+":"+(subElements == null?data:subElements);
    }
}
