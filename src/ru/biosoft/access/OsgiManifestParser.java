package ru.biosoft.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OsgiManifestParser
{
    private enum TokenType {
        NONE, WHITESPACE, WORD, EQUALS, SEMICOLON, STRING, COMMA
    }

    private static final Pattern TOKENS = Pattern.compile( "(\\s+)|([\\w\\-.]+)|([:]?=)|(;)|[\"]([^\"]+)[\"]|(,)" );
    private final String input;
    private int pos;
    private final Matcher matcher;
    
    @SuppressWarnings ( "serial" )
    public class ParseException extends Exception
    {
        public ParseException(String message)
        {
            super( String.format( Locale.ENGLISH, "%s%n%s%n%" + ( pos + 1 ) + "s", message, input, "^" ) );
        }
    }
    
    public OsgiManifestParser(String input)
    {
        this.input = input;
        this.pos = 0;
        this.matcher = TOKENS.matcher( input );
    }
    
    public String consume(OsgiManifestParser.TokenType token, boolean optional) throws ParseException
    {
        while(true)
        {
            if(!matcher.find(pos) || matcher.start() != pos)
                throw new ParseException( "Invalid symbol" );
            pos = matcher.end();
            if(matcher.group( TokenType.WHITESPACE.ordinal() ) == null)
                break;
        }
        String tokenStr = matcher.group( token.ordinal() );
        if(tokenStr == null)
        {
            if(!optional)
                throw new ParseException( "Unexpected token "+matcher.group()+" (wanted: "+token+")" );
            pos = matcher.start();
        } else
        {
            if(matcher.find( pos ) && matcher.start() == pos && matcher.group( TokenType.WHITESPACE.ordinal() ) != null)
                pos = matcher.end();
        }
        return tokenStr;
    }
    
    public boolean finished()
    {
        return pos == input.length();
    }
    
    public static List<String> getStrings(String value) throws ParseException
    {
        List<String> result = new ArrayList<>();
        OsgiManifestParser tokenizer = new OsgiManifestParser( value );
        while(!tokenizer.finished())
        {
            result.add(tokenizer.consume( TokenType.WORD, false ));
            while(!tokenizer.finished() && tokenizer.consume( TokenType.COMMA, true ) == null)
            {
                tokenizer.consume( TokenType.SEMICOLON, false );
                tokenizer.consume( TokenType.WORD, false );
                tokenizer.consume( TokenType.EQUALS, false );
                if(tokenizer.consume( TokenType.STRING, true ) == null)
                    tokenizer.consume( TokenType.WORD, false );
            }
        }
        return result;
    }
}