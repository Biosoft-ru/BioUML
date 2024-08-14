package ru.biosoft.plugins.jri._test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.plugins.jri.lex.RLexer;
import ru.biosoft.plugins.jri.lex.RToken;

import com.Ostermiller.Syntax.Lexer.Token;

import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class TestRLexer extends TestCase
{
    private Token[] getTokens(String r) throws IOException
    {
        RLexer rLexer = new RLexer(new StringReader(r));
        List<Token> tokens = new ArrayList<>();
        Token t;
        while ((t = rLexer.getNextToken()) != null) {
            tokens.add(t);
        }
        return tokens.toArray(new Token[tokens.size()]);
    }
    
    public void testSimple() throws Exception
    {
        Token[] tokens = getTokens("# test comment\n" +
                "a <- list(1,\"literal#\\b\\388\",T);\n" +
                "print(a[[2]])\n" +
                "a <<- .5f %/% -1e-6\n" +
                "a = .qwe");
        int[] expectedTypes = {
                RToken.COMMENT_HASH,            // # test comment
                
                RToken.IDENTIFIER,              // a
                RToken.OPERATOR_ASSIGN_ARROW,   // <-
                RToken.RESERVED_WORD_LIST,      // list
                RToken.SEPARATOR_LPAREN,        // (
                RToken.LITERAL_INTEGER_DECIMAL, // 1
                RToken.SEPARATOR_COMMA,         // ,
                RToken.LITERAL_STRING,          // "literal#\b\388"
                RToken.SEPARATOR_COMMA,         // ,
                RToken.LITERAL_BOOLEAN,         // T
                RToken.SEPARATOR_RPAREN,        // )
                RToken.SEPARATOR_SEMICOLON,     // ;
                
                RToken.IDENTIFIER,              // print
                RToken.SEPARATOR_LPAREN,        // (
                RToken.IDENTIFIER,              // a
                RToken.SEPARATOR_LDBRACKET,     // [[
                RToken.LITERAL_INTEGER_DECIMAL, // 2
                RToken.SEPARATOR_RDBRACKET,     // ]]
                RToken.SEPARATOR_RPAREN,        // )
                
                RToken.IDENTIFIER,              // a
                RToken.OPERATOR_ASSIGN_DARROW,  // <<-
                RToken.LITERAL_FLOATING_POINT,  // .5f
                RToken.OPERATOR_SPECIAL,        // %/%
                RToken.OPERATOR_SUBTRACT,       // -
                RToken.LITERAL_DOUBLE,          // 1e-6

                RToken.IDENTIFIER,              // a
                RToken.OPERATOR_ASSIGN,         // =
                RToken.IDENTIFIER               // .qwe
        };
        int pos = 0;
        for(Token token: tokens)
        {
            if(token.isWhiteSpace())
                continue;
            assertEquals(token.toString(), expectedTypes[pos++], token.getID());
        }
        assertEquals(expectedTypes.length, pos);
    }
}
