package biouml.plugins.chebi;

import biouml.standard.type.SubstanceBeanInfo;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ChebiMoleculeBeanInfo extends SubstanceBeanInfo
{
    public ChebiMoleculeBeanInfo()
    {
        super( ChebiMolecule.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property( new PropertyDescriptorEx( "structure", beanClass, "getStructureView", null ) ).canBeNull().readOnly().add();
    }

}
