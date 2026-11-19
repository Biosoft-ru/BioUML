package ru.biosoft.plugins.javascript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mozilla.javascript.ClassShutter;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;
import org.mozilla.javascript.Context;

/**
 * Limits the Java classes a server-side JavaScript may use.
 *
 * <p>Rhino (1.7R3) asks the shutter for every Java class a script names ({@code Packages.x.Y}, {@code importClass})
 * and for the exact runtime class of every Java object handed to a script, a host object's return values included:
 * a class that is not visible fails the script with "Access to Java class ... is prohibited". There is no fallback to
 * a visible superclass, so the list names the concrete classes and packages the scripting API returns.</p>
 *
 * <p>A class is visible when an {@code allow} entry matches it and no {@code deny} entry does. An entry ending with
 * {@code .} or {@code $} is a prefix (a package, or the nested classes of a class), any other entry one class. The
 * built-in list covers what the Genome Enhancer scripts need; a file named by {@value #FILE_PROPERTY} adds entries,
 * one per line: {@code allow <entry>} or {@code deny <entry>}, {@code #} starting a comment. The classes of the host
 * objects declared in plugin.xml are allowed as they are defined ({@link #allowHostObjectClass}).</p>
 *
 * <p>The mode, the {@value #MODE_PREFERENCE} entry of the server's preferences.xml (read once, at the first script):
 * {@code enforce} hides what the list does not allow; {@code audit}, the default, allows it but logs each such class
 * once, so the list can be completed from a real run before enforcing; {@code off} installs no shutter.</p>
 */
public class ScriptClassShutter implements ClassShutter
{
    private static final Logger log = Logger.getLogger( ScriptClassShutter.class.getName() );

    /** The preferences.xml entry with the mode: enforce, audit or off. */
    public static final String MODE_PREFERENCE = "JavaScriptClassShutter";
    public static final String FILE_PROPERTY = "biouml.javascript.classShutter.file";

    public enum Mode
    {
        ENFORCE, AUDIT, OFF
    }

    /**
     * Visible to scripts by default: value types, collections, and the BioUML data API the Genome Enhancer scripts
     * (megaworkflow script nodes and reports) use and get back from the host objects.
     */
    static final String[] DEFAULT_ALLOW = {
            // values a script reads and passes back
            "java.lang.String", "java.lang.Boolean", "java.lang.Character", "java.lang.Number", "java.lang.Byte",
            "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double",
            "java.lang.StringBuilder", "java.lang.StringBuffer", "java.lang.Object", "java.math.",
            // collections, dates and their iterators (narrowed by DEFAULT_DENY)
            "java.util.",
            // images (DiagramImageGenerator.generateDiagramImage, image elements, report.addImage) and geometry values
            "java.awt.image.BufferedImage", "java.awt.Dimension", "java.awt.Point", "java.awt.Rectangle", "java.awt.Color",
            // strings built by the concat host function
            "ru.biosoft.util.LazyStringBuilder", "ru.biosoft.util.LazySubSequence",
            // the data API: paths, collections, tables, rows and columns, diagrams
            "ru.biosoft.access.core.", "ru.biosoft.access.", "ru.biosoft.table.", "biouml.model.", "biouml.standard.type.",
            // scripting host objects and reports
            "ru.biosoft.plugins.javascript.", "ru.biosoft.plugins.jsreport.",
            // the model of an SBML diagram (diagram.getRole())
            "biouml.plugins.sbml.SbmlEModel",
            // tracks, genome browser projects and CMA results, diagram views and charts, element properties: seen in
            // the audit log of Genome Enhancer runs on strange (MapAsVector, ProjectAsLists, ExtendedGeneTrack,
            // FullModelTrack, CompositeView, ChartDataElement, PropertiesDPS)
            "ru.biosoft.bsa.", "biouml.plugins.ensembl.tracks.", "biouml.plugins.cma.", "ru.biosoft.graphics.",
            "com.developmentontheedge.beans.PropertiesDPS" };

