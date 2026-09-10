package biouml.plugins.mcp._test;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * A trivial, self-contained analysis used as the sandbox-safe fixture for the phase-3 MCP analysis
 * tests. It takes no input and produces a small constant table (id + value), so the full
 * run → monitor → read-result lifecycle can be exercised without real bioinformatics compute.
 *
 * <p>Registered under tests via {@code AnalysisMethodRegistry.addTestAnalysis("mcp.test", ...)}
 * (guarded by the test-mode security manager).</p>
 */
public class McpTestAnalysis extends AnalysisMethodSupport<McpTestAnalysis.Parameters>
{
	public static final String NAME = "mcp.test";

	public McpTestAnalysis( DataCollection<?> origin, String name )
	{
		super( origin, name, new Parameters() );
	}

	@Override
	public void validateParameters() throws IllegalArgumentException
	{
		// Auto-fill the output path if the caller didn't provide one. The GUI normally materializes
		// the BeanInfo 'auto' default, but headless runs (this test, MCP tools) must not depend on
		// that, so the stub is self-contained.
		if ( parameters.getOutput() == null && getOrigin() != null )
			parameters.setOutput( DataElementPath.create( getOrigin(), "mcpTestResult" ) );
		if ( parameters.getRows() <= 0 )
			throw new IllegalArgumentException( "rows must be > 0" );
		super.validateParameters();
	}

	@Override
	public Object justAnalyzeAndPut() throws Exception
	{
		DataElementPath outPath = parameters.getOutput();
		if ( outPath == null )
			outPath = DataElementPath.create( getOrigin(), "mcpTestResult" );
		DataCollection<?> parent = outPath.getParentCollection();
		TableDataCollection out = TableDataCollectionUtils.createTableDataCollection( parent, outPath.getName() );
		// Name the columns explicitly so the result has stable, discoverable column names.
		out.getColumnModel().addColumn( "id", Integer.class );
		out.getColumnModel().addColumn( "value", Double.class );
		int n = parameters.getRows();
		for ( int i = 0; i < n; i++ )
		{
			TableDataCollectionUtils.addRow( out, "row" + i, new Object[] { Integer.valueOf( i ), Double.valueOf( i * 1.5 ) }, true );
		}
		out.finalizeAddition();
		// createTableDataCollection does not register the collection in the repository; put it into
		// its parent so the result is findable by path (this is how real analyses materialize
		// outputs).
		if ( !parent.contains( outPath.getName() ) )
		{
			@SuppressWarnings( "unchecked" )
			DataCollection<ru.biosoft.access.core.DataElement> parentObj = (DataCollection<ru.biosoft.access.core.DataElement>) parent;
			parentObj.put( out );
		}
		getJobControl().setPreparedness( 100 );
		return out;
	}

	public static class Parameters extends AbstractAnalysisParameters
	{
		private int rows = 3;
		@PropertyName( "Number of rows" )
		public int getRows()
		{
			return rows;
		}
		public void setRows( int rows )
		{
			int oldValue = this.rows;
			this.rows = rows;
			firePropertyChange( "rows", oldValue, rows );
		}

		private DataElementPath output;
		@PropertyName( "Output table" )
		public DataElementPath getOutput()
		{
			return output;
		}
		public void setOutput( DataElementPath output )
		{
			DataElementPath oldValue = this.output;
			this.output = output;
			firePropertyChange( "output", oldValue, output );
		}
	}

	public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
	{
		public ParametersBeanInfo()
		{
			super( Parameters.class );
		}

		@Override
		protected void initProperties() throws Exception
		{
			add( "rows" );
			// 'output' is deliberately NOT registered as an outputElement: it is optional for the
			// stub. Registering it would make super.validateParameters()/checkPaths() require a
			// non-null path, which fails on headless runs where the origin may be null. The result
			// table is created inside justAnalyzeAndPut() at a path under the origin (or a sensible
			// default) instead.
			add( "output" );
		}
	}
}
