package ru.biosoft.access.biohub;

import java.beans.FeatureDescriptor;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.eclipse.core.runtime.IConfigurationElement;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.Assert;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.ExtensionRegistrySupport;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.StaticDescriptor;

/**
 * Registry which stores information about all registered ReferenceType's
 * @author lan
 */
public class ReferenceTypeRegistry extends ExtensionRegistrySupport<ReferenceType>
{
    /**
     * Name of property for FeatureDescriptor associated with ReferenceType
     */
    public static final String REFERENCE_TYPE_PROPERTY = "referenceType";
    public static final PropertyDescriptor REFERENCE_TYPE_PD = StaticDescriptor.create(REFERENCE_TYPE_PROPERTY);

    private static ReferenceTypeRegistry instance = new ReferenceTypeRegistry();
    private final List<CollectionSpecificReferenceType> dynamicTypes = new ArrayList<>();
    private final @Nonnull ReferenceType defaultType = new DefaultReferenceType();

    private ReferenceTypeRegistry()
    {
        super("ru.biosoft.access.referenceType", "typeClass");
    }

    @Override
    protected void postInit()
    {
        try
        {
            SecurityManager.runPrivileged(() -> {
                loadCollectionReferenceTypes(CollectionFactoryUtils.getDatabases());
                return null;
            });
        }
        catch( Exception e )
        {
        }
    }

    @Override
    protected ReferenceType loadElement(IConfigurationElement element, String elementName) throws Exception
    {
        Class<? extends ReferenceType> clazz = getClassAttribute(element, "typeClass", ReferenceType.class);
        String urlTemplate = element.getAttribute("urlTemplate");
        ReferenceType instance = clazz.newInstance();
        if(clazz.equals(defaultType.getClass())) instance = defaultType;
        if(instance instanceof ReferenceTypeSupport)
        {
            ((ReferenceTypeSupport)instance).setUrlTemplate(urlTemplate);
        }
        return instance;
    }

    /**
     * Initialize ReferenceTypes for DataCollections in database repository
     */
    protected void loadCollectionReferenceTypes(DataCollection<DataCollection<?>> repository)
    {
        for( DataCollection<?> dc : repository )
        {
            try
            {
                addDataCollectionType(dc);
            }
            catch( Exception e )
            {
            }
        }
        repository.addDataCollectionListener(new DataCollectionListenerSupport(){
            @Override
            public void elementAdded(DataCollectionEvent e) throws Exception
            {
                DataElement de = e.getDataElement();
                if(de instanceof DataCollection)
                {
                    try
                    {
                        addDataCollectionType((DataCollection<?>)de);
                    }
                    catch( Exception ex )
                    {
                    }
                }
            }
            @Override
            public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
            {
                DataElement de = e.getDataElement();
                if( de instanceof DataCollection )
                {
                    DataCollection<?> dc = (DataCollection<?>)de;
                    String referenceTypeName = dc.getInfo().getProperty(ReferenceType.REFERENCE_TYPE_PROPERTY);
                    if( referenceTypeName != null )
                    {
                        ReferenceType type = getReferenceType(dc);
                        if( type != null )
                            dynamicTypes.remove(type);
                    }
                }
            }
        });
    }

    /**
     * Initialize reference type for data collection dc, if possible
     */
    protected void addDataCollectionType(DataCollection<?> dc) throws Exception
    {
        String className = dc.getInfo().getProperty(ReferenceType.REFERENCE_TYPE_PROPERTY);
        if( className != null && className.startsWith(CollectionSpecificReferenceType.class.getSimpleName()))
        {
            dynamicTypes.add(new CollectionSpecificReferenceType(dc));
        }
        ExProperties.getSubProperties( dc.getInfo().getProperties(), ReferenceType.REFERENCE_TYPE_PROPERTY )
            .forEach( (key, props) -> {
                dynamicTypes.add(new CollectionSpecificReferenceType( dc, props ));
            });
    }

    /**
     * Returns stream of all reference types
     * @return stream of possible reference types
     */
    public static @Nonnull StreamEx<ReferenceType> types()
    {
        return instance.stream().append( instance.dynamicTypes.stream().filter( type -> type.getPath().exists() ) );
    }

