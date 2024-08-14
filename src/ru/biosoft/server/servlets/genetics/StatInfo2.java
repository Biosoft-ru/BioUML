package ru.biosoft.server.servlets.genetics;

import java.util.HashMap;
import java.util.Map;

/**
 * Table column statistic bean for not numeric columns
 */
public class StatInfo2
{
    public int number;
    protected Map<String, Integer> values;

    public StatInfo2()
    {
        this.number = 0;
        this.values = new HashMap<>();
    }

    public int getNumber()
    {
        return number;
    }

    public Map<String, Integer> getValues()
    {
        return values;
    }

    public void addValue(String value)
    {
        Integer vCount = values.get(value);
        if( vCount == null )
        {
            values.put(value, 1);
        }
        else
        {
            values.put(value, vCount.intValue() + 1);
        }
    }
}
