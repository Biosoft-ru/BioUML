package ru.biosoft.plugins.jri.lex;

import com.Ostermiller.Syntax.Lexer.Token;

/**
 * @author lan
 *
 */
public class RToken extends Token
{
    public final static int RESERVED_WORD_LIST = 0x111;
    public final static int RESERVED_WORD_QUOTE = 0x112;
    public final static int RESERVED_WORD_BQUOTE = 0x113;
    public final static int RESERVED_WORD_EVAL = 0x114;
    public final static int RESERVED_WORD_RETURN = 0x115;
    public final static int RESERVED_WORD_CALL = 0x116;
    public final static int RESERVED_WORD_PARSE = 0x117;
    public final static int RESERVED_WORD_DEPARSE = 0x118;

    public final static int RESERVED_WORD_IF = 0x121;
    public final static int RESERVED_WORD_ELSE = 0x122;
    public final static int RESERVED_WORD_REPEAT = 0x123;
    public final static int RESERVED_WORD_WHILE = 0x124;
    public final static int RESERVED_WORD_FUNCTION = 0x125;
    public final static int RESERVED_WORD_FOR = 0x126;
    public final static int RESERVED_WORD_IN = 0x127;
    public final static int RESERVED_WORD_NEXT = 0x128;
    public final static int RESERVED_WORD_BREAK = 0x129;

    public final static int IDENTIFIER = 0x200;

    public final static int LITERAL_BOOLEAN = 0x300;
    public final static int LITERAL_INTEGER_DECIMAL = 0x310;
    public final static int LITERAL_INTEGER_OCTAL = 0x311;
    public final static int LITERAL_INTEGER_HEXIDECIMAL = 0x312;
    public final static int LITERAL_LONG_DECIMAL = 0x320;
    public final static int LITERAL_LONG_OCTAL = 0x321;
    public final static int LITERAL_LONG_HEXIDECIMAL = 0x322;
    public final static int LITERAL_FLOATING_POINT = 0x330;
    public final static int LITERAL_DOUBLE = 0x340;
    public final static int LITERAL_CHARACTER = 0x350;
    public final static int LITERAL_STRING = 0x360;

    public final static int ATOM_NULL = 0x371;
    public final static int ATOM_NA = 0x372;
    public final static int ATOM_Inf = 0x373;
    public final static int ATOM_NaN = 0x374;
    public final static int ATOM_NA_integer_ = 0x375;
    public final static int ATOM_NA_real_ = 0x376;
    public final static int ATOM_NA_complex_ = 0x377;
    public final static int ATOM_NA_character_ = 0x378;

    public final static int SEPARATOR_LPAREN = 0x400;
    public final static int SEPARATOR_RPAREN = 0x401;
    public final static int SEPARATOR_LBRACE = 0x410;
    public final static int SEPARATOR_RBRACE = 0x411;
    public final static int SEPARATOR_LBRACKET = 0x420;
    public final static int SEPARATOR_RBRACKET = 0x421;
    public final static int SEPARATOR_LDBRACKET = 0x422;    // [[
    public final static int SEPARATOR_RDBRACKET = 0x423;    // ]]
    public final static int SEPARATOR_SEMICOLON = 0x430;
    public final static int SEPARATOR_COMMA = 0x440;
    public final static int SEPARATOR_DOT_DOT_DOT = 0x450;  // ...
    public final static int SEPARATOR_DOT_DOT_NUM = 0x451;  // ..1, ..2, ...

    public final static int OPERATOR_GREATER_THAN = 0x500;
    public final static int OPERATOR_LESS_THAN = 0x501;
    public final static int OPERATOR_LESS_THAN_OR_EQUAL = 0x502;
    public final static int OPERATOR_GREATER_THAN_OR_EQUAL = 0x503;
    public final static int OPERATOR_EQUAL = 0x504;
    public final static int OPERATOR_NOT_EQUAL = 0x505;
    public final static int OPERATOR_LOGICAL_NOT = 0x510;
    public final static int OPERATOR_LOGICAL_AND = 0x511;
    public final static int OPERATOR_LOGICAL_OR = 0x512;
    public final static int OPERATOR_BITWISE_AND = 0x531;
    public final static int OPERATOR_BITWISE_OR = 0x532;
    public final static int OPERATOR_ADD = 0x520;
    public final static int OPERATOR_SUBTRACT = 0x521;
    public final static int OPERATOR_MULTIPLY = 0x522;
    public final static int OPERATOR_DIVIDE = 0x523;
    public final static int OPERATOR_SPECIAL = 0x524;   // %...%
    public final static int OPERATOR_SEQUENCE = 0x530;  // :
    public final static int OPERATOR_TILDE = 0x533;
    public final static int OPERATOR_HELP = 0x540;
    public final static int OPERATOR_EXPONENT = 0x546;  // ^
    public final static int OPERATOR_COMPONENT = 0x547; // $
    public final static int OPERATOR_SLOT = 0x548;  // @
    public final static int OPERATOR_DCOLON = 0x549; // ::
    public final static int OPERATOR_TCOLON = 0x54A; // :::

    public final static int OPERATOR_ASSIGN = 0x581;
    public final static int OPERATOR_ASSIGN_ARROW = 0x582;
    public final static int OPERATOR_ASSIGN_DARROW = 0x583;
    public final static int OPERATOR_ASSIGN_RARROW = 0x584;
    public final static int OPERATOR_ASSIGN_RDARROW = 0x585;

    public final static int COMMENT_HASH = 0xD00;

    public final static int WHITE_SPACE = 0xE00;

