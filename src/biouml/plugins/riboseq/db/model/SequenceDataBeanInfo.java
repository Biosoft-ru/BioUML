package biouml.plugins.riboseq.db.model;

import com.developmentontheedge.beans.BeanInfoEx;

import biouml.plugins.riboseq.db.editors.FormatSelector;

public class SequenceDataBeanInfo extends BeanInfoEx
{
    public SequenceDataBeanInfo()
    {
        super( SequenceData.class, true );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        property( "format" ).simple().editor( FormatSelector.class ).add();
        add( "url" );
    }
    
}
