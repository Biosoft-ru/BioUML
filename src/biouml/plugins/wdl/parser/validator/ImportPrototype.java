package biouml.plugins.wdl.parser.validator;

public class ImportPrototype extends Scope
{
    String alias;

    public ImportPrototype(String name) throws Exception
    {
        super(ValidatorUtils.getWorkflowName(name));
    }

    public void setAliasName(String alias)
    {
        this.alias = alias;
    }

}
