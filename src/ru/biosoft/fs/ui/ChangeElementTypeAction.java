package ru.biosoft.fs.ui;

import com.developmentontheedge.application.Application;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.fs.FileSystemCollection;
import ru.biosoft.util.PropertiesDialog;
import ru.biosoft.util.bean.BeanInfoEx2;

@SuppressWarnings ( "serial" )
public class ChangeElementTypeAction extends AbstractElementAction
{

    @Override
    protected void performAction(DataElement de) throws Exception
    {
        ElementTypeOptions options = new ElementTypeOptions(de);
        FileSystemCollection parent = de.getOrigin().cast( FileSystemCollection.class );
        options.setType( parent.getElementType( de.getName() ) );
        PropertiesDialog propertiesDialog = new PropertiesDialog( Application.getApplicationFrame(), "Change element type", options );
        if(propertiesDialog.doModal())
        {
            parent.setElementType( de.getName(), options.getType() );
        }
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return DataCollectionUtils.checkPrimaryElementType( de.getOrigin(), FileSystemCollection.class );
    }
    
    public static class ElementTypeOptions
    {
        private String type;
        private final ru.biosoft.access.core.DataElement de;
        
        public ElementTypeOptions(DataElement de)
        {
            super();
            this.de = de;
        }

        public DataElement getDataElement()
        {
            return de;
        }

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }
    }
    
    public static class ElementTypeOptionsBeanInfo extends BeanInfoEx2<ElementTypeOptions>
    {
        public ElementTypeOptionsBeanInfo()
        {
            super(ElementTypeOptions.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            addWithTags( "type", bean -> FileSystemCollection.getAvailableTypes( bean.getDataElement() ).sorted() );
        }
    }
}
