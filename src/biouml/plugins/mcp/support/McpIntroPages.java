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
 * links and the page text; for a wizard (elements with a {@code step-id} attribute), its steps
 * with their help pages, the analyses its scripts start and the repository paths they build.
 *
 * <p>Intro pages belong to the web UI web application ({@code bioumlweb}), not to the BioUML server, so they are
 * read from disk when the web UI is deployed next to the server (see {@link #webRoot()}). Pages are parsed
 * statically: links and string literals of the page scripts (inline ones and the page's own script files, not
 * shared libraries) are read, scripts are not run. Server-side includes ({@code <!--#include file="..."-->}) are
 * expanded, as Tomcat does for {@code .shtml} pages. Pages embedded with an iframe or jQuery {@code .load()} are
 * followed within the web application. Parsed pages are cached until a file they were read from (or the war)
 * changes.</p>
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
	private static final int MAX_INCLUDE_DEPTH = 4;
	private static final int MAX_SCRIPTS = 30;
	private static final int MAX_ENTRY_POINTS = 50;
	private static final int MAX_PRESETS = 20;
	private static final int MAX_LINKS = 20;
	private static final int MAX_PATH_TEMPLATES = 25;
	private static final int MAX_STEPS = 20;
	private static final int MAX_SUMMARY = 2000;
	private static final int MAX_CONTEXT = 300;
	private static final int MAX_LEAD = 300;
	private static final int MAX_STEP_GUIDE = 700;
	/** At most this many characters of a page are returned by {@link #pageText} in one call. */
	public static final int MAX_PAGE_TEXT = 50000;

	private static final Pattern DE_LINK = Pattern.compile( "#de=([^&\"']+)" );
	private static final Pattern ANALYSIS_CONTROL = Pattern.compile(
			"showAnalysisControl\\s*\\(\\s*[^,]+,\\s*([\"'])([^\"']+)\\1\\s*(?:,\\s*\\{(.*?)\\}\\s*\\))?", Pattern.DOTALL );
	private static final Pattern PRESET_PARAM = Pattern.compile(
			"\\{\\s*name\\s*:\\s*([\"'])([^\"']+)\\1\\s*,\\s*value\\s*:\\s*([\"'])([^\"']*)\\3\\s*\\}" );
	private static final Pattern IMPORT_TARGET = Pattern.compile(
			"(?:doImport\\w*\\s*\\(|\\buserData\\s*=)\\s*([\"'])([^\"']+)\\1" );
	private static final Pattern DATA_COLLECTION = Pattern.compile(
			"(?:getDataCollection|openBranch|refreshTreeBranch|isPathExists)\\s*\\(\\s*([\"'])([^\"'+]+)\\1\\s*[,)]" );
	private static final Pattern LOAD_PAGE = Pattern.compile( "\\.load\\s*\\(\\s*([\"'])([^\"']+)\\1" );
	private static final Pattern SSI_INCLUDE = Pattern.compile( "<!--#include\\s+(file|virtual)\\s*=\\s*\"([^\"]+)\"\\s*-->" );
	/** An analysis named in a script: by its repository path ... */
	private static final Pattern ANALYSIS_PATH = Pattern.compile( "([\"'])(analyses/(?:Methods|Galaxy)/[^\"'\\n]+/[^\"'\\n]+)\\1" );
	/** ... or by the parameters bean the web UI keeps for it. */
	private static final Pattern ANALYSIS_BEAN = Pattern.compile( "([\"'])properties/method/parameters/([^\"'\\n]+)\\1" );
	/** A parameter in the {name, value} form the web UI sends: a literal value, or an expression (chosen by the user). */
	private static final Pattern NAME_VALUE = Pattern.compile( "\\{\\s*[\"']?name[\"']?\\s*:\\s*([\"'])(\\w+)\\1\\s*,\\s*[\"']?value[\"']?\\s*:\\s*"
			+ "(?:([\"'])([^\"'\\n]*)\\3|(true|false|-?\\d+(?:\\.\\d+)?)\\b|([A-Za-z_$][\\w$.]*))" );
	private static final Pattern PROJECTS_DATA = Pattern.compile( "[\"']support/getProjectsData[\"']" );
	private static final Pattern PROJECT_FLAG = Pattern.compile( "\\.(demo\\w*)\\s*===?\\s*[\"']true[\"']" );
	private static final String JS_OPERAND = "(?:\"[^\"\\n]*\"|'[^'\\n]*'|[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*(?:\\(\\))?)";
	/**
	 * A string concatenation, e.g. {@code "data/Projects/" + getCurrentProject() + "/Data"}; not the tail of
	 * one that starts with a member of something else ({@code list[0].name + "/x"}).
	 */
	private static final Pattern CONCAT = Pattern.compile( "(?<![\\w$.\\])])" + JS_OPERAND + "(?:\\s*\\+\\s*" + JS_OPERAND + ")+" );
	/** Comments of a script: a block comment, or a line that is only a comment ({@code //} in a string is no comment). */
	private static final Pattern JS_COMMENT = Pattern.compile( "/\\*.*?\\*/|(?m)^[ \\t]*//[^\\n]*", Pattern.DOTALL );
	private static final Pattern OPERAND = Pattern.compile( JS_OPERAND );
	private static final Pattern ASSIGNMENT = Pattern.compile( "\\b(?:var|let|const)\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*("
			+ JS_OPERAND + "(?:\\s*\\+\\s*" + JS_OPERAND + ")*)\\s*;" );
	private static final Pattern NOT_IN_PATH = Pattern.compile( "[#=?:<>\"'\\\\]|//|\\.(?:s?html?|js|css|png|gif|jpe?g|jsp)$" );
	private static final Pattern PAGE_EXTENSION = Pattern.compile( "\\.(?:s?html?)$" );

	private static final Map<String, Intro> CACHE = new ConcurrentHashMap<String, Intro>();

	private McpIntroPages()
	{
	}

	/** An analysis the page offers or starts, with the parameter values its scripts pass. */
	public static final class Preset
	{
		private String path;
		private final String name;
		private final Map<String, List<String>> params = new LinkedHashMap<String, List<String>>();
		private final Set<String> chosenByUser = new LinkedHashSet<String>();

		Preset( String path, String name )
		{
			this.path = path;
			this.name = name;
		}

		/** @return the repository path of the analysis, or null when the page names it only */
		public String getPath()
		{
			return path;
		}

		/** @return the analysis name, as biouml_analysis_describe and biouml_analysis_run take it */
		public String getName()
		{
			return name;
		}

		private void addParam( String param, String value )
		{
			List<String> values = params.get( param );
			if ( values == null )
				params.put( param, values = new ArrayList<String>() );
			if ( !values.contains( value ) )
				values.add( value );
		}
	}

	/** Checks the values a page passes to an analysis against the analysis itself. */
	public interface ParameterCheck
	{
		/**
		 * @param value the value the page passes, or null when the page lets the user choose it
		 * @return whether the analysis has the parameter and accepts the value; null when the analysis is not known
		 */
		Boolean accepts( String analysis, String parameter, String value );
	}

	/** A wizard step: its title and the lead of its help page. */
	private static final class Step
	{
		final String id;
		String title = "";
		String helpPage;
		String guide;

		Step( String id )
		{
			this.id = id;
		}
	}

	/**
	 * Parsed intro page together with the pages it embeds. Not modified after parsing.
	 */
	public static final class Intro
	{
		private final List<String> pages = new ArrayList<String>();
		/** Every file read (pages, included fragments, scripts, help pages), for cache checks. */
		private final Set<String> files = new LinkedHashSet<String>();
		private final StringBuilder text = new StringBuilder();
		private final Map<String, Map<String, Object>> entryPoints = new LinkedHashMap<String, Map<String, Object>>();
		private final Map<String, Preset> presets = new LinkedHashMap<String, Preset>();
		private final Set<String> importTargets = new LinkedHashSet<String>();
		private final Map<String, String> help = new LinkedHashMap<String, String>();
		private final Map<String, Step> steps = new LinkedHashMap<String, Step>();
		private final Set<String> pathTemplates = new LinkedHashSet<String>();
		private final Set<String> projectFlags = new LinkedHashSet<String>();
		private String title;
		private String lead;
		private long version;

		/**
		 * @return short description of the page (its first heading and paragraph; for a wizard, its title and the
		 *         first paragraph of the first step's help), or null
		 */
		public String getDescription()
		{
			if ( title == null )
				return lead;
			return lead == null ? title : title + ": " + lead;
		}

		/**
		 * @return the project flag the page tells example projects by ("demo": a project whose client property
		 *         {@code demo} is "true" is listed as a demo project), or null when the page does not list projects.
		 *         A page with several flags (one per mode of the page) checks the
		 *         first one in its default mode.
		 */
		public String getProjectFlag()
		{
			return projectFlags.isEmpty() ? null : projectFlags.iterator().next();
		}

		/**
		 * @param knownPaths entry points the caller already reports (e.g. from the perspective's XML); they are skipped
		 * @return JSON-serializable form; entry points carry an {@code exists} flag for the current session
		 */
		public Map<String, Object> toMap( Collection<String> knownPaths )
		{
			return toMap( knownPaths, null );
		}

		/**
		 * @param check checks the parameter values passed to analyses, dropping those the analysis does not take;
		 *            null to report them as found
		 */
		public Map<String, Object> toMap( Collection<String> knownPaths, ParameterCheck check )
		{
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "source", pages.get( 0 ) );
			if ( pages.size() > 1 )
				m.put( "embeddedPages", new ArrayList<String>( pages.subList( 1, pages.size() ) ) );
			if ( title != null )
				m.put( "title", title );
			if ( text.length() > 0 )
				m.put( "summary", truncate( text.toString(), MAX_SUMMARY ) );
			List<Map<String, Object>> stepList = new ArrayList<Map<String, Object>>();
			for ( Step step : steps.values() )
			{
				Map<String, Object> s = new LinkedHashMap<String, Object>();
				s.put( "id", step.id );
				s.put( "title", step.title );
				if ( step.guide != null )
					s.put( "guide", step.guide );
				if ( step.helpPage != null )
					s.put( "helpPage", step.helpPage );
				stepList.add( s );
			}
			if ( !stepList.isEmpty() )
				m.put( "steps", stepList );
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
			for ( Preset preset : presets.values() )
				presetList.add( presetMap( preset, check ) );
			if ( !presetList.isEmpty() )
				m.put( "analysisPresets", presetList );
			List<String> templates = new ArrayList<String>();
			for ( String template : pathTemplates )
			{
				// a fixed start must be in the repository: "importProgress/{id}" is a message channel, not a path
				String start = template.substring( 0, template.indexOf( '{' ) ).replaceAll( "/+$", "" );
				if ( start.isEmpty() || exists( start ) )
					templates.add( template );
			}
			if ( !templates.isEmpty() )
				m.put( "pathTemplates", templates );
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

		private static Map<String, Object> presetMap( Preset preset, ParameterCheck check )
		{
			Map<String, Object> p = new LinkedHashMap<String, Object>();
			p.put( "analysis", preset.path == null ? preset.name : preset.path );
			p.put( "name", preset.name );
			if ( preset.path != null )
				p.put( "exists", Boolean.valueOf( exists( preset.path ) ) );
			Map<String, Object> params = new LinkedHashMap<String, Object>();
			for ( Map.Entry<String, List<String>> param : preset.params.entrySet() )
			{
				List<String> values = new ArrayList<String>();
				for ( String value : param.getValue() )
					if ( accepted( check, preset, param.getKey(), value ) )
						values.add( value );
				if ( !values.isEmpty() )
					params.put( param.getKey(), values.size() == 1 ? values.get( 0 ) : values );
			}
			if ( !params.isEmpty() )
				p.put( "params", params );
			List<String> chosen = new ArrayList<String>();
			for ( String param : preset.chosenByUser )
				if ( !params.containsKey( param ) && accepted( check, preset, param, null ) )
					chosen.add( param );
			if ( !chosen.isEmpty() )
				p.put( "chosenByUser", chosen );
			return p;
		}

		private static boolean accepted( ParameterCheck check, Preset preset, String param, String value )
		{
			Boolean accepts = check == null ? null : check.accepts( preset.name, param, value );
			return accepts == null || accepts.booleanValue();
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

		/** The preset of an analysis, found by its path or name, or a new one (null when there are too many). */
		private Preset preset( String path, String name )
		{
			Preset known = presets.get( name );
			if ( known != null )
			{
				if ( known.path == null )
					known.path = path;
				return known;
			}
			if ( presets.size() >= MAX_PRESETS )
				return null;
			Preset preset = new Preset( path, name );
			presets.put( name, preset );
			return preset;
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
		if ( cached != null && cached.version == version( root, cached.files ) )
			return cached;
		Intro parsed = new Intro();
		parsePage( root, page, 0, parsed );
		if ( parsed.pages.isEmpty() )
		{
			CACHE.remove( key );
			return null;
		}
		parsed.version = version( root, parsed.files );
		CACHE.put( key, parsed );
		return parsed;
	}

	/**
	 * Text of a web UI page (an intro page, a wizard step's help page), with its includes expanded.
	 *
	 * @param page path of the page in the web application, e.g. "wizard/help/step-start.shtml"
	 * @param offset first character of the text to return
	 * @return {@code {page, title, text, offset, totalChars, nextOffset, links}}, or null when the web UI files are
	 *         not found, the path is not a page of the web application, or there is no such page
	 */
	public static Map<String, Object> pageText( String page, int offset )
	{
		WebRoot root = webRoot();
		String path = page == null ? null : normalize( page.trim() );
		if ( root == null || path == null || !PAGE_EXTENSION.matcher( path.toLowerCase() ).find() )
			return null;
		String html;
		try
		{
			html = readExpanded( root, path, new LinkedHashSet<String>(), 0 );
		}
		catch ( IOException e )
		{
			return null;
		}
		if ( html == null )
			return null;
		Document doc = Jsoup.parse( html );
		List<Map<String, Object>> links = new ArrayList<Map<String, Object>>();
		Set<String> seen = new LinkedHashSet<String>();
		for ( Element link : doc.select( "a[href]" ) )
		{
			String href = link.attr( "href" ).trim();
			Matcher de = DE_LINK.matcher( href );
			boolean element = de.find();
			String target = element ? decode( de.group( 1 ) ) : href.startsWith( "http://" ) || href.startsWith( "https://" ) ? href : null;
			if ( target == null || !seen.add( target ) || links.size() >= MAX_ENTRY_POINTS )
				continue;
			Map<String, Object> l = new LinkedHashMap<String, Object>();
			l.put( element ? "path" : "url", target );
			String title = linkTitle( link );
			if ( !title.isEmpty() )
				l.put( "title", title );
			links.add( l );
		}
		String title = clean( doc.title() );
		doc.select( "script, style, noscript" ).remove();
		String text = blockText( doc.body() == null ? doc : doc.body() );
		int from = Math.max( 0, Math.min( offset, text.length() ) );
		int to = Math.min( text.length(), from + MAX_PAGE_TEXT );
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "page", path );
		if ( !title.isEmpty() )
			m.put( "title", title );
		m.put( "text", text.substring( from, to ) );
		m.put( "offset", Integer.valueOf( from ) );
		m.put( "totalChars", Integer.valueOf( text.length() ) );
		if ( to < text.length() )
			m.put( "nextOffset", Integer.valueOf( to ) );
		if ( !links.isEmpty() && from == 0 )
			m.put( "links", links );
		return m;
	}

	/** Text of an HTML document: one line per heading ("## ..."), paragraph, list item and table row. */
	static String htmlText( String html )
	{
		Document doc = Jsoup.parse( html );
		doc.select( "script, style, noscript" ).remove();
		return blockText( doc.body() == null ? doc : doc.body() );
	}

	/** Text of an element with a line break after each heading, paragraph, list item and table row. */
	private static String blockText( Element element )
	{
		StringBuilder out = new StringBuilder();
		for ( Element block : element.select( "h1, h2, h3, h4, h5, h6, p, li, tr, dt, dd, pre, caption" ) )
		{
			// a block inside another listed block is part of that one's text
			boolean nested = false;
			for ( Element parent = block.parent(); parent != null && !nested; parent = parent.parent() )
				nested = parent.is( "p, li, tr, dt, dd, pre, caption" );
			if ( nested )
				continue;
			String text = clean( block.text() );
			if ( text.isEmpty() )
				continue;
			if ( block.tagName().matches( "h\\d" ) )
				out.append( out.length() == 0 ? "" : "\n" ).append( "## " ).append( text ).append( '\n' );
			else
				out.append( block.tagName().equals( "li" ) ? "- " : "" ).append( text ).append( '\n' );
		}
		String text = out.toString().trim();
		return text.isEmpty() ? clean( element.text() ) : text;
	}

	private static void parsePage( WebRoot root, String page, int depth, Intro intro )
	{
		if ( intro.pages.contains( page ) || intro.pages.size() >= MAX_PAGES )
			return;
		String html;
		try
		{
			html = readExpanded( root, page, intro.files, 0 );
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

		// inline scripts together, each of the page's own script files on its own: parameters found in a script
		// belong to the analysis that script starts
		StringBuilder js = new StringBuilder();
		for ( Element script : doc.select( "script" ) )
			js.append( script.data() ).append( '\n' );
		for ( Element element : doc.select( "[onclick]" ) )
			js.append( element.attr( "onclick" ) ).append( '\n' );
		readScripts( js.toString(), intro, embedded );
		int scripts = 0;
		for ( Element script : doc.select( "script[src]" ) )
		{
			String path = scriptPath( page, script.attr( "src" ) );
			if ( path == null || scripts >= MAX_SCRIPTS )
				continue;
			try
			{
				String code = root.read( path );
				if ( code == null )
					continue;
				intro.files.add( path );
				scripts++;
				readScripts( code, intro, embedded );
			}
			catch ( IOException e )
			{
				// an unreadable script tells nothing
			}
		}

		boolean wizard = readSteps( root, page, doc, intro );
		if ( intro.title == null )
		{
			// a wizard's headings belong to its steps; its page title names it
			Element heading = wizard ? null : doc.select( "h1, h2, h3" ).first();
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
		// a wizard's text is its controls and dialogs; its steps say what it does
		String text = wizard ? "" : clean( doc.body() == null ? doc.text() : doc.body().text() );
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

	/**
	 * Read the steps of a wizard page: elements with a {@code step-id} attribute, titled by their text. A step's
	 * help is the page {@code help/<step-id>.shtml} (or .html) next to the wizard.
	 * @return whether the page is a wizard
	 */
	private static boolean readSteps( WebRoot root, String page, Document doc, Intro intro )
	{
		boolean found = false;
		for ( Element element : doc.select( "[step-id]" ) )
		{
			String id = element.attr( "step-id" ).trim();
			if ( id.isEmpty() )
				continue;
			found = true;
			Step step = intro.steps.get( id );
			if ( step == null )
			{
				if ( intro.steps.size() >= MAX_STEPS )
					continue;
				intro.steps.put( id, step = new Step( id ) );
			}
			if ( step.title.isEmpty() )
				step.title = clean( element.text() );
		}
		if ( !found )
			return false;
		int slash = page.lastIndexOf( '/' );
		String dir = slash < 0 ? "" : page.substring( 0, slash + 1 );
		for ( Step step : intro.steps.values() )
		{
			if ( step.helpPage != null )
				continue;
			for ( String candidate : new String[] { dir + "help/" + step.id + ".shtml", dir + "help/" + step.id + ".html" } )
			{
				String html;
				try
				{
					html = readExpanded( root, candidate, intro.files, 0 );
				}
				catch ( IOException e )
				{
					html = null;
				}
				if ( html == null )
					continue;
				step.helpPage = candidate;
				StringBuilder guide = new StringBuilder();
				for ( Element paragraph : Jsoup.parse( html ).select( "p" ) )
				{
					String text = clean( paragraph.text() );
					if ( text.isEmpty() )
						continue;
					if ( intro.lead == null && text.length() >= 40 && step == intro.steps.values().iterator().next() )
						intro.lead = truncate( text, MAX_LEAD );
					if ( guide.length() > 0 && guide.length() + text.length() > MAX_STEP_GUIDE )
						break;
					guide.append( guide.length() == 0 ? "" : " " ).append( text );
				}
				if ( guide.length() > 0 )
					step.guide = truncate( guide.toString(), MAX_STEP_GUIDE );
				break;
			}
		}
		return true;
	}

	private static void readScripts( String script, Intro intro, List<String> embedded )
	{
		// commented-out code is not what the page does
		String js = JS_COMMENT.matcher( script ).replaceAll( " " );
		Matcher control = ANALYSIS_CONTROL.matcher( js );
		while ( control.find() )
		{
			String path = control.group( 2 );
			Preset preset = intro.preset( path, DataElementPath.create( path ).getName() );
			if ( preset == null || control.group( 3 ) == null )
				continue;
			Matcher param = PRESET_PARAM.matcher( control.group( 3 ) );
			while ( param.find() )
				preset.addParam( param.group( 2 ), param.group( 4 ) );
		}

		// analyses a script starts, and the parameters it passes when it starts only one
		Set<Preset> started = new LinkedHashSet<Preset>();
		Matcher analysis = ANALYSIS_PATH.matcher( js );
		while ( analysis.find() )
		{
			Preset preset = intro.preset( analysis.group( 2 ), DataElementPath.create( analysis.group( 2 ) ).getName() );
			if ( preset != null )
				started.add( preset );
		}
		Matcher bean = ANALYSIS_BEAN.matcher( js );
		while ( bean.find() )
		{
			Preset preset = intro.preset( null, bean.group( 2 ) );
			if ( preset != null )
				started.add( preset );
		}
		if ( started.size() == 1 )
		{
			Preset preset = started.iterator().next();
			Matcher param = NAME_VALUE.matcher( js );
			while ( param.find() )
			{
				String literal = param.group( 4 ) != null ? param.group( 4 ) : param.group( 5 );
				if ( literal != null )
					preset.addParam( param.group( 2 ), literal );
				else
					preset.chosenByUser.add( param.group( 2 ) );
			}
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
		if ( PROJECTS_DATA.matcher( js ).find() )
		{
			Matcher flag = PROJECT_FLAG.matcher( js );
			while ( flag.find() )
				intro.projectFlags.add( flag.group( 1 ) );
		}
		readPathTemplates( js, intro );
	}

	/**
	 * Repository paths a script builds by concatenation, with the variable parts as placeholders:
	 * {@code "data/Projects/" + getCurrentProject() + "/Data"} is "data/Projects/{currentProject}/Data".
	 * A variable assigned a string or a concatenation in the same script is replaced by its value.
	 */
	private static void readPathTemplates( String js, Intro intro )
	{
		Map<String, String> variables = new LinkedHashMap<String, String>();
		Matcher assignment = ASSIGNMENT.matcher( js );
		while ( assignment.find() )
			variables.put( assignment.group( 1 ), template( assignment.group( 2 ), variables ) );
		Matcher concat = CONCAT.matcher( js );
		while ( concat.find() && intro.pathTemplates.size() < MAX_PATH_TEMPLATES )
		{
			String template = template( concat.group(), variables );
			String literal = template.replaceAll( "\\{[^}]*\\}", "" );
			// a path has a separator between its variable parts: "{hr}{serverPath}web/..." is a URL
			if ( template.contains( "/" ) && template.contains( "{" ) && !template.contains( "}{" ) && literal.matches( ".*\\p{L}.*" ) && !template.startsWith( "/" )
					&& !template.startsWith( "." ) && template.length() <= 200 && !NOT_IN_PATH.matcher( template ).find() )
				intro.pathTemplates.add( template );
		}
	}

	private static String template( String expression, Map<String, String> variables )
	{
		StringBuilder out = new StringBuilder();
		Matcher operand = OPERAND.matcher( expression );
		while ( operand.find() )
		{
			String token = operand.group();
			if ( token.startsWith( "\"" ) || token.startsWith( "'" ) )
				out.append( token, 1, token.length() - 1 );
			else if ( variables.containsKey( token ) )
				out.append( variables.get( token ) );
			else
				out.append( '{' ).append( placeholder( token ) ).append( '}' );
		}
		return out.toString();
	}

	/** {@code window.parent.getCurrentProject()} is "currentProject", {@code outPath} is "outPath". */
	private static String placeholder( String expression )
	{
		String name = expression.endsWith( "()" ) ? expression.substring( 0, expression.length() - 2 ) : expression;
		name = name.substring( name.lastIndexOf( '.' ) + 1 );
		if ( name.length() > 3 && name.startsWith( "get" ) && Character.isUpperCase( name.charAt( 3 ) ) )
			name = Character.toLowerCase( name.charAt( 3 ) ) + name.substring( 4 );
		return name;
	}

	/**
	 * A page with its server-side includes ({@code <!--#include file="..."-->}, {@code virtual="..."}) expanded.
	 * @param files collects the files read
	 * @return the text, or null if there is no such page or it is too big
	 */
	static String readExpanded( WebRoot root, String path, Set<String> files, int depth ) throws IOException
	{
		String text = root.read( path );
		if ( text == null )
			return null;
		files.add( path );
		if ( depth >= MAX_INCLUDE_DEPTH || text.indexOf( "<!--#include" ) < 0 )
			return text;
		StringBuilder out = new StringBuilder();
		Matcher include = SSI_INCLUDE.matcher( text );
		int last = 0;
		while ( include.find() )
		{
			out.append( text, last, include.start() );
			last = include.end();
			String ref = include.group( 2 ).trim();
			String target = ref.startsWith( "/" ) ? absolute( root, ref ) : relative( path, ref );
			String included = target == null || target.equals( path ) ? null : readExpanded( root, target, files, depth + 1 );
			if ( included != null && out.length() + included.length() <= MAX_PAGE_BYTES )
				out.append( included );
		}
		out.append( text, last, text.length() );
		return out.toString();
	}

	/**
	 * The page's own script file named by a {@code <script src>}, or null for scripts of other sites, shared
	 * libraries (under a "lib" folder) and minified files, which tell nothing about the page.
	 */
	private static String scriptPath( String page, String src )
	{
		String ref = stripQuery( src );
		String lower = ref.toLowerCase();
		if ( ref.isEmpty() || ref.startsWith( "/" ) || ref.contains( ":" ) || !lower.endsWith( ".js" ) || lower.endsWith( ".min.js" )
				|| ( "/" + lower ).contains( "/lib/" ) )
			return null;
		return relative( page, ref );
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
				.replace( "→", " " ).replace( ' ', ' ' ).replaceAll( "\\s+", " " ).trim();
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

	private static long version( WebRoot root, Collection<String> files )
	{
		long version = 17;
		for ( String file : files )
			version = version * 31 + root.version( file );
		return version;
	}

	/**
	 * Resolve a page reference ({@code src} of an iframe, a {@code .load()} URL) found on {@code fromPage}.
	 * @return path relative to the web root, or null if it is not a page (.html, .htm, .shtml) of this web application
	 */
	static String resolve( WebRoot root, String fromPage, String src )
	{
		if ( src == null )
			return null;
		String ref = stripQuery( src );
		if ( ref.isEmpty() || ref.startsWith( "//" ) || ref.contains( ":" ) || !PAGE_EXTENSION.matcher( ref.toLowerCase() ).find() )
			return null;
		return ref.startsWith( "/" ) ? absolute( root, ref ) : relative( fromPage, ref );
	}

	private static String stripQuery( String src )
	{
		String ref = src == null ? "" : src.trim();
		int cut = ref.length();
		for ( char c : new char[] { '?', '#', ' ' } )
			if ( ref.indexOf( c ) >= 0 )
				cut = Math.min( cut, ref.indexOf( c ) );
		return ref.substring( 0, cut );
	}

	/** @return a "/<web app>/..." reference relative to the web root, or null for another web application */
	private static String absolute( WebRoot root, String ref )
	{
		String prefix = "/" + root.name() + "/";
		return ref.startsWith( prefix ) ? normalize( ref.substring( prefix.length() ) ) : null;
	}

	private static String relative( String fromPage, String ref )
	{
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
