package ru.biosoft.workbench.editors;

import java.beans.IntrospectionException;
import java.util.Properties;

import one.util.streamex.StreamEx;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.model.ComponentFactory;

import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.util.BeanUtil;

public class ReferenceTypeSelector extends GenericComboBoxEditor
{
    public static final String NO_TYPES_MESSAGE = "(no types available)";
    public static final String AUTO_DETECT_MESSAGE = "(auto)";
    private static final String REACHABLE_FROM_PROPERTY = "typesReachableFrom";
    private static final String AUTO_DETECT_PROPERTY = "autoDetect";
    private static final String SPECIES_PROPERTY = "species";

    private ReferenceType getSourceType()
    {
        try
        {
            String property = getDescriptor().getValue(REACHABLE_FROM_PROPERTY).toString();
            return ReferenceTypeRegistry.optReferenceType(ComponentFactory.getModel(getBean()).findProperty(property).getValue().toString());
        }
        catch( Exception e )
        {
            return null;
        }
    }
    
    private String getSpeciesName()
    {
        try
        {
            String property = getDescriptor().getValue( SPECIES_PROPERTY ).toString();
            Species value = (Species)ComponentFactory.getModel(getBean()).findProperty(property).getValue();
            return value.getLatinName();
        }
        catch( Exception e )
        {
            return null;
        }
    }
    
    private boolean isAutoDetect()
    {
        return BeanUtil.getBooleanValue(this, AUTO_DETECT_PROPERTY);
    }
    
    @Override
    protected Object[] getAvailableValues()
    {
        ReferenceType sourceType = getSourceType();
        StreamEx<String> typeNames;
        if(sourceType == null)
        {
            typeNames = ReferenceTypeRegistry.types().map( ReferenceType::toString );
        } else
        {
            Properties properties = BioHubSupport.createProperties( getSpeciesName(), sourceType );
            ReferenceType[] types = BioHubRegistry.getReachableTypes(properties);
            typeNames = StreamEx.of(types).map( ReferenceType::toString );
        }
        if(isAutoDetect())
            typeNames = typeNames.prepend(AUTO_DETECT_MESSAGE);
        String[] types = typeNames.sorted().toArray( String[]::new );
        return types.length == 0 ? new String[] {NO_TYPES_MESSAGE} : types;
    }
    
    public static PropertyDescriptorEx registerSelector(String property, Class<?> beanClass, String reachableFromProperty, String speciesProperty) throws IntrospectionException
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx(property, beanClass);
        pde.setPropertyEditorClass(ReferenceTypeSelector.class);
        if(reachableFromProperty != null)
            pde.setValue(REACHABLE_FROM_PROPERTY, reachableFromProperty);
        if(speciesProperty != null)
            pde.setValue( SPECIES_PROPERTY, speciesProperty );
        return pde;
    }

    public static PropertyDescriptorEx registerSelector(String property, Class<?> beanClass, boolean autoDetect) throws IntrospectionException
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx(property, beanClass);
        pde.setPropertyEditorClass(ReferenceTypeSelector.class);
        if(autoDetect)
            pde.setValue(AUTO_DETECT_PROPERTY, true);
        return pde;
    }

    public static PropertyDescriptorEx registerSelector(String property, Class<?> beanClass) throws IntrospectionException
    {
        return registerSelector(property, beanClass, null, null);
    }
}