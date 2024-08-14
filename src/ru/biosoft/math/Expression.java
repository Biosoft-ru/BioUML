
package ru.biosoft.math;

import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Node;

import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.ParserContext;
import ru.biosoft.math.parser.Parser;

import com.developmentontheedge.beans.Option;

/**
 * Mathematical expression.
 *
 * Expression has two presentations:
 * <ol>
 *  <li> text - text string using linear syntaxes; </li>
 *  <li> ast - AST tree that corresponds to parsed expression. </li>
 * </ol>
 */
public class Expression extends Option
{
    protected static final Logger log = Logger.getLogger(Expression.class.getName());

    /** Constructs new expression from linear syntax or MathML text. */
    public Expression(Option parent, String text)
    {
        super(parent);

        if( text.startsWith("<math") )
            parseExpression(text);
        else
            this.text = text;
    }

    /** Constructs new expression from linear syntax or MathML text. */
    public Expression(Option parent, Node mathElement)
    {
        super(parent);
        parseExpression(mathElement);
    }

    protected String text;
    public String getText()
    {
        return text;
    }
    public void setText(String text)
    {
        String oldValue = this.text;
        parseExpression(text);
        firePropertyChange("text", oldValue, text);
    }

    protected AstStart astStart;
    public AstStart getAstStart()
    {
        return astStart;
    }
    public void setAstStart(AstStart astStart)
    {
        this.astStart = astStart;
    }

    protected ParserContext context;
    public ParserContext getParserContext()
    {
        if( context == null )
            context = new DefaultParserContext();

        return context;
    }

    public void setParserContext(ParserContext context)
    {
        this.context = context;
    }

    ///////////////////////////////////////////////////////////////////
    //
    //

    private Parser linearParser;

    /**
     * @pending - log messages.
     */
    public void parseExpression(String text)
    {
        this.text = text;

        try
        {
            if( text.startsWith("<math") )
            {
//                astStart = MathMLParser.buildASTree(text);
//                text = LinearFormatter.generate(astStart);
            }
            else
            {
                StringReader expr = new StringReader(text);

                if( linearParser == null )
                    linearParser = new Parser(expr);
                else
                    linearParser.ReInit(expr);

                astStart = linearParser.Start();
            }
        }
        catch(Throwable t)
        {
            log.log(Level.SEVERE, "There were errors during expression processing, expression='" + text + "', error: " + t, t);
        }
    }

    public void parseExpression(Node math)
    {
/*        try
        {
            astStart = LinearParser.buildAstTree(text);
            text = LinearFormatter.generate(astStart);
        }
        catch(Throwable t)
        {
            log.log(Level.SEVERE, "Can not process expression '" + expression + "', error: " + t, t);
        }
*/
    }
}


