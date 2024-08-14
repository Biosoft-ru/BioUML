package biouml.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.PropertyChangeObservable;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.dynamics.FormulaDelegate;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.standard.type.Base;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.MutableDataElementSupport;
import ru.biosoft.exception.LoggedClassCastException;
import ru.biosoft.graphics.View;
import ru.biosoft.util.WeakPropertyChangeForwarder;

/**
 * Common definition of diagram element.
 *
 * Diagram element is wrapper for some data element - kernel. This kernel is
 * storing all specific data.
 *
 * Arbitrary attributes can be associated with diagram element using <code>DynamicPropertySet</code>.
 *
 * @pending use <code>CompositeView</code> instead of <code>View</code>.
 */
public abstract class DiagramElement extends MutableDataElementSupport
{
    private PropertyChangeListener listener;
    private String predefinedStyle = DiagramElementStyle.STYLE_DEFAULT;
    private DiagramElementStyle customStyle = new DiagramElementStyle(this);

    /** Indicates whether this node location should be preserved during layout. */
    protected boolean fixed;
    protected Base kernel;

    /** The <code>DiagramElement</code> view. */
    protected volatile View view;

    /** Any {@link Role} can be associated with a <code>DiagramElement</code>.*/
    protected Role role;

    /** Any comment can be associated with a <code>DiagramElement</code>.*/
    protected String comment;

    /** The <code>DiagramElement</code> title. By default title is kernel element name. However a user can change the diagram element title.*/
    protected String title;

    protected DynamicPropertySet attributes;

    /**
     * Constructs the diagram element.
     *
     * @param parent the parent compartment
     * @param name the diagram element name
     * @kernel the data element storing specific data.
     */
    public DiagramElement(DataCollection parent, String name, final Base kernel)
    {
        super(parent, name);
        this.kernel = kernel;

        title = kernel != null ? kernel.getTitle() : name;

        if( kernel instanceof PropertyChangeObservable )
        {
            listener = evt -> {
                evt.setPropagationId(kernel);
                firePropertyChange(evt);
            };
            new WeakPropertyChangeForwarder(listener, (PropertyChangeObservable)kernel);
        }
    }

    /**
     * Constructs the diagram element.
     *
     * @param parent the parent compartment
     * @kernel the data element storing specific data.
     */
    public DiagramElement(DataCollection parent, Base kernel)
    {
        this(parent, kernel.getName(), kernel);
    }

    @PropertyName ( "Data" )
    @PropertyDescription ( "Digram element is wrapper for some data entry. This data entry is storing all specific data from the database." )
    public Base getKernel()
    {
        return kernel;
    }
    public void setKernel(Base kernel)
    {
        throw new UnsupportedOperationException("In diagram element kernel can not be replaced.");
    }

    @PropertyName ( "Attributes" )
    @PropertyDescription ( "Dynamic set of attributes.<br>This attributes can be added:<br><ul>"
            + "<li>during mapping of information from a database into Java objects<li>by plug-in for some specific usage"
            + "<li>by customer to store some specific information formally<li>during import of experimental data</ul>" )
    public DynamicPropertySet getAttributes()
    {
        if( attributes == null )
        {
            attributes = new DynamicPropertySetAsMap();
            attributes.addPropertyChangeListener(evt -> {
                firePropertyChange(new PropertyChangeEvent(DiagramElement.this, "attributes/" + evt.getPropertyName(), evt.getOldValue(),
                        evt.getNewValue()));
            });
        }
        return attributes;
    }

    public void originChanged(Object oldValue, Object newValue)
    {
        this.firePropertyChange("origin", oldValue, newValue);
    }

    /** @returns the <code>DiagramElement</code> view.  */
    public View getView()
    {
        return view;
    }
    /** Normally this function should be called by <code>DiagramViewBuilder</code>.*/
    public void setView(View view)
    {
        this.view = view;
    }