    public static void registerTypeForDescriptor(FeatureDescriptor fd, ReferenceType type)
    {
        if(type != null)
            fd.setValue(REFERENCE_TYPE_PROPERTY, type);
    }

    public static void registerTypeForDescriptor(FeatureDescriptor fd, String type)
    {
        registerTypeForDescriptor(fd, optReferenceType(type));
    }

    public static ReferenceType getTypeForDescriptor(FeatureDescriptor fd)
    {
        try
        {
            return (ReferenceType)fd.getValue(REFERENCE_TYPE_PROPERTY);
        }
        catch( Exception e )
        {
            return null;
        }
    }

    /**
     * Returns default reference type
     * If several default reference type are available, any of them may be returned.
     * @return ReferenceType object for default reference type
     */
    public static @Nonnull ReferenceType getDefaultReferenceType()
    {
        return instance.defaultType;
    }

    /**
     * Lookups reference type by given display name first, if nothing found, try to lookup by stable name
     * @param name reference type's display name to lookup
     * @return found reference type
     * @throws InternalException if no such reference type found
     */
    public static @Nonnull ReferenceType getReferenceType(@Nonnull String name) throws InternalException
    {
        Assert.notNull("name", name);
        ReferenceType result = optReferenceType(name);
        if(result == null)
            throw new InternalException("Reference type "+name+" not found");
        return result;
    }

    /**
     * Lookups reference type by given display name first, if nothing found, try to lookup by stable name
     * @param name reference type's display name to lookup
     * @return found reference type or null if no reference type found
     */
    public static @CheckForNull ReferenceType optReferenceType(String name)
    {
        if(name == null || name.isEmpty())
            return null;
        if(getDefaultReferenceType().getDisplayName().equals(name)
                || getDefaultReferenceType().getStableName().equals(name))
            return getDefaultReferenceType();
        return types().findFirst( type -> type.getDisplayName().equals( name ) || type.getStableName().equals( name ) ).orElse( null );
    }

    /**
     * Lookups reference type by given class
     * @param typeClass class of reference type
     * @return found reference type
     * @throws InternalException if no such type is registered
     */
    public static @Nonnull ReferenceType getReferenceType(Class<? extends ReferenceType> typeClass) throws InternalException
    {
        return instance.stream().findAny( type -> type.getClass().equals( typeClass ) )
                .orElseThrow( () -> new InternalException( "Reference type is not registered: " + typeClass ) );
    }

    /**
     * Lookups reference type by given data collection name
     * @param dc ru.biosoft.access.core.DataCollection
     * @return found reference type or null if no reference type found
     */
    public static @CheckForNull ReferenceType getReferenceType(DataCollection<?> dc)
    {
        instance.init();
        return StreamEx.of( instance.dynamicTypes ).findAny( type -> type.getSource().equals( dc.getName() ) ).orElse( null );
    }

    /**
     * Lookups list of reference types by given classes
     * This method is handy for BioHub methods like getSupportedInputTypes and getSupportedMatching
     * @param typeClasses list of classes of reference type
     * @return array of found reference types or empty array if no reference types found
     */
    @SafeVarargs
    public static @Nonnull ReferenceType[] getReferenceTypes(Class<? extends ReferenceType>... typeClasses)
    {
        return Stream.of( typeClasses ).map( ReferenceTypeRegistry::getReferenceType ).toArray( ReferenceType[]::new );
    }

    /**
     * Returns reference type of given DataCollection element
     * @param collection DataCollection to fetch reference type from
     * @return ReferenceType reference type of this ru.biosoft.access.core.DataCollection. If no reference type was specified explicitly, default reference type will be returned (or null if no default reference type applicable).
     */
    public static @Nonnull ReferenceType getElementReferenceType(DataCollection<?> collection)
    {
        if(collection == null) return getDefaultReferenceType();
        String typeName = collection.getInfo().getProperty(ReferenceType.REFERENCE_TYPE_PROPERTY);
        if(typeName != null)
        {
            ReferenceType type = optReferenceType(typeName);
            if(type != null) return type;
        }
        return getDefaultReferenceType();
    }

