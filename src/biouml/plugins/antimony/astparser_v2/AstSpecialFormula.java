/* Generated By:JJTree: Do not edit this line. AstSpecialFormula.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=true,NODE_PREFIX=Ast,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package biouml.plugins.antimony.astparser_v2;

import biouml.plugins.antimony.astparser.AntimonyParser;

public class AstSpecialFormula extends AstFormulaElement
{
    public final static String POWER = "power";
    public final static String OR = "or";
    public final static String AND = "and";
    public final static String GEQ = "geq";
    public final static String LEQ = "leq";
    public final static String PIECEWISE = "piecewise";

    public final static String PIECEWISE_ARROW = "piecewise_arrow";

    public AstSpecialFormula(int id)
    {
        super(id);
    }

    public AstSpecialFormula(AntimonyNotationParser p, int id)
    {
        super(p, id);
    }

    //power, piecewise and etc.
    String type = "";

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    public String toAntimonyString()
    {
        return "";
    }

    @Override
    public String toString()
    {
        StringBuilder formula = new StringBuilder("");
        if( OR.equals(type) || AND.equals(type) || GEQ.equals(type) || LEQ.equals(type) )
        {
            for( int i = 0; i < jjtGetNumChildren(); i++ )
            {
                if( jjtGetChild(i) instanceof AstSpecialFormula )
                    formula.append(jjtGetChild(i).toString());
                else if( isComma(jjtGetChild(i)) )
                    if( AND.equals(type) )
                        formula.append("&&");
                    else if( OR.equals(type) )
                        formula.append("||");
                    else if( GEQ.equals(type) )
                        formula.append(">=");
                    else if( LEQ.equals(type) )
                        formula.append("<=");
            }
        }
        else if( POWER.equals(type) )
        {
            formula.append("(");
            for( int i = 0; i < jjtGetNumChildren(); i++ )
            {
                if( jjtGetChild(i) instanceof AstSpecialFormula )
                    formula.append(jjtGetChild(i).toString());
                else if( isComma(jjtGetChild(i)) )
                    formula.append(")^(");
            }
            formula.append(")");
        }
        else if( PIECEWISE.equals(type) )
        {
            // add "piecewise"
            formula.append(jjtGetChild(0).toString());
            // add "("
            formula.append(jjtGetChild(1).toString());

            int i = 2;
            int length = jjtGetNumChildren() - 1;
            while( i < length )
            {
                Node result = jjtGetChild(i);
                Node comma = jjtGetChild(i + 1);
                Node condition = jjtGetChild(i + 2);
                if( result instanceof AstSpecialFormula && isComma(comma) && condition instanceof AstSpecialFormula )
                {
                    if( i > 2 )
                        formula.append("; ");
                    formula.append(condition + "=>" + result);
                    i += 4;
                }
                else if( result instanceof AstSpecialFormula && !isComma(comma) )
                {
                    if( i > 2 )
                        formula.append("; ");
                    formula.append(result);
                    break;
                }
                else
                    return "Incorrect formula";
            }
            // add ")"
            formula.append(jjtGetChild(length).toString());
        }
        else
        {
            for( int i = 0; i < jjtGetNumChildren(); i++ )
            {
                formula.append(jjtGetChild(i).toString());
            }
        }
        return formula.toString();
    }

    public AstEquation convertToEquation()
    {
        AstEquation equation = new AstEquation(AntimonyParser.JJTEQUATION);
        if( OR.equals(type) || AND.equals(type) || GEQ.equals(type) || LEQ.equals(type) )
        {
            for( int i = 0; i < jjtGetNumChildren(); i++ )
            {
                if( isComma(jjtGetChild(i)) )
                {
                    if( AND.equals(type) )
                        addRegularElement("&&", equation);
                    else if( OR.equals(type) )
                        addRegularElement("||", equation);
                    else if( GEQ.equals(type) )
                        addRegularElement(">=", equation);
                    else if( LEQ.equals(type) )
                        addRegularElement("<=", equation);
                }
                else if( jjtGetChild(i) instanceof AstSpecialFormula )
                    equation.jjtAddChild(jjtGetChild(i), equation.jjtGetNumChildren());
            }
        }
        else if( POWER.equals(type) )
        {
            addRegularElement("(", equation);
            for( int i = 0; i < jjtGetNumChildren(); i++ )
            {
                if( jjtGetChild(i) instanceof AstSpecialFormula )
                    equation.jjtAddChild(jjtGetChild(i), equation.jjtGetNumChildren());
                else if( isComma(jjtGetChild(i)) )
                    addRegularElement(")^(", equation);
            }
            addRegularElement(")", equation);
        }
        else if( PIECEWISE.equals(type) )
        {
            // add "piecewise"
            equation.jjtAddChild(jjtGetChild(0), 0);
            // add "("
            equation.jjtAddChild(jjtGetChild(1), 1);

            int i = 2;
            int length = jjtGetNumChildren() - 1;
            while( i < length )
            {
                Node result = jjtGetChild(i);
                Node comma = jjtGetChild(i + 1);
                Node condition = jjtGetChild(i + 2);
                if( result instanceof AstSpecialFormula && isComma(comma) && condition instanceof AstSpecialFormula )
                {
                    if( i > 2 )
                        addRegularElement(";", equation);
                    equation.jjtAddChild(condition, equation.jjtGetNumChildren());
                    addRegularElement("=>", equation);
                    equation.jjtAddChild(result, equation.jjtGetNumChildren());
                    i += 4;
                }
                else if( result instanceof AstSpecialFormula && !isComma(comma) )
                {
                    if( i > 2 )
                        addRegularElement(";", equation);
                    equation.jjtAddChild(result, equation.jjtGetNumChildren());
                    break;
                }
                else
                    throw new IllegalArgumentException("Incorrect piecewise formula");
            }
            // add ")"
            equation.jjtAddChild(jjtGetChild(length), equation.jjtGetNumChildren());
        }
        else
        {
            for( int i = 0; i < jjtGetNumChildren(); i++ )
            {
                equation.jjtAddChild(jjtGetChild(i), i);
            }
        }
        return equation;
    }
    private void addRegularElement(String value, AstEquation parent)
    {
        AstRegularFormulaElement element = new AstRegularFormulaElement(AntimonyParser.JJTREGULARFORMULAELEMENT);
        element.setElement(value);

        parent.addAsLast(element);
    }

    private boolean isComma(Node element)
    {
        return element instanceof AstRegularFormulaElement && ( (AstRegularFormulaElement)element ).toString().equals(",");
    }
}
/* JavaCC - OriginalChecksum=0ac7ccb7f52b4cda8b4be2fd3165c4d0 (do not edit this line) */