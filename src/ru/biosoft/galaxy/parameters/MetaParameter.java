package ru.biosoft.galaxy.parameters;

/**
 * Metadata element
 */
public class MetaParameter
{
    protected String name;
    protected Object value;
    protected String type;
    protected String description;

    public MetaParameter(String name, Object value, String type, String description)
    {
        this.name = name;
        this.value = value;
        this.type = type;
        this.description = description;
    }

    public String getName()
    {
        return name;
    }

    public Object getValue()
    {
        return value;
    }

    public String getType()
    {
        return type;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public MetaParameter clone()
    {
        return new MetaParameter(name, value, type, description);
    }
}
