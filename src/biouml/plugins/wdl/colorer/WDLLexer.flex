/* $Id: RLexer.lex,v 1.1 2013/08/29 08:55:41 lan Exp $ */
/* RLexer.java is a generated file.  You probably want to
 * edit RLexer.lex to make changes.  Use JFlex to generate it.
 * To generate RLexer.java
 * Install <a href="http://jflex.de/">JFlex</a> v1.3.2 or later.
 * Once JFlex is in your classpath run<br>
 * <code>java JFlex.Main RLexer.lex</code><br>
 * You will then have a file called RLexer.java
 */

package com.wdl.colorer;

import java.io.IOException;
import com.Ostermiller.Syntax.Lexer.Lexer;
import com.Ostermiller.Syntax.Lexer.Token;
import java.util.logging.Level;
import java.util.logging.Logger;

%%

%public
%class WDLColorer
%implements Lexer
%function getNextToken
%type Token 

%{
    int lastToken;

    private int nextState=YYINITIAL;
    
    protected Logger log = Logger.getLogger(WDLColorer.class.getName());
    
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

Comment=([#][^\r\n]*)

DecimalNum=(([0]|{NonZeroDigit}{Digit}*))


Double1=({Digit}+[\.]{Digit}*)
Double2=({Digit}+)
Double=({Double1}|{Double2}) 

ZeroDouble=([0]+)


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



%%

<YYINITIAL> ";" {
    lastToken = WDLToken.SEPARATOR_SEMICOLON;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "(" { 
    lastToken = WDLToken.SEPARATOR_LPAREN;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
    }
<YYINITIAL> ")" {
    lastToken = WDLToken.SEPARATOR_RPAREN;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "{" {
    lastToken = WDLToken.SEPARATOR_LBRACE;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "}" {
    lastToken = WDLToken.SEPARATOR_RBRACE;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "[" {
    lastToken = WDLToken.SEPARATOR_LBRACKET;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "]" {
    lastToken = WDLToken.SEPARATOR_RBRACKET;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "," {
    lastToken = WDLToken.SEPARATOR_COMMA;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> ">" {
    lastToken = WDLToken.OPERATOR_GREATER_THAN;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "<" {
    lastToken = WDLToken.OPERATOR_LESS_THAN;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "<=" {
    lastToken = WDLToken.OPERATOR_LESS_THAN_OR_EQUAL;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "~" {
    lastToken = WDLToken.OPERATOR_TILDE;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> ">=" {
    lastToken = WDLToken.OPERATOR_GREATER_THAN_OR_EQUAL;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "==" {
    lastToken = WDLToken.OPERATOR_EQUAL;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "!=" {
    lastToken = WDLToken.OPERATOR_NOT_EQUAL;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "!" {
    lastToken = WDLToken.OPERATOR_LOGICAL_NOT;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "&&" {
    lastToken = WDLToken.OPERATOR_LOGICAL_AND;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+2, nextState));
    return (t);
}
<YYINITIAL> "+" {
    lastToken = WDLToken.OPERATOR_ADD;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "-" {
    lastToken = WDLToken.OPERATOR_SUBTRACT;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "*" {
    lastToken = WDLToken.OPERATOR_MULTIPLY;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "/" {
    lastToken = WDLToken.OPERATOR_DIVIDE;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> ":" {
    lastToken = WDLToken.OPERATOR_SEQUENCE;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "?" {
    lastToken = WDLToken.OPERATOR_HELP;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "=" {
    lastToken = WDLToken.OPERATOR_ASSIGN;
    WDLToken t = (new WDLToken(lastToken,yytext(),yyline,yychar,yychar+1, nextState));
    return (t);
}
<YYINITIAL> "command" {
    lastToken = WDLToken.RESERVED_WORD_COMMAND;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+7, nextState));
    return (t);
}
<YYINITIAL> "workflow" {
    lastToken = WDLToken.RESERVED_WORD_WORKFLOW;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+8, nextState));
    return (t);
}
<YYINITIAL> "runtime" {
    lastToken = WDLToken.RESERVED_RUNTIME;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+7, nextState));
    return (t);
}
<YYINITIAL> "task" {
    lastToken = WDLToken.RESERVED_WORD_TASK;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "scatter" {
    lastToken = WDLToken.RESERVED_WORD_SCATTER;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+7, nextState));
    return (t);
}
<YYINITIAL> "import" {
    lastToken = WDLToken.RESERVED_WORD_IMPORT;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+6, nextState));
    return (t);
}
<YYINITIAL> "requirements" {
    lastToken = WDLToken.RESERVED_WORD_REQUIREMENTS;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+12, nextState));
    return (t);
}
<YYINITIAL> "hints" {
    lastToken = WDLToken.RESERVED_WORD_HINTS;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> "struct" {
    lastToken = WDLToken.RESERVED_WORD_STRUCT;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+6, nextState));
    return (t);
}
<YYINITIAL> "meta" {
    lastToken = WDLToken.RESERVED_WORD_META;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "parameter_meta" {
    lastToken = WDLToken.RESERVED_WORD_PARAMETER_META;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+14, nextState));
    return (t);
}
<YYINITIAL> "call" {
    lastToken = WDLToken.RESERVED_WORD_CALL;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "version" {
    lastToken = WDLToken.RESERVED_WORD_VERSION;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+7, nextState));
    return (t);
}
<YYINITIAL> "in" {
    lastToken = WDLToken.RESERVED_WORD_IN;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+2, nextState));
    return (t);
}
<YYINITIAL> "else" {
    lastToken = WDLToken.RESERVED_WORD_ELSE;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "input" {
    lastToken = WDLToken.RESERVED_WORD_INPUT;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> "output" {
    lastToken = WDLToken.RESERVED_WORD_OUTPUT;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+6, nextState));
    return (t);
}
<YYINITIAL> "true" { 
    lastToken = WDLToken.LITERAL_BOOLEAN;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+4, nextState));
    return (t);
}
<YYINITIAL> "false" { 
    lastToken = WDLToken.LITERAL_BOOLEAN;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar+5, nextState));
    return (t);
}
<YYINITIAL> {Identifier} { 
    lastToken = WDLToken.IDENTIFIER;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
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
        if (lastToken == WDLToken.OPERATOR_SUBTRACT){
            Integer.decode('-' + yytext());
        } else {
            Integer.decode(yytext());
        }
        lastToken = WDLToken.LITERAL_INTEGER_DECIMAL;
    } catch (NumberFormatException e){
        lastToken = WDLToken.ERROR_INTEGER_DECIMIAL_SIZE;
    }
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {ZeroDouble} {
    lastToken = WDLToken.LITERAL_DOUBLE;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {Double} {
    Double d;
    try {
        d = Double.valueOf(yytext());
        if (d.isInfinite() || d.compareTo(new Double(0d)) == 0){
            lastToken = WDLToken.ERROR_DOUBLE_SIZE;
        } else {
            lastToken = WDLToken.LITERAL_DOUBLE;
        }
    } catch (NumberFormatException e){
        lastToken = WDLToken.ERROR_DOUBLE_SIZE;
    } 
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {Character} { 
    lastToken = WDLToken.LITERAL_CHARACTER;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {String} { 
    lastToken = WDLToken.LITERAL_STRING;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> ({WhiteSpace}+) { 
    lastToken = WDLToken.WHITE_SPACE;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {Comment} { 
    lastToken = WDLToken.COMMENT_HASH;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {UnclosedString} { 
    /* most of these errors have to be caught down near the end of the file.
     * This way, previous expressions of the same length have precedence.
     * This is really useful for catching anything bad by just allowing it 
     * to slip through the cracks. 
     */ 
    lastToken = WDLToken.ERROR_UNCLOSED_STRING;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {MalformedUnclosedString} { 
    lastToken = WDLToken.ERROR_MALFORMED_UNCLOSED_STRING;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {MalformedString} { 
    lastToken = WDLToken.ERROR_MALFORMED_STRING;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {MalformedCharacter} { 
    lastToken = WDLToken.ERROR_MALFORMED_CHARACTER;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}
<YYINITIAL> {ErrorIdentifier} { 
    lastToken = WDLToken.ERROR_IDENTIFIER;
    WDLToken t = (new WDLToken(lastToken, yytext(), yyline, yychar, yychar + yytext().length(), nextState));
    return (t);
}

[^]                              { log.log(Level.SEVERE, ("Can't highlight illegal character <"+
                                                        yytext()+"> at line " + this.yyline)); }
