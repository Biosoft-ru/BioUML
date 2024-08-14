package biouml.plugins.test.access;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.dialog.OkCancelDialog;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.swing.PropertyInspector;

import biouml.plugins.test.TestModel;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.gui.DocumentManager;

/**
 * Action to create new test document
 */
public class NewTestDocumentAction extends AbstractElementAction
{

    @Override
    protected void performAction(DataElement de) throws Exception
    {
        DataCollection dc = (DataCollection)de;
        TestOptions options = new TestOptions( "New test", null );
        OkCancelDialog dialog = createPropertiesDialog( options );
        if( dialog.doModal() )
        {
            TestModel testModel = new TestModel( dc, options.getName(), options.getPath() );
            dc.put( testModel );
            DocumentManager.getDocumentManager().openDocument( testModel );
        }
    }

    protected OkCancelDialog createPropertiesDialog(Object properties)
    {
        PropertyInspector propertyInspector = new PropertyInspector();
        propertyInspector.explore( properties );
        OkCancelDialog dialog = new OkCancelDialog( Application.getApplicationFrame(), "New test document" );
        dialog.add( propertyInspector );
        return dialog;
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return (de instanceof DataCollection) && DataCollectionUtils.isAcceptable((DataCollection<?>)de, TestModel.class);
    }

    @PropertyName( "Test document oprions" )
    public static class TestOptions
    {
        private String name;
        private DataElementPath path;

        public TestOptions(String name, DataElementPath path)
        {
            this.name = name;
            this.path = path;
        }

        @PropertyName("Name")
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }

        @PropertyName("Diagram")
        public DataElementPath getPath()
        {
            return path;
        }
        public void setPath(DataElementPath path)
        {
            this.path = path;
        }
    }

    public static class TestOptionsBeanInfo extends BeanInfoEx
    {
        public TestOptionsBeanInfo()
        {
            super( TestOptions.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "name" );
            add( "path" );
        }
    }
}