    public final static int ERROR_IDENTIFIER = 0xF00;
    public final static int ERROR_UNCLOSED_STRING = 0xF10;
    public final static int ERROR_MALFORMED_STRING = 0xF11;
    public final static int ERROR_MALFORMED_UNCLOSED_STRING = 0xF12;
    public final static int ERROR_UNCLOSED_CHARACTER = 0xF20;
    public final static int ERROR_MALFORMED_CHARACTER = 0xF21;
    public final static int ERROR_MALFORMED_UNCLOSED_CHARACTER = 0xF22;
    public final static int ERROR_INTEGER_DECIMIAL_SIZE = 0xF30;
    public final static int ERROR_INTEGER_OCTAL_SIZE = 0xF31;
    public final static int ERROR_INTEGER_HEXIDECIMAL_SIZE = 0xF32;
    public final static int ERROR_LONG_DECIMIAL_SIZE = 0xF33;
    public final static int ERROR_LONG_OCTAL_SIZE = 0xF34;
    public final static int ERROR_LONG_HEXIDECIMAL_SIZE = 0xF35;
    public final static int ERROR_FLOAT_SIZE = 0xF36;
    public final static int ERROR_DOUBLE_SIZE = 0xF37;
    public final static int ERROR_FLOAT = 0xF38;

    private final int ID;
    private final String contents;
    private final int lineNumber;
    private final int charBegin;
    private final int charEnd;
    private final int state;

    public RToken(int ID, String contents, int lineNumber, int charBegin, int charEnd)
    {
        this(ID, contents, lineNumber, charBegin, charEnd, Token.UNDEFINED_STATE);
    }

    public RToken(int ID, String contents, int lineNumber, int charBegin, int charEnd, int state)
    {
        this.ID = ID;
        this.contents = contents;
        this.lineNumber = lineNumber;
        this.charBegin = charBegin;
        this.charEnd = charEnd;
        this.state = state;
    }

    @Override
    public int getID()
    {
        return ID;
    }

    @Override
    public String getDescription()
    {
        if( isReservedWord() )
        {
            return ( "reservedWord" );
        }
        else if( isIdentifier() )
        {
            return ( "identifier" );
        }
        else if( isLiteral() )
        {
            return ( "literal" );
        }
        else if( isSeparator() )
        {
            return ( "separator" );
        }
        else if( isOperator() )
        {
            return ( "operator" );
        }
        else if( isComment() )
        {
            return ( "comment" );
        }
        else if( isWhiteSpace() )
        {
            return ( "whitespace" );
        }
        else if( isError() )
        {
            return ( "error" );
        }
        else
        {
            return ( "unknown" );
        }
    }

    @Override
    public String getContents()
    {
        return contents;
    }

    public boolean isReservedWord()
    {
        return ( ( ID >> 8 ) == 0x1 );
    }

    public boolean isIdentifier()
    {
        return ( ( ID >> 8 ) == 0x2 );
    }

    public boolean isLiteral()
    {
        return ( ( ID >> 8 ) == 0x3 );
    }
    public boolean isSeparator()
    {
        return ( ( ID >> 8 ) == 0x4 );
    }
    public boolean isOperator()
    {
        return ( ( ID >> 8 ) == 0x5 );
    }
    @Override
    public boolean isComment()
    {
        return ( ( ID >> 8 ) == 0xD );
    }

    @Override
    public boolean isWhiteSpace()
    {
        return ( ( ID >> 8 ) == 0xE );
    }

    @Override
    public boolean isError()
    {
        return ( ( ID >> 8 ) == 0xF );
    }

    @Override
    public int getLineNumber()
    {
        return lineNumber;
    }

    @Override
    public int getCharBegin()
    {
        return charBegin;
    }

    @Override
    public int getCharEnd()
    {
        return charEnd;
    }

    @Override
    public String errorString()
    {
        String s;
        if( isError() )
        {
            s = "Error on line " + lineNumber + ": ";
            switch( ID )
            {
                case ERROR_IDENTIFIER:
                    s += "Unrecognized Identifier: " + contents;
                    break;
                case ERROR_UNCLOSED_STRING:
                    s += "'\"' expected after " + contents;
                    break;
                case ERROR_MALFORMED_STRING:
                case ERROR_MALFORMED_UNCLOSED_STRING:
                    s += "Illegal character in " + contents;
                    break;
                case ERROR_UNCLOSED_CHARACTER:
                    s += "\"'\" expected after " + contents;
                    break;
                case ERROR_MALFORMED_CHARACTER:
                case ERROR_MALFORMED_UNCLOSED_CHARACTER:
                    s += "Illegal character in " + contents;
                    break;
                case ERROR_INTEGER_DECIMIAL_SIZE:
                case ERROR_INTEGER_OCTAL_SIZE:
                case ERROR_FLOAT:
                    s += "Illegal character in " + contents;
                    break;
                case ERROR_INTEGER_HEXIDECIMAL_SIZE:
                case ERROR_LONG_DECIMIAL_SIZE:
                case ERROR_LONG_OCTAL_SIZE:
                case ERROR_LONG_HEXIDECIMAL_SIZE:
                case ERROR_FLOAT_SIZE:
                case ERROR_DOUBLE_SIZE:
                    s += "Literal out of bounds: " + contents;
                    break;
            }

        }
        else
        {
            s = null;
        }
        return ( s );
    }

    @Override
    public int getState()
    {
        return state;
    }

    @Override
    public String toString()
    {
        return ( "Token #" + Integer.toHexString(ID) + ": " + getDescription() + " Line " + lineNumber + " from " + charBegin + " to "
                + charEnd + " : " + contents );
    }
}
