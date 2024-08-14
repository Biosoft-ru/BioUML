package ru.biosoft.analysiscore;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import one.util.streamex.EntryStream;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.BeanAsMapUtil;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.TextUtil;

/**
 * Base implementation of {@link AnalysisParameters}
 */
@PropertyName("Parameters")
@PropertyDescription("Analysis parameters")
public class AbstractAnalysisParameters extends OptionEx implements AnalysisParameters
{
    private boolean expertMode;

    protected AbstractAnalysisParameters(boolean lateInit)
    {
        super(lateInit);
    }
    
    public AbstractAnalysisParameters()
    {
    }
    
    /**
     * Default implementation reads all bean properties as strings and constructs them using constructor(String)
     * It doesn't go deep into ComplexProperty/ArrayProperty
     * Override this to save in a custom way (be sure to override write() also)
     */
    @Override
    public void read(Properties properties, String prefix)
    {
        BeanUtil.readBeanFromProperties( this, properties, prefix );
    }

    /**
     * Default implementation saves all bean properties as strings (using Object.toString)
     * It doesn't go deep into ComplexProperty/ArrayProperty
     * Override this to save in a custom way (be sure to override read() also)
     */
    @Override
    public void write(Properties properties, String prefix)
    {
        BeanUtil.writeBeanToProperties( this, properties, prefix );
    }

    /**
     * clone public implementation
     */
    @Override
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            return null;
        }
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return findTerminalProperties( this::isInput ).keys().toArray( String[]::new );
    }

    /**
     * Default implementation gets all properties which has "isOutput" descriptor value set
     * Typically it's all the properties created by DataElementPathEditor.registerOutput
     * Override this if you need more complex behavior
     */
    @Override
    public @Nonnull String[] getOutputNames()
    {
        return findTerminalProperties( this::isOutput ).keys().toArray( String[]::new );
    }
    
    private EntryStream<String, Object> findTerminalProperties(Predicate<Property> filter)
    {
        Map<String, Object> hMap = BeanAsMapUtil.convertBeanToMap( this,
                p -> isPotentialParent( p ) || filter.test( p ) );
        Map<String, Object> flatMap = BeanAsMapUtil.flattenMap( hMap );
        return EntryStream.of( flatMap )
                    .removeValues( v->v instanceof Map && ((Map<?, ?>)v).isEmpty() )
                    .removeValues( v->v instanceof List && ((List<?>)v).isEmpty() );
    }

    private boolean isPotentialParent(Property p)
    {
        if(!p.isVisible( Property.SHOW_EXPERT ))
            return false;
        return (BeanAsMapUtil.isCompositeProperty( p ) || BeanAsMapUtil.isArrayProperty( p )) && p.getValue() != null;
    }

    @Override
    public DataElementPath[] getExistingOutputNames()
    {
        return findTerminalProperties( this::isOutput ).values()
                .select( String.class ).map( DataElementPath::create )
                .filter( DataElementPath::exists )
                .toArray( DataElementPath[]::new );
    }

    private boolean isInput(Property property)
    {
        return property.isVisible( Property.SHOW_EXPERT ) && property.getBooleanAttribute( "isInput" );
    }
    
    private boolean isOutput(Property property)
    {
        return property.isVisible( Property.SHOW_EXPERT ) && property.getBooleanAttribute( "isOutput" );
    }
    
    public boolean isExpertMode()
    {
        return expertMode;
    }

    @Override
    public void setExpertMode(boolean expertMode)
    {
        this.expertMode = expertMode;
    }

    /**
     * Init parameters from Scriptable object
     * @param params Scriptable object to set parameters from
     * @throws Exception
     */
    public void setFromScriptable(Scriptable params) throws Exception
    {
        if(params == null) return;
        ComponentModel model = ComponentFactory.getModel(this);
        for(Object id: params.getIds())
        {
            String key = id.toString();
            Property property = model.findProperty(key);
            if(property == null) continue;
            Object value = params.get(key, params);
            if(value instanceof Wrapper)
                value = ((Wrapper)value).unwrap();
            if(!property.getValueClass().isInstance(value))
                value = TextUtil.fromString(property.getValueClass(), TextUtil.toString(value));
            property.setValue(value);
        }
    }
}
