package ru.biosoft.access.biohub;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.IConfigurationElement;

import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionListenerSupport;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.journal.ProjectUtils;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.ExtensionRegistrySupport;
import ru.biosoft.util.Maps;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.Util;

/**
 * Registry class for BioHub access
 */
public class BioHubRegistry extends ExtensionRegistrySupport<BioHubRegistry.BioHubInfo>
{
    private static Logger log = Logger.getLogger( BioHubRegistry.class.getName() );

    public static final String NAME_ATTR = "name";
    public static final String CLASS_ATTR = "class";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String PROPERTY_NAME_ATTR = "name";
    public static final String PROPERTY_VALUE_ATTR = "value";

    private static ExtensionRegistrySupport<BioHubInfo> specialHubs = new ExtensionRegistrySupport<BioHubRegistry.BioHubInfo>(
            "ru.biosoft.access.biohubSpecial", "name" )
    {
        @Override
        protected BioHubInfo loadElement(IConfigurationElement element, String elementName) throws Exception
        {
            return new BioHubInfo( elementName, null )
            {
                @Override
                public boolean isSpecial()
                {
                    return true;
                }
            };
        }
    };

    public static class BioHubInfo
    {
        protected String name;
        protected BioHub bioHub;
        protected DataElementPath path;

        public BioHubInfo(String name, BioHub bioHub)
        {
            this.name = name;
            this.bioHub = bioHub;
        }

        public BioHubInfo(String name, BioHub bioHub, DataElementPath path)
        {
            this.name = name;
            this.bioHub = bioHub;
            this.path = path;
        }

        public boolean isSpecial()
        {
            return false;
        }

        public DataElementPath getPath()
        {
            return path;
        }

        public String getName()
        {
            return name;
        }

        public BioHub getBioHub()
        {
            return bioHub;
        }

        public static BioHubInfo createInstance(String hubName)
        {
            BioHubInfo specialHub = specialHubs.getExtension( hubName );
            if( specialHub != null )
                return specialHub;
            return BioHubRegistry.getBioHubInfo( hubName );
        }

        @Override
        public String toString()
        {
            return getName();
        }
    }

    private static BioHubRegistry instance = new BioHubRegistry();

    private BioHubRegistry()
    {
        super( "ru.biosoft.access.biohub", NAME_ATTR );
    }

    @Override
    protected void postInit()
    {
        try
        {
            SecurityManager.runPrivileged( () -> {
                loadCollectionHubs( CollectionFactoryUtils.getDatabases() );
                return null;
            } );
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
        }
    }

    @Override
    protected BioHubInfo loadElement(IConfigurationElement element, String name) throws Exception
    {
        Class<? extends BioHub> bioHubClass = getClassAttribute( element, CLASS_ATTR, BioHub.class );

        Properties properties = new Properties();
        IConfigurationElement[] propElements = element.getChildren( PROPERTY_ELEMENT );
        if( propElements != null )
        {
            for( IConfigurationElement propElement : propElements )
            {
                String pName = getStringAttribute( propElement, PROPERTY_NAME_ATTR );
                String pValue = getStringAttribute( propElement, PROPERTY_VALUE_ATTR );
                properties.put( pName, pValue );
            }
        }
        properties.put( DataCollectionConfigConstants.NAME_PROPERTY, name );

        Constructor<? extends BioHub> constructor = bioHubClass.getConstructor( Properties.class );
        BioHub bioHub = constructor.newInstance( properties );

        return new BioHubInfo( name, bioHub );
    }

