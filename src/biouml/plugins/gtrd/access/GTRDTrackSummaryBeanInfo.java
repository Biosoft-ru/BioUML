package biouml.plugins.gtrd.access;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.util.bean.BeanInfoEx2;

public class GTRDTrackSummaryBeanInfo extends BeanInfoEx2<GTRDTrackSummary>
{
    public GTRDTrackSummaryBeanInfo()
    {
        super( GTRDTrackSummary.class );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        add( new PropertyDescriptorEx( "name", beanClass, "getName", null ) );
        property("target").hidden( "isTargetHidden" ).add();
        property("cellName").hidden( "isCellNameHidden" ).add();
        property("treatment").hidden( "isTreatmentHidden" ).add();
        add( new PropertyDescriptorEx( "size", beanClass, "getSize", null ) );
        add( new PropertyDescriptorEx( "path", beanClass, "getPath", null ) );
    }
}
