package biouml.plugins.virtualcell.diagram;

import biouml.model.Role;

public interface DataOwner extends Role
{
    public String[] getNames();
}