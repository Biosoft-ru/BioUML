/* $Id: RLexer.lex,v 1.1 2013/08/29 08:55:41 lan Exp $ */
/* RLexer.java is a generated file.  You probably want to
 * edit RLexer.lex to make changes.  Use JFlex to generate it.
 * To generate RLexer.java
 * Install <a href="http://jflex.de/">JFlex</a> v1.3.2 or later.
 * Once JFlex is in your classpath run<br>
 * <code>java JFlex.Main RLexer.lex</code><br>
 * You will then have a file called RLexer.java
 */

package biouml.plugins.antimony.lex;

import java.io.IOException;
import com.Ostermiller.Syntax.Lexer.Lexer;
import com.Ostermiller.Syntax.Lexer.Token;
import java.util.logging.Level;
import java.util.logging.Logger;

%%

%public
%class AntimonyLexer
%implements Lexer
%function getNextToken
%type Token 

%{
    int lastToken;

    private int nextState=YYINITIAL;
    
    protected Logger log = Logger.getLogger(AntimonyLexer.class.getName());
    
    /** 
     * next Token method that allows you to control if whitespace and comments are
     * returned as tokens.
     */
    public Token getNextToken(boolean returnComments, boolean returnWhiteSpace)throws IOException{
        Token t = getNextToken();
        while (t != null && ((!returnWhiteSpace && t.isWhiteSpace()) || (!returnComments && t.isComment()))){
            t = getNextToken();
        }
        return (t); 
    }        
    
    /**
     * Closes the current input stream, and resets the scanner to read from a new input stream.
	 * All internal variables are reset, the old input stream  cannot be reused
	 * (content of the internal buffer is discarded and lost).
	 * The lexical state is set to the initial state.
     * Subsequent tokens read from the lexer will start with the line, char, and column
     * values given here.
     *
     * @param reader The new input.
     * @param yyline The line number of the first token.
     * @param yychar The position (relative to the start of the stream) of the first token.
     * @param yycolumn The position (relative to the line) of the first token.
     * @throws IOException if an IOExecption occurs while switching readers.
     */
    public void reset(java.io.Reader reader, int yyline, int yychar, int yycolumn) throws IOException{
        yyreset(reader);
        this.yyline = yyline;
		this.yychar = yychar;
		this.yycolumn = yycolumn;
	}
%}

%line
%char
%full

