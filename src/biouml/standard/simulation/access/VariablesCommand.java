package biouml.standard.simulation.access;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import java.util.logging.Logger;

import ru.biosoft.access.support.TagCommandSupport;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.simulation.SimulationResult;
import one.util.streamex.StreamEx;

public class VariablesCommand extends TagCommandSupport<SimulationResult>
{
    protected static final Logger log = Logger.getLogger( VariablesCommand.class.getName() );

    protected int varCount = -1;

    public VariablesCommand(TagEntryTransformer<SimulationResult> transformer)
    {
        super( "VR", transformer );
    }

    @Override
    public String getTaggedValue()
    {
        SimulationResult sr = transformer.getProcessedObject();
        StringBuilder result = new StringBuilder( "VR    time\t" );

        Map<String, Integer> variableMap = sr.getVariablePathMap();

        if( variableMap == null )
            return result.toString();
        Map<Integer, String> invertedVariableMap = invert(variableMap);
        String[] temp = new String[invertedVariableMap.size()];

        for( Map.Entry<Integer, String> entry : invertedVariableMap.entrySet() )
        {            
            int index = entry.getKey();
            if( index >= variableMap.size() )
            {
                log.log( Level.SEVERE, "Value " + index + " of index in variable map is out of bound." );
                return result.toString();
            }
            temp[entry.getKey()] = entry.getValue();
        }

        result.append( String.join( "\t", temp ) );
        return result.toString();
    }

    private Map<Integer, String> invert(Map<String, Integer> varMap)
    {
        Map<Integer, List<String>> temp = new HashMap<>();
        for( Entry<String, Integer> entry : varMap.entrySet() )
        {
            if (entry.getValue() != null)
            temp.computeIfAbsent( entry.getValue(), k -> new ArrayList<String>() ).add( entry.getKey() );
        }

        Map<Integer, String> result = new HashMap<>();
        for( Entry<Integer, List<String>> entry : temp.entrySet() )
            result.put( entry.getKey(), StreamEx.of( entry.getValue() ).sorted().joining( "," ) );
        return result;
    }

    @Override
    public void addValue(String string)
    {
        SimulationResult sr = transformer.getProcessedObject();
        StringTokenizer strtok = new StringTokenizer( string, "\t" );
        HashMap<String, Integer> variableMap = new HashMap<>();
        int counter = 0;

        // skip "time" token
        if( strtok.hasMoreTokens() )
        {
            strtok.nextToken();
        }

        while( strtok.hasMoreTokens() )
        {
            String str = strtok.nextToken();
            if( str.contains( "," ) ) //multiple variables for single index
            {
                for( String subStr : str.split( "," ) )
                    variableMap.put( subStr, counter );
                counter++;
            }
            else
                variableMap.put( str, counter++ );
        }

        sr.setVariableMap( variableMap );
        sr.setVariablePathMap( variableMap );

        varCount = counter;
    }

    public int getVarCount()
    {
        return varCount;
    }
}