    /** Never visible, even inside an allowed package: what reaches past the data API and its permission checks. */
    static final String[] DEFAULT_DENY = {
            // java.util parts that load classes, start threads, or reach files
            "java.util.ServiceLoader", "java.util.concurrent.", "java.util.prefs.", "java.util.logging.", "java.util.jar.",
            "java.util.zip.", "java.util.spi.", "java.util.Timer",
            // the repository registry, class loading, security and sessions
            "ru.biosoft.access.core.CollectionFactory", "ru.biosoft.access.core.ClassLoading", "ru.biosoft.access.core.Environment",
            "ru.biosoft.access.CollectionFactoryUtils", "ru.biosoft.access.BiosoftSecurityManager", "ru.biosoft.access.ClassLoading",
            "ru.biosoft.access.security.SecurityManager", "ru.biosoft.access.security.SessionCache",
            // direct database access and running other script types (R, Java, ...) from a script
            "ru.biosoft.access.sql.", "ru.biosoft.access.script.ScriptTypeRegistry",
            // reading and writing files by path, and server connections, inside the track packages
            "ru.biosoft.bsa.importer.", "ru.biosoft.bsa.exporter.", "ru.biosoft.bsa.server.",
            // the scripting engine itself
            "ru.biosoft.plugins.javascript.ScriptClassShutter", "ru.biosoft.plugins.javascript.JScriptContext" };

    private static volatile ScriptClassShutter instance;

    private final Mode mode;
    private final List<String> allow = new ArrayList<>();
    private final List<String> deny = new ArrayList<>();
    private final Set<String> hostObjectClasses = ConcurrentHashMap.newKeySet();
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    ScriptClassShutter(Mode mode, List<String> extraAllow, List<String> extraDeny)
    {
        this.mode = mode;
        Collections.addAll( allow, DEFAULT_ALLOW );
        Collections.addAll( deny, DEFAULT_DENY );
        allow.addAll( extraAllow );
        deny.addAll( extraDeny );
    }

    /** The shutter configured by the system properties, created on first use. */
    public static ScriptClassShutter getInstance()
    {
        ScriptClassShutter result = instance;
        if( result == null )
        {
            synchronized( ScriptClassShutter.class )
            {
                result = instance;
                if( result == null )
                    instance = result = fromConfiguration();
            }
        }
        return result;
    }

    private static ScriptClassShutter fromConfiguration()
    {
        Mode mode = modeFrom( Application.getPreferences() );
        List<String> extraAllow = new ArrayList<>(), extraDeny = new ArrayList<>();
        String file = System.getProperty( FILE_PROPERTY );
        if( file != null && !file.isEmpty() )
        {
            try
            {
                parseEntries( Files.readAllLines( Paths.get( file ), StandardCharsets.UTF_8 ), extraAllow, extraDeny );
            }
            catch( IOException | IllegalArgumentException e )
            {
                log.log( Level.SEVERE, "JavaScript class list " + file + " not read, built-in list only: " + e.getMessage() );
            }
        }
        log.info( "JavaScript class shutter: mode " + mode.name().toLowerCase() + ", " + ( DEFAULT_ALLOW.length + extraAllow.size() )
                + " allow and " + ( DEFAULT_DENY.length + extraDeny.size() ) + " deny entries" );
        return new ScriptClassShutter( mode, extraAllow, extraDeny );
    }

    /** The shutter for a mode and the lines of a class-list file, with the built-in list. */
    public static ScriptClassShutter create(Mode mode, List<String> fileLines)
    {
        List<String> extraAllow = new ArrayList<>(), extraDeny = new ArrayList<>();
        parseEntries( fileLines, extraAllow, extraDeny );
        return new ScriptClassShutter( mode, extraAllow, extraDeny );
    }

    /** The mode the preferences set ({@value #MODE_PREFERENCE}); audit without preferences or the entry. */
    public static Mode modeFrom(Preferences preferences)
    {
        return parseMode( preferences == null ? null : preferences.getStringValue( MODE_PREFERENCE, null ) );
    }

    /** The mode a {@value #MODE_PREFERENCE} value names: audit when unset, enforce when unknown. */
    public static Mode parseMode(String value)
    {
        if( value == null || value.trim().isEmpty() )
            return Mode.AUDIT;
        try
        {
            return Mode.valueOf( value.trim().toUpperCase() );
        }
        catch( IllegalArgumentException e )
        {
            // an unknown mode must not switch the shutter off
            log.severe( "Unknown " + MODE_PREFERENCE + " '" + value + "' in preferences.xml, using enforce" );
            return Mode.ENFORCE;
        }
    }

