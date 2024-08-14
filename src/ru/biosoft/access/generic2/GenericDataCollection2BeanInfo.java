package ru.biosoft.access.generic2;

import ru.biosoft.access.generic.TableImplementationRecord;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.application.Application;

public class GenericDataCollection2BeanInfo extends BeanInfoEx2<GenericDataCollection2>
{
    public GenericDataCollection2BeanInfo()
    {
        super(GenericDataCollection2.class );
    }
    
    @Override
    public void initProperties() throws Exception
    {
        if(Application.getApplicationFrame() != null)
        {
            property( "preferedTableImplementation" ).editor( TableImplementationRecord.TableImplementationSelector.class ).simple().add();
            
            add("databaseURL");
            add("databaseUser");
            add("databasePassword");
        }
        addHidden("description");
    }

}
