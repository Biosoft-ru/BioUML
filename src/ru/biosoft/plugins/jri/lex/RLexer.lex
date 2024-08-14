/* $Id: RLexer.lex,v 1.1 2013/08/29 08:55:41 lan Exp $ */
/* RLexer.java is a generated file.  You probably want to
 * edit RLexer.lex to make changes.  Use JFlex to generate it.
 * To generate RLexer.java
 * Install <a href="http://jflex.de/">JFlex</a> v1.3.2 or later.
 * Once JFlex is in your classpath run<br>
 * <code>java JFlex.Main RLexer.lex</code><br>
 * You will then have a file called RLexer.java
 */

package ru.biosoft.plugins.jri.lex;

import java.io.IOException;
import com.Ostermiller.Syntax.Lexer.Lexer;
import com.Ostermiller.Syntax.Lexer.Token;

%%

%public
%class RLexer
%implements Lexer
%function getNextToken
%type Token 

%{
    int lastToken;

    private int nextState=YYINITIAL;
    
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

BooleanLiteral=("true"|"false"|"T"|"F")
HexDigit=([0-9a-fA-F])
Digit=([0-9])
OctalDigit=([0-7])
TetraDigit=([0-3])
NonZeroDigit=([1-9])
Letter=([a-zA-Z])
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

OctEscape1=({EscChar}{OctalDigit})
OctEscape2=({EscChar}{OctalDigit}{OctalDigit})
OctEscape3=({EscChar}{TetraDigit}{OctalDigit}{OctalDigit})
OctEscape=({OctEscape1}|{OctEscape2}|{OctEscape3})

HexEscape=({EscChar}[x]{HexDigit}{HexDigit})
UnicodeEscape=({EscChar}[u]{HexDigit}{HexDigit}{HexDigit}{HexDigit}|{EscChar}[U]{HexDigit}{HexDigit}{HexDigit}{HexDigit}{HexDigit}{HexDigit}{HexDigit}{HexDigit})

Escape=({EscChar}([r]|[n]|[b]|[f]|[t]|[a]|[v]|[\\]|[\']|[\"]))
Identifier=(({Letter}|{Dot}({Letter}|{Dot}|{Underscore}))({Letter}|{Digit}|{Underscore}|{Dot})*)
ErrorIdentifier=({AnyNonSeparator}+)

Comment=([#][^\r\n]*)

Sign=([\+]|[\-])
LongSuffix=([l]|[L])
DecimalNum=(([0]|{NonZeroDigit}{Digit}*))
OctalNum=([0]{OctalDigit}*)
HexNum=([0]([x]|[X]){HexDigit}{HexDigit}*)
DecimalLong=({DecimalNum}{LongSuffix})
OctalLong=({OctalNum}{LongSuffix})
HexLong=({HexNum}{LongSuffix})

SignedInt=({Sign}?{Digit}+)
Expo=([e]|[E])
ExponentPart=({Expo}{SignedInt})
FloatSuffix=([f]|[F])
DoubleSuffix=([d]|[D])
FloatDouble1=({Digit}+[\.]{Digit}*{ExponentPart}?)
FloatDouble2=([\.]{Digit}+{ExponentPart}?)
FloatDouble3=({Digit}+{ExponentPart})
FloatDouble4=({Digit}+)
Double1=({FloatDouble1}{DoubleSuffix}?)
Double2=({FloatDouble2}{DoubleSuffix}?)
Double3=({FloatDouble3}{DoubleSuffix}?)
Double4=({FloatDouble4}{DoubleSuffix})
Float1=({FloatDouble1}{FloatSuffix})
Float2=({FloatDouble2}{FloatSuffix})
Float3=({FloatDouble3}{FloatSuffix})
Float4=({FloatDouble4}{FloatSuffix})
Float=({Float1}|{Float2}|{Float3}|{Float4})
Double=({Double1}|{Double2}|{Double3}|{Double4}) 

ZeroFloatDouble1=([0]+[\.][0]*{ExponentPart}?)
ZeroFloatDouble2=([\.][0]+{ExponentPart}?)
ZeroFloatDouble3=([0]+{ExponentPart})
ZeroFloatDouble4=([0]+)
ZeroDouble1=({ZeroFloatDouble1}{DoubleSuffix}?)
ZeroDouble2=({ZeroFloatDouble2}{DoubleSuffix}?)
ZeroDouble3=({ZeroFloatDouble3}{DoubleSuffix}?)
ZeroDouble4=({ZeroFloatDouble4}{DoubleSuffix})
ZeroFloat1=({ZeroFloatDouble1}{FloatSuffix})
ZeroFloat2=({ZeroFloatDouble2}{FloatSuffix})
ZeroFloat3=({ZeroFloatDouble3}{FloatSuffix})
ZeroFloat4=({ZeroFloatDouble4}{FloatSuffix})
ZeroFloat=({ZeroFloat1}|{ZeroFloat2}|{ZeroFloat3}|{ZeroFloat4})
ZeroDouble=({ZeroDouble1}|{ZeroDouble2}|{ZeroDouble3}|{ZeroDouble4})

ErrorFloat=({Digit}({AnyNonSeparator}|[\.])*)

AnyChrChr=([^\'\n\r\\])
UnclosedCharacter=([\']({Escape}|{OctEscape}|{UnicodeEscape}|{AnyChrChr}))
Character=({UnclosedCharacter}[\'])
MalformedUnclosedCharacter=([\']({AnyChrChr}|({EscChar}[^\n\r]))*)
MalformedCharacter=([\'][\']|{MalformedUnclosedCharacter}[\'])

AnyStrChr=([^\"\n\r\\])
UnclosedString=([\"]({Escape}|{OctEscape}|{UnicodeEscape}|{AnyStrChr})*)
String=({UnclosedString}[\"])
MalformedUnclosedString=([\"]({EscChar}|{AnyStrChr})*)
MalformedString=({MalformedUnclosedString}[\"])

DotDotOp=({Dot}{Dot}{Digit})
SpecialOp=([%][^\r\n\%]*[%])

%%

<YYINITIAL> "(" { 
    lastToken = RToken.SEPARATOR_LPAREN;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
    }
<YYINITIAL> ")" {
    lastToken = RToken.SEPARATOR_RPAREN;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "{" {
    lastToken = RToken.SEPARATOR_LBRACE;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "}" {
    lastToken = RToken.SEPARATOR_RBRACE;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "[" {
    lastToken = RToken.SEPARATOR_LBRACKET;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "]" {
    lastToken = RToken.SEPARATOR_RBRACKET;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "[[" {
    lastToken = RToken.SEPARATOR_LDBRACKET;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "]]" {
    lastToken = RToken.SEPARATOR_RDBRACKET;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> ";" {
    lastToken = RToken.SEPARATOR_SEMICOLON;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "," {
    lastToken = RToken.SEPARATOR_COMMA;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "..." {
    lastToken = RToken.SEPARATOR_DOT_DOT_DOT;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+3, nextState));
    return (t);
}
<YYINITIAL> ">" {
    lastToken = RToken.OPERATOR_GREATER_THAN;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "<" {
    lastToken = RToken.OPERATOR_LESS_THAN;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "<=" {
    lastToken = RToken.OPERATOR_LESS_THAN_OR_EQUAL;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> ">=" {
    lastToken = RToken.OPERATOR_GREATER_THAN_OR_EQUAL;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "==" {
    lastToken = RToken.OPERATOR_EQUAL;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "!=" {
    lastToken = RToken.OPERATOR_NOT_EQUAL;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "!" {
    lastToken = RToken.OPERATOR_LOGICAL_NOT;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "&&" {
    lastToken = RToken.OPERATOR_LOGICAL_AND;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "||" {
    lastToken = RToken.OPERATOR_LOGICAL_OR;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "&" {
    lastToken = RToken.OPERATOR_BITWISE_AND;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "|" {
    lastToken = RToken.OPERATOR_BITWISE_OR;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "+" {
    lastToken = RToken.OPERATOR_ADD;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "-" {
    lastToken = RToken.OPERATOR_SUBTRACT;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "*" {
    lastToken = RToken.OPERATOR_MULTIPLY;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "/" {
    lastToken = RToken.OPERATOR_DIVIDE;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> ":" {
    lastToken = RToken.OPERATOR_SEQUENCE;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "~" {
    lastToken = RToken.OPERATOR_TILDE;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "?" {
    lastToken = RToken.OPERATOR_HELP;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "^" {
    lastToken = RToken.OPERATOR_EXPONENT;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "$" {
    lastToken = RToken.OPERATOR_COMPONENT;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "@" {
    lastToken = RToken.OPERATOR_SLOT;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "::" {
    lastToken = RToken.OPERATOR_DCOLON;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> ":::" {
    lastToken = RToken.OPERATOR_TCOLON;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+3, nextState));
    return (t);
}
<YYINITIAL> "=" {
    lastToken = RToken.OPERATOR_ASSIGN;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "<-" {
    lastToken = RToken.OPERATOR_ASSIGN_ARROW;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "<<-" {
    lastToken = RToken.OPERATOR_ASSIGN_DARROW;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+3, nextState));
    return (t);
}
<YYINITIAL> "->" {
    lastToken = RToken.OPERATOR_ASSIGN_RARROW;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "->>" {
    lastToken = RToken.OPERATOR_ASSIGN_RDARROW;
    RToken t = (new RToken(lastToken,yytext(),yyline,yychar,yychar+3, nextState));
    return (t);
}

<YYINITIAL> "list" {
    lastToken = RToken.RESERVED_WORD_LIST;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "quote" {
    lastToken = RToken.RESERVED_WORD_QUOTE;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> "bquote" {
    lastToken = RToken.RESERVED_WORD_BQUOTE;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+6, nextState));
    return (t);
}
<YYINITIAL> "eval" {
    lastToken = RToken.RESERVED_WORD_EVAL;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "return" {
    lastToken = RToken.RESERVED_WORD_RETURN;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+6, nextState));
    return (t);
}
<YYINITIAL> "call" {
    lastToken = RToken.RESERVED_WORD_CALL;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "parse" {
    lastToken = RToken.RESERVED_WORD_PARSE;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> "deparse" {
    lastToken = RToken.RESERVED_WORD_DEPARSE;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+7, nextState));
    return (t);
}
<YYINITIAL> "if" {
    lastToken = RToken.RESERVED_WORD_IF;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+2, nextState));
    return (t);
}
<YYINITIAL> "else" {
    lastToken = RToken.RESERVED_WORD_ELSE;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "repeat" {
    lastToken = RToken.RESERVED_WORD_REPEAT;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+6, nextState));
    return (t);
}
<YYINITIAL> "while" {
    lastToken = RToken.RESERVED_WORD_WHILE;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> "function" {
    lastToken = RToken.RESERVED_WORD_FUNCTION;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+8, nextState));
    return (t);
}
<YYINITIAL> "for" {
    lastToken = RToken.RESERVED_WORD_FOR;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+3, nextState));
    return (t);
}
<YYINITIAL> "in" {
    lastToken = RToken.RESERVED_WORD_IN;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+2, nextState));
    return (t);
}
<YYINITIAL> "next" {
    lastToken = RToken.RESERVED_WORD_NEXT;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "break" {
    lastToken = RToken.RESERVED_WORD_BREAK;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}

<YYINITIAL> "NULL" {
    lastToken = RToken.ATOM_NULL;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "NA" {
    lastToken = RToken.ATOM_NA;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+2, nextState));
    return (t);
}
<YYINITIAL> "Inf" {
    lastToken = RToken.ATOM_Inf;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+3, nextState));
    return (t);
}
<YYINITIAL> "NaN" {
    lastToken = RToken.ATOM_NaN;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+3, nextState));
    return (t);
}
<YYINITIAL> "NA_integer_" {
    lastToken = RToken.ATOM_NA_integer_;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+11, nextState));
    return (t);
}
<YYINITIAL> "NA_real_" {
    lastToken = RToken.ATOM_NA_real_;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+8, nextState));
    return (t);
}
<YYINITIAL> "NA_complex_" {
    lastToken = RToken.ATOM_NA_complex_;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+11, nextState));
    return (t);
}
<YYINITIAL> "NA_character_" {
    lastToken = RToken.ATOM_NA_character_;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+13, nextState));
    return (t);
}

<YYINITIAL> "TRUE" { 
    lastToken = RToken.LITERAL_BOOLEAN;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "FALSE" { 
    lastToken = RToken.LITERAL_BOOLEAN;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> "T" { 
    lastToken = RToken.LITERAL_BOOLEAN;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+1, nextState));
    return (t);
}
<YYINITIAL> "F" { 
    lastToken = RToken.LITERAL_BOOLEAN;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar+1, nextState));
    return (t);
}

<YYINITIAL> {Identifier} { 
    lastToken = RToken.IDENTIFIER;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {DotDotOp} { 
    lastToken = RToken.SEPARATOR_DOT_DOT_NUM;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {SpecialOp} { 
    lastToken = RToken.OPERATOR_SPECIAL;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
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
        if (lastToken == RToken.OPERATOR_SUBTRACT){
            Integer.decode('-' + yytext());
        } else {
            Integer.decode(yytext());
        }
        lastToken = RToken.LITERAL_INTEGER_DECIMAL;
    } catch (NumberFormatException e){
        lastToken = RToken.ERROR_INTEGER_DECIMIAL_SIZE;
    }
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {OctalNum} {
    /* An Octal number cannot be too big.  After removing 
     * initial zeros, It can have 11 digits, the first
     * of which must be 3 or less.
     */
    lastToken = RToken.LITERAL_INTEGER_OCTAL;
    int i;
    int length =yytext().length();
    for (i=1 ; i<length-11; i++){
        //check for initial zeros
        if (yytext().charAt(i) != '0'){ 
            lastToken = RToken.ERROR_INTEGER_OCTAL_SIZE;
        }
    }
    if (length - i > 11){
        lastToken = RToken.ERROR_INTEGER_OCTAL_SIZE;
    } else if (length - i == 11){
        // if the rest of the number is as big as possible
        // the first digit can only be 3 or less
        if (yytext().charAt(i) != '0' && yytext().charAt(i) != '1' && 
        yytext().charAt(i) != '2' && yytext().charAt(i) != '3'){
            lastToken = RToken.ERROR_INTEGER_OCTAL_SIZE;
        }
    }
    // Otherwise, it should be OK   
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {HexNum} {
    /* A Hex number cannot be too big.  After removing 
     * initial zeros, It can have 8 digits
     */
    lastToken = RToken.LITERAL_INTEGER_HEXIDECIMAL;
    int i;
    int length =yytext().length();
    for (i=2 ; i<length-8; i++){
        //check for initial zeros
        if (yytext().charAt(i) != '0'){ 
            lastToken = RToken.ERROR_INTEGER_HEXIDECIMAL_SIZE;
        }
    }
    if (length - i > 8){
        lastToken = RToken.ERROR_INTEGER_HEXIDECIMAL_SIZE;
    }
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {DecimalLong} { 
    try {
        if (lastToken == RToken.OPERATOR_SUBTRACT){
            Long.decode('-' + yytext().substring(0,yytext().length()-1));
        } else {
            Long.decode(yytext().substring(0,yytext().length()-1));
        }
        lastToken = RToken.LITERAL_LONG_DECIMAL;
    } catch (NumberFormatException e){  
        lastToken = RToken.ERROR_LONG_DECIMIAL_SIZE;
    }
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {OctalLong} {
    /* An Octal number cannot be too big.  After removing 
     * initial zeros, It can have 23 digits, the first
     * of which must be 1 or less.  The last will be the L or l
     * at the end.
     */
    lastToken = RToken.LITERAL_LONG_OCTAL;
    int i;
    int length =yytext().length();
    for (i=1 ; i<length-23; i++){
        //check for initial zeros
        if (yytext().charAt(i) != '0'){ 
            lastToken = RToken.ERROR_LONG_OCTAL_SIZE;
        }
    }
    if (length - i > 23){
        lastToken = RToken.ERROR_LONG_OCTAL_SIZE;
    } else if (length - i == 23){
        // if the rest of the number is as big as possible
        // the first digit can only be 3 or less
        if (yytext().charAt(i) != '0' && yytext().charAt(i) != '1'){
            lastToken = RToken.ERROR_LONG_OCTAL_SIZE;
        }
    }
    // Otherwise, it should be OK   
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {HexLong} {
    /* A Hex long cannot be too big.  After removing 
     * initial zeros, It can have 17 digits, the last of which is
     * the L or l
     */
    lastToken = RToken.LITERAL_LONG_HEXIDECIMAL;
    int i;
    int length =yytext().length();
    for (i=2 ; i<length-17; i++){
        //check for initial zeros
        if (yytext().charAt(i) != '0'){ 
            lastToken = RToken.ERROR_LONG_HEXIDECIMAL_SIZE;
        }
    }
    if (length - i > 17){
        lastToken = RToken.ERROR_LONG_HEXIDECIMAL_SIZE;
    }
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {ZeroFloat} {
    /* catch the case of a zero in parsing, so that we do not incorrectly
     * give an error that a number was rounded to zero
     */
    lastToken = RToken.LITERAL_FLOATING_POINT;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {ZeroDouble} {
    lastToken = RToken.LITERAL_DOUBLE;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {Float} {
    /* Sun s java has a few bugs here.  Their MAX_FLOAT and MIN_FLOAT do not
     * quite match the spec.  Its not far off, so we will deal.  If they fix
     * then we are fixed.  So all good.
     */ 
    Float f;
    try {
        f = Float.valueOf(yytext());
        if (f.isInfinite() || f.compareTo(new Float(0f)) == 0){
            lastToken = RToken.ERROR_FLOAT_SIZE;
        } else {
            lastToken = RToken.LITERAL_FLOATING_POINT;
        }
    } catch (NumberFormatException e){
        lastToken = RToken.ERROR_FLOAT_SIZE;
    }
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {Double} {
    Double d;
    try {
        d = Double.valueOf(yytext());
        if (d.isInfinite() || d.compareTo(new Double(0d)) == 0){
            lastToken = RToken.ERROR_DOUBLE_SIZE;
        } else {
            lastToken = RToken.LITERAL_DOUBLE;
        }
    } catch (NumberFormatException e){
        lastToken = RToken.ERROR_DOUBLE_SIZE;
    } 
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}


<YYINITIAL> {Character} { 
    lastToken = RToken.LITERAL_CHARACTER;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {String} { 
    lastToken = RToken.LITERAL_STRING;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}

<YYINITIAL> ({WhiteSpace}+) { 
    lastToken = RToken.WHITE_SPACE;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}

<YYINITIAL> {Comment} { 
    lastToken = RToken.COMMENT_HASH;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {UnclosedString} { 
    /* most of these errors have to be caught down near the end of the file.
     * This way, previous expressions of the same length have precedence.
     * This is really useful for catching anything bad by just allowing it 
     * to slip through the cracks. 
     */ 
    lastToken = RToken.ERROR_UNCLOSED_STRING;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {MalformedUnclosedString} { 
    lastToken = RToken.ERROR_MALFORMED_UNCLOSED_STRING;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {MalformedString} { 
    lastToken = RToken.ERROR_MALFORMED_STRING;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {UnclosedCharacter} { 
    lastToken = RToken.ERROR_UNCLOSED_CHARACTER;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {MalformedUnclosedCharacter} { 
    lastToken = RToken.ERROR_MALFORMED_UNCLOSED_CHARACTER;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {MalformedCharacter} { 
    lastToken = RToken.ERROR_MALFORMED_CHARACTER;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {ErrorFloat} { 
    lastToken = RToken.ERROR_FLOAT;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {ErrorIdentifier} { 
    lastToken = RToken.ERROR_IDENTIFIER;
    RToken t = (new RToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
