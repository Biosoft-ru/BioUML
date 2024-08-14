package biouml.plugins.research.workflow.items;

import ru.biosoft.util.TextUtil;

/**
 * Cycle which iterates over semicolon-separated list of values
 * @author lan
 */
public class EnumCycleType implements CycleType
{
    @Override
    public String getName()
    {
        return "List of values separated by semicolon";
    }
    
    private String[] parse(String expression)
    {
        if(expression == null) return new String[0];
        return TextUtil.split(expression, ';');
    }

    @Override
    public int getCount(String expression)
    {
        String[] values = parse(expression);
        return values.length;
    }

    @Override
    public String getValue(String expression, int number)
    {
        String[] values = parse(expression);
        if(values.length == 0) throw new IllegalArgumentException("Empty range: no iterations possible");
        if(number < 0 || number >= values.length) return null;
        return values[number];
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
