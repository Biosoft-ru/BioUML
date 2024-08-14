package biouml.plugins.chebi;

import biouml.standard.StandardModuleType;

public class ChebiModuleType extends StandardModuleType
{
    @Override
    public boolean canCreateEmptyModule()
    {
        return false;
    }

    public ChebiModuleType()
    {
        super("ChEBI");
    }
}
