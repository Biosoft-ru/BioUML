package biouml.model.dynamics;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.TagEditorSupport;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.standard.type.Compartment;
import biouml.standard.type.Unit;
import one.util.streamex.StreamEx;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class VariableRoleBeanInfo extends VariableBeanInfo
{
    public VariableRoleBeanInfo()
    {
        super(VariableRole.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        addHidden( "name" );
        add( "shortName" );
        property( new PropertyDescriptorEx( "compartment", beanClass, "getCompartment", null ) ).readOnly().add();
        
        add( "title" );
        addReadOnly("type");
        property("initialValue").numberFormat( BeanInfoConstants.NUMBER_FORMAT_NONE ).add();
        property("initialQuantityType").editor(QuantityTypeEditor.class).hidden("isCompartment").add();
        property("quantityType").editor(QuantityTypeEditor.class).hidden("isCompartment").add();
        property("outputQuantityType").editor(QuantityTypeEditor.class).hidden("isCompartment").add();       
        add("units", SubstanceUnitsEditor.class);
        property("boundaryCondition").hidden( "isCompartment" ).add();
        add("constant");
        add("comment");
    }

    public static class QuantityTypeEditor extends TagEditorSupport
    {
        public QuantityTypeEditor()
        {
            super(new String[] {"amount", "concentration"}, 0);
        }
    }

    public static class SubstanceUnitsEditor extends GenericComboBoxEditor
    {
        @Override
        public String[] getAvailableValues()
        {
            if( getBean() instanceof EModel )
                return getEmodelUnits( (EModel)getBean() );

            if( getBean() instanceof VariableRole )
            {
                DiagramElement de = ( (VariableRole)getBean() ).getDiagramElement();
                if( de.getKernel() instanceof Compartment )
                    return Unit.getBaseVolumeUnits().stream().toArray(String[]::new);
            }

            if( getBean() instanceof Variable )
            {
                Option parent = ( (Variable)getBean() ).getParent();
                if( parent instanceof EModel )
                    return getEmodelUnits( (EModel)parent );
                else if( parent instanceof DiagramElement )
                {
                    Diagram diagram = Diagram.getDiagram( (DiagramElement)parent );
                    if( diagram.getRole() instanceof EModel )
                        return getEmodelUnits( diagram.getRole( EModel.class ) );
                }
            }
            return new String[] {};
        }

        protected String[] getEmodelUnits(EModel emodel)
        {
            return StreamEx.ofKeys( emodel.getUnits() ).append( Unit.UNDEFINED ).append( Unit.getBaseUnitsList() ).toArray( String[]::new );
        }
    }
}