BooleanLiteral=("true"|"false")
Digit=([0-9])
NonZeroDigit=([1-9])
Letter=([a-zA-Z])
AnnotationSymbol=([@])
Underscore=([_])
Dot=([.])
BLANK=([ ])
TAB=([\t])
FF=([\f])
EscChar=([\\])
CR=([\r])
LF=([\n])
EOL=({CR}|{LF}|{CR}{LF})
WhiteSpace=({BLANK}|{TAB}|{FF}|{EOL})
AnyNonSeparator=([^\t\f\r\n\a\v\ \(\)\{\}\[\]\;\,\=\>\<\!\~\?\:\+\-\*\/\&\|\^\%\"\'])
Identifier=(({Letter}|{Dot}({Letter}|{Dot}|{Underscore}))({Letter}|{Digit}|{Underscore}|{Dot})*)
ErrorIdentifier=({AnyNonSeparator}+)

Comment=([#][^\r\n]* | [/][/][^\r\n]*)

Sign=([\+]|[\-])
DecimalNum=(([0]|{NonZeroDigit}{Digit}*))
SignedInt=({Sign}?{Digit}+)
Expo=([e]|[E])
ExponentPart=({Expo}{SignedInt})

Double1=({Digit}+[\.]{Digit}*{ExponentPart}?)
Double2=([\.]{Digit}+{ExponentPart}?)
Double3=({Digit}+{ExponentPart})
Double4=({Digit}+)
Double=({Double1}|{Double2}|{Double3}|{Double4}) 

ZeroDouble1=([0]+[\.][0]*{ExponentPart}?)
ZeroDouble2=([\.][0]+{ExponentPart}?)
ZeroDouble3=([0]+{ExponentPart})
ZeroDouble4=([0]+)
ZeroDouble=({ZeroDouble1}|{ZeroDouble2}|{ZeroDouble3}|{ZeroDouble4})

ErrorFloat=({Digit}({AnyNonSeparator}|[\.])*)

AnyChrChr=([^\'\n\r\\])
Character=([\']{AnyChrChr}[\'])
MalformedUnclosedCharacter=([\']({AnyChrChr}|({EscChar}[^\n\r]))*)
MalformedCharacter=([\'][\']|{MalformedUnclosedCharacter}[\'])

AnyStrChr=([^\"\n\r\\])
UnclosedString=([\"]({AnyStrChr})*)
String=({UnclosedString}[\"])
MalformedUnclosedString=([\"]({EscChar}|{AnyStrChr})*)
MalformedString=({MalformedUnclosedString}[\"])

SpecialOp=([%][^\r\n\%]*[%])

Annotation=[@]{Identifier}({Dot}{Identifier})+

%%

<YYINITIAL> "(" { 
    lastToken = AntimonyToken.SEPARATOR_LPAREN;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
    }
<YYINITIAL> ")" {
    lastToken = AntimonyToken.SEPARATOR_RPAREN;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "{" {
    lastToken = AntimonyToken.SEPARATOR_LBRACE;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "}" {
    lastToken = AntimonyToken.SEPARATOR_RBRACE;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "[" {
    lastToken = AntimonyToken.SEPARATOR_LBRACKET;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "]" {
    lastToken = AntimonyToken.SEPARATOR_RBRACKET;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> ";" {
    lastToken = AntimonyToken.SEPARATOR_SEMICOLON;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "," {
    lastToken = AntimonyToken.SEPARATOR_COMMA;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> ">" {
    lastToken = AntimonyToken.OPERATOR_GREATER_THAN;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "<" {
    lastToken = AntimonyToken.OPERATOR_LESS_THAN;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "<=" {
    lastToken = AntimonyToken.OPERATOR_LESS_THAN_OR_EQUAL;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> ">=" {
    lastToken = AntimonyToken.OPERATOR_GREATER_THAN_OR_EQUAL;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "==" {
    lastToken = AntimonyToken.OPERATOR_EQUAL;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "!=" {
    lastToken = AntimonyToken.OPERATOR_NOT_EQUAL;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "'" {
    lastToken = AntimonyToken.OPERATOR_DIFF;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "!" {
    lastToken = AntimonyToken.OPERATOR_LOGICAL_NOT;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "&&" {
    lastToken = AntimonyToken.OPERATOR_LOGICAL_AND;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "||" {
    lastToken = AntimonyToken.OPERATOR_LOGICAL_OR;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "&" {
    lastToken = AntimonyToken.OPERATOR_BITWISE_AND;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "|" {
    lastToken = AntimonyToken.OPERATOR_BITWISE_OR;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "+" {
    lastToken = AntimonyToken.OPERATOR_ADD;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "-" {
    lastToken = AntimonyToken.OPERATOR_SUBTRACT;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "*" {
    lastToken = AntimonyToken.OPERATOR_MULTIPLY;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "/" {
    lastToken = AntimonyToken.OPERATOR_DIVIDE;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> ":" {
    lastToken = AntimonyToken.OPERATOR_SEQUENCE;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "~" {
    lastToken = AntimonyToken.OPERATOR_TILDE;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "?" {
    lastToken = AntimonyToken.OPERATOR_HELP;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "^" {
    lastToken = AntimonyToken.OPERATOR_EXPONENT;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "$" {
    lastToken = AntimonyToken.OPERATOR_COMPONENT;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "=" {
    lastToken = AntimonyToken.OPERATOR_ASSIGN;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "<-" {
    lastToken = AntimonyToken.OPERATOR_ASSIGN_ARROW;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "->" {
    lastToken = AntimonyToken.OPERATOR_ASSIGN_RARROW;
    AntimonyToken t = (new AntimonyToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "substanceOnly" {
    lastToken = AntimonyToken.RESERVED_WORD_SUBSTANCEONLY;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+13, nextState));
    return (t);
}
<YYINITIAL> "species" {
    lastToken = AntimonyToken.RESERVED_WORD_SPECIES;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+7, nextState));
    return (t);
}
<YYINITIAL> "gene" {
    lastToken = AntimonyToken.RESERVED_WORD_GENE;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "model" {
    lastToken = AntimonyToken.RESERVED_WORD_MODEL;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> "var" {
    lastToken = AntimonyToken.RESERVED_WORD_VAR;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+3, nextState));
    return (t);
}
<YYINITIAL> "at" {
    lastToken = AntimonyToken.RESERVED_WORD_AT;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+2, nextState));
    return (t);
}
<YYINITIAL> "after" {
    lastToken = AntimonyToken.RESERVED_WORD_AFTER;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> "is" {
    lastToken = AntimonyToken.RESERVED_WORD_IS;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+2, nextState));
    return (t);
}
<YYINITIAL> "has" {
    lastToken = AntimonyToken.RESERVED_WORD_HAS;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+3, nextState));
    return (t);
}
<YYINITIAL> "subtype" {
    lastToken = AntimonyToken.RESERVED_WORD_SUBTYPE;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+7, nextState));
    return (t);
}
<YYINITIAL> "in" {
    lastToken = AntimonyToken.RESERVED_WORD_IN;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+2, nextState));
    return (t);
}
<YYINITIAL> "unit" {
    lastToken = AntimonyToken.RESERVED_WORD_UNIT;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "compartment" {
    lastToken = AntimonyToken.RESERVED_WORD_COMPARTMENT;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+11, nextState));
    return (t);
}
<YYINITIAL> "const" {
    lastToken = AntimonyToken.RESERVED_WORD_CONST;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> "delete" {
    lastToken = AntimonyToken.RESERVED_WORD_DELETE;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+6, nextState));
    return (t);
}
<YYINITIAL> "function" {
    lastToken = AntimonyToken.RESERVED_WORD_FUNCTION;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+8, nextState));
    return (t);
}
<YYINITIAL> "assert" {
    lastToken = AntimonyToken.RESERVED_WORD_ASSERT;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+6, nextState));
    return (t);
}
<YYINITIAL> "else" {
    lastToken = AntimonyToken.RESERVED_WORD_ELSE;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "import" {
    lastToken = AntimonyToken.RESERVED_WORD_UNIT;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+6, nextState));
    return (t);
}
<YYINITIAL> "end" {
    lastToken = AntimonyToken.RESERVED_WORD_END;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+3, nextState));
    return (t);
}
<YYINITIAL> "notanumber" {
    lastToken = AntimonyToken.ATOM_NOTANUMBER;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+10, nextState));
    return (t);
}
<YYINITIAL> "infinity" {
    lastToken = AntimonyToken.ATOM_INFINITY;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+8, nextState));
    return (t);
}
<YYINITIAL> "pi" {
    lastToken = AntimonyToken.ATOM_PI;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+2, nextState));
    return (t);
}
<YYINITIAL> "avogadro" {
    lastToken = AntimonyToken.ATOM_AVOGADRO;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+8, nextState));
    return (t);
}
<YYINITIAL> "exponentiale" {
    lastToken = AntimonyToken.ATOM_EXPONENTIALE;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+12, nextState));
    return (t);
}
<YYINITIAL> "true" { 
    lastToken = AntimonyToken.LITERAL_BOOLEAN;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "false" { 
    lastToken = AntimonyToken.LITERAL_BOOLEAN;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> {Identifier} { 
    lastToken = AntimonyToken.IDENTIFIER;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {SpecialOp} { 
    lastToken = AntimonyToken.OPERATOR_SPECIAL;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {DecimalNum} {
    /* At this point, the number we found could still be too large.
     * If it is too large, we need to return an error.
     * Java has methods built in that will decode from a string
     * and throw an exception the number is too large 
     */
    try {
        /* bigger negatives are allowed than positives.  Thus
         * we have to be careful to make sure a neg sign is preserved
         */
        if (lastToken == AntimonyToken.OPERATOR_SUBTRACT){
            Integer.decode('-' + yytext());
        } else {
            Integer.decode(yytext());
        }
        lastToken = AntimonyToken.LITERAL_INTEGER_DECIMAL;
    } catch (NumberFormatException e){
        lastToken = AntimonyToken.ERROR_INTEGER_DECIMIAL_SIZE;
    }
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {ZeroDouble} {
    lastToken = AntimonyToken.LITERAL_DOUBLE;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {Double} {
    Double d;
    try {
        d = Double.valueOf(yytext());
        if (d.isInfinite() || d.compareTo(new Double(0d)) == 0){
            lastToken = AntimonyToken.ERROR_DOUBLE_SIZE;
        } else {
            lastToken = AntimonyToken.LITERAL_DOUBLE;
        }
    } catch (NumberFormatException e){
        lastToken = AntimonyToken.ERROR_DOUBLE_SIZE;
    } 
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {Character} { 
    lastToken = AntimonyToken.LITERAL_CHARACTER;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {String} { 
    lastToken = AntimonyToken.LITERAL_STRING;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> ({WhiteSpace}+) { 
    lastToken = AntimonyToken.WHITE_SPACE;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {Comment} { 
    lastToken = AntimonyToken.COMMENT_HASH;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {Annotation} { 
    lastToken = AntimonyToken.IDENTIFIER;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {UnclosedString} { 
    /* most of these errors have to be caught down near the end of the file.
     * This way, previous expressions of the same length have precedence.
     * This is really useful for catching anything bad by just allowing it 
     * to slip through the cracks. 
     */ 
    lastToken = AntimonyToken.ERROR_UNCLOSED_STRING;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {MalformedUnclosedString} { 
    lastToken = AntimonyToken.ERROR_MALFORMED_UNCLOSED_STRING;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {MalformedString} { 
    lastToken = AntimonyToken.ERROR_MALFORMED_STRING;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {MalformedCharacter} { 
    lastToken = AntimonyToken.ERROR_MALFORMED_CHARACTER;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {ErrorIdentifier} { 
    lastToken = AntimonyToken.ERROR_IDENTIFIER;
    AntimonyToken t = (new AntimonyToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}

[^]                              { log.log(Level.SEVERE, ("Can't highlight illegal character <"+
                                                        yytext()+"> at line " + this.yyline)); }
