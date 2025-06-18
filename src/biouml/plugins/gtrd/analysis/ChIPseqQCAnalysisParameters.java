package biouml.plugins.gtrd.analysis;

import java.util.Arrays;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.gtrd.analysis.QualityControlAnalysis.MessageBundle;
import biouml.plugins.gtrd.analysis.QualityControlAnalysis.PathToDataSet;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;

public class ChIPseqQCAnalysisParameters extends AbstractAnalysisParameters {

	public static final String SINGLE_FOLDER = "Datasets (BED-files or tracks) are located in single folder";
	public static final String SEVERAL_FOLDERS = "BED-files or tracks";
	public static final String RAW_CHIPSEQ_DATA = "Raw ChIP-seq data (FASTQ-files)";
	
	private String dataType = SEVERAL_FOLDERS;
	private DataElementPath pathToOutputFolder;
	private InputDataParameters inputDataParameters;
	private QCAnalysisParameters qCAnalysisParameters;
	private PathToDataSet[] pathsToDataSets = new PathToDataSet[]{new PathToDataSet()};
	
	public ChIPseqQCAnalysisParameters()
	{
		setDataType(SEVERAL_FOLDERS);
		setInputDataParameters(new InputDataParameters(this));
		setQCAnalysisParameters(new QCAnalysisParameters(this));
	}

	@PropertyName("Select BED-files")
	public PathToDataSet[] getPathsToDataSets() 
	{
		return pathsToDataSets;
	}
	
	public void setPathsToDataSets(PathToDataSet[] pathsToDataSets)
	{
		Object oldValue = this.pathsToDataSets;
		this.pathsToDataSets = pathsToDataSets;
		firePropertyChange("pathsToDataSets", oldValue, pathsToDataSets);
	}
	public boolean isPathsToDataSetsHidden()
	{
		return( ! getDataType().equals(SEVERAL_FOLDERS));
	}
	
	@PropertyName(MessageBundle.PN_DATA_TYPE)
	@PropertyDescription(MessageBundle.PD_DATA_TYPE)
	public String getDataType()
	{
		return dataType;
	}
	public void setDataType(String dataType)
	{
		Object oldValue = this.dataType;
		this.dataType = dataType;
		firePropertyChange("*", oldValue, dataType);
	}
	@PropertyName(MessageBundle.PN_PATH_TO_OUTPUT_FOLDER)
	@PropertyDescription(MessageBundle.PD_PATH_TO_OUTPUT_FOLDER)
	public DataElementPath getPathToOutputFolder()
	{
		return pathToOutputFolder;
	}
	public void setPathToOutputFolder(DataElementPath pathToOutputFolder)
	{
		Object oldValue = this.pathToOutputFolder;
		this.pathToOutputFolder = pathToOutputFolder;
		firePropertyChange("pathToOutputFolder", oldValue, pathToOutputFolder);
	}

	public boolean areInputParametersHidden()
	{
		return( ! getDataType().equals(RAW_CHIPSEQ_DATA) );
	}
	
	@PropertyName("ChIP-seq raw data processing parameters")
	public InputDataParameters getInputDataParameters() 
	{
		return inputDataParameters;
	}

	public void setInputDataParameters(InputDataParameters inputDataParameters) 
	{
		Object oldValue = this.inputDataParameters;
		this.inputDataParameters = inputDataParameters;
		firePropertyChange("inputDataParameters", oldValue, inputDataParameters);
	}
	
	@PropertyName("FPCM and FNCM Estimation Parameters")
	public QCAnalysisParameters getQCAnalysisParameters() 
	{
		return qCAnalysisParameters;
	}

	public void setQCAnalysisParameters(QCAnalysisParameters qCAnalysisParameters) 
	{
		Object oldValue = this.qCAnalysisParameters;
		this.qCAnalysisParameters = qCAnalysisParameters;
		firePropertyChange("qCAnalysisParameters", oldValue, qCAnalysisParameters);
	}
	
	public static class AUCParametersBeanInfo extends BeanInfoEx2<AUCParameters>
    {
		public AUCParametersBeanInfo()
        {
            super(AUCParameters.class);
        }
		
		@Override
		protected void initProperties() throws Exception
        {
			add(DataElementPathEditor.registerInputChild("pathToFolderWithSiteModels", beanClass, SiteModel.class, true));
            add("siteModelName", SiteModelNameSelector.class);
            add("dbSelector"); 
        }
    }
	
	public static class AUCParameters extends OptionEx implements JSONBean
	{
		private DataElementPath pathToFolderWithSiteModels = DataElementPath.create("databases/HOCOMOCO v11/Data/PWM_HUMAN_mono_pval=0.001");
		private String siteModelName;
		private BasicGenomeSelector dbSelector;
		
