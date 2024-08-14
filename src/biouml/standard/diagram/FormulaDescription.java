package biouml.standard.diagram;

public class FormulaDescription
{
    protected String name;
    protected String description;
    protected String math;
    public FormulaDescription(String name, String description, String math)
    {
        this.name = name;
        this.description = description;
        this.math = math;
    }
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }
    public String getMath()
    {
        return math;
    }
    public void setMath(String math)
    {
        this.math = math;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
}
