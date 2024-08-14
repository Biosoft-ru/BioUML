package biouml.plugins.gtrd.analysis;

import java.util.Properties;
import java.util.Objects;

import biouml.plugins.gtrd.analysis.ChIPseqQCAnalysisParameters.AUCParameters;
import biouml.plugins.gtrd.analysis.ChIPseqQCAnalysisParameters.InputDataParameters;
import biouml.plugins.gtrd.analysis.ChIPseqQCAnalysisParameters.QCAnalysisParameters;
import biouml.plugins.gtrd.analysis.QualityControlAnalysis.PathToDataSet;
import biouml.plugins.gtrd.analysis.QualityControlAnalysis.QualityControlAnalysisParameters;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.ComplexAnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisParameters;

public class ChIPseqQCAnalysis extends ComplexAnalysisMethodSupport<ChIPseqQCAnalysisParameters>{

	public ChIPseqQCAnalysis(DataCollection<?> origin, String name)
	{
		super(origin, name, new ChIPseqQCAnalysisParameters());
	}
	
	private static final String CHIP_PROCCESSING_JOB = "Raw data processing";
    private static final String QC_ANALYSIS_JOB = "Quality control analysis";
    private static final String ENCODE_QC_ANALYSIS_JOB = "Encode Quality control analysis";
    private static final String REPORT_GENERATION_JOB = "Report generation";
	private AnalysisMethod chipseqProcessing;
	private QualityControlAnalysis qcAnalysis;
	private QCAnalysisReportGenerator qcReportGenerator;
	private AnalysisMethod encodeQc;


	protected void initChIPseqProcessingAnalysis()
	{
		AnalysisParameters parameters = chipseqProcessing.getParameters();
		
		Properties properties = new Properties();
		InputDataParameters inputDataParameters = this.parameters.getInputDataParameters();
		
        if( inputDataParameters.getExpTypeSelector().equals( InputDataParameters.EXP_TYPE_SINGLE_END ) )
		{
			properties.setProperty("exp_library|type", "{\"displayName\":\"Single-end\",\"name\":\"single\"}");
			properties.setProperty("exp_library|single|input_exp_1", Objects.toString(inputDataParameters.getPathToInputExp1()));
		} else
		{
			properties.setProperty("exp_library|type", "{\"displayName\":\"Paired-end\",\"name\":\"paired\"}");
			properties.setProperty("exp_library|paired|input_exp_1", Objects.toString(inputDataParameters.getPathToInputExp1()));
			properties.setProperty("exp_library|paired|input_exp_2", Objects.toString(inputDataParameters.getPathToInputExp2()));
		}
		if(inputDataParameters.getHasControl()) {
			properties.setProperty("input_library|hasInput", "{\"displayName\":\"Yes\",\"name\":\"yes\"}");
            if( inputDataParameters.getCtrlTypeSelector().equals( InputDataParameters.EXP_TYPE_SINGLE_END ) )
			{
				properties.getProperty("input_library|yes||type", "{\"displayName\":\"Single-end\",\"name\":\"single\"}");
				properties.setProperty("input_library|yes||single|input_ctrl_1", Objects.toString(inputDataParameters.getPathToInputCtrl1()));
			} else
			{
			properties.getProperty("input_library|yes||type", "{\"displayName\":\"Paired-end\",\"name\":\"paired\"}");
			properties.setProperty("input_library|yes||paired|input_ctrl_1", Objects.toString(inputDataParameters.getPathToInputCtrl1()));
			properties.setProperty("input_library|yes||paired|input_ctrl_2", Objects.toString(inputDataParameters.getPathToInputCtrl2()));
			}
		} else
		{
			properties.setProperty("input_library|hasInput", "{\"displayName\":\"No\",\"name\":\"no\"}");
		}
		properties.setProperty("genome", "{\"displayName\":\""+ inputDataParameters.getRefGenome() +"\","
				+ "\"name\":\""+ inputDataParameters.getRefGenome() +"\"}");
		properties.setProperty("MACS_output", this.parameters.getPathToOutputFolder().toString() + "/MACS_peaks");
		properties.setProperty("GEM_output", this.parameters.getPathToOutputFolder().toString() + "/GEM_peaks");
		properties.setProperty("SISSRS_output", this.parameters.getPathToOutputFolder().toString() + "/SISSRS_peaks");
		properties.setProperty("PICS_output", this.parameters.getPathToOutputFolder().toString() + "/PICS_peaks");
		properties.setProperty("alignment_exp_output", this.parameters.getPathToOutputFolder().toString() + "/alignment_exp");
		if( !this.parameters.getInputDataParameters().isPathToInputCtrl1Hidden() )
			properties.setProperty("alignment_ctrl_output", this.parameters.getPathToOutputFolder().toString() + "/alignment_ctrl");
		parameters.read(properties, "");
		
	}
	
	protected void initEncodeQualityControlAnalysis()
	{
		AnalysisParameters parameters = encodeQc.getParameters();
		InputDataParameters inputDataParameters = this.parameters.getInputDataParameters();
		Properties properties = new Properties();
		
		properties.setProperty("alignment", this.parameters.getPathToOutputFolder().toString() + "/alignment_exp");
		properties.setProperty("peaks_macs", this.parameters.getPathToOutputFolder().toString() + "/MACS_peaks");
		properties.setProperty("peaks_gem", this.parameters.getPathToOutputFolder().toString() + "/GEM_peaks");
		properties.setProperty("peaks_sissrs", this.parameters.getPathToOutputFolder().toString() + "/SISSRS_peaks");
		properties.setProperty("peaks_pics", this.parameters.getPathToOutputFolder().toString() + "/PICS_peaks");
		properties.setProperty("output", this.parameters.getPathToOutputFolder().toString() + "/Encode_quality_metrics");
		properties.setProperty("genome", "{\"displayName\":\"" + inputDataParameters.getRefGenome() + "\","
				+ "\"name\":\"" + inputDataParameters.getRefGenome() + "\"}");
		parameters.read(properties, "");
	}
	