		public AUCParameters(Option parent) {
			super( parent );
			setDbSelector(new BasicGenomeSelector());
		}
		
		@PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_SITE_MODELS)
		@PropertyDescription("Select a folder with transcription factor binding sites (TFBSs) models")
		public DataElementPath getPathToFolderWithSiteModels()
		{
			return pathToFolderWithSiteModels;
		}
		public void setPathToFolderWithSiteModels(DataElementPath pathToFolderWithSiteModels)
		{
			Object oldValue = this.pathToFolderWithSiteModels;
			this.pathToFolderWithSiteModels = pathToFolderWithSiteModels;
			firePropertyChange("pathToFolderWithSiteModels", oldValue, pathToFolderWithSiteModels);
		}
		
		@PropertyName(MessageBundle.PN_SITE_MODEL_NAME)
		@PropertyDescription("Select a TFBS model name")
		public String getSiteModelName()
		{
			return siteModelName;
		}
		public void setSiteModelName(String siteModelName)
		{
			Object oldValue = this.siteModelName;
			this.siteModelName = siteModelName;
			firePropertyChange("siteModelName", oldValue, siteModelName);
		}

		@PropertyName(MessageBundle.PN_DB_SELECTOR)
		@PropertyDescription(MessageBundle.PD_DB_SELECTOR)
		public BasicGenomeSelector getDbSelector()
		{
			return dbSelector;
		}
		public void setDbSelector(BasicGenomeSelector dbSelector)
		{
			Object oldValue = this.dbSelector;
			this.dbSelector = dbSelector;
			dbSelector.setParent(this);
			firePropertyChange("dbSelector", oldValue, dbSelector);
		}
	}
	
	public static class SiteModelNameSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            DataCollection<DataElement> dc = ((AUCParameters) getBean()).getPathToFolderWithSiteModels().getDataCollection(DataElement.class);
            String[] siteModelNames = dc.getNameList().toArray(new String[0]);
            Arrays.sort(siteModelNames, String.CASE_INSENSITIVE_ORDER);
            return siteModelNames;
        }
    }
	
	public static class QCAnalysisParametersBeanInfo extends BeanInfoEx2<QCAnalysisParameters>
    {
		public QCAnalysisParametersBeanInfo()
        {
            super(QCAnalysisParameters.class);
        }
		
		@Override
		protected void initProperties() throws Exception
        {
			add("minimalLengthOfBindingRegion");
            add("maximalLengthOfBindingRegion"); 
            add("fpcmThreshold");
            add("doAucs");
            addHidden("aUCParameters", "areAUCParametersHidden");
        }
    }
	
	public static class QCAnalysisParameters extends OptionEx implements JSONBean
	{
		private int minimalLengthOfBindingRegion = 20;
		private int maximalLengthOfBindingRegion = 300;
		private double fpcmThreshold = 2.0;
		private boolean doAucs = false;
		private DataElementPath pathToFolderWithFiles;
		private String[] fileNames;
		private AUCParameters aUCParameters;
		
		public QCAnalysisParameters(Option parent) {
			super( parent );
			setAUCParameters(new AUCParameters(this));
		}

		@PropertyName("AUC Calculation Parameters")
		public AUCParameters getAUCParameters() 
		{
			return aUCParameters;
		}

		public void setAUCParameters(AUCParameters aUCParameters) 
		{
			Object oldValue = this.aUCParameters;
			this.aUCParameters = aUCParameters;
			firePropertyChange("aUCParameters", oldValue, aUCParameters);
		}
		@PropertyName(MessageBundle.PN_MINIMAL_LENGTH_OF_BINDING_REGION)
		@PropertyDescription("Binding regions shorter than the minimal length would be extended")
		public int getMinimalLengthOfBindingRegion()
		{
			return minimalLengthOfBindingRegion;
		}
		public void setMinimalLengthOfBindingRegion(int minimalLengthOfBindingRegion)
		{
			Object oldValue = this.minimalLengthOfBindingRegion;
			this.minimalLengthOfBindingRegion = minimalLengthOfBindingRegion;
			firePropertyChange("minimalLengthOfBindingRegion", oldValue, minimalLengthOfBindingRegion);
		}
		@PropertyName(MessageBundle.PN_MAXIMAL_LENGTH_OF_BINDING_REGION)
		@PropertyDescription("Binding regions longer than the minimal length would be narrowed")
		public int getMaximalLengthOfBindingRegion()
		{
			return maximalLengthOfBindingRegion;
		}
		public void setMaximalLengthOfBindingRegion(int maximalLengthOfBindingRegion)
		{
			Object oldValue = this.maximalLengthOfBindingRegion;
			this.maximalLengthOfBindingRegion = maximalLengthOfBindingRegion;
			firePropertyChange("maximalLengthOfBindingRegion", oldValue, maximalLengthOfBindingRegion);
		}
		@PropertyName(MessageBundle.PN_FPCM_THRESHOLD)
		@PropertyDescription("If the FPCM exceeds the threshold FNCM will be calculated based on Chapman’s estimate")
		public double getFpcmThreshold()
		{
			return fpcmThreshold;
		}
		public void setFpcmThreshold(double fpcmThreshold)
		{
			Object oldValue = this.fpcmThreshold;
			this.fpcmThreshold = fpcmThreshold;
			firePropertyChange("fpcmThreshold", oldValue, fpcmThreshold);
		}
		
		@PropertyName("Calculate AUC?")
		@PropertyDescription("Calculate AUC?")
		public boolean getDoAucs()
		{
			return doAucs;
		}
		public void setDoAucs(boolean doAucs)
		{
			Object oldValue = this.doAucs;
			this.doAucs = doAucs;
			firePropertyChange("*", oldValue, doAucs);
		}
		public boolean areAUCParametersHidden()
		{
			return( ! getDoAucs() );
		}
	}
	
    public static class InputDataParametersBeanInfo extends BeanInfoEx2<InputDataParameters>
    {
        public InputDataParametersBeanInfo()
        {
            super( InputDataParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property("expTypeSelector").tags( InputDataParameters.EXP_TYPE_SINGLE_END, InputDataParameters.EXP_TYPE_PAIRED_END ).add();
            property("pathToInputExp1").inputElement( FileDataElement.class ).add();
            property("pathToInputExp2").inputElement( FileDataElement.class ).hidden( "isPathToInputExp2Hidden" ).add();
            add( "hasControl" );
            property("ctrlTypeSelector").tags( InputDataParameters.EXP_TYPE_SINGLE_END, InputDataParameters.EXP_TYPE_PAIRED_END ).hidden("isCtrlTypeSelectorHidden").add();
            property("pathToInputCtrl1").inputElement( FileDataElement.class ).hidden("isPathToInputCtrl1Hidden").add();
            property("pathToInputCtrl2").inputElement( FileDataElement.class ).hidden("isPathToInputCtrl2Hidden").add();
            property("refGenome").tags(InputDataParameters.H_SAPIENS, InputDataParameters.M_MUSCULUS, InputDataParameters.R_NORVEGICUS,
            		InputDataParameters.D_RERIO, InputDataParameters.D_MELANOGASTER, InputDataParameters.C_ELEGANS,
            		InputDataParameters.S_CEREVISIAE, InputDataParameters.S_POMBE, InputDataParameters.A_THALIANA).add();
        }
    }

    public static class InputDataParameters extends OptionEx implements JSONBean
    {
        private boolean hasControl;
		
		private static String H_SAPIENS = "Homo sapiens hg38";
		private static String A_THALIANA = "Arabidopsis thaliana TAIR10";
		private static String C_ELEGANS = "Caenorhabditis elegans wbcel235";
		private static String D_RERIO = "Danio rerio GRCz11";
		private static String D_MELANOGASTER = "Drosophila melanogaster dm6";
		private static String M_MUSCULUS = "Mus musculus mm10";
		private static String R_NORVEGICUS = "Rattus norvegicus Rnor_6.0";
		private static String S_CEREVISIAE = "Saccharomyces cerevisiae R64-1-1";
		private static String S_POMBE = "Schizosaccharomyces pombe ASM294v2";
		private String refGenome;
		
		static final String EXP_TYPE_PAIRED_END = "Paired-end";
        static final String EXP_TYPE_SINGLE_END = "Single-end";
		private String expTypeSelector;
		
		private String pathToMacsOutput;
		private String pathToGemOutput;
		private String pathToSissrsOutput;
		private String pathToPicsOutput;
		
		private String ctrlTypeSelector;
		private DataElementPath pathToInputExp1;
		private DataElementPath pathToInputExp2;
		private DataElementPath pathToInputCtrl1;
		private DataElementPath pathToInputCtrl2;

		public InputDataParameters(Option parent) {
		    super( parent );
			hasControl = false;
			expTypeSelector = EXP_TYPE_SINGLE_END;
			ctrlTypeSelector = EXP_TYPE_SINGLE_END;
			refGenome = H_SAPIENS;
		}
		
		@PropertyName("Reference Genome")
		public String getRefGenome() 
		{
			return refGenome;
		}

		public void setRefGenome(String refGenome) 
		{
			this.refGenome = refGenome;
		}

		@PropertyName("Has Input Control?")
		public boolean getHasControl()
		{
			return hasControl;
		}

		@PropertyName("Experiment library layout")
		public String getExpTypeSelector()
		{
			return expTypeSelector;
		}

		public void setExpTypeSelector(String expTypeSelector)
		{
			Object oldValue = this.expTypeSelector;
			this.expTypeSelector = expTypeSelector;
			firePropertyChange("*", oldValue, expTypeSelector);
		}

		@PropertyName("Input control library layout")
		public String getCtrlTypeSelector()
		{
			return ctrlTypeSelector;
		}

		public void setCtrlTypeSelector(String ctrlTypeSelector)
		{
			Object oldValue = this.ctrlTypeSelector;
			this.ctrlTypeSelector = ctrlTypeSelector;
			firePropertyChange("*", oldValue, ctrlTypeSelector);
		}
		
		public String getPathToMacsOutput() {
			return this.pathToMacsOutput;
		}
		public void setPathToMacsOutput(String path) {
			this.pathToMacsOutput = path;
		}
		public String getPathToGemOutput() {
			return this.pathToGemOutput;
		}
		public void setPathToGemOutput(String path) {
			this.pathToGemOutput = path;
		}
		public String getPathToSissrsOutput() {
			return this.pathToSissrsOutput;
		}
		public void setPathToSissrsOutput(String path) {
			this.pathToSissrsOutput = path;
		}
		public String getPathToPicsOutput() {
			return this.pathToPicsOutput;
		}
		public void setPathToPicsOutput(String path) {
			this.pathToPicsOutput = path;
		}
		
		public boolean isPathToInputExp2Hidden()
		{
			return( ! getExpTypeSelector().equals(EXP_TYPE_PAIRED_END) );
		}
		public boolean isPathToInputCtrl1Hidden()
		{
			return( ! getHasControl() );
		}
		public boolean isPathToInputCtrl2Hidden()
		{
			return( ! getCtrlTypeSelector().equals(EXP_TYPE_PAIRED_END) || ! getHasControl() );
		}
		public boolean isCtrlTypeSelectorHidden()
		{
			return( ! getHasControl() );
		}
		@PropertyName("FASTQ-file")
		@PropertyDescription("ChIP-seq raw data")
		public DataElementPath getPathToInputExp1() 
		{
			return pathToInputExp1;
		}

		public void setPathToInputExp1(DataElementPath pathToInputExp1) 
		{
			Object oldValue = this.pathToInputExp1;
			this.pathToInputExp1 = pathToInputExp1;
			firePropertyChange("pathToInputExp1", oldValue, pathToInputExp1);
		}
		@PropertyName("FASTQ-file")
		@PropertyDescription("ChIP-seq raw data")
		public DataElementPath getPathToInputExp2() 
		{
			if(getExpTypeSelector().equals(EXP_TYPE_SINGLE_END))
				return null;
			return pathToInputExp2;
		}

		public void setPathToInputExp2(DataElementPath pathToInputExp2) 
		{
			Object oldValue = this.pathToInputExp2;
			this.pathToInputExp2 = pathToInputExp2;
			firePropertyChange("pathToInputExp2", oldValue, pathToInputExp2);
		}
		@PropertyName("FASTQ-file")
		@PropertyDescription("ChIP-seq raw data")
		public DataElementPath getPathToInputCtrl1() 
		{
			if( ! getHasControl())
				return null;
			return pathToInputCtrl1;
		}

		public void setPathToInputCtrl1(DataElementPath pathToInputCtrl1) 
		{
			Object oldValue = this.pathToInputCtrl1;
			this.pathToInputCtrl1 = pathToInputCtrl1;
			firePropertyChange("pathToInputCtrl1", oldValue, pathToInputCtrl1);
		}
		@PropertyName("FASTQ-file")
		@PropertyDescription("ChIP-seq raw data")
		public DataElementPath getPathToInputCtrl2() {
			if( getHasControl() && getCtrlTypeSelector().equals(EXP_TYPE_PAIRED_END))
				return pathToInputCtrl2;
			return null;
		}

		public void setPathToInputCtrl2(DataElementPath pathToInputCtrl2) {
			Object oldValue = this.pathToInputCtrl2;
			this.pathToInputCtrl2 = pathToInputCtrl2;
			firePropertyChange("pathToInputCtrl2", oldValue, pathToInputCtrl2);
		}

		public void setHasControl(boolean hasControl)
		{
			Object oldValue = this.hasControl;
			this.hasControl = hasControl;
			firePropertyChange("*", oldValue, hasControl);
		}
	}
}



