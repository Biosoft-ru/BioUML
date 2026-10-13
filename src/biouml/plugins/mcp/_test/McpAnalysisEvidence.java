package biouml.plugins.mcp._test;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import biouml.plugins.mcp.support.McpAnalysisSupport;
import biouml.plugins.mcp.support.McpEnvelope;

/**
 * Evidence runner (NOT a JUnit test) for the phase-3 analysis tools. Prints the in-process
 * equivalents of the MCP tool results required by the phase-3 spec: analyses list, describe,
 * sync run → result rows, bad-param error, and (best-effort) an async run → poll → result
 * transcript.
 *
 * <p>Run against a temp repository (see McpAnalysisToolsTest) or any initialized BioUML repo.
 * It registers the {@link McpTestAnalysis} stub the same way the tests do.</p>
 */
public final class McpAnalysisEvidence
{
	private McpAnalysisEvidence()
	{
	}

	private static void print( ObjectMapper m, String label, McpEnvelope env )
	{
		System.out.println( "--- " + label + " (ok=" + env.isOk() + ( env.getCode() != null ? ", code=" + env.getCode() : "" ) + " )" );
		try
		{
			System.out.println( m.writerWithDefaultPrettyPrinter().writeValueAsString( env.toMap() ) );
		}
		catch ( Exception e )
		{
			System.out.println( "(serialization error: " + e.getMessage() + ")" );
		}
	}

	public static void main( String[] args ) throws Exception
	{
		ObjectMapper m = new ObjectMapper();
		// Register the stub so list/describe/run have something concrete to show.
		AnalysisMethodRegistry.getAnalysisGroups().findAny();
		ru.biosoft.analysiscore.AnalysisMethodInfo info =
				new ru.biosoft.analysiscore.AnalysisMethodInfo( McpTestAnalysis.NAME, "Test stub analysis", null, McpTestAnalysis.class );
		AnalysisMethodRegistry.addMethodToGroup( McpTestAnalysis.NAME, "McpTest", info );

		// 1. analyses list
		print( m, "biouml_analysis_list", McpAnalysisSupport.listMethods() );

		// 2. describe the stub
		print( m, "biouml_analysis_describe mcp.test", McpAnalysisSupport.describeMethod( McpTestAnalysis.NAME ) );

		// 3. bad param → structured error listing valid keys
		Map<String, Object> bad = new LinkedHashMap<String, Object>();
		bad.put( "noSuchKey", 1 );
		print( m, "biouml_analysis_run bad-param", McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, bad, null, true ) );

		// 4. sync run (needs a repository to write the result; if no repo is initialized this will
		//    report an error envelope, which is itself useful transcript evidence).
		Map<String, Object> good = new LinkedHashMap<String, Object>();
		good.put( "rows", 3 );
		good.put( "output", args.length > 0 ? args[ 0 ] : "analyses/mcpEvidenceResult" );
		print( m, "biouml_analysis_run sync", McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, good, null, true ) );

		// 5. async run → poll → result (best-effort; requires a TaskManager)
		print( m, "biouml_analysis_run async", McpAnalysisSupport.runAnalysis( McpTestAnalysis.NAME, good, null, false ) );
	}
}
