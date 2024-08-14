package biouml.plugins.hemodynamics._test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;

/**
 * Container class which describes patient parameters.
 * @author Ilya
 *
 */
public class GenericPatientInfo
{
    public static final String TAB_DELIMITER = "\t";
    private Map<String, Object> data;
    private List<String> propertyNames;
    private String id;
    private boolean isValid = true;
    
    public boolean isValid()
    {
        return isValid;
    }
    
    public String getId()
    {
       return id; 
    }
    
    public GenericPatientInfo(String str, String[] propertyNames)
    {
        data = new HashMap<>();
        this.propertyNames = StreamEx.of(propertyNames).toList();
        String[] entries = str.split(SolodyannikovValidationTest.TAB_DELIMITER);
        
        //first should be title
        id = entries[0];

        for( int i = 1; i < entries.length; i++ )
            data.put(propertyNames[i], read(entries[i]));
    }
    
    public void addProoperty(String name, Double value)
    {
        if (data.containsKey(name))
            throw new IllegalArgumentException("Property "+name+" already exists in "+id);
        propertyNames.add(name);
        data.put(name, value);
    }
    
    public Object getValue(String name)
    {
        return data.get(name);
    }
    
    public double getDoubleValue(String name)
    {
        return (double)data.get(name);
    }

    public String getDescription()
    {
        return String.join(TAB_DELIMITER, propertyNames);
    }

    @Override
    public String toString()
    {
        return StreamEx.of(propertyNames.subList(1, propertyNames.size())).map(s -> data.get(s)).prepend(id).joining(SolodyannikovValidationTest.TAB_DELIMITER);
    }

    public static String[] readHeader(String str)
    {
        return str.split(TAB_DELIMITER);
    }
    
    private Double read(String str)
    {
        try
        {
            Double val = Double.parseDouble(str.replaceAll(",", "."));
            isValid = !Double.isNaN(val);
            return val;
        }
        catch( Exception ex )
        {
            isValid = false;
            return Double.NaN;
        }
    }
}