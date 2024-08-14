package biouml.plugins.sbgn.title;

import java.util.ArrayList;

/**
 * @author lan
 *
 */
public class LexemeList extends ArrayList<Lexeme>
{
    public LexemeList(String title)
    {
        LexemeType currentType = LexemeType.INTEGER;
        int lexemeStart = 0;
        int openBrackets = 0;
        int openSquareBrackets = 0;
        boolean quoteOpened = false;
        Lexeme lexeme = null;
        Lexeme prevLexeme = null;
        for( int titlePos = 0; titlePos < title.length(); titlePos++ )
        {
            char c = title.charAt( titlePos );
            prevLexeme = lexeme;
            lexeme = null;
            if( quoteOpened )
            {
                currentType = LexemeType.STRING;
                if( c == '\'' )
                    quoteOpened = false;
                continue;
            }
            else if( c == '\'' && ( titlePos == 0 || ( prevLexeme != null && ( prevLexeme.getType().equals( LexemeType.LEFT_PARENTHESIS )
                    || prevLexeme.getType().equals( LexemeType.RIGHT_PARENTHESIS )
                    || prevLexeme.getType().equals( LexemeType.RIGHT_BRACKET )
                    || prevLexeme.getType().equals( LexemeType.RIGHT_SQUARE_BRACKET )
                    || prevLexeme.getType().equals( LexemeType.RIGHT_SQUARE_BRACKET )
                    || prevLexeme.getType().equals( LexemeType.ANGLE_BRACKET_RIGHT )
                    || prevLexeme.getType().equals( LexemeType.COLON ) ) ) ) )
            {
                quoteOpened = true;
            }
            else if( c == '{' )
            {
                openBrackets++;
                if( openBrackets == 1 )
                    lexeme = new Lexeme(LexemeType.LEFT_BRACKET, "{");
            }
            else if( c == '}' )
            {
                openBrackets--;
                if( openBrackets == 0 )
                    lexeme = new Lexeme(LexemeType.RIGHT_BRACKET, "}");
            }
            else if( c == '[' )
            {
                openSquareBrackets++;
                if( openSquareBrackets == 1 )
                    lexeme = new Lexeme(LexemeType.LEFT_SQUARE_BRACKET, "[");
            }
            else if( c == ']' )
            {
                openSquareBrackets--;
                if( openSquareBrackets == 0 )
                    lexeme = new Lexeme(LexemeType.RIGHT_SQUARE_BRACKET, "]");
            }
            else if( isEmpty() || openSquareBrackets == 0 )
            {
                if( c == ':' )
                    lexeme = new Lexeme(LexemeType.COLON, ":");
                else if( openBrackets == 0 )
                {
                    if( c == '(' )
                        lexeme = new Lexeme(LexemeType.LEFT_PARENTHESIS, "(");
                    else if( c == ')' )
                        lexeme = new Lexeme(LexemeType.RIGHT_PARENTHESIS, ")");
                    else if( c == '<' )
                        lexeme = new Lexeme(LexemeType.ANGLE_BRACKET_LEFT, "<");
                    else if( c == '>' )
                        lexeme = new Lexeme(LexemeType.ANGLE_BRACKET_RIGHT, ">");
                }
            }
            if( lexeme != null )
            {
                if( lexemeStart < titlePos )
                {
                    String substring = title.substring(lexemeStart, titlePos);
                    if( substring.equals("n") )
                        currentType = LexemeType.INTEGER;
                    add(new Lexeme(currentType, substring));
                }
                lexemeStart = titlePos + 1;
                currentType = LexemeType.INTEGER;
                add(lexeme);
            }
            else
            {
                if( !Character.isDigit(c) )
                    currentType = LexemeType.STRING;
            }
        }
        if( lexemeStart < title.length() )
        {
            String substring = title.substring(lexemeStart);
            if( substring.equals("n") )
                currentType = LexemeType.INTEGER;
            add(new Lexeme(currentType, substring));
        }
        for( int i = 0; i < size() - 3; i++ ) //transform [ ( , STRING | Integer , ) , STRING ] => [ STRING ] 
        {
            if( get(i).getType() == LexemeType.LEFT_PARENTHESIS
                    && ( get(i + 1).getType() == LexemeType.STRING || get(i + 1).getType() == LexemeType.INTEGER )
                    && get(i + 2).getType() == LexemeType.RIGHT_PARENTHESIS && get(i + 3).getType() == LexemeType.STRING )
            {
                Lexeme lex = new Lexeme( LexemeType.STRING,
                        get(i).getText() + get(i + 1).getText() + get(i + 2).getText() + get(i + 3).getText());
                subList(i, i + 4).clear();
                add( i, lex );
            }
        }
        for( int i = 0; i < size() - 1; i++ ) //join [INTEGER | STRING] + [INTEGER | STRING] => [STRING] //TODO: probably following code is useless now
        {
            while( i < size() - 1 && ( get(i).getType() == LexemeType.STRING || get(i).getType() == LexemeType.INTEGER )
                    && ( get(i + 1).getType() == LexemeType.STRING || get(i + 1).getType() == LexemeType.INTEGER ) )
            {
                Lexeme lex = new Lexeme( LexemeType.STRING, get( i ).getText() + get( i + 1 ).getText() );
                subList(i, i + 2).clear();
                add( i, lex );
            }
        }
    }
}
