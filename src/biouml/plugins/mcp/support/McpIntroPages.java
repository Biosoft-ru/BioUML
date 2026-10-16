package biouml.plugins.mcp.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import ru.biosoft.access.core.DataElementPath;

/**
 * Reads a perspective's intro page — the start page the web UI shows for the perspective — so MCP agents get what
 * it offers users: links to analyses, workflows and collections, analysis parameter presets, import folders, help
 * links and the page text.
 *
 * <p>Intro pages belong to the web UI web application ({@code bioumlweb}), not to the BioUML server, so they are
 * read from disk when the web UI is deployed next to the server (see {@link #webRoot()}). Pages are parsed
 * statically: links and string literals of the page scripts are read, scripts are not run. Pages embedded with an
 * iframe or jQuery {@code .load()} are followed within the web application. Parsed pages are cached until a page
 * file (or the war) changes.</p>
 */
public final class McpIntroPages
{
	/** System property with the web UI location, a directory or a .war, when it is not found by default. */
	public static final String WEB_PATH_PROPERTY = "biouml.web.path";

	private static final String WEB_APP = "bioumlweb";
	/** Intro page of a perspective without the intro attribute, as in the web UI (framework.js). */
	private static final String DEFAULT_INTRO = "intro";

	private static final int MAX_DEPTH = 2;
	private static final int MAX_PAGES = 5;
	private static final int MAX_PAGE_BYTES = 2 * 1024 * 1024;
	private static final int MAX_ENTRY_POINTS = 50;
	private static final int MAX_PRESETS = 20;
	private static final int MAX_LINKS = 20;
	private static final int MAX_SUMMARY = 2000;
	private static final int MAX_CONTEXT = 300;
	private static final int MAX_LEAD = 300;

	private static final Pattern DE_LINK = Pattern.compile( "#de=([^&\"']+)" );
	private static final Pattern ANALYSIS_CONTROL = Pattern.compile(
			"showAnalysisControl\\s*\\(\\s*[^,]+,\\s*([\"'])([^\"']+)\\1\\s*(?:,\\s*\\{(.*?)\\}\\s*\\))?", Pattern.DOTALL );
	private static final Pattern PRESET_PARAM = Pattern.compile(
			"\\{\\s*name\\s*:\\s*([\"'])([^\"']+)\\1\\s*,\\s*value\\s*:\\s*([\"'])([^\"']*)\\3\\s*\\}" );
	private static final Pattern IMPORT_TARGET = Pattern.compile(
			"(?:doImport\\w*\\s*\\(|\\buserData\\s*=)\\s*([\"'])([^\"']+)\\1" );
	private static final Pattern DATA_COLLECTION = Pattern.compile( "getDataCollection\\s*\\(\\s*([\"'])([^\"']+)\\1" );
	private static final Pattern LOAD_PAGE = Pattern.compile( "\\.load\\s*\\(\\s*([\"'])([^\"']+)\\1" );

	private static final Map<String, Intro> CACHE = new ConcurrentHashMap<String, Intro>();

	private McpIntroPages()
	{
	}

	/**
	 * Parsed intro page together with the pages it embeds. Not modified after parsing.
	 */
	public static final class Intro
	{
		private final List<String> pages = new ArrayList<String>();
		private final StringBuilder text = new StringBuilder();
		private final Map<String, Map<String, Object>> entryPoints = new LinkedHashMap<String, Map<String, Object>>();
		private final Map<String, Map<String, String>> presets = new LinkedHashMap<String, Map<String, String>>();
		private final Set<String> importTargets = new LinkedHashSet<String>();
		private final Map<String, String> help = new LinkedHashMap<String, String>();
		private String title;
		private String lead;
		private long version;

		/**
		 * @return short description of the page (its first heading and paragraph), or null
		 */
		public String getDescription()
		{
			if ( title == null )
				return lead;
			return lead == null ? title : title + ": " + lead;
		}

