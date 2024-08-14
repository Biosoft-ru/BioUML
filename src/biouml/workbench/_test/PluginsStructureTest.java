package biouml.workbench._test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class PluginsStructureTest extends AbstractBioUMLTest
{
    private static Map<String, Plugin> plugins;

    @Override
    protected synchronized void setUp() throws Exception
    {
        super.setUp();
        if( plugins == null )
        {
            plugins = new LinkedHashMap<>();
            for( Plugin plugin : Plugin.loadPlugins( new File( "../plugins" ) ) )
                plugins.put( plugin.getName(), plugin );
        }
    }

    public void testPluginsClassPath()
    {
        for( Plugin plugin : plugins.values() )
            for( String e : plugin.getClassPath() )
            {
                File file = new File(e);
                // Exception: org.eclipse.equinox.registry refers to this file, but it's actually not used
                if( file.getName().equals("runtime_registry_compatibility.jar") )
                    continue;
                if( !file.exists() )
                    collectError("Plugin " + plugin + " classpath element " + e + " doesn't exist");
                if( file.isFile() && !file.getName().endsWith(".jar") )
                    collectError("Plugin " + plugin + " classpath element " + e + " not a jar file");
            }
        failOnErrors();
    }

    public void testUnmetDependencies()
    {
        for( Plugin plugin : plugins.values() )
            for( String dep : plugin.getDependencies() )
                if ( !plugin.isOptionalDependency(dep) && !plugins.containsKey(dep) )
                    collectError("Plugin " + plugin + " depends on not existing plugin " + dep);
        failOnErrors();
    }

    public void testDuplicateDependencies()
    {
        for( Plugin plugin : plugins.values() )
        {
            Set<String> dependencies = new HashSet<>();
            for( String dep : plugin.getDependencies() )
                if( dependencies.contains( dep ) )
                    collectError( "Duplicate dependency of " + plugin + " on " + dep );
                else
                    dependencies.add( dep );
        }
        failOnErrors();
    }

    public void testCyclicDependencies()
    {
        Set<String> torn = new LinkedHashSet<>();
        while(true)
        {
            try
            {
                Set<String> visited = new LinkedHashSet<>();
                for( String plugin : plugins.keySet() )
                {
                    if( visited.contains( plugin ) )
                        continue;
                    LinkedList<String> path = new LinkedList<>();
                    path.add( plugin );

                    Set<String> active = new LinkedHashSet<>();
                    active.add( plugin );

                    while( !path.isEmpty() )
                    {
                        String last = path.getLast();
                        List<String> children = new ArrayList<>();
                        for(String dep : plugins.get( last ).getDependencies())
                        {
                            if(!torn.contains(last+"->"+dep))
                                children.add(dep);
                        }

                        boolean goBack = true;
                        for( String child : children )
                        {
                            if( !plugins.containsKey( child ) )
                                //unmet or optional dependency
                                continue;
                            if( active.contains( child ) )
                            {
                                List<String> cycle = path.subList( path.indexOf( child ), path.size() );
                                StreamEx.of( cycle ).append( cycle.get( 0 ) ).pairMap( (a, b) -> a + "->" + b ).forEach( torn::add );
                                int minIndex = 0;
                                for( int i = 1; i < cycle.size(); i++ )
                                    if( cycle.get( i ).compareTo( cycle.get( minIndex ) ) < 0 )
                                        minIndex = i;
                                List<String> sortedCycle = new ArrayList<>( cycle.subList( minIndex, cycle.size() ) );
                                sortedCycle.addAll( cycle.subList( 0, minIndex ) );
                                throw new Exception( "Cyclic dependency: " + sortedCycle );
                            }
                            if( !visited.contains( child ) )
                            {
                                path.add( child );
                                active.add( child );
                                goBack = false;
                                break;
                            }
                        }
                        if( goBack )
                        {
                            active.remove( path.getLast() );
                            visited.add( path.getLast() );
                            path.removeLast();
                        }
                    }
                }
            }
            catch( Exception e )
            {
                collectError(e.getMessage());
                if(errors.size() > 200)
                    break;
                continue;
            }
            break;
        }
        //TODO: try to resolve some of this dependencies
        // This is the current state of cyclic dependencies
        List<String> expectedCycles = Arrays.asList(
                "Cyclic dependency: [biouml.plugins.simulation, biouml.plugins.state, biouml.workbench]",
                "Cyclic dependency: [biouml.workbench, biouml.workbench.graph]", "Cyclic dependency: [biouml.workbench, ru.biosoft.access]",
                "Cyclic dependency: [biouml.workbench, ru.biosoft.bsa]",
                "Cyclic dependency: [biouml.workbench, ru.biosoft.graphics, ru.biosoft.access, ru.biosoft.table]",
                "Cyclic dependency: [biouml.workbench, ru.biosoft.math, ru.biosoft.access, ru.biosoft.workbench]",
                "Cyclic dependency: [biouml.workbench, ru.biosoft.plugins, ru.biosoft.access, ru.biosoft.workbench.editors]",
                "Cyclic dependency: [biouml.workbench, ru.biosoft.plugins.javascript, ru.biosoft.table, ru.biosoft.analysis]",
                //"Cyclic dependency: [biouml.plugins.research, biouml.plugins.server, biouml.workbench, ru.biosoft.server.servlets]",
                "Cyclic dependency: [ru.biosoft.analysiscore, ru.biosoft.plugins.javascript, ru.biosoft.workbench]",
                "Cyclic dependency: [ru.biosoft.table, ru.biosoft.workbench]",
                //"Cyclic dependency: [biouml.plugins.server, ru.biosoft.server, ru.biosoft.server.servlets]",
                "Cyclic dependency: [biouml.plugins.sbgn, biouml.plugins.sbml]",
                "Cyclic dependency: [biouml.plugins.server, biouml.workbench]",
                "Cyclic dependency: [biouml.plugins.research, biouml.plugins.server, ru.biosoft.server, ru.biosoft.server.servlets]",
                "Cyclic dependency: [biouml.workbench, ru.biosoft.server.servlets]" 
        );

        List<String> unexpectedCycles = errors.stream().
                filter( line -> !expectedCycles.contains(line) )
                .collect(Collectors.toList());
        List<String> extraCycles = expectedCycles.stream()
                .filter( cycle -> !errors.contains( cycle ) )
                .collect(Collectors.toList());
        if( !unexpectedCycles.isEmpty() || !extraCycles.isEmpty() )
            failOnErrors( "Unexpected cycles: " + unexpectedCycles + "\nExtra cycles: " + extraCycles );
    }

    /** Create diagram of plugin dependencies */
    public void testCreateDiagram() throws Exception
    {
        DataCollection parent = CollectionFactory.createRepository( "../data/test/Diagrams" );
        Diagram diagram = new Diagram( parent, new Stub( null, "Plugins structure diagram" ), new PathwayDiagramType() );

        for( String id : plugins.keySet() )
        {
            Stub stub = new Stub( null, id.replace( '.', '-' ), Stub.TYPE_CONCEPT );
            stub.setTitle( id );
            biouml.model.Node node = new biouml.model.Node( diagram, stub );
            diagram.put( node );
        }

        for( Map.Entry<String, Plugin> entry : plugins.entrySet() )
        {
            String id = entry.getKey();
            List<String> requires = entry.getValue().getDependencies();
            id = id.replace( '.', '-' );
            biouml.model.Node node1 = (biouml.model.Node)diagram.get( id );
            for( String target : requires )
            {
                target = target.replace( '.', '-' );
                biouml.model.Node node2 = (biouml.model.Node)diagram.get( target );
                if( node1 != null && node2 != null )
                {
                    Stub stub = new Stub( null, node1.getName() + "->" + node2.getName(), Stub.TYPE_SEMANTIC_RELATION );
                    stub.setTitle( "" );
                    Edge edge = new Edge( diagram, stub, node1, node2 );
                    edge.setTitle( "" );
                    diagram.put( edge );
                }
            }
        }

        parent.put( diagram );
    }

    /** Create diagram of plugin dependencies, only plugins with cyclic dependencies included */
    public void testCycleDiagram() throws Exception
    {
        Set<String> cyclic = new HashSet<>();
        Set<String> normal = new HashSet<>();
        for( String plugin : plugins.keySet() )
        {
            if( cyclic.contains( plugin ) || normal.contains( plugin ) )
                continue;
            LinkedList<String> path = new LinkedList<>();
            path.add( plugin );

            Set<String> active = new HashSet<>();
            active.add( plugin );

            while( !path.isEmpty() )
            {
                List<String> childs = plugins.get( path.getLast() ).getDependencies();

                boolean goBack = true;
                for( String child : childs )
                {
                    if( !plugins.containsKey( child ) )
                        //unmet or optional dependency
                        continue;
                    if( active.contains( child ) )
                    {
                        cyclic.addAll( path.subList( path.indexOf( child ), path.size() ) );
                        continue;
                    }
                    if( !normal.contains( child ) )
                    {
                        path.add( child );
                        active.add( child );
                        goBack = false;
                        break;
                    }
                }
                if( goBack )
                {
                    active.remove( path.getLast() );
                    normal.add( path.getLast() );
                    path.removeLast();
                }
            }
        }

        DataCollection parent = CollectionFactory.createRepository( "../data/test/Diagrams" );
        Diagram diagram = new Diagram( parent, new Stub( null, "Cyclic plugins" ), new PathwayDiagramType() );

        for( String id : cyclic )
        {
            Stub stub = new Stub( null, id.replace( '.', '-' ), Stub.TYPE_CONCEPT );
            stub.setTitle( id );
            biouml.model.Node node = new biouml.model.Node( diagram, stub );
            diagram.put( node );
        }

        for( String id : cyclic )
        {
            List<String> requires = plugins.get( id ).getDependencies();
            id = id.replace( '.', '-' );
            biouml.model.Node node1 = (biouml.model.Node)diagram.get( id );
            for( String target : requires )
                if( cyclic.contains( target ) )
                {
                    target = target.replace( '.', '-' );
                    biouml.model.Node node2 = (biouml.model.Node)diagram.get( target );
                    if( node1 != null && node2 != null )
                    {
                        Stub stub = new Stub( null, node1.getName() + "->" + node2.getName(), Stub.TYPE_SEMANTIC_RELATION );
                        stub.setTitle( " " );
                        Edge edge = new Edge( diagram, stub, node1, node2 );
                        edge.setTitle( " " );
                        diagram.put( edge );
                    }
                }
        }

        diagram.getViewOptions().setAutoLayout( true );

        parent.put( diagram );
    }


    public void testDuplicateClasses() throws IOException
    {
        Map<String, Plugin> classToPlugin = new HashMap<>();
        for( Plugin plugin : plugins.values() )
        {
            for( String clazz : plugin.getExportedClasses() )
            {
                if( classToPlugin.containsKey( clazz ) )
                    collectError( "Class " + clazz + " is exported by 2 or more plugins (" + plugin + "," + classToPlugin.get( clazz )
                            + ")" );
                classToPlugin.put( clazz, plugin );
            }
        }
        failOnErrors();
    }

    public void testUnseenPackages() throws Exception
    {
        for( Plugin plugin : plugins.values() )
        {
            //TODO: fix beakerx
            if( plugin.getName().equals( "biouml.plugins.beakerx" ) )
                continue;
            for(String packageName : plugin.getUnseenPackages())
            {
                collectError( "Plugin "+plugin.getName()+" exports unknown package "+packageName );
            }
        }
        failOnErrors();
    }

    public void testRequiredClassesAvailable() throws Exception
    {
        Map<String, List<String>> absentClasses = new HashMap<>();
        for( Plugin plugin : plugins.values() )
        {
            String name = plugin.getName();
            if( name.equals( "biouml.plugins.junittest" ) )
                //this plugin depend on all plugins to run tests
                continue;
            if( name.equals( "biouml.plugins.pride" ) )
                //Ignore pride plugin that requires itext-2.0.7.jar that has unsatisfied internal dependencies and no sources to recompile
                continue;
            //Check only plugins with our code
            if( Plugin.isBioUMLPrefix( name ) )
            {
                Set<String> availableClasses = new HashSet<>();
                for( String dep : plugin.getDependencies() )
                    if( plugins.containsKey( dep ) )
                    {
                        List<String> exportedClasses = plugins.get( dep ).getExportedClasses();
                        availableClasses.addAll( exportedClasses );
                    }

                //Check requirements of biouml classes
                for( String requiredClass : plugin.getBioUMLRequiredClasses() )
                {
                    if( !availableClasses.contains( requiredClass ) )
                    {
                        absentClasses.computeIfAbsent( plugin.getName(), k -> new ArrayList<>() ).add( requiredClass );
                    }
                }
            }
        }
        if( !absentClasses.isEmpty() )
        {
            collectError( "Plugins with unresolved classes: " + StringUtils.join( absentClasses.keySet(), ",\n " ) );
            Map<String, String> classToPlugin = new HashMap<>();
            for( Plugin plugin : plugins.values() )
                for( String clazz : plugin.getExportedClasses() )
                    classToPlugin.put( clazz, plugin.getName() );

            for( Map.Entry<String , List<String>> entry : absentClasses.entrySet() )
            {
                String plugin = entry.getKey();
                Set<String> requiredPlugins = new HashSet<>();
                List<String> unknownClasses = new ArrayList<>();
                for( String unresolvedClass : entry.getValue() )
                {
                    String foundPlugin = classToPlugin.get( unresolvedClass );
                    if( foundPlugin == null )
                        unknownClasses.add( unresolvedClass );
                    else
                        requiredPlugins.add( foundPlugin );
                }
                if( !requiredPlugins.isEmpty() )
                    collectError( "Plugin " + plugin + " needs additional dependencies: " + StringUtils.join( requiredPlugins, ",\n " ) );
                if( !unknownClasses.isEmpty() )
                    collectError( "Plugin " + plugin + " has unresolved classes: " + StringUtils.join( unknownClasses, ",\n " ) );
            }
        }
        // This is the current state of required classes
        List<String> expectedErrors = Arrays.asList(
                "Plugin biouml.plugins.googledrive needs additional dependencies: com.developmentontheedge.beans",
                "Plugin ru.biosoft.server needs additional dependencies: biouml.plugins.server",
                "Plugin biouml.plugins.ensembl needs additional dependencies: biouml.plugins.server",
                "Plugin biouml.plugins.enrichment needs additional dependencies: biouml.plugins.biopax",
                "Plugin biouml.plugins.dropbox needs additional dependencies: com.developmentontheedge.beans",
                "Plugin biouml.plugins.expasy needs additional dependencies: ru.biosoft.bsa",
                "Plugin ru.biosoft.plugins.jri needs additional dependencies: com.developmentontheedge.beans",
                "Plugin biouml.plugins.simulation needs additional dependencies: biouml.plugins.server",
                "Plugin biouml.plugins.endonet needs additional dependencies: biouml.plugins.enrichment",
                "Plugin biouml.plugins.node needs additional dependencies: com.google.api.services.drive" );

        List<String> unexpectedErrors = errors.stream()
                .filter(line -> line.startsWith("Plugin ") && !expectedErrors.contains(line))
                .collect(Collectors.toList());
        if( !unexpectedErrors.isEmpty() )
            failOnErrors("Unexpected errors: " + unexpectedErrors);
    }

    public void testNoExtraDependencies() throws Exception
    {
        for( Plugin plugin : plugins.values() )
            if( Plugin.isBioUMLPrefix(plugin.getName()) )
                for( String extraDep : plugin.findExtraDependencies(plugins) )
                    collectError("Extra dependency of " + plugin + " on " + extraDep);
        failOnErrors();
    }

    private final List<String> errors = new ArrayList<>();
    private void collectError(String msg)
    {
        errors.add( msg );
    }

    private void failOnErrors(String error)
    {
        if( !errors.isEmpty() )
            fail( error + "\n["+errors.size()+" errors]:\n"+StringUtils.join( errors, '\n' ) );
    }

    private void failOnErrors()
    {
        if( !errors.isEmpty() )
            fail( "["+errors.size()+" errors]:\n"+StringUtils.join( errors, '\n' ) );
    }

    public void formatManifest() throws Exception
    {
        for( Plugin plugin : plugins.values() )
        {
            if( plugin.getPluginFile().isDirectory() )
            {
                File manifestFile = new File(plugin.getPluginFile(), "META-INF/MANIFEST.MF");
                Manifest manifest = readManifest(manifestFile);
                for(String key : new String[] {"Require-Bundle", "Export-Package"})
                {
                    String value = manifest.getMainAttributes().getValue(key);
                    if(value == null) continue;
                    value = StreamEx.split(value, ',').sorted().joining( ",\n" );
                    manifest.getMainAttributes().putValue(key, value);
                }
                writeManifest(manifest, manifestFile);
            }
        }
    }

    public void fixExtraDependencies() throws Exception
    {
        for( Plugin plugin : plugins.values() )
            if( plugin.getPluginFile().isDirectory() )
            {
                List<String> extraDeps = plugin.findExtraDependencies(plugins);

                File manifestFile = new File(plugin.getPluginFile(), "META-INF/MANIFEST.MF");
                Manifest manifest = readManifest(manifestFile);

                List<String> dependencies = new ArrayList<>(plugin.getDependencies());
                dependencies.removeAll(extraDeps);

                if( dependencies.isEmpty() )
                {
                    manifest.getMainAttributes().remove(new Attributes.Name("Require-Bundle"));
                }
                else
                {
                    dependencies.replaceAll( dep -> plugin.isOptionalDependency(dep) ? dep + ";resolution:=optional" : dep );

                    manifest.getMainAttributes().putValue("Require-Bundle", StringUtils.join(dependencies, ","));
                }

                writeManifest(manifest, manifestFile);
            }
        formatManifest();
    }

    private Manifest readManifest(File file) throws IOException
    {
        Manifest manifest;
        try (FileInputStream is = new FileInputStream( file ))
        {
            manifest = new Manifest(is);
        }
        return manifest;
    }

    private void writeManifest(Manifest manifest, File file) throws IOException
    {
        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8")))
        {
            Attributes attrs = manifest.getMainAttributes();

            String[] order = new String[] {"Manifest-Version",
                    "Bundle-ManifestVersion",
                    "Bundle-Localization",
                    "Bundle-Name",
                    "Bundle-SymbolicName",
                    "Bundle-Version",
                    "Bundle-Vendor",
                    "Bundle-Activator",
                    "Bundle-ActivationPolicy",
                    "Require-Bundle",
                    "Bundle-ClassPath",
                    "Export-Package"};

            for( String key : order )
            {
                String value = attrs.getValue(key);
                if(value == null) continue;
                writer.write(key);
                writer.write(": ");
                List<String> lines = new ArrayList<>();
                for( String line : value.split("\n") )
                    if( line.length() <= 70 )
                        lines.add(line);
                    else
                    {
                        for( int i = 0; i < line.length(); i += 70 )
                            lines.add(line.substring(i, Math.min(line.length(), i + 70)));
                    }
                for(String line : lines)
                {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
    }
}
