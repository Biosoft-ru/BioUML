package biouml.standard.type;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class StructureBeanInfo extends ReferrerBeanInfo<Structure>
{
    public StructureBeanInfo()
    {
        super(Structure.class, "STRUCTURE");
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        PropertyDescriptorEx pde;

        property("moleculeReferences").htmlDisplayName("MR").add();
        property("format").htmlDisplayName("FT").add();
        property("data").htmlDisplayName("DA").add();
    }
}