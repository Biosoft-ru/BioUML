package biouml.plugins.wdl.parser.validator;

public class Field extends NamedPrototype
{
    String type;
    boolean canBeEmpty = true;
    String value;
    boolean isOptional = false;
    String alias;

    public Field(String name, NamedPrototype parent)
    {
        super(name, parent);
    }

    public Field(String name, String type, NamedPrototype parent)
    {
        this(name, parent);
        this.type = type;
    }

    public void setCanBeEmpty(boolean canBeEmpty)
    {
        this.canBeEmpty = canBeEmpty;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }
}