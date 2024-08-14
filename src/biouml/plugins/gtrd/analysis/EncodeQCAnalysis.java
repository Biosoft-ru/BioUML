package biouml.plugins.gtrd.analysis;

import biouml.plugins.gtrd.analysis.QualityControlAnalysis.PathToDataSet;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;

public class EncodeQCAnalysis extends AnalysisMethodSupport<EncodeQCAnalysis.EncodeQCAnalysisParameters> {
	public EncodeQCAnalysis(DataCollection<?> origin, String name) {
		super(origin, name, new EncodeQCAnalysisParameters());
	}
	
	@Override
    public DataCollection<?> justAnalyzeAndPut() {
		ru.biosoft.access.core.DataElementPath pathToOutputfolder = parameters.getPathToOutputFolder();
		ru.biosoft.access.core.DataElementPath pathToAlignment = parameters.getPathToAlignment();
		PathToDataSet[] pathsToPeaks = parameters.getPathsToPeaks();
		
		
		return null;
    }
	
	public static class EncodeQCAnalysisParameters extends AbstractAnalysisParameters {
		private DataElementPath pathToAlignment;
		private PathToDataSet[] pathsToPeaks;
		private DataElementPath pathToOutputFolder;
		
		EncodeQCAnalysisParameters() {
			pathsToPeaks = new PathToDataSet[] {new PathToDataSet()};
		}
		
		public DataElementPath getPathToAlignment() {
			return pathToAlignment;
		}
		public void setPathToAlignment(DataElementPath pathToAlignment) {
			Object oldValue = this.pathToAlignment;
			this.pathToAlignment = pathToAlignment;
			firePropertyChange("alignment", oldValue, pathToAlignment);
		}
		public PathToDataSet[] getPathsToPeaks() {
			return pathsToPeaks;
		}
		public void setPathsToPeaks(PathToDataSet[] pathsToPeaks) {
			Object oldValue = this.pathsToPeaks;
			this.pathsToPeaks = pathsToPeaks;
			firePropertyChange("peaks", oldValue, pathsToPeaks);
		}
		public DataElementPath getPathToOutputFolder() {
			return pathToOutputFolder;
		}
		public void setPathToOutputFolder(DataElementPath pathToOutputFolder) {
			Object oldValue = this.pathToOutputFolder;
			this.pathToOutputFolder = pathToOutputFolder;
			firePropertyChange("pathToOutputFolder", oldValue, pathToOutputFolder);
		}	
	}
	
	
	
	public static class EncodeQCAnalysisParametersBeanInfo extends BeanInfoEx2<EncodeQCAnalysisParameters> {
		 public EncodeQCAnalysisParametersBeanInfo()
	        {
	            super(EncodeQCAnalysisParameters.class);
	        }

		 @Override
		 protected void initProperties() throws Exception
		 {
			 property("alignment").add();
			 property("peaks").add();
		 }
	}
}
