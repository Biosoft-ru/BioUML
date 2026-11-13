package biouml.plugins.mcp.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Guides for agents: what an analysis or a workflow is for, what it takes and gives, what usually follows it —
 * and research topics, which lead from a research question ("what do my genes have in common?") to the analyses
 * and workflows that answer it. The parameter schema of an analysis says how to call it; a guide says when and
 * why, in the words of someone who has never used the platform.
 *
 * <p>Guides are text files bundled with this plugin ({@code resources/analysis-guides}, listed by the
 * {@code files} key of its {@code index.properties}); the {@code *.txt} files of the {@code analysis-guides}
 * folder of the {@value McpPerspectiveSupport#GUIDES_PROPERTY} directory are read after them, so a server can add
 * guides for its own analyses or replace bundled ones. A file is a sequence of entries:</p>
 *
 * <pre>
 * # a comment
 * == analysis: Enrichment analysis
 * summary: one line, shown in biouml_analysis_list
 * use: when to pick it
 * input: what it needs (identifier types, formats)
 * output: what it produces and how to read it
 * next: what usually follows
 * tips: pitfalls
 * param inputTable: what this parameter means and which values make sense
 * param expertSettings/cutoff: a nested parameter, by its key
 *
 * == workflow: Common/Explain my genes || Common/Another workflow sharing this entry
 * summary: ...
 *
 * == topic: gene-list | I have a list of genes
 * summary: one line, shown in the topic list
 * free text: the body of the topic
 *
 * == parameters: common
 * param outputTable: for a parameter of this name in any analysis whose own description says nothing
 * </pre>
 *
 * <p>A line that does not start with a field name continues the field above it. A workflow is named by its path
 * below the workflows root ({@code folder/name}) or by its name alone.</p>
 *
 * <p>A {@code param} line describes a parameter for an agent where the analysis itself does not: many declare
 * a description that is empty or repeats the parameter's name ("Promoter from"). The parameter is named by its
 * key as biouml_analysis_describe lists it, or by the last part of the key; the index of an array item is left
 * out ({@code conditions/condition}). See {@link #describeParameters}.</p>
 */
public final class McpGuides
{
	private static final Logger log = Logger.getLogger( McpGuides.class.getName() );

	public static final String ANALYSIS = "analysis";
	public static final String WORKFLOW = "workflow";
	public static final String TOPIC = "topic";
	/** An entry whose {@code param} lines hold for the parameters of that name of every analysis and workflow. */
	public static final String PARAMETERS = "parameters";
	private static final String PARAM = "param ";

	/** The fields of an analysis or workflow guide, in the order they are returned. */
	public static final List<String> FIELDS = Collections.unmodifiableList( Arrays.asList( "summary", "use", "input", "output", "next", "tips" ) );

	private static final String WORKFLOW_NAME = "workflow name";
	private static final String RESOURCE = "resources/analysis-guides/";
	private static final String INDEX = "index.properties";
	private static final String EXTERNAL_FOLDER = "analysis-guides";
	private static final String ENTRY = "== ";
	private static final String ALIASES = "\\s*\\|\\|\\s*";

	/** One entry of a guide file. */
	public static final class Guide
	{
		public final String kind;
		/** The name of the analysis, the path of the workflow below the workflows root, or the id of the topic. */
		public final String name;
		/** The title of a topic; null otherwise. */
		public final String title;
		private final Map<String, String> fields = new LinkedHashMap<String, String>();
		private Map<String, String> params = new LinkedHashMap<String, String>();
		private final StringBuilder body = new StringBuilder();

		Guide( String kind, String name, String title )
		{
			this.kind = kind;
			this.name = name;
			this.title = title;
		}

		public String getSummary()
		{
			return fields.get( "summary" );
		}

		/** The text of a topic. */
		public String getBody()
		{
			return body.toString().trim();
		}

		/** The fields that are set, in the order of {@link McpGuides#FIELDS}. */
		public Map<String, Object> toMap()
		{
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			for ( String field : FIELDS )
				if ( fields.get( field ) != null )
					m.put( field, fields.get( field ) );
			return m;
		}

		/** Everything the guide says, lower-cased, for searching. */
		String searchText()
		{
			return ( name + " " + ( title == null ? "" : title ) + " " + String.join( " ", fields.values() ) + " " + body ).toLowerCase( Locale.ENGLISH );
		}
	}

	/** The guides of one load: by kind, then by lower-cased name. */
	private static final class Catalog
	{
		final Map<String, Map<String, Guide>> byKind = new LinkedHashMap<String, Map<String, Guide>>();
		final List<Guide> topics = new ArrayList<Guide>();
		final Map<String, String> commonParams = new LinkedHashMap<String, String>();
		long stamp;
		String dir;

		void add( Guide guide, List<String> names )
		{
			Map<String, Guide> guides = byKind.computeIfAbsent( guide.kind, k -> new LinkedHashMap<String, Guide>() );
			for ( String name : names )
				guides.put( key( name ), guide );
			if ( WORKFLOW.equals( guide.kind ) )
			{
				// by name alone too: a copy of a workflow kept elsewhere (e.g. in a project) has no folder to go by
				Map<String, Guide> byName = byKind.computeIfAbsent( WORKFLOW_NAME, k -> new LinkedHashMap<String, Guide>() );
				for ( String name : names )
					byName.put( key( name.substring( name.lastIndexOf( '/' ) + 1 ) ), guide );
			}
			if ( TOPIC.equals( guide.kind ) )
			{
				topics.removeIf( t -> t.name.equalsIgnoreCase( guide.name ) );
				topics.add( guide );
			}
		}
	}

	private static volatile Catalog catalog;

	private McpGuides()
	{
	}

	/** The guide of an analysis, or null. */
	public static Guide analysis( String name )
	{
		return name == null ? null : guides( ANALYSIS ).get( key( name ) );
	}

	/**
	 * The guide of a workflow, or null.
	 * @param relativePath its path below the workflows root, e.g. "Common/Explain my genes"
	 */
	public static Guide workflow( String relativePath )
	{
		if ( relativePath == null )
			return null;
		Map<String, Guide> guides = guides( WORKFLOW );
		Guide guide = guides.get( key( relativePath ) );
		int slash = relativePath.lastIndexOf( '/' );
		return guide != null ? guide : guides( WORKFLOW_NAME ).get( key( relativePath.substring( slash + 1 ) ) );
	}

	/** The research topics, in file order. */
	public static List<Guide> topics()
	{
		return new ArrayList<Guide>( load().topics );
	}

	/** A research topic by its id, or null. */
	public static Guide topic( String id )
	{
		return id == null ? null : guides( TOPIC ).get( key( id ) );
	}

	/**
	 * Describe parameters for an agent, in place. A parameter the guide of its analysis or workflow describes gets
	 * that text: as its {@code description} when the declared one says nothing (it is missing or repeats the
	 * parameter's name), otherwise beside it as {@code hint}. A parameter that is still undescribed gets the text
	 * the {@value #PARAMETERS} entries give for its name, if any.
	 * @param guide the guide of the analysis or workflow, or null
	 * @param properties the parameters as biouml_analysis_describe lists them: maps with name, displayName, description
	 */
	public static void describeParameters( Guide guide, List<Map<String, Object>> properties )
	{
		Map<String, String> common = load().commonParams;
		for ( Map<String, Object> property : properties )
		{
			Object name = property.get( "name" );
			if ( !( name instanceof String ) )
				continue;
			String declared = property.get( "description" ) instanceof String ? (String) property.get( "description" ) : null;
			boolean vague = saysNothing( declared, (String) name, property.get( "displayName" ) );
			String own = guide == null ? null : lookup( guide.params, (String) name );
			if ( own != null && !vague )
				property.put( "hint", own );
			else if ( own != null )
				property.put( "description", own );
			else if ( vague && lookup( common, (String) name ) != null )
				property.put( "description", lookup( common, (String) name ) );
		}
	}

	/** The text for a parameter key: by the key, by the key without array indexes, by its last part. */
	private static String lookup( Map<String, String> params, String key )
	{
		if ( params.isEmpty() )
			return null;
		String text = params.get( key );
		if ( text != null )
			return text;
		String plain = key.replaceAll( "/\\d+(?=/|$)", "" );
		text = params.get( plain );
		return text != null ? text : params.get( plain.substring( plain.lastIndexOf( '/' ) + 1 ) );
	}

	/** Whether a declared description tells no more than the name of the parameter does. */
	public static boolean saysNothing( String description, String key, Object displayName )
	{
		String text = letters( description );
		if ( text.isEmpty() )
			return true;
		String name = key.substring( key.lastIndexOf( '/' ) + 1 );
		return text.equals( letters( name ) ) || text.equals( letters( displayName == null ? null : displayName.toString() ) );
	}

	private static String letters( String text )
	{
		return text == null ? "" : text.toLowerCase( Locale.ENGLISH ).replaceAll( "[^a-z0-9]+", "" );
	}

	/** Whether all {@code words} (lower-cased) occur in the guide or in {@code more} (lower-cased text about the same thing). */
	public static boolean matches( Guide guide, String more, String[] words )
	{
		String text = ( guide == null ? "" : guide.searchText() ) + " " + ( more == null ? "" : more );
		for ( String word : words )
			if ( !text.contains( word ) )
				return false;
		return true;
	}

	/** The words of a search query, lower-cased; empty for a null or blank query. */
	public static String[] words( String query )
	{
		if ( query == null || query.trim().isEmpty() )
			return new String[0];
		return query.trim().toLowerCase( Locale.ENGLISH ).split( "\\s+" );
	}

	private static Map<String, Guide> guides( String kind )
	{
		Map<String, Guide> guides = load().byKind.get( kind );
		return guides == null ? Collections.<String, Guide> emptyMap() : guides;
	}

	private static String key( String name )
	{
		return name.trim().replaceAll( "\\s+", " " ).toLowerCase( Locale.ENGLISH );
	}

	/** The catalog, re-read when the guides directory of the server is set, unset or changed. */
	private static Catalog load()
	{
		String dir = System.getProperty( McpPerspectiveSupport.GUIDES_PROPERTY );
		File[] external = externalFiles( dir );
		long stamp = external.length;
		for ( File file : external )
			stamp = stamp * 31 + file.lastModified() + file.length();
		Catalog current = catalog;
		if ( current != null && current.stamp == stamp && java.util.Objects.equals( current.dir, dir ) )
			return current;
		Catalog loaded = new Catalog();
		loaded.stamp = stamp;
		loaded.dir = dir;
		for ( String file : bundledFiles() )
			try (InputStream in = McpGuides.class.getResourceAsStream( RESOURCE + file ))
			{
				if ( in == null )
					log.warning( "No bundled guide file " + file );
				else
					parse( new InputStreamReader( in, StandardCharsets.UTF_8 ), loaded );
			}
			catch ( IOException e )
			{
				log.log( Level.WARNING, "Cannot read the bundled guide file " + file, e );
			}
		for ( File file : external )
			try (Reader reader = Files.newBufferedReader( file.toPath(), StandardCharsets.UTF_8 ))
			{
				parse( reader, loaded );
			}
			catch ( IOException e )
			{
				log.log( Level.WARNING, "Cannot read the guide file " + file, e );
			}
		catalog = loaded;
		return loaded;
	}

	private static File[] externalFiles( String dir )
	{
		if ( dir == null || dir.isEmpty() )
			return new File[0];
		File[] files = new File( dir, EXTERNAL_FOLDER ).listFiles( ( d, name ) -> name.endsWith( ".txt" ) );
		if ( files == null )
			return new File[0];
		Arrays.sort( files );
		return files;
	}

	private static List<String> bundledFiles()
	{
		List<String> files = new ArrayList<String>();
		try (InputStream in = McpGuides.class.getResourceAsStream( RESOURCE + INDEX ))
		{
			if ( in == null )
				return files;
			Properties properties = new Properties();
			properties.load( new InputStreamReader( in, StandardCharsets.UTF_8 ) );
			for ( String file : properties.getProperty( "files", "" ).split( "," ) )
				if ( !file.trim().isEmpty() && !file.contains( "/" ) && !file.contains( "\\" ) && !file.contains( ".." ) )
					files.add( file.trim() );
		}
		catch ( IOException e )
		{
			log.log( Level.WARNING, "Cannot read the index of the bundled guides", e );
		}
		return files;
	}

	/** The entries of guide text, in file order; an entry with several names is returned once. */
	public static List<Guide> parse( String text ) throws IOException
	{
		Catalog parsed = new Catalog();
		parse( new StringReader( text ), parsed );
		List<Guide> guides = new ArrayList<Guide>();
		for ( Map<String, Guide> byName : parsed.byKind.values() )
			for ( Guide guide : byName.values() )
				if ( !guides.contains( guide ) )
					guides.add( guide );
		return guides;
	}

	private static void parse( Reader reader, Catalog into ) throws IOException
	{
		BufferedReader in = new BufferedReader( reader );
		Guide guide = null;
		String field = null;
		for ( String line = in.readLine(); line != null; line = in.readLine() )
		{
			if ( line.startsWith( "#" ) )
				continue;
			if ( line.startsWith( ENTRY ) )
			{
				guide = null;
				field = null;
				int colon = line.indexOf( ':' );
				if ( colon < 0 )
					continue;
				String kind = line.substring( ENTRY.length(), colon ).trim().toLowerCase( Locale.ENGLISH );
				String rest = line.substring( colon + 1 ).trim();
				if ( rest.isEmpty() || !( ANALYSIS.equals( kind ) || WORKFLOW.equals( kind ) || TOPIC.equals( kind ) || PARAMETERS.equals( kind ) ) )
					continue;
				if ( PARAMETERS.equals( kind ) )
				{
					guide = new Guide( kind, rest, null );
					guide.params = into.commonParams; // straight into the shared map: a later file replaces a name
					continue;
				}
				if ( TOPIC.equals( kind ) )
				{
					int bar = rest.indexOf( '|' );
					String id = ( bar < 0 ? rest : rest.substring( 0, bar ) ).trim();
					guide = new Guide( kind, id, bar < 0 ? id : rest.substring( bar + 1 ).trim() );
					into.add( guide, Collections.singletonList( id ) );
				}
				else
				{
					List<String> names = new ArrayList<String>();
					for ( String name : rest.split( ALIASES ) )
						if ( !name.trim().isEmpty() )
							names.add( name.trim() );
					if ( names.isEmpty() )
						continue;
					guide = new Guide( kind, names.get( 0 ), null );
					into.add( guide, names );
				}
				continue;
			}
			if ( guide == null )
				continue;
			String name = fieldName( line, guide );
			String param = name == null ? paramName( line, guide ) : null;
			if ( param != null )
			{
				field = PARAM + param;
				guide.params.put( param, line.substring( line.indexOf( ':' ) + 1 ).trim() );
			}
			else if ( name == null && field != null && field.startsWith( PARAM ) && !TOPIC.equals( guide.kind ) )
			{
				String key = field.substring( PARAM.length() );
				if ( !line.trim().isEmpty() )
					guide.params.put( key, ( guide.params.get( key ) + " " + line.trim() ).trim() );
			}
			else if ( name != null )
			{
				field = name;
				guide.fields.put( field, line.substring( name.length() + 1 ).trim() );
			}
			else if ( TOPIC.equals( guide.kind ) )
			{
				field = null;
				guide.body.append( line ).append( '\n' );
			}
			else if ( field != null && !line.trim().isEmpty() )
				guide.fields.put( field, ( guide.fields.get( field ) + " " + line.trim() ).trim() );
		}
	}

	/** The parameter a line describes ("param inputTable: ..."), or null; a topic has none. */
	private static String paramName( String line, Guide guide )
	{
		if ( TOPIC.equals( guide.kind ) || !line.startsWith( PARAM ) )
			return null;
		int colon = line.indexOf( ':' );
		String name = colon < 0 ? "" : line.substring( PARAM.length(), colon ).trim();
		return name.isEmpty() ? null : name; // a key may have spaces: 'taskProcessorsDPS/Trim low quality/...'
	}

	/** The field a line starts ("use: ..."), or null; a topic has only a summary, the rest is its body. */
	private static String fieldName( String line, Guide guide )
	{
		int colon = line.indexOf( ':' );
		if ( colon <= 0 )
			return null;
		String name = line.substring( 0, colon );
		if ( TOPIC.equals( guide.kind ) )
			return "summary".equals( name ) && guide.getSummary() == null ? name : null;
		return FIELDS.contains( name ) ? name : null;
	}
}
