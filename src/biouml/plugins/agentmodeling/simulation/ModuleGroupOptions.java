package biouml.plugins.agentmodeling.simulation;

import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName ( "Module Group Options" )
public class ModuleGroupOptions
{
    private String name;
    private String[] available;
    private String[] subdiagrams = new String[0];

    public ModuleGroupOptions(String name, String[] available)
    {
        this.name = name;
        this.available = available;
    }

    @PropertyName ( "Name" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName ( "SubDiagrams" )
    public String[] getSubdiagrams()
    {
        return subdiagrams;
    }
    public void setSubdiagrams(String[] subdiagrams)
    {
        this.subdiagrams = subdiagrams;
    }

    public String[] getAvailableSubDiagrams()
    {
        return available;
    }
}