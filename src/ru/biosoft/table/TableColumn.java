package ru.biosoft.table;

import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil2;

/**
 *
 */
@SuppressWarnings ( "serial" )
public class TableColumn extends ColumnEx
{
    public static final String EXPRESSION_LOCKED = "expressionLocked";
    public static final String ANNOTATION_SOURCE_PROPERTY = "annotationSource";
    private String displayName;
    private String shortDesc;
    private DataType type;
    private Nature nature = Nature.NONE;
    private String expression;
    private Sample sample;

    public TableColumn(String name, String displayName, String shortDesc, DataType type, String expression)
    {
        this(name, name, displayName, shortDesc, type, expression);
    }

    public TableColumn(String key, String name, String displayName, String shortDesc, DataType type, String expression)
    {
        super(null, key, name, true);

        this.displayName = displayName;
        this.shortDesc = shortDesc;
        this.type = type;
        this.expression = expression;
    }

    public TableColumn(String name, Class<?> valueClass, String expression)
    {
        this(name, name, name, DataType.fromClass( valueClass ), expression);
    }

    public TableColumn(String name, Class<?> valueClass)
    {
        this(name, name, name, DataType.fromClass( valueClass ), null);
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public String getShortDescription()
    {
        return shortDesc;
    }

    public void setShortDescription(String shortDesc)
    {
        this.shortDesc = shortDesc;
    }
    
    public Class<?> getValueClass()
    {
        return type.getType();
    }
    
    public void setValueClass(Class<?> valueClass)
    {
        setType(DataType.fromClass( valueClass ));
    }

    public DataType getType()
    {
        return type;
    }

    public void setType(DataType type)
    {
        this.type = type;
    }

    public Nature getNature()
    {
        return nature;
    }

    public void setNature(Nature nature)
    {
        this.nature = nature;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public void setExpressionLocked(boolean locked)
    {
        setValue(EXPRESSION_LOCKED, locked ? "true" : "false");
    }

    public boolean isExpressionLocked()
    {
        return getValue(EXPRESSION_LOCKED) != null && getValue(EXPRESSION_LOCKED).equals("true");
    }

    public boolean isExpressionEmpty()
    {
        return TextUtil2.isEmpty(this.expression);
    }

    public boolean isHidden()
    {
        return !super.getEnabled();
    }

    public void setHidden(boolean hidden)
    {
        super.setEnabled( !hidden);
    }

    /**
     * Converts text representation to corresponding type
     * @param input
     * @return
     */
    public Object convert(String input)
    {
        Object result = input;

        if( this.getType() == DataType.Float )
        {
            result = Double.parseDouble(input);
        }
        else if( this.getType() == DataType.Integer )
        {
            result = Integer.parseInt(input);
        }
        else
        // Text
        {
        }

        return result;
    }

    public Sample getSample()
    {
        return sample;
    }

    public void setSample(Sample sample)
    {
        this.sample = sample;
    }

    public static enum Nature
    {
        NONE, DBREF, SAMPLE;
    }
}