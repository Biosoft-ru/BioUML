package biouml.plugins.physicell;

import com.developmentontheedge.beans.swing.table.RowModel;

public class SubstrateViewPart extends PhysicellTab
{
    public SubstrateViewPart(MulticellEModel emodel)
    {
        super( emodel );
    }

    @Override
    protected RowModel getRowModel()
    {
        return new ListRowModel( emodel.getSubstrates(), SubstrateProperties.class );
    }

    @Override
    protected Object createTemplate()
    {
        return new SubstrateProperties( "" );
    }
}