    @PropertyName ( "Title" )
    @PropertyDescription ( "The diagram element title.<br>By default title is data entry name. However a user can change the diagram element title." )
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        if( Objects.equals(title, this.title) )
            return;
        String oldValue = this.title;
        this.title = title;
        this.firePropertyChange("title", oldValue, title);
    }

    @PropertyName ( "Comment" )
    @PropertyDescription ( "Arbitrary text comment." )
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        if( Objects.equals(comment, this.comment) )
            return;
        String oldValue = this.comment;
        this.comment = comment;
        this.firePropertyChange("comment", oldValue, comment);
    }

    @PropertyName ( "Role" )
    @PropertyDescription ( "The diagram element role in executable model e.g. variable, equation etc." )
    public @CheckForNull Role getRole()
    {
        return role;
    }
    public @Nonnull <T extends Role> T getRole(@Nonnull
    Class<T> clazz) throws DataElementReadException
    {
        Role r = role;
        if( r == null )
        {
            throw new DataElementReadException(this, "role");
        }
        if( !clazz.isInstance(r) )
        {
            throw new DataElementReadException(new LoggedClassCastException(r.getClass().getName(), clazz.getName()), this, "role");
        }
        return (T)r;
    }
    public void setRole(Role role)
    {
        Role oldValue = this.role;
        this.role = role;
        this.firePropertyChange("role", oldValue, role);
    }

    public DiagramElement clone(Compartment newParent, String newName)
    {
        return null;
    }

    // //////////////////////////////////////////////////////////////////////////
    // Metadata issues
    //

    public boolean isRoleHidden()
    {
        return role == null || kernel instanceof FormulaDelegate;
    }

    /**
     *  Returns the full name of diagram element. For example, "comp1.comp2.name"
     */
    public String getCompleteNameInDiagram()
    {
        Diagram diagram = Diagram.getDiagram(this);
        return StreamEx.iterate(this, DataElement::getOrigin).takeWhile(dc -> dc != null && dc != diagram)
                .map(ru.biosoft.access.core.DataElement::getName).foldLeft((str, name) -> name + "." + str).get();
    }

    @Override
    protected void firePropertyChange(PropertyChangeEvent evt)
    {

        if( shouldFire(evt) )
            super.firePropertyChange(evt);
    }

    /**
     * @return true if this event should be fired
     */
    protected boolean shouldFire(PropertyChangeEvent evt)
    {
        if( evt.getSource() == this )
            return true;
        Object propagationSource = evt.getPropagationId();
        if( propagationSource != null ) // Do not accept propagation events from non-children
        {
            if( propagationSource == getRole() || propagationSource == getKernel() )
                return true;

            try
            {
                if( ! ( propagationSource instanceof VarColumn )
                        && ( ! ( propagationSource instanceof DataElement ) || ! ( this instanceof DataCollection )
                                || propagationSource != ( (DataCollection)this ).get( ( (DataElement)propagationSource ).getName()) ) )
                    return false;
            }
            catch( Exception e )
            {
                return false;
            }
        }
        return true;
    }

    public void save() throws Exception
    {
        getOrigin().put(this);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + ":" + getTitle();
    }

    @PropertyName ( "Fixed" )
    @PropertyDescription ( "Indicates that Diagram element location is fixed." )
    public boolean isFixed()
    {
        return fixed;
    }
    public void setFixed(boolean fixed)
    {
        Object oldValue = this.fixed;
        this.fixed = fixed;
        firePropertyChange("fixed", oldValue, fixed);
    }

    public @Nonnull Compartment getCompartment()
    {
        return getOrigin().cast(Compartment.class);
    }

    /**
     * @return stream containing this element as well as all the nested elements if applicable
     */
    public StreamEx<DiagramElement> recursiveStream()
    {
        return StreamEx.ofTree(this, Compartment.class, Compartment::stream);
    }

    public boolean hasNoAttributes()
    {
        return getAttributes().isEmpty();
    }

    ///Style issues
    @PropertyName ( "Predefined style" )
    public String getPredefinedStyle()
    {
        return predefinedStyle;
    }

    public Stream<String> getAvailableStyles()
    {
        return StreamEx.of(Diagram.getDiagram(this).getViewOptions().getStyles()).map(s -> s.getName())
                .append(DiagramElementStyle.STYLE_NOT_SELECTED, DiagramElementStyle.STYLE_DEFAULT);
    }

    public void setPredefinedStyle(String predefinedStyle)
    {
        String oldValue = this.predefinedStyle;
        if( oldValue.equals(predefinedStyle) )
            return;
        if( predefinedStyle.equals(DiagramElementStyle.STYLE_NOT_SELECTED) )
        {
            setCustomStyle(new DiagramElementStyle(this));
        }
        else if( !predefinedStyle.equals(DiagramElementStyle.STYLE_DEFAULT) )
        {
            DiagramElementStyle style = Diagram.getDiagram(this).getViewOptions().getStyle(predefinedStyle);
            if( style == null )
                return;
            setCustomStyle(style);
        }
        this.predefinedStyle = predefinedStyle;
        firePropertyChange("predefinedStyle", oldValue, predefinedStyle);
        firePropertyChange("*", null, null);
    }

    @PropertyName ( "Custom style" )
    public DiagramElementStyle getCustomStyle()
    {
        return customStyle;
    }

    public void setCustomStyle(DiagramElementStyle customStyle)
    {
        DiagramElementStyle oldValue = this.customStyle;
        this.customStyle = customStyle;
        firePropertyChange("customStyle", oldValue, customStyle);
    }

    public boolean isStylePredefined()
    {
        return !predefinedStyle.equals(DiagramElementStyle.STYLE_NOT_SELECTED);
    }
}