	protected void initQualityControlAnalysis() throws Exception
	{
		
		QualityControlAnalysisParameters parameters = qcAnalysis.getParameters();
		QCAnalysisParameters qCAnalysisParameters = this.parameters.getQCAnalysisParameters();
		AUCParameters aUCParameters = this.parameters.getQCAnalysisParameters().getAUCParameters();
		
		parameters.setPathToOutputFolder(this.parameters.getPathToOutputFolder());
		parameters.setDoAucs(qCAnalysisParameters.getDoAucs());
		parameters.setPathToFolderWithSiteModels(qCAnalysisParameters.getDoAucs() ? aUCParameters.getPathToFolderWithSiteModels() : null);
        parameters.setSiteModelName(qCAnalysisParameters.getDoAucs() ? aUCParameters.getSiteModelName() : null); //
        parameters.setDataType(QualityControlAnalysis.SEVERAL_FOLDERS);
        
        if(this.parameters.getDataType().equals(ChIPseqQCAnalysisParameters.RAW_CHIPSEQ_DATA)) {
        	PathToDataSet macsPeaks = new PathToDataSet();
        	PathToDataSet gemPeaks = new PathToDataSet();
        	PathToDataSet sissrsPeaks = new PathToDataSet();
        	PathToDataSet picsPeaks = new PathToDataSet();
        	
        	DataCollection<DataElement> collection = this.parameters.getPathToOutputFolder().getDataCollection();
        	macsPeaks.setPathToDataSet(collection.get("MACS_peaks").getCompletePath());
        	gemPeaks.setPathToDataSet(collection.get("GEM_peaks").getCompletePath());
        	sissrsPeaks.setPathToDataSet(collection.get("SISSRS_peaks").getCompletePath());
        	picsPeaks.setPathToDataSet(collection.get("PICS_peaks").getCompletePath());
        	parameters.getParametersForSeveralFolders().setPathsToDataSets(new PathToDataSet[] {macsPeaks, gemPeaks, sissrsPeaks, picsPeaks});
 	
        } else {
			
        	parameters.getParametersForSeveralFolders().setPathsToDataSets(this.parameters.getPathsToDataSets());
	       
		}
	}
	
	protected void initQcReportGenerator()
	{
		qcReportGenerator.setParameters(this.parameters);
	}
	
	
	
	private void initSubJobs()
	{
		if(this.parameters.getDataType().equals(ChIPseqQCAnalysisParameters.RAW_CHIPSEQ_DATA)) {
			chipseqProcessing = DataElementPath.create( "analyses/Galaxy/test/chip-seq_processing" ).getDataElement( AnalysisMethodInfo.class ).createAnalysisMethod();
			chipseqProcessing.setLogger(getLogger());
			encodeQc = DataElementPath.create( "analyses/Galaxy/test/encode_qc_metrics_estimation" ).getDataElement( AnalysisMethodInfo.class ).createAnalysisMethod();
			encodeQc.setLogger(getLogger());
		}
		qcAnalysis = AnalysisMethodRegistry.getAnalysisMethod( QualityControlAnalysis.class );
		qcAnalysis.setLogger(getLogger());
		qcReportGenerator = AnalysisMethodRegistry.getAnalysisMethod( QCAnalysisReportGenerator.class );
		qcReportGenerator.setLogger(getLogger());
		if( ! this.parameters.getDataType().equals(ChIPseqQCAnalysisParameters.RAW_CHIPSEQ_DATA)) {
			addAnalysis(qcAnalysis, 99, QC_ANALYSIS_JOB);
			addAnalysis(qcReportGenerator, 100, REPORT_GENERATION_JOB);
		}else if(parameters.getQCAnalysisParameters().getDoAucs()) {
			addAnalysis(chipseqProcessing, 50, CHIP_PROCCESSING_JOB);
			addAnalysis(encodeQc, 65, ENCODE_QC_ANALYSIS_JOB);
			addAnalysis(qcAnalysis, 99, QC_ANALYSIS_JOB);
			addAnalysis(qcReportGenerator, 100, REPORT_GENERATION_JOB);
		} else {
			addAnalysis(chipseqProcessing, 25, CHIP_PROCCESSING_JOB);
			addAnalysis(encodeQc, 30, ENCODE_QC_ANALYSIS_JOB);
			addAnalysis(qcAnalysis, 99, QC_ANALYSIS_JOB);
			addAnalysis(qcReportGenerator, 100, REPORT_GENERATION_JOB);
		}
	}
	
	@Override
    public void beforeRun() throws Exception
    {
        initSubJobs();
    }
	
	@Override
    public void beforeJob(String name) throws Exception
    {
        if(name.equals(CHIP_PROCCESSING_JOB))
        	initChIPseqProcessingAnalysis();
        else if(name.equals(ENCODE_QC_ANALYSIS_JOB))
        	initEncodeQualityControlAnalysis();
        else if(name.equals(QC_ANALYSIS_JOB))
        	initQualityControlAnalysis();
        else if(name.equals(REPORT_GENERATION_JOB))
        	initQcReportGenerator();
    }
	

}