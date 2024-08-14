package biouml.plugins.agentmodeling;

import java.io.File;
import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.simulation.Span;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TempFile;

/**
 * 
 * @author Ilya Agent performs custom python code
 */
public class PythonAgent extends SimulationAgent {

	public final static String PYTHON_AGENT = "PythonAgent";

	private int spanIndex = 0;
	private static String header = "#!/usr/bin/env python\n";
	private String resultFile1 = "f = open(r\"";
	private String resultFile2 = "\", \"w\")\n";
	private static String close = "f.write(str(data))\nf.close()";
	private DataElementPath resultPath = DataElementPath.create("data/Collaboration/Ilya/Data/Diagrams/result");
	private String script;

	public PythonAgent(String name, Span span, String script) {
		super(name, span);
		this.script = script;
	}

	@Override
	public void applyChanges() {

	}

	public String getUserCode() {
//		String result = "data =\"" + script + "\"\n";
		String result = "import random\ndata = random.random()\n";
		return result;
	}

	public static PythonAgent createAgent(String name, Span span, String script) throws Exception {
		return new PythonAgent(name, span, script);
	}

	public String runCommand() throws Exception {
		File resultFile = TempFile.createTempFile("result", ".txt");
		File f = TempFile.createTempFile("code", ".py");
		String command = header + getUserCode() + resultFile1 + resultFile.getAbsolutePath() + resultFile2 + close;
//		System.out.println(command);
		ApplicationUtils.writeString(f, command);
		File dir = new File(f.getParent());
		Process p = Runtime.getRuntime().exec(new String[] { "python", f.getName() }, null, dir);
		p.waitFor();
		String result = ApplicationUtils.readAsString(resultFile);
		f.delete();
		resultFile.delete();
		return result;
	}

	public void loadResult(String result) {
		TableDataCollection tdc = resultPath.getDataElement(TableDataCollection.class);
//		tdc.getColumnModel().addColumn("Result", DataType.Text);
//		System.out.println("ADD " + currentTime + " " + result);
		TableDataCollectionUtils.addRow(tdc, String.valueOf(currentTime), new Object[] { result });
		tdc.getOrigin().put(tdc);
	}

	@Override
	public void iterate() {
		spanIndex++;
		if (spanIndex >= span.getLength())
			isAlive = false;
		else {
			currentTime = span.getTime(spanIndex);
			try {
				String result = runCommand();
				loadResult(result);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void defineVariableValues() {

	}

	public void retrieveVariableValues() throws Exception {

	}

	@Override
	public double[] getCurrentValues() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getVariableNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[] getUpdatedValues() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void setUpdated() throws Exception {
		// TODO Auto-generated method stub

	}
}