		/**
		 * @param knownPaths entry points the caller already reports (e.g. from the perspective's XML); they are skipped
		 * @return JSON-serializable form; entry points carry an {@code exists} flag for the current session
		 */
		public Map<String, Object> toMap( Collection<String> knownPaths )
		{
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "source", pages.get( 0 ) );
			if ( pages.size() > 1 )
				m.put( "embeddedPages", new ArrayList<String>( pages.subList( 1, pages.size() ) ) );
			if ( title != null )
				m.put( "title", title );
			if ( text.length() > 0 )
				m.put( "summary", truncate( text.toString(), MAX_SUMMARY ) );
			List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
			for ( Map<String, Object> entry : entryPoints.values() )
			{
				if ( knownPaths.contains( entry.get( "path" ) ) )
					continue;
				if ( entries.size() == MAX_ENTRY_POINTS )
				{
					m.put( "entryPointsTruncated", Boolean.TRUE );
					break;
				}
				Map<String, Object> copy = new LinkedHashMap<String, Object>( entry );
				copy.put( "exists", Boolean.valueOf( exists( (String) entry.get( "path" ) ) ) );
				entries.add( copy );
			}
			if ( !entries.isEmpty() )
				m.put( "entryPoints", entries );
			List<Map<String, Object>> presetList = new ArrayList<Map<String, Object>>();
			for ( Map.Entry<String, Map<String, String>> preset : presets.entrySet() )
			{
				Map<String, Object> p = new LinkedHashMap<String, Object>();
				p.put( "analysis", preset.getKey() );
				if ( !preset.getValue().isEmpty() )
					p.put( "params", new LinkedHashMap<String, String>( preset.getValue() ) );
				presetList.add( p );
			}
			if ( !presetList.isEmpty() )
				m.put( "analysisPresets", presetList );
			if ( !importTargets.isEmpty() )
				m.put( "importTargets", new ArrayList<String>( importTargets ) );
			List<Map<String, Object>> helpList = new ArrayList<Map<String, Object>>();
			for ( Map.Entry<String, String> link : help.entrySet() )
			{
				Map<String, Object> h = new LinkedHashMap<String, Object>();
				h.put( "url", link.getKey() );
				if ( !link.getValue().isEmpty() )
					h.put( "title", link.getValue() );
				helpList.add( h );
			}
			if ( !helpList.isEmpty() )
				m.put( "help", helpList );
			return m;
		}

		private void addEntryPoint( String path, String title, String context )
		{
			if ( path == null || path.isEmpty() || entryPoints.containsKey( path ) || entryPoints.size() >= MAX_ENTRY_POINTS * 4 )
				return;
			Map<String, Object> entry = new LinkedHashMap<String, Object>();
			entry.put( "path", path );
			entry.put( "title", title == null || title.isEmpty() ? DataElementPath.create( path ).getName() : title );
			if ( context != null )
				entry.put( "context", context );
			entryPoints.put( path, entry );
		}

