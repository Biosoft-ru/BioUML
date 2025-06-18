package biouml.plugins.wdl.colorer;

import com.Ostermiller.Syntax.Lexer.Token;

/**
 * @author ilya
 */
public class WDLToken extends Token
{

    public final static int RESERVED_WORD_IN = 0x126;

    public final static int RESERVED_WORD_IMPORT = 0x127;

    public final static int RESERVED_WORD_ELSE = 0x128;

    public static final int RESERVED_WORD_CALL = 0x129;
    public static final int RESERVED_WORD_META = 0x130;
    public static final int RESERVED_WORD_TASK = 0x131;
    public static final int RESERVED_WORD_HINTS = 0x132;
    public static final int RESERVED_WORD_STRUCT = 0x133;
    public static final int RESERVED_WORD_COMMAND = 0x134;
    public static final int RESERVED_RUNTIME = 0x135;
    public static final int RESERVED_WORD_SCATTER = 0x136;
    public static final int RESERVED_WORD_VERSION = 0x137;
    public static final int RESERVED_WORD_WORKFLOW = 0x138;
    public static final int RESERVED_WORD_PARAMETER_META = 0x139;
    public static final int RESERVED_WORD_REQUIREMENTS = 0x140;
    public static final int RESERVED_WORD_OUTPUT = 0x141;
    public static final int RESERVED_WORD_INPUT = 0x142;

    public final static int IDENTIFIER = 0x200;

    public final static int LITERAL_BOOLEAN = 0x300;
    public final static int LITERAL_INTEGER_DECIMAL = 0x310;
    public final static int LITERAL_LONG_DECIMAL = 0x320;
    public final static int LITERAL_FLOATING_POINT = 0x330;
    public final static int LITERAL_DOUBLE = 0x340;
    public final static int LITERAL_CHARACTER = 0x350;
    public final static int LITERAL_STRING = 0x360;

    public final static int ATOM_NOTANUMBER = 0x135;


    public final static int SEPARATOR_LPAREN = 0x400;
    public final static int SEPARATOR_RPAREN = 0x401;
    public final static int SEPARATOR_LBRACE = 0x410;
    public final static int SEPARATOR_RBRACE = 0x411;
    public final static int SEPARATOR_LBRACKET = 0x420;
    public final static int SEPARATOR_RBRACKET = 0x421;
    public final static int SEPARATOR_SEMICOLON = 0x430;
    public final static int SEPARATOR_COMMA = 0x440;

    public final static int OPERATOR_GREATER_THAN = 0x500;
    public final static int OPERATOR_LESS_THAN = 0x501;
    public final static int OPERATOR_LESS_THAN_OR_EQUAL = 0x502;
    public final static int OPERATOR_GREATER_THAN_OR_EQUAL = 0x503;
    public final static int OPERATOR_EQUAL = 0x504;
    public final static int OPERATOR_NOT_EQUAL = 0x505;
    public final static int OPERATOR_LOGICAL_NOT = 0x510;
    public final static int OPERATOR_LOGICAL_AND = 0x511;
    public final static int OPERATOR_LOGICAL_OR = 0x512;
    public final static int OPERATOR_ADD = 0x520;
    public final static int OPERATOR_SUBTRACT = 0x521;
    public final static int OPERATOR_MULTIPLY = 0x522;
    public final static int OPERATOR_DIVIDE = 0x523;
    public final static int OPERATOR_SEQUENCE = 0x530; // :
    public final static int OPERATOR_HELP = 0x540;
    public static final int OPERATOR_TILDE = 0x541;

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
    public final static int ERROR_INTEGER_DECIMIAL_SIZE = 0xF30;
    public final static int ERROR_DOUBLE_SIZE = 0xF37;

    private final int ID;
    private final String contents;
    private final int lineNumber;
    private final int charBegin;
    private final int charEnd;
    private final int state;

    public WDLToken(int ID, String contents, int lineNumber, int charBegin, int charEnd)
    {
        this( ID, contents, lineNumber, charBegin, charEnd, Token.UNDEFINED_STATE );
    }

    public WDLToken(int ID, String contents, int lineNumber, int charBegin, int charEnd, int state)
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
                case ERROR_MALFORMED_CHARACTER:
                    s += "Illegal character in " + contents;
                    break;
                case ERROR_INTEGER_DECIMIAL_SIZE:
                    s += "Illegal character in " + contents;
                    break;
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
        return ( "Token #" + Integer.toHexString( ID ) + ": " + getDescription() + " Line " + lineNumber + " from " + charBegin + " to "
                + charEnd + " : " + contents );
    }
}