package biouml.standard.simulation.access;

import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.support.TagCommandSupport;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.model.dynamics.Variable;
import biouml.standard.simulation.SimulationResult;

public class InitialValuesCommand extends TagCommandSupport<SimulationResult>
{
    protected static final Logger log = Logger.getLogger(InitialValuesCommand.class.getName());

    public InitialValuesCommand(TagEntryTransformer<SimulationResult> transformer)
    {
        super("IV", transformer);
    }

    @Override
    public String getTaggedValue()
    {
        String endl = System.getProperty("line.separator");
        SimulationResult sr = transformer.getProcessedObject();
        List<Variable> initialValues = sr.getInitialValues();
        StringBuffer result = new StringBuffer();
        if( initialValues == null )
            return result.toString();
        for(Variable var : initialValues)
        {
            result.append("IV    " + var.getName() + '\t' + var.getInitialValue() + '\t' + replaceSpaces(var.getUnits()) + endl);
        }
        return result.toString();
    }

    @Override
    public void addValue(String string)
    {
        SimulationResult sr = transformer.getProcessedObject();
        StringTokenizer strtok = new StringTokenizer(string, "\t" );

        try
        {
            int countTokens = strtok.countTokens();
            if( countTokens < 2 )
            {
                log.log(Level.SEVERE, "Not enough tokens at line " + string);
                return;
            }
            else if( countTokens > 3 )
                log.warning("Too many tokens at line " + string);

            String name = strtok.nextToken();
            String value = strtok.nextToken();
            String units = countTokens > 2 ? replaceSpacesBack(strtok.nextToken()) : null;

            Variable var = new Variable(name, null, null);
            var.setInitialValue(Double.parseDouble(value));
            var.setUnits(units);
            sr.addInitialValue(var);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Syntax error: could not parse value in string " + ex);
        }
    }

    private String replaceSpaces(String str)
    {
        if(str == null) return str;
        String result = str.replaceAll(" ", "_");
        if( result.equals("") )
        {
            result = "_";
        }
        return result;
    }

    private String replaceSpacesBack(String str)
    {
        String result = str;
        if( result.equals("_") )
        {
            result = "";
        }
        result = result.replaceAll("_", " ");
        return result;
    }
}