    /**
     * Initialize BioHubs for DataCollections in database repository
     */
    protected void loadCollectionHubs(DataCollection<DataCollection<?>> repository)
    {
        for( DataCollection<?> dc : repository )
        {
            try
            {
                addDataCollectionHub( dc );
            }
            catch( Exception e )
            {
                ExceptionRegistry.log( e );
            }
        }
        repository.addDataCollectionListener( new DataCollectionListenerSupport()
        {
            @Override
            public void elementAdded(DataCollectionEvent e) throws Exception
            {
                DataElement de = e.getDataElement();
                if( de instanceof DataCollection )
                {
                    try
                    {
                        addDataCollectionHub( (DataCollection<?>)de );
                    }
                    catch( Exception ex )
                    {
                        log.log(Level.SEVERE,  "Unable to register hub for " + ( (DataCollection<?>)de ).getCompletePath(), ex );
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
                    ArrayList<BioHubInfo> newBioHubs = new ArrayList<>();
                    DataElementPath path = DataElementPath.create( dc );
                    for( BioHubInfo info : extensions )
                    {
                        if( info.getPath() == null || !info.getPath().equals( path ) )
                            newBioHubs.add( info );
                    }
                    extensions = newBioHubs;
                }
            }
        } );
    }

    protected void addDataCollectionHub(DataCollection<?> dc) throws Exception
    {
        Properties dcProperties = dc.getInfo().getProperties();
        DataElementPath path = dc.getCompletePath();
        // Old-style hub definition
        // bioHub=<class1>[;<class2>[;...]]
        // bioHubName=<name1>[;<name2>[;...]]
        String classNamesProperty = dcProperties.getProperty( BioHub.BIOHUB_CLASS );
        if( classNamesProperty != null )
        {
            String[] classNames = TextUtil.split( classNamesProperty, ';' );
            String[] hubNames = TextUtil.split( dcProperties.getProperty( BioHub.BIOHUB_NAME ), ';' );
            for( int i = 0; i < classNames.length; i++ )
            {
                try
                {
                    Class<? extends BioHub> c = ClassLoading.loadSubClass( classNames[i], BioHub.class );
                    Constructor<? extends BioHub> constructor = c.getConstructor( new Class[] {Properties.class} );
                    Properties properties = new Properties( dcProperties );
                    properties.setProperty( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY, path.toString() );
                    properties.setProperty( "moduleName", path.toString() );
                    properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, hubNames[i] );
                    BioHub bioHub = constructor.newInstance( new Object[] {properties} );
                    if( bioHub != null )
                    {
                        BioHubInfo bioHubInfo = new BioHubInfo( hubNames[i], bioHub, DataElementPath.create( dc ) );
                        nameToExtension.put( bioHubInfo.getName(), bioHubInfo );
                        extensions.add( bioHubInfo );
                    }
                }
                catch( Throwable e )
                {
                    log.log(Level.SEVERE,  "BioHubRegistry: Cannot load class " + classNames[i] + ": hub is disabled", e );
                }
            }
        }
        // New style hub definition
        // bioHub.<randomId>=<class>[;name=<name>][;<property1>=<value1>[;<property2>=<value2>[;...]]]
        ExProperties.getSubProperties( dcProperties, "bioHub" ).forEach( (key, props) -> {
            try
            {
                Class<? extends BioHub> c = ClassLoading.loadSubClass( props.get( "default" ), BioHub.class );
                Constructor<? extends BioHub> constructor = c.getConstructor( new Class[] {Properties.class} );
                Properties properties = new Properties( dcProperties );
                properties.setProperty( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY, path.toString() );
                properties.setProperty( "moduleName", path.toString() );
                properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, c.getSimpleName() );
                properties.putAll( props );
                String name = properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY );
                String version = dcProperties.getProperty( "version" );
                if( version != null )
                {
                    name = name + " (" + version + ")";
                    properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
                }
                BioHub bioHub = constructor.newInstance( properties );
                if( bioHub != null )
                {
                    BioHubInfo bioHubInfo = new BioHubInfo( name, bioHub, DataElementPath.create( dc ) );
                    if( nameToExtension.putIfAbsent( name, bioHubInfo ) == null )
                        extensions.add( bioHubInfo );
                    else
                        log.warning( "Attempt to register several hubs with the name " + name );
                }
            }
            catch( Throwable e )
            {
                log.log(Level.SEVERE,  "BioHubRegistry: hub " + key + ": Cannot initialize, hub is disabled", e );
            }
        } );
    }

    public static void addCollectionHub(DataCollection<?> dc) throws Exception
    {
        if( !SecurityManager.isTestMode() )
            throw new SecurityException( "Not allowed" );
        instance.init();
        instance.addDataCollectionHub( dc );
    }

    public static Map<String, BioHubInfo> getBioHubs()
    {
        return bioHubs( true ).toMap( BioHubInfo::getName, Function.identity() );
    }

    private static boolean isHubPathAvailable(DataElementPath path, boolean addOtherVersions, DataElementPath projectPath)
    {
        if( path == null )
            return true;
        if( !path.exists() )
            return false;
        return addOtherVersions || ( projectPath == null && ProjectUtils.isDatabasePreferred( path ) )
                || ProjectUtils.isDatabasePreferred( projectPath, path );
    }

    public static StreamEx<BioHubInfo> specialHubs()
    {
        return specialHubs.stream();
    }

    public static StreamEx<BioHubInfo> bioHubs()
    {
        return bioHubs( false );
    }

    public static StreamEx<BioHubInfo> bioHubs(boolean addOtherVersions)
    {
        return instance.stream().filter( info -> isHubPathAvailable( info.getPath(), addOtherVersions, null ) );
    }

    public static StreamEx<BioHubInfo> bioHubs(final TargetOptions dbOptions)
    {
        return bioHubs( dbOptions, true );
    }

    public static StreamEx<BioHubInfo> bioHubs(final TargetOptions dbOptions, boolean addOtherVersions)
    {
        return bioHubs( dbOptions, addOtherVersions, ProjectUtils.getProjectPath( null ) );
    }

    public static StreamEx<BioHubInfo> bioHubs(final TargetOptions dbOptions, boolean addOtherVersions, DataElementPath wantedPath)
    {
        return bioHubs( addOtherVersions, wantedPath ).filter(
                Util.safePredicate( info -> info.getBioHub().getPriority( dbOptions ) > 0,
                        (info, ex) -> log.log(Level.SEVERE,  "While getting priority for biohub " + info.getName(), ex ) ) ).sorted(
                Comparator.comparingInt( (BioHubInfo info) -> info.getBioHub().getPriority( dbOptions ) ).reversed()
                        .thenComparing( BioHubInfo::getName ) );
    }

    public static StreamEx<BioHubInfo> bioHubs(boolean addOtherVersions, DataElementPath wantedPath)
    {
        return instance.stream().filter( info -> isHubPathAvailable( info.getPath(), addOtherVersions, wantedPath ) );
    }

    public static @Nullable BioHubInfo getBioHubInfo(String name)
    {
        BioHubInfo info = instance.getExtension( name );
        if( info == null || ( info.getPath() != null && !info.getPath().exists() ) )
            return null;
        return info;
    }

    public static @Nullable BioHub getBioHub(String name)
    {
        BioHubInfo info = getBioHubInfo( name );
        return info == null ? null : info.getBioHub();
    }

    /**
     * Get max priority BioHub for collections
     */
    public static BioHub getBioHub(TargetOptions dbOptions)
    {
        return bioHubs()
                .mapToEntry(
                        Util.safeFunction( bhi -> bhi.getBioHub().getPriority( dbOptions ), 0,
                                (bhi, ex) -> log.log(Level.SEVERE,  "While getting priority for biohub " + bhi.getName(), ex ) ) )
                .filterValues( priority -> priority > 0 )
                .maxBy( Entry::getValue )
                .map( e -> e.getKey().getBioHub() )
                .orElse( null );
    }

    /**
     * Get array of suitable BioHubs sorted by priority
     */
    public static BioHubInfo[] getBioHubInfos(final TargetOptions dbOptions)
    {
        return bioHubs( dbOptions, true, null ).toArray( BioHubInfo[]::new );
    }

    public static class MatchingStep
    {
        private BioHub hub = null;
        private MatchingStep from = null;
        private final Properties properties;
        private double quality = 0;
        private int length = -1;
        boolean dirty = false;

        public MatchingStep(Properties properties)
        {
            this.properties = properties;
        }

        public boolean isCompatible(Properties properties)
        {
            return ( !properties.containsKey( BioHub.SPECIES_PROPERTY )
                    || properties.getProperty( BioHub.SPECIES_PROPERTY ).equals( this.properties.getProperty( BioHub.SPECIES_PROPERTY ) ) )
                    && properties.getProperty( BioHub.TYPE_PROPERTY ).equals( this.properties.getProperty( BioHub.TYPE_PROPERTY ) );
        }

        public void update(BioHub hub, MatchingStep from, double quality, int length)
        {
            this.hub = hub;
            this.from = from;
            this.quality = quality;
            this.length = length;
            this.dirty = true;
        }

        protected void setProcessed()
        {
            this.dirty = false;
        }

        protected boolean isDirty()
        {
            return this.dirty;
        }

        protected boolean isBetter(double newQuality, int newLength)
        {
            return newQuality > 0 && ( newQuality > getQuality() || ( newQuality == getQuality() && newLength < length ) );
        }

        public ReferenceType getType()
        {
            return ReferenceTypeRegistry.getReferenceType( properties.getProperty( BioHub.TYPE_PROPERTY ) );
        }

        public MatchingStep getFrom()
        {
            return from;
        }

        public double getQuality()
        {
            return quality;
        }

        public int getLength()
        {
            return length;
        }

        public BioHub getBioHub()
        {
            return hub;
        }

        public Map<String, String[]> getReferences(String[] inputList, FunctionJobControl jobControl)
        {
            return getBioHub().getReferences( inputList, getFrom().getProperties(), getProperties(), jobControl );
        }

        public Properties getProperties()
        {
            return properties;
        }

        @Override
        public String toString()
        {
            return getProperties().toString();
        }
    }

    private static List<MatchingStep> getMatchingGraph(Properties inputType)
    {
        List<MatchingStep> steps = new ArrayList<>();
        MatchingStep startStep = new MatchingStep( inputType );
        steps.add( startStep );
        startStep.update( null, null, 1, 0 );

        boolean changed = true;
        DataElementPath projectPath = ( inputType.containsKey( BioHub.PROJECT_PROPERTY ) )
                ? DataElementPath.create( inputType.getProperty( BioHub.PROJECT_PROPERTY ) ) : null;
        List<BioHubInfo> bioHubs = bioHubs( false, projectPath ).toList();
        while( changed )
        {
            changed = false;
            for( MatchingStep step : steps.toArray( new MatchingStep[steps.size()] ) )
            {
                if( step.isDirty() )
                {
                    for( BioHubInfo biohubInfo : bioHubs )
                    {
                        BioHub bioHub = biohubInfo.getBioHub();
                        Properties[] propertiesList;
                        try
                        {
                            propertiesList = bioHub.getSupportedMatching( step.getProperties() );
                        }
                        catch( Exception e )
                        {
                            log.log(Level.SEVERE,  "BioHub " + bioHub.getName() + ": " + ExceptionRegistry.log( e ) );
                            continue;
                        }
                        if( propertiesList == null )
                            continue;
                        for( Properties properties : propertiesList )
                        {
                            if( properties == null )
                                continue;
                            boolean found = false;
                            for( MatchingStep element2 : steps )
                            {
                                if( element2.isCompatible( properties ) )
                                {
                                    double newQuality = step.getQuality() * bioHub.getMatchingQuality( step.getProperties(), properties );
                                    int newLength = step.getLength() + 1;
                                    if( element2.isBetter( newQuality, newLength ) )
                                    {
                                        element2.update( bioHub, step, newQuality, newLength );
                                        changed = true;
                                    }
                                    found = true;
                                    break;
                                }
                            }
                            if( !found )
                            {
                                MatchingStep newStep = new MatchingStep( properties );
                                newStep.update( bioHub, step,
                                        step.getQuality() * bioHub.getMatchingQuality( step.getProperties(), properties ),
                                        step.getLength() + 1 );
                                steps.add( newStep );
                                changed = true;
                            }
                        }
                    }
                    step.setProcessed();
                }
            }
        }
        return steps;
    }

    /**
     * Returns sequence of MatchingStep's which is best suitable for matching from input to output
     * Or null if no such sequence available
     */
    public static MatchingStep[] getMatchingPath(Properties input, Properties output)
    {
        if( input.equals( output ) )
        {
            return new MatchingStep[0];
        }
        if( ReferenceTypeRegistry.getReferenceType( input.getProperty( BioHub.TYPE_PROPERTY ) ).getClass()
                .equals( DefaultReferenceType.class )
                || ReferenceTypeRegistry.getReferenceType( output.getProperty( BioHub.TYPE_PROPERTY ) ).getClass()
                        .equals( DefaultReferenceType.class ) )
            return null;
        Optional<MatchingStep> startStep = StreamEx.of( getMatchingGraph( input ) ).findAny( step -> step.isCompatible( output ) );
        if( !startStep.isPresent() )
            return null;
        List<MatchingStep> list = new ArrayList<>();
        for( MatchingStep step = startStep.get(); !step.isCompatible( input ); step = step.getFrom() )
        {
            list.add( step );
        }
        Collections.reverse( list );
        return list.toArray( new MatchingStep[list.size()] );
    }

    /**
     * Returns list of properties which are reachable from given input
     */
    public static Properties[] getReachableProperties(Properties input)
    {
        return getMatchingGraph( input ).stream().map( MatchingStep::getProperties ).toArray( Properties[]::new );
    }

    /**
     * Returns list of types which are reachable from given input
     */
    public static ReferenceType[] getReachableTypes(Properties input)
    {
        return getMatchingGraph( input )
                .stream()
                .filter(
                        step -> step.getProperties().getProperty( BioHub.SPECIES_PROPERTY )
                                .equals( input.getProperty( BioHub.SPECIES_PROPERTY ) ) )
                .map( step -> ReferenceTypeRegistry.getReferenceType( step.getProperties().getProperty( BioHub.TYPE_PROPERTY ) ) )
                .distinct().toArray( ReferenceType[]::new );
    }

    /**
     * Performs matching of IDs in inputList of type inputType to type outputType by building matching path and performing its steps subsequently
     */
    public static Map<String, String[]> getReferences(String[] inputList, Properties input, Properties output,
            final FunctionJobControl jobControl)
    {
        final MatchingStep[] steps = getMatchingPath( input, output );
        // No matching possible
        if( steps == null ) {
            return null;
        }
        return getReferences( inputList, steps, jobControl );
    }

    /**
     * @param inputList
     * @param steps
     * @param jobControl
     * @return
     */
    public static Map<String, String[]> getReferences(String[] inputList, final MatchingStep[] steps, final FunctionJobControl jobControl)
    {
        if( steps == null )
            return null;
        // 0 steps = no matching required (inputType == outputType?)
        if( steps.length == 0 )
        {
            if( jobControl != null )
                jobControl.functionStarted();
            Map<String, String[]> result = StreamEx.of( inputList ).toMap( i -> new String[] {i} );
            if( jobControl != null )
                jobControl.functionFinished();
            return result;
        }
        // 1 step = just ask corresponding BioHub
        if( steps.length == 1 )
        {
            return steps[0].getReferences( inputList, jobControl );
        }
        // 2+ steps
        if( jobControl != null )
            jobControl.functionStarted();
        Map<String, Set<String>> curResult = new HashMap<>();
        Set<String> curInput = new HashSet<>();
        for( String input : inputList )
        {
            Set<String> set = new HashSet<>();
            set.add( input );
            curResult.put( input, set );
            curInput.add( input );
        }
        for( int i = 0; i < steps.length; i++ )
        {
            final int ii = i;
            final FunctionJobControl fjc = jobControl == null ? null : new FunctionJobControl( null );
            if( fjc != null )
            {
                fjc.addListener( new JobControlListenerAdapter()
                {
                    @Override
                    public void valueChanged(JobControlEvent event)
                    {
                        jobControl.setPreparedness( ( ii * 100 + event.getPreparedness() ) / steps.length );
                        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                        {
                            fjc.terminate();
                        }
                    }

                    @Override
                    public void jobTerminated(JobControlEvent event)
                    {
                        if( event.getStatus() == JobControl.TERMINATED_BY_ERROR )
                        {
                            jobControl.functionTerminatedByError( new Exception( event.getMessage() ) );
                        }
                    }
                } );
                fjc.functionStarted();
            }
            Map<String, String[]> curOutput = steps[i].getReferences( curInput.toArray( new String[curInput.size()] ), fjc );
            if( curOutput == null
                    || ( jobControl != null && ( jobControl.getStatus() == JobControl.TERMINATED_BY_ERROR || jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST ) ) )
            {
                return null;
            }
            curInput.clear();
            for( Map.Entry<String, Set<String>> entry : curResult.entrySet() )
            {
                Set<String> newSet = StreamEx.of( entry.getValue() ).map( curOutput::get ).nonNull().flatMap( Stream::of ).toSet();
                curResult.put( entry.getKey(), newSet );
                curInput.addAll( newSet );
            }
            if( curInput.isEmpty() )
                break;

            if( jobControl != null )
            {
                jobControl.setPreparedness( ( i + 1 ) * 100 / steps.length );
                if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    return null;
            }
        }
        Map<String, String[]> result = Maps.transformValues( curResult, val -> val.toArray( new String[val.size()] ) );
        if( jobControl != null )
            jobControl.functionFinished();
        return result;
    }

    public static Map<String, String[]> getReferences(String[] inputList, Properties output, final FunctionJobControl jobControl)
    {
        Properties input = (Properties)output.clone();
        input.setProperty( BioHub.TYPE_PROPERTY, ReferenceTypeRegistry.detectReferenceType( inputList ).toString() );
        return getReferences( inputList, input, output, jobControl );
    }

    public static Set<String> getReferencesFlat(String[] inputList, Properties input, Properties output, final FunctionJobControl jobControl)
    {
        Map<String, String[]> references = getReferences( inputList, input, output, jobControl );
        if( references == null )
            return null;
        return StreamEx.ofValues( references ).flatMap( Stream::of ).toSet();
    }
}
