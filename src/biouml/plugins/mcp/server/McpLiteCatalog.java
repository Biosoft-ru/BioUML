package biouml.plugins.mcp.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import biouml.plugins.mcp.McpConstants;
import biouml.plugins.mcp.support.McpEnvelope;

import io.modelcontextprotocol.server.McpSyncServerExchange;

/**
 * The tools of the lite mode ({@code /biouml/mcp?mode=lite}): instead of the whole tool list, which
 * takes much of a small model's context, the server lists two tools — {@code biouml_catalog}, to drill
 * down from the tool groups to one tool's input schema, and {@code biouml_call}, to call a tool by name.
 * Every tool of the full catalog stays callable by its own name as well.
 */
public final class McpLiteCatalog
{
	public static final String CATALOG = "biouml_catalog";
	public static final String CALL = "biouml_call";

	/** Longest tool summary in a group listing. */
	private static final int MAX_SUMMARY = 200;

	/** The tool groups (the word after {@code biouml_} in a tool's name), in listing order. */
	private static final Map<String, String> GROUPS = new LinkedHashMap<String, String>();
	static
	{
		GROUPS.put( "perspective", "Task-oriented entry points (e.g. GTRD, Microbiome): what each offers, how its work is done with these tools, and the web UI's help pages. A good place to start." );
		GROUPS.put( "project", "The user's projects: list, create, delete, size." );
		GROUPS.put( "repo", "The repository: browse, search, describe, create, copy and remove elements, import and export files, read and write documents, run scripts and context-menu actions." );
		GROUPS.put( "analysis", "Analysis methods: list, describe their parameters, run, repeat, and read results (tables, tracks, folders, simulation results)." );
		GROUPS.put( "task", "Background tasks of analyses: list, status, cancel." );
		GROUPS.put( "diagram", "Diagrams and their models: create, describe, add nodes, edges, reactions, equations and events, set parameters and formulas, move, align and remove elements, sub-models and clones, render as an image, DML files." );
		GROUPS.put( "simulation", "Simulate a diagram's model: solvers, start a job, poll it, then plot, read or save the result." );
		GROUPS.put( "table", "Edit tables: add a row, replace content, save a subset of rows." );
		GROUPS.put( "bsa", "Tracks and site models: a track's genome, save selected sites or site models." );
		GROUPS.put( "brain", "Brain models: generate equations, a composite diagram or a multi-level model from a brain diagram." );
		GROUPS.put( "user", "The user's account: profile and password." );
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private McpLiteCatalog()
	{
	}

	/** The lite catalog: lists {@code biouml_catalog} and {@code biouml_call}, finds every tool of {@code full}. */
	public static McpToolCatalog create( final McpToolCatalog full )
	{
		McpToolCatalog lite = new McpToolCatalog( full );
		lite.register( CATALOG,
				"The BioUML tools, step by step. Without arguments: the tool groups, each with a summary and its tool names. With group: that group's tools, each with a one-sentence summary, its ops (a tool with ops does one of several things, chosen by its op argument) and its required arguments. With tool: the tool's full description and input schema. Then run the tool with " + CALL + ".",
				"{\"type\":\"object\",\"properties\":{\"group\":{\"type\":\"string\",\"description\":\"a group name, e.g. repo\"},\"tool\":{\"type\":\"string\",\"description\":\"a tool name, e.g. biouml_repo_list\"}}}",
				( ex, args ) -> catalog( full, str( args, "group" ), str( args, "tool" ) ) );
		lite.register( CALL,
				"Call a BioUML tool by name: tool is its name (from " + CATALOG + "), arguments the object of its arguments as its input schema describes them. Returns what the tool returns.",
				"{\"type\":\"object\",\"properties\":{\"tool\":{\"type\":\"string\"},\"arguments\":{\"type\":\"object\"}},\"required\":[\"tool\"]}",
				( ex, args ) -> call( full, ex, args ) );
		return lite;
	}

	static McpEnvelope catalog( McpToolCatalog full, String group, String toolName )
	{
		if ( toolName != null && !toolName.isEmpty() )
		{
			McpToolCatalog.Tool tool = listed( full, toolName );
			if ( tool == null )
				return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no tool " + toolName + "; the tools of group " + group( toolName ) + ": "
						+ names( full, group( toolName ) ) );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "name", tool.name );
			m.put( "description", tool.description );
			m.put( "inputSchema", schema( tool ) );
			return McpEnvelope.ok( m );
		}
		if ( group != null && !group.isEmpty() )
		{
			List<Map<String, Object>> tools = new ArrayList<Map<String, Object>>();
			for ( McpToolCatalog.Tool tool : full.getTools() )
				if ( group.equals( group( tool.name ) ) )
					tools.add( summary( tool ) );
			if ( tools.isEmpty() )
				return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no group " + group + "; the groups: " + groups( full ) );
			Map<String, Object> m = new LinkedHashMap<String, Object>();
			m.put( "group", group );
			m.put( "summary", GROUPS.get( group ) );
			m.put( "tools", tools );
			m.put( "next", "call " + CATALOG + " with tool for a tool's input schema, then " + CALL );
			return McpEnvelope.ok( m );
		}
		List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();
		for ( String name : groups( full ) )
		{
			Map<String, Object> g = new LinkedHashMap<String, Object>();
			g.put( "group", name );
			g.put( "summary", GROUPS.get( name ) );
			g.put( "tools", names( full, name ) );
			groups.add( g );
		}
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "groups", groups );
		m.put( "next", "call " + CATALOG + " with group for its tools' summaries, or with tool for one tool's input schema, then " + CALL );
		return McpEnvelope.ok( m );
	}

	@SuppressWarnings( "unchecked" )
	static McpEnvelope call( McpToolCatalog full, McpSyncServerExchange ex, Map<String, Object> args ) throws Exception
	{
		String name = str( args, "tool" );
		if ( name == null || name.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "missing required argument: tool" );
		McpToolCatalog.Tool tool = full.find( name );
		if ( tool == null )
			return McpEnvelope.error( McpConstants.CODE_NOT_FOUND, "no tool " + name + "; " + CATALOG + " lists the tools" );
		Object value = args.get( "arguments" );
		if ( value instanceof String && !( (String) value ).trim().isEmpty() )
		{
			// a model may pass the arguments as a JSON string instead of an object
			try
			{
				value = MAPPER.readValue( (String) value, LinkedHashMap.class );
			}
			catch ( Exception e )
			{
				return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "arguments must be an object, got a string that is not JSON" );
			}
		}
		if ( value != null && !( value instanceof Map ) && !( value instanceof String ) )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "arguments must be an object" );
		Map<String, Object> toolArgs = value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
		Map<String, Object> schema = schema( tool );
		List<String> missing = new ArrayList<String>();
		if ( schema.get( "required" ) instanceof List )
			for ( Object key : (List<Object>) schema.get( "required" ) )
				if ( toolArgs.get( String.valueOf( key ) ) == null )
					missing.add( String.valueOf( key ) );
		if ( !missing.isEmpty() )
			return McpEnvelope.error( McpConstants.CODE_INVALID_PARAMS, "missing required arguments " + missing + " of " + name + "; its input schema: "
					+ MAPPER.writeValueAsString( schema ) );
		return tool.handler.handle( ex, toolArgs );
	}

	/** A group listing's entry for a tool: name, summary, ops, required arguments. */
	@SuppressWarnings( "unchecked" )
	private static Map<String, Object> summary( McpToolCatalog.Tool tool )
	{
		Map<String, Object> m = new LinkedHashMap<String, Object>();
		m.put( "name", tool.name );
		m.put( "summary", firstSentence( tool.description ) );
		Map<String, Object> schema = schema( tool );
		Object properties = schema.get( "properties" );
		if ( properties instanceof Map && ( (Map<String, Object>) properties ).get( "op" ) instanceof Map )
			m.put( "ops", ( (Map<String, Object>) ( (Map<String, Object>) properties ).get( "op" ) ).get( "enum" ) );
		if ( schema.get( "required" ) instanceof List )
			m.put( "required", schema.get( "required" ) );
		return m;
	}

	static String firstSentence( String text )
	{
		if ( text == null )
			return "";
		int end = text.indexOf( ". " );
		while ( end > 0 && ( text.startsWith( "e.g", end - 3 ) || text.startsWith( "i.e", end - 3 ) ) )
			end = text.indexOf( ". ", end + 1 );
		String sentence = end < 0 ? text : text.substring( 0, end + 1 );
		return sentence.length() <= MAX_SUMMARY ? sentence : sentence.substring( 0, MAX_SUMMARY - 1 ) + "…";
	}

	@SuppressWarnings( "unchecked" )
	private static Map<String, Object> schema( McpToolCatalog.Tool tool )
	{
		try
		{
			Object schema = MAPPER.readValue( tool.inputSchema, Object.class );
			if ( schema instanceof Map )
				return (Map<String, Object>) schema;
		}
		catch ( Exception e )
		{
			// a no-argument tool's "{}" or no schema
		}
		Map<String, Object> empty = new LinkedHashMap<String, Object>();
		empty.put( "type", "object" );
		return empty;
	}

	private static McpToolCatalog.Tool listed( McpToolCatalog full, String name )
	{
		for ( McpToolCatalog.Tool tool : full.getTools() )
			if ( tool.name.equals( name ) )
				return tool;
		return null;
	}

	/** The group of a tool: the word after {@code biouml_} in its name. */
	static String group( String toolName )
	{
		String rest = toolName.startsWith( "biouml_" ) ? toolName.substring( "biouml_".length() ) : toolName;
		int end = rest.indexOf( '_' );
		return end < 0 ? rest : rest.substring( 0, end );
	}

	/** The groups that have tools: the known ones in their order, then any others. */
	private static List<String> groups( McpToolCatalog full )
	{
		List<String> found = new ArrayList<String>();
		for ( McpToolCatalog.Tool tool : full.getTools() )
			if ( !found.contains( group( tool.name ) ) )
				found.add( group( tool.name ) );
		List<String> result = new ArrayList<String>();
		for ( String name : GROUPS.keySet() )
			if ( found.contains( name ) )
				result.add( name );
		for ( String name : found )
			if ( !result.contains( name ) )
				result.add( name );
		return result;
	}

	private static List<String> names( McpToolCatalog full, String group )
	{
		List<String> names = new ArrayList<String>();
		for ( McpToolCatalog.Tool tool : full.getTools() )
			if ( group.equals( group( tool.name ) ) )
				names.add( tool.name );
		return names;
	}

	private static String str( Map<String, Object> args, String key )
	{
		Object v = args == null ? null : args.get( key );
		return v == null ? null : String.valueOf( v );
	}
}