		private void addPreset( String analysis, Map<String, String> params )
		{
			Map<String, String> known = presets.get( analysis );
			if ( known == null && presets.size() >= MAX_PRESETS )
				return;
			// the same analysis may be shown twice (e.g. re-created with other values); keep the richer preset
			if ( known == null || known.size() < params.size() )
				presets.put( analysis, params );
		}
	}

	/**
	 * @param intro the perspective's intro page name (its "intro" attribute), or null for the default intro page
	 * @return the parsed page, or null when the web UI files are not found or there is no such page
	 */
	public static Intro read( String intro )
	{
		WebRoot root = webRoot();
		if ( root == null )
			return null;
		String page = normalize( ( intro == null || intro.isEmpty() ? DEFAULT_INTRO : intro ) + ".html" );
		if ( page == null )
			return null;
		String key = root.location() + "|" + page;
		Intro cached = CACHE.get( key );
		if ( cached != null && cached.version == version( root, cached.pages ) )
			return cached;
		Intro parsed = new Intro();
		parsePage( root, page, 0, parsed );
		if ( parsed.pages.isEmpty() )
		{
			CACHE.remove( key );
			return null;
		}
		parsed.version = version( root, parsed.pages );
		CACHE.put( key, parsed );
		return parsed;
	}

	private static void parsePage( WebRoot root, String page, int depth, Intro intro )
	{
		if ( intro.pages.contains( page ) || intro.pages.size() >= MAX_PAGES )
			return;
		String html;
		try
		{
			html = root.read( page );
		}
		catch ( IOException e )
		{
			return;
		}
		if ( html == null )
			return;
		intro.pages.add( page );
		Document doc = Jsoup.parse( html );
		List<String> embedded = new ArrayList<String>();

		for ( Element link : doc.select( "a[href]" ) )
		{
			String href = link.attr( "href" ).trim();
			Matcher de = DE_LINK.matcher( href );
			if ( de.find() )
			{
				String title = linkTitle( link );
				intro.addEntryPoint( decode( de.group( 1 ) ), title, context( link, title ) );
			}
			else if ( ( href.startsWith( "http://" ) || href.startsWith( "https://" ) ) && intro.help.size() < MAX_LINKS
					&& !intro.help.containsKey( href ) )
				intro.help.put( href, linkTitle( link ) );
		}
		for ( Element frame : doc.select( "iframe[src], frame[src]" ) )
			embedded.add( frame.attr( "src" ) );

		StringBuilder js = new StringBuilder();
		for ( Element script : doc.select( "script" ) )
			js.append( script.data() ).append( '\n' );
		for ( Element element : doc.select( "[onclick]" ) )
			js.append( element.attr( "onclick" ) ).append( '\n' );
		readScripts( js.toString(), intro, embedded );

		if ( intro.title == null )
		{
			Element heading = doc.select( "h1, h2, h3" ).first();
			String title = heading == null ? clean( doc.title() ) : clean( heading.text() );
			if ( !title.isEmpty() )
				intro.title = title;
		}
		if ( intro.lead == null )
			for ( Element paragraph : doc.select( "p" ) )
			{
				String text = clean( paragraph.text() );
				if ( text.length() >= 40 )
				{
					intro.lead = truncate( text, MAX_LEAD );
					break;
				}
			}
		doc.select( "script, style, noscript" ).remove();
		String text = clean( doc.body() == null ? doc.text() : doc.body().text() );
		if ( !text.isEmpty() && intro.text.length() < MAX_SUMMARY )
			intro.text.append( intro.text.length() == 0 ? "" : " " ).append( text );

		if ( depth < MAX_DEPTH )
			for ( String src : embedded )
			{
				String path = resolve( root, page, src );
				if ( path != null )
					parsePage( root, path, depth + 1, intro );
			}
	}

	private static void readScripts( String js, Intro intro, List<String> embedded )
	{
		Matcher control = ANALYSIS_CONTROL.matcher( js );
		while ( control.find() )
		{
			Map<String, String> params = new LinkedHashMap<String, String>();
			if ( control.group( 3 ) != null )
			{
				Matcher param = PRESET_PARAM.matcher( control.group( 3 ) );
				while ( param.find() )
					params.put( param.group( 2 ), param.group( 4 ) );
			}
			intro.addPreset( control.group( 2 ), params );
		}
		Matcher target = IMPORT_TARGET.matcher( js );
		while ( target.find() )
			intro.importTargets.add( target.group( 2 ) );
		Matcher collection = DATA_COLLECTION.matcher( js );
		while ( collection.find() )
			intro.addEntryPoint( collection.group( 2 ), null, null );
		Matcher load = LOAD_PAGE.matcher( js );
		while ( load.find() )
			embedded.add( load.group( 2 ) );
	}

	/**
	 * Text around a link, which explains links whose own text says little ("a table", "Open example"): the line the
	 * link is on, otherwise its closest enclosing block.
	 * @return the text, or null if there is none short enough
	 */
	private static String context( Element link, String title )
	{
		String line = clean( lineText( link ) );
		if ( line.length() > title.length() + 10 && line.length() <= MAX_CONTEXT )
			return line;
		Element block = link.parent();
		while ( block != null && !block.isBlock() )
			block = block.parent();
		for ( int i = 0; i < 2 && block != null; i++, block = block.parent() )
		{
			String text = clean( block.text() );
			if ( text.length() > title.length() + 10 )
				return text.length() <= MAX_CONTEXT ? text : null;
		}
		return null;
	}

	/** Text of the link and its siblings up to the nearest line break or block element on each side. */
	private static String lineText( Element link )
	{
		StringBuilder before = new StringBuilder();
		for ( Node node = link.previousSibling(); node != null && !isLineBreak( node ); node = node.previousSibling() )
			before.insert( 0, nodeText( node ) );
		StringBuilder after = new StringBuilder();
		for ( Node node = link.nextSibling(); node != null && !isLineBreak( node ); node = node.nextSibling() )
			after.append( nodeText( node ) );
		return before + " " + link.text() + " " + after;
	}

	private static boolean isLineBreak( Node node )
	{
		return node instanceof Element && ( ( (Element) node ).tagName().equals( "br" ) || ( (Element) node ).isBlock() );
	}

	private static String nodeText( Node node )
	{
		if ( node instanceof TextNode )
			return ( (TextNode) node ).text();
		return node instanceof Element ? ( (Element) node ).text() : "";
	}

	/** @return the link text, or an empty string when it has no words (e.g. "?" of a help link) */
	private static String linkTitle( Element link )
	{
		String text = clean( link.text() );
		return text.matches( ".*[\\p{L}\\p{N}].*" ) ? text : "";
	}

	private static String clean( String text )
	{
		return text == null ? "" : text.replace( "[?]", " " ).replace( "»", " " ).replace( "«", " " ).replace( "←", " " )
				.replace( "→", " " ).replace( ' ', ' ' ).replaceAll( "\\s+", " " ).trim();
	}

	private static String truncate( String text, int max )
	{
		if ( text.length() <= max )
			return text;
		int cut = text.lastIndexOf( ' ', max );
		return text.substring( 0, cut > max / 2 ? cut : max ) + "…";
	}

	/** Decode a {@code #de=} link value: %-escapes only, a literal '+' stays (paths may contain it). */
	private static String decode( String value )
	{
		try
		{
			return java.net.URLDecoder.decode( value.replace( "+", "%2B" ), StandardCharsets.UTF_8 ).trim();
		}
		catch ( IllegalArgumentException e )
		{
			return value.trim();
		}
	}

	private static boolean exists( String path )
	{
		try
		{
			return DataElementPath.create( path ).exists();
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	private static long version( WebRoot root, List<String> pages )
	{
		long version = 17;
		for ( String page : pages )
			version = version * 31 + root.version( page );
		return version;
	}

	/**
	 * Resolve a page reference ({@code src} of an iframe, a {@code .load()} URL) found on {@code fromPage}.
	 * @return path relative to the web root, or null if it is not a page of this web application
	 */
	static String resolve( WebRoot root, String fromPage, String src )
	{
		if ( src == null )
			return null;
		String ref = src.trim();
		int cut = ref.length();
		for ( char c : new char[] { '?', '#', ' ' } )
			if ( ref.indexOf( c ) >= 0 )
				cut = Math.min( cut, ref.indexOf( c ) );
		ref = ref.substring( 0, cut );
		String lower = ref.toLowerCase();
		if ( ref.isEmpty() || ref.startsWith( "//" ) || ref.contains( ":" ) || !( lower.endsWith( ".html" ) || lower.endsWith( ".htm" ) ) )
			return null;
		if ( ref.startsWith( "/" ) )
		{
			String prefix = "/" + root.name() + "/";
			if ( !ref.startsWith( prefix ) )
				return null; // another web application
			return normalize( ref.substring( prefix.length() ) );
		}
		int slash = fromPage.lastIndexOf( '/' );
		return normalize( ( slash < 0 ? "" : fromPage.substring( 0, slash + 1 ) ) + ref );
	}

	/**
	 * @return the path without "." and ".." segments, or null if it is empty or leaves the web root
	 */
	static String normalize( String path )
	{
		List<String> parts = new ArrayList<String>();
		for ( String part : path.replace( '\\', '/' ).split( "/" ) )
		{
			if ( part.isEmpty() || part.equals( "." ) )
				continue;
			if ( part.equals( ".." ) )
			{
				if ( parts.isEmpty() )
					return null;
				parts.remove( parts.size() - 1 );
			}
			else
				parts.add( part );
		}
		return parts.isEmpty() ? null : String.join( "/", parts );
	}

	/**
	 * Find the web UI files: the {@value #WEB_PATH_PROPERTY} system property if set, otherwise the war of the embedded
	 * Tomcat ({@code bioumlweb.war} property), otherwise {@code webapps/bioumlweb} (directory, then .war) of the
	 * Tomcat running the server ({@code catalina.base}) or of the server installation ({@code biouml.server.path}).
	 * @return the web root, or null if the web UI files are not found
	 */
	static WebRoot webRoot()
	{
		String configured = System.getProperty( WEB_PATH_PROPERTY );
		if ( configured != null && !configured.isEmpty() )
			return open( new File( configured ) );
		List<File> candidates = new ArrayList<File>();
		String war = System.getProperty( "bioumlweb.war" );
		if ( war != null )
			candidates.add( new File( war ) );
		String serverPath = System.getProperty( "biouml.server.path" );
		for ( String base : new String[] { System.getProperty( "catalina.base" ), serverPath == null ? null : serverPath + "/tomcat" } )
			if ( base != null )
			{
				candidates.add( new File( base, "webapps/" + WEB_APP ) );
				candidates.add( new File( base, "webapps/" + WEB_APP + ".war" ) );
			}
		for ( File candidate : candidates )
		{
			WebRoot root = open( candidate );
			if ( root != null )
				return root;
		}
		return null;
	}

	private static WebRoot open( File file )
	{
		try
		{
			if ( file.isDirectory() )
				return new DirectoryRoot( file );
			if ( file.isFile() && file.getName().endsWith( ".war" ) )
				return new WarRoot( file );
		}
		catch ( IOException e )
		{
			// not readable: no web root
		}
		return null;
	}

	/** Deployed web UI files: an unpacked web application or its war. */
	interface WebRoot
	{
		/** @return web application name, which prefixes absolute links to its pages ("/bioumlweb/...") */
		String name();

		/** @return location for cache keys */
		String location();

		/** @return page text, or null if there is no such page or it is too big */
		String read(String path) throws IOException;

		/** @return a value which changes when the page file changes */
		long version(String path);
	}

	static final class DirectoryRoot implements WebRoot
	{
		private final File dir;

		DirectoryRoot( File dir ) throws IOException
		{
			this.dir = dir.getCanonicalFile();
		}

		@Override
		public String name()
		{
			return dir.getName();
		}

		@Override
		public String location()
		{
			return dir.getPath();
		}

		private File file( String path ) throws IOException
		{
			File file = new File( dir, path ).getCanonicalFile();
			return file.toPath().startsWith( dir.toPath() ) && file.isFile() ? file : null;
		}

		@Override
		public String read( String path ) throws IOException
		{
			File file = file( path );
			if ( file == null || file.length() > MAX_PAGE_BYTES )
				return null;
			return new String( Files.readAllBytes( file.toPath() ), StandardCharsets.UTF_8 );
		}

		@Override
		public long version( String path )
		{
			try
			{
				File file = file( path );
				return file == null ? 0 : file.lastModified();
			}
			catch ( IOException e )
			{
				return 0;
			}
		}
	}

	static final class WarRoot implements WebRoot
	{
		private final File war;

		WarRoot( File war ) throws IOException
		{
			this.war = war.getCanonicalFile();
		}

		@Override
		public String name()
		{
			String name = war.getName();
			return name.substring( 0, name.length() - ".war".length() );
		}

		@Override
		public String location()
		{
			return war.getPath();
		}

		@Override
		public String read( String path ) throws IOException
		{
			try (ZipFile zip = new ZipFile( war ))
			{
				ZipEntry entry = zip.getEntry( path );
				if ( entry == null || entry.isDirectory() || entry.getSize() > MAX_PAGE_BYTES )
					return null;
				try (InputStream in = zip.getInputStream( entry ))
				{
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte[] buffer = new byte[8192];
					int n;
					while ( ( n = in.read( buffer ) ) > 0 )
					{
						out.write( buffer, 0, n );
						if ( out.size() > MAX_PAGE_BYTES )
							return null;
					}
					return new String( out.toByteArray(), StandardCharsets.UTF_8 );
				}
			}
		}

		@Override
		public long version( String path )
		{
			return war.lastModified();
		}
	}
}
