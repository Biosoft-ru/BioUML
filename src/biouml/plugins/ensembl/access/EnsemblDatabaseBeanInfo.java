package biouml.plugins.ensembl.access;

import com.developmentontheedge.beans.BeanInfoEx;

public class EnsemblDatabaseBeanInfo extends BeanInfoEx
{
    public EnsemblDatabaseBeanInfo()
    {
        super( EnsemblDatabase.class, true );
        setSimple( true );
        setHideChildren( true );
        setBeanEditor( EnsemblDatabaseSelector.class );
    }
}