package biouml.plugins.antimony.astparser;

public abstract class AstFormulaElement extends SimpleNode
{

    public AstFormulaElement(int id)
    {
        super(id);
    }

    public AstFormulaElement(AntimonyParser p, int id)
    {
        super(p, id);
    }


    abstract public String toString();

    public boolean isNumber()
    {
        return false;
    }

    public boolean isString()
    {
        return false;
    }
}