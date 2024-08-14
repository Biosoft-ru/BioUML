package ru.biosoft.templates;

/**
 * Filter for {@link TemplateInfo} by object property.
 * Is used by {@link TemplateFilter}
 */
public class PropertyFilter
{
    protected String propertyName;
    protected String value;
    protected String className;
    protected boolean isRegexp;

    public PropertyFilter(String propertyName, String value, String className, boolean isRegexp)
    {
        this.propertyName = propertyName;
        this.value = value;
        this.className = className;
        this.isRegexp = isRegexp;
    }

    public String getPropertyName()
    {
        return propertyName;
    }

    public String getValue()
    {
        return value;
    }

    public String getClassName()
    {
        return className;
    }

    public boolean isRegexp()
    {
        return isRegexp;
    }
}