    static void parseEntries(List<String> lines, List<String> allow, List<String> deny)
    {
        for( String line : lines )
        {
            int hash = line.indexOf( '#' );
            String text = ( hash >= 0 ? line.substring( 0, hash ) : line ).trim();
            if( text.isEmpty() )
                continue;
            String[] parts = text.split( "\\s+" );
            if( parts.length != 2 )
                throw new IllegalArgumentException( "expected 'allow <entry>' or 'deny <entry>': " + line );
            if( parts[0].equals( "allow" ) )
                allow.add( parts[1] );
            else if( parts[0].equals( "deny" ) )
                deny.add( parts[1] );
            else
                throw new IllegalArgumentException( "expected 'allow <entry>' or 'deny <entry>': " + line );
        }
    }

    /**
     * Use this shutter for the contexts entered from now on; null configures it from preferences.xml again.
     * For tests and embedders (Java code; scripts cannot see this class).
     */
    public static void setInstance(ScriptClassShutter shutter)
    {
        instance = shutter;
    }

    public Mode getMode()
    {
        return mode;
    }

    /** Install the shutter on a context just entered, before any script or host object uses it. */
    public static void install(Context context)
    {
        ScriptClassShutter shutter = getInstance();
        if( shutter.mode == Mode.OFF )
            return;
        try
        {
            context.setClassShutter( shutter );
        }
        catch( SecurityException e )
        {
            // a context gets its shutter once; this one already has it
        }
    }

    /** A host object declared in plugin.xml: its class is vetted by the declaration and stays visible. */
    public static void allowHostObjectClass(Class<?> type)
    {
        getInstance().hostObjectClasses.add( type.getName() );
    }

    /**
     * Whether a value would be hidden from scripts: in enforce mode, its class is not on the list. Binding such a
     * value into a scope fails the whole script, as Rhino checks a Java object's class when it wraps it; a caller
     * that binds many values of which a script may use few (the variables of a workflow) leaves these out instead.
     * Always false in audit and off mode, where everything stays visible.
     */
    public static boolean hides(Object value)
    {
        ScriptClassShutter shutter = getInstance();
        if( value == null || shutter.mode != Mode.ENFORCE || value instanceof org.mozilla.javascript.Scriptable )
            return false;
        return !shutter.isAllowed( value.getClass().getName() );
    }

    @Override
    public boolean visibleToScripts(String fullClassName)
    {
        if( isAllowed( fullClassName ) )
            return true;
        // Rhino also asks about each package prefix while resolving Packages.a.b.C: not worth a log line
        if( !isPackageName( fullClassName ) && reported.add( fullClassName ) )
        {
            if( mode == Mode.AUDIT )
                log.warning( "JavaScript used Java class " + fullClassName + ", which the class list does not allow (audit mode: allowed)" );
            else
                log.warning( "JavaScript was denied Java class " + fullClassName );
        }
        return mode == Mode.AUDIT;
    }

    /**
     * Whether a name looks like one Rhino probes rather than a class: its last segment does not start in upper case
     * (a package such as "ru.biosoft", or a member probe such as "java.lang.System.out.__noSuchMethod__").
     */
    private static boolean isPackageName(String name)
    {
        String last = name.substring( name.lastIndexOf( '.' ) + 1 );
        return last.isEmpty() || !Character.isUpperCase( last.charAt( 0 ) );
    }

    boolean isAllowed(String fullClassName)
    {
        String name = componentName( fullClassName );
        if( name == null )
            return true; // an array of primitives
        if( matches( deny, name ) )
            return false;
        return hostObjectClasses.contains( name ) || matches( allow, name );
    }

    /** The element class of an array class name ("[Ljava.lang.String;"), null for a primitive array. */
    private static String componentName(String name)
    {
        if( !name.startsWith( "[" ) )
            return name;
        String element = name.replaceFirst( "^\\[+", "" );
        if( element.startsWith( "L" ) && element.endsWith( ";" ) )
            return element.substring( 1, element.length() - 1 );
        return null;
    }

    private static boolean matches(List<String> entries, String name)
    {
        for( String entry : entries )
        {
            if( entry.endsWith( "." ) || entry.endsWith( "$" ) ? name.startsWith( entry ) : name.equals( entry ) )
                return true;
        }
        return false;
    }
}
