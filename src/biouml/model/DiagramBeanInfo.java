package biouml.model;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class DiagramBeanInfo extends CompartmentBeanInfo
{
    public DiagramBeanInfo()
    {
        super(Diagram.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        add(1, new PropertyDescriptorEx("digramType", beanClass, "getDiagramType", null));
        add(2, new PropertyDescriptorEx("layouter", beanClass, "getLayouterName", null));
        property( "currentStateName" ).editor( StateNamesEditor.class ).add( 3 );
        addHidden( "description" );
        properties.removeIf( p -> p.getName().equals( "shapeSize" ) || p.getName().equals( "shapeType" ) );
        add( "hideInvisibleElements" );
    }

    public static class StateNamesEditor extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ( (Diagram)getBean() ).getStateNames().toArray( new String[0] );
        }
    }

    //    public static class StateNamesEditor extends CustomEditorSupport
    //    {
    //        private final JComboBox<String> namesBox;
    //        public StateNamesEditor()
    //        {
    //            namesBox = new JComboBox<>();
    //        }
    //
    //        private Component createComponent()
    //        {
    //            List<String> names = ( (Diagram)getBean() ).getStateNames();
    //            names.forEach( namesBox::addItem );
    //            namesBox.setSelectedItem( ( (Diagram)getBean() ).getCurrentStateName());
    //
    //            namesBox.addActionListener(ae -> doSet());
    //
    //            return namesBox;
    //        }
    //
    //        public void doSet()
    //        {
    //            String value = namesBox.getSelectedItem().toString();
    //            ( (Diagram)getBean() ).setCurrentStateName(value);
    //            setValue(value);
    //        }
    //
    //        @Override
    //        public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
    //        {
    //            return createComponent();
    //        }
    //        @Override
    //        public Component getCustomEditor(Component parent, boolean isSelected)
    //        {
    //            return createComponent();
    //        }
    //    }

}
