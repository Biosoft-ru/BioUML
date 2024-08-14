package biouml.plugins.antimony.astparser_v2;

import java.io.Reader;

/**
 * Abstract class for antimony parsers
 */
public abstract class Parser
{
    //protected abstract AstStart parse(Reader reader) throws ParseException;

    protected abstract AstEquation parseFormule(Reader reader) throws ParseException;

    protected abstract AstUnitFormula parseUnitFormule(Reader reader) throws ParseException;

}