    /**
     * Returns reference type of ru.biosoft.access.core.DataElement by given element path.
     * This method doesn't instantiate ru.biosoft.access.core.DataElement and works only for elements inside GenericDataCollection
     * @param path path to ru.biosoft.access.core.DataElement to fetch reference type from
     * @return ReferenceType reference type of specified ru.biosoft.access.core.DataElement. If no reference type was specified explicitly, default reference type will be returned (or null if no default reference type applicable).
     */
    public static @CheckForNull ReferenceType getElementReferenceType(DataElementPath path)
    {
        if(path == null) return null;
        DataElementDescriptor descriptor = path.getDescriptor();
        if(descriptor == null) return null;
        return ReferenceTypeRegistry.optReferenceType( descriptor.getValue( REFERENCE_TYPE_PROPERTY ) );
    }

    /**
     * Copies reference type of one collection to another
     * @param dest destination ru.biosoft.access.core.DataCollection
     * @param source source ru.biosoft.access.core.DataCollection
     */
    public static void copyCollectionReferenceType(DataCollection<?> dest, DataCollection<?> source)
    {
        setCollectionReferenceType(dest, getElementReferenceType(source));
    }

    /**
     * Sets reference type for ru.biosoft.access.core.DataCollection
     * Note: this method doesn't check whether reference type is applicable for given ru.biosoft.access.core.DataCollection
     * @param collection DataCollection to set reference type for
     * @param referenceType reference type to set
     */
    public static void setCollectionReferenceType(DataCollection<?> collection, ReferenceType referenceType)
    {
        if(referenceType == null)
            collection.getInfo().getProperties().remove(ReferenceType.REFERENCE_TYPE_PROPERTY);
        else
            collection.getInfo().getProperties().setProperty(ReferenceType.REFERENCE_TYPE_PROPERTY, referenceType.getStableName());
    }

    /**
     * Sets reference type for ru.biosoft.access.core.DataCollection
     * Note: this method doesn't check whether reference type is applicable for given ru.biosoft.access.core.DataCollection
     * @param collection DataCollection to set reference type for
     * @param typeName display name of reference type to set
     */
    public static void setCollectionReferenceType(DataCollection<?> collection, String typeName)
    {
        setCollectionReferenceType(collection, ReferenceTypeRegistry.optReferenceType(typeName));
    }

    /**
     * Sets reference type for ru.biosoft.access.core.DataCollection
     * Note: this method doesn't check whether reference type is applicable for given ru.biosoft.access.core.DataCollection
     * @param collection DataCollection to set reference type for
     * @param typeClass class of reference type to set
     */
    public static void setCollectionReferenceType(DataCollection<?> collection, Class<? extends ReferenceType> typeClass)
    {
        collection.getInfo().getProperties().setProperty(ReferenceType.REFERENCE_TYPE_PROPERTY, typeClass.getSimpleName());
    }

    /**
     * Tries to resolve type of given references list
     * @param references array of references of interest
     * @param baseType allows you to limit search to sub-hierarchy of types with common ancestor baseType
     * @return {@link ReferenceType} which is the best guess
     */
    public static @Nonnull ReferenceType detectReferenceType(String[] references, Class<? extends ReferenceType> baseType)
    {
        return types().filter( baseType::isInstance )
                .maxBy( type -> StreamEx.of( references ).filter( TextUtil::nonEmpty ).mapToInt( type::getIdScore ).sum() )
                .orElse( getDefaultReferenceType() );
    }

    /**
     * Tries to resolve type of given references list
     * @param references array of references of interest
     * @return {@link ReferenceType} which is the best guess
     */
    public static @Nonnull ReferenceType detectReferenceType(String[] references)
    {
        return detectReferenceType(references, ReferenceType.class);
    }

    /**
     * Tries to resolve type of given reference
     * @param reference reference of interest
     * @return {@link ReferenceType} which is the best guess
     */
    public static @Nonnull ReferenceType detectReferenceType(String reference)
    {
        return reference == null ? getDefaultReferenceType() : detectReferenceType(new String[]{reference});
    }

    public static Optional<ReferenceType> byMiriamId(@Nonnull String miriamId)
    {
        return types().findFirst( type -> miriamId.equals( type.getMiriamId() ) );
    }
}
