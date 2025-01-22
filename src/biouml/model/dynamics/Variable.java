package biouml.model.dynamics;

import java.beans.PropertyChangeEvent;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.dynamics.util.EModelHelper;
import biouml.standard.type.Unit;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElementSupport;

/**
 * Model variable.
 */
@PropertyName ( "Parameter" )
@PropertyDescription ( "Model parameter." )
public class Variable extends MutableDataElementSupport
{
    public static final String TYPE_CALCULATED = "Calculated";
    public static final String TYPE_ALGEBRAIC = "Algebraic";
    public static final String TYPE_PARAMETER = "Parameter";
    public static final String TYPE_DIFFERENTIAL = "Differential";
    public static final String TYPE_DISCRETE = "Discrete";
    public static final String TYPE_UNUSED = "Not used";
    public static final String TYPE_TIME = "Time";

    protected double initialValue;
    protected DynamicPropertySet attributes;
    protected String units = Unit.UNDEFINED;
    protected boolean showInPlot = false;
    protected boolean constant;
    protected String comment;
    private String title;
    private String type = TYPE_UNUSED;

    protected Variable(String name, DataCollection<?> origin)
    {
        super( origin, name );
    }

    public Variable(String name, EModel emodel, DataCollection<?> origin)
    {
        this( emodel == null ? name : emodel.getDiagramElement().getType().getSemanticController().validateName( name ), origin );
        setTitle( name );
        setParent( emodel, "vars/" + name );
    }

    @Override
    @PropertyName ( "Name" )
    @PropertyDescription ( "Parameter name." )
    public String getName()
    {
        return super.getName();
    }
    /** Warning: for internal usage only. */
    public void setName(String name)
    {
        if( "time".equals( this.name ) || name.equals( this.name ) )
            return;

        String oldName = this.name;

        if( getParent() instanceof EModel )
        {
            EModel emodel = (EModel)getParent();
            name = emodel.getDiagramElement().getType().getSemanticController().validateName( name );
            EModelHelper helper = new EModelHelper( emodel );
            helper.renameVariable( oldName, name );
            try
            {
                ( (EModel)getParent() ).getVariables().remove( oldName );
            }
            catch( Exception ex )
            {
            }
        }
    }

    @PropertyName ( "Initial value" )
    @PropertyDescription ( "Initial value of the variable." )
    public double getInitialValue()
    {
        return initialValue;
    }
    public void setInitialValue(double initialValue)
    {
        if( "time".equals( this.name ) )
            return;
        double oldValue = this.initialValue;
        this.initialValue = initialValue;
        firePropertyChange( "initialValue", oldValue, initialValue );
    }

    @PropertyName ( "Units" )
    @PropertyDescription ( "Units." )
    public String getUnits()
    {
        return units;
    }
    public void setUnits(String units)
    {
        String oldValue = this.units;
        this.units = units;
        firePropertyChange( "units", oldValue, units );
    }

    @PropertyName ( "Constant" )
    @PropertyDescription ( "If true then variable value is fixed and can not ba changed by any equation or event." )
    public boolean isConstant()
    {
        return constant;
    }
    public void setConstant(boolean constant)
    {
        if( "time".equals( this.name ) )
            return;
        Object oldValue = this.constant;
        this.constant = constant;
        firePropertyChange( "constant", oldValue, constant );
    }

    @PropertyName ( "Comment" )
    @PropertyDescription ( "Comment." )
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        String oldValue = this.comment;
        this.comment = comment;
        firePropertyChange( "comment", oldValue, comment );
    }

    @PropertyName ( "Type" )
    @PropertyDescription ( "Variable type." )
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    public String toString()
    {
        String str = "Variable: " + name + "=" + initialValue + " " + units;
        if( comment != null )
            str = str + "; " + comment;
        return str;
    }

    @Override
    public Object clone()
    {
        return clone( name );
    }

    public Variable clone(String newName)
    {
        Variable var = (Variable)internalClone( getOrigin(), newName );
        var.type = type;
        var.attributes = null;
        if( attributes != null )
        {
            for( DynamicProperty oldProp : attributes )
            {
                DynamicProperty prop = null;
                try
                {
                    prop = DynamicPropertySetSupport.cloneProperty( oldProp );
                }
                catch( Exception e )
                {
                    prop = oldProp;
                }
                var.getAttributes().add( prop );
            }
        }
        return var;
    }

    public DynamicPropertySet getAttributes()
    {
        if( attributes == null )
        {
            attributes = new DynamicPropertySetAsMap();
            attributes.addPropertyChangeListener( e -> {
                firePropertyChange(
                        new PropertyChangeEvent( this, "attributes/" + e.getPropertyName(), e.getOldValue(), e.getNewValue() ) );
            } );
        }
        return attributes;
    }

    @PropertyName ( "Title" )
    @PropertyDescription ( "Title" )
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
    }
}