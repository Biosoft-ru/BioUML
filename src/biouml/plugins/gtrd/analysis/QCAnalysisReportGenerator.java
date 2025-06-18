package biouml.plugins.gtrd.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.gtrd.analysis.ChIPseqQCAnalysisParameters;
import biouml.plugins.gtrd.analysis.ChIPseqQCAnalysisParameters.InputDataParameters;
import biouml.plugins.gtrd.analysis.QualityControlAnalysis.PathToDataSet;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.access.DataElementExporterRegistry.ExporterInfo;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.project.ProjectAsLists;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.BamTrackViewBuilder.BamTrackViewOptions;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.TempFiles;

public class QCAnalysisReportGenerator extends AnalysisMethodSupport<ChIPseqQCAnalysisParameters>{
	private DataElementPath macsPeaks;
	private DataElementPath gemPeaks;
	private DataElementPath sissrsPeaks;
	private DataElementPath picsPeaks;
	private DataElementPath mergedPeaks;
	private DataElementPath mergedNoOrphsPeaks;
	private DataElementPath qualityMetrics;
	private DataElementPath encodeQualityMetrics;
	private DataElementPath aucPic;
	private DataElementPath aucTable;
	private DataElementPath outFolder;
	private List<SqlTrack> sqlTracks;
	private File outFolderTmp;
	private boolean isFPCMgreaterThanThreshold;
	ru.biosoft.access.core.DataElementPath resultView;
    public QCAnalysisReportGenerator(DataCollection<?> origin, String name) 
    {
    	super(origin, name, new ChIPseqQCAnalysisParameters());
    }
    
    @Override
    public Object justAnalyzeAndPut() throws RepositoryException, Exception
    {
    	outFolder = this.parameters.getPathToOutputFolder();
    	macsPeaks = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/MACS_peaks");
    	gemPeaks = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/GEM_peaks");
    	sissrsPeaks = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/SISSRS_peaks");
    	picsPeaks = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/PICS_peaks");
    	mergedPeaks = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/all_merged_regions_track");
    	mergedNoOrphsPeaks = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/merged_regions_without_orphans_track");
    	qualityMetrics = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/quality_metrics");
    	encodeQualityMetrics = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/Encode_quality_metrics");
    	aucPic = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/ROC-curves");
    	aucTable = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/AUCs");
    	outFolderTmp = TempFiles.dir( "_report" );
    	resultView = DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/view_the_result");
    	
    	qualityMetrics = DataElementPath.create( outFolder.toString() + "/quality_metrics" );
    	LinkedHashMap<String, String> tableMap = readTwoRowsTableFromFile( qualityMetrics );
    	double fpcm = Double.parseDouble(tableMap.get("FPCM"));
    	isFPCMgreaterThanThreshold = fpcm > this.parameters.getQCAnalysisParameters().getFpcmThreshold() ? true : false;
    	
    	generateReport();
    	return DataElementPath.create(this.parameters.getPathToOutputFolder().toString() + "/report.zhtml").optDataElement();
    }
    
    public void setQcAnalysisParameters(ChIPseqQCAnalysisParameters parameters) 
    {
    	this.parameters = parameters;
    }
    
    public void generateReport() throws RepositoryException, Exception
    {
    	Document doc = readTemplateHTML();
    	doc.getElementById( "encode_qc" );
    	doc.outputSettings().escapeMode( EscapeMode.extended );
        doc.outputSettings().charset( StandardCharsets.US_ASCII );
        preprocessReport( doc );
        writeReportHTML( doc , "report");
    }
    
    private Document readTemplateHTML() throws IOException 
    {
        InputStream is = QCAnalysisReportGenerator.class.getResourceAsStream( "resources/index.html" );
        String htmlStr = ApplicationUtils.readAsString( is );
        return Jsoup.parse( htmlStr );
    }
    
    private List<SqlTrack> getSqlTracks()
    {
    	List<SqlTrack> result = new ArrayList<>();
    	sqlTracks.add(mergedNoOrphsPeaks.getDataElement( SqlTrack.class ));
    	sqlTracks.add(mergedPeaks.getDataElement( SqlTrack.class ));
    	if(this.parameters.getDataType().equals(ChIPseqQCAnalysisParameters.RAW_CHIPSEQ_DATA))
    	{
    		sqlTracks.add(macsPeaks.getDataElement( SqlTrack.class ));
        	sqlTracks.add(gemPeaks.getDataElement( SqlTrack.class ));
        	sqlTracks.add(sissrsPeaks.getDataElement( SqlTrack.class ));
        	sqlTracks.add(picsPeaks.getDataElement( SqlTrack.class ));
    	} else 
    	{
    		PathToDataSet[] pathToDataSets = this.parameters.getPathsToDataSets();
    		for(PathToDataSet pathToDataSet : pathToDataSets)
    		{
    			SqlTrack track = pathToDataSet.getPathToDataSet().getDataElement(SqlTrack.class);
    			sqlTracks.add(track);
    		}
    	}
    	return result;
    }
    
    private List<ru.biosoft.access.core.DataElementPath> getAligns()
    {
    	List<ru.biosoft.access.core.DataElementPath> result = new ArrayList<>();
    	if(this.parameters.getDataType().equals(ChIPseqQCAnalysisParameters.RAW_CHIPSEQ_DATA))
    	{
    		ru.biosoft.access.core.DataElementPath filePath = DataElementPath.create(outFolder.toString() + "/alignment_exp");
    		result.add(filePath);
    		if( !this.parameters.getInputDataParameters().isCtrlTypeSelectorHidden() ) 
    		{
    			filePath = DataElementPath.create(outFolder.toString() + "/alignment_ctrl");
        		result.add(filePath);
    		}
    	}
    	return result;
    }
    private void writeReportHTML(Document doc, String name) throws RepositoryException, Exception
    {
    	writeIndexHTML( doc );
        importAsZipHTML( name );
        
    	/*File html = new File(outFolderTmp, "report.html");
        ApplicationUtils.writeString( new File(outFolderTmp, "report.html"), doc.toString() );
        HtmlFileImporter fileImporter = new HtmlFileImporter();
        fileImporter.getProperties( outFolder.getDataCollection(), html, "report.html" ).setPreserveExtension( false );
        fileImporter.doImport( outFolder.getDataCollection(), html, "report.html", null, log );
        html.delete();*/
    }

    private void writeIndexHTML(Document doc) throws IOException
    {
        ApplicationUtils.writeString( new File(outFolderTmp, "index.html"), doc.toString() );
    }

    private void importAsZipHTML(String name) throws Exception
    {
        File zip = TempFiles.file( "report.zhtml" );
        try (ZipOutputStream out = new ZipOutputStream( new FileOutputStream( zip ) ))
        {
            addDirectoryToZip( out, outFolderTmp, "" );
        }
        FileImporter fileImporter = new FileImporter();
        fileImporter.getProperties( outFolder.getDataCollection(), zip, name ).setPreserveExtension( false );
        fileImporter.doImport( outFolder.getDataCollection(), zip, name, null, log );
        zip.delete();
    }

    private void exportChart(@Nonnull ChartDataElement chart, String fileName) throws Exception
    {
        ExporterInfo exporterInfo = DataElementExporterRegistry.getExporterInfo( "Portable Network Graphics (*.png)", chart )[0];
        DataElementExporter exporter = exporterInfo.cloneExporter();
        File imgFile = new File(outFolderTmp, fileName);
        exporter.getProperties( chart, imgFile );
        exporter.doExport( chart, imgFile );
    }

    protected void addDirectoryToZip(ZipOutputStream out, File path, String prefix) throws IOException
    {
        File[] pathList = path.listFiles();
        if( pathList == null )
        {
            log.warning( "Can not zip directory '" + path.getPath() + "'" );
            return;
        }
        for( File file : pathList )
        {
            if( file.isDirectory() )
            {
                addDirectoryToZip( out, file, prefix + file.getName() + "/" );
            }
            else
            {
                ZipEntry entry = new ZipEntry( prefix + file.getName() );
                out.putNextEntry( entry );
                try( FileInputStream in = new FileInputStream( file ) )
                {
                    ApplicationUtils.copyStreamNoClose( out, in );
                }
                out.closeEntry();
            }
        }
    }
    
    private void createView(DataElementPath resultPath, List<SqlTrack> sqlTracks, List<ru.biosoft.access.core.DataElementPath> aligns) 
    {
    	ProjectAsLists result = new ProjectAsLists( resultPath.getName(), resultPath.optParentCollection() );
    	ru.biosoft.access.core.DataElementPath genomePath = TrackUtils.getTrackSequencesPath( sqlTracks.get(0) );
    	String positionStr = sqlTracks.get(0).getInfo().getProperties().getProperty( SqlTrack.DEFAULT_POSITION_PROPERTY );
    	for(SqlTrack sqlTrack : sqlTracks)
    	{
    		result.addTrack( new TrackInfo( sqlTrack )  );
    		SiteViewOptions vo = result.getViewOptions().getTrackViewOptions( sqlTrack.getCompletePath() );
            vo.setShowTitle( false );
    	}
    	for(DataElementPath align : aligns)
    	{
    		Track track = align.getDataElement( Track.class );
    		TrackInfo ti = new TrackInfo( track );
    		String title = align.getName();
    		ti.setTitle( title );
    		result.addTrack( ti );
    		BamTrackViewOptions viewOptions = (BamTrackViewOptions)result.getViewOptions().getTrackViewOptions( align );
    		viewOptions.setProfileView( true );
    	}
    	resultPath.save( result );
    }
    
    private void createView(DataElementPath resultPath, List<SqlTrack> sqlTracks) 
    {
    	ProjectAsLists result = new ProjectAsLists( resultPath.getName(), resultPath.optParentCollection() );
    	ru.biosoft.access.core.DataElementPath genomePath = TrackUtils.getTrackSequencesPath( sqlTracks.get(0) );
    	String positionStr = sqlTracks.get(0).getInfo().getProperties().getProperty( SqlTrack.DEFAULT_POSITION_PROPERTY );
    	for(SqlTrack sqlTrack : sqlTracks)
    	{
    		result.addTrack( new TrackInfo( sqlTrack )  );
    		SiteViewOptions vo = result.getViewOptions().getTrackViewOptions( sqlTrack.getCompletePath() );
            vo.setShowTitle( false );
    	}
    	
    	resultPath.save( result );
    }
    
    private void preprocessReport( Document doc ) throws Exception 
    {

    	fillAndCloneRow( doc, createInputFilesList(), "INPUT" );
    	doc.getElementById("OUTPUT_FOLDER").html( makeDirHyperLink(outFolder.toString()) );
    	fillAndCloneRow( doc, createOutputFilesList(), "OUTPUT" );
    	reportFncmAndFpcm( doc );
    	reportPeaksNum( doc );
    	if(isFPCMgreaterThanThreshold)
    	{
    		doc.getElementById( "FPCMGreaterThanTh" ).removeClass( "hidden" );
    	} else
    	{
    		doc.getElementById( "FPCMLessThanTh" ).removeClass( "hidden" );
    	}
    	if( this.parameters.getDataType().equals(ChIPseqQCAnalysisParameters.RAW_CHIPSEQ_DATA) ) 
    	{
    		doc.getElementsByClass( "encode_qc" ).removeClass( "hidden" );
    		reportEncodeMetrics( doc );
    		//createView(resultView, getSqlTracks(), getAligns());
    	} else 
    	{
    		//createView(resultView, getSqlTracks());
    	}
    	if( this.parameters.getQCAnalysisParameters().getDoAucs() ) 
    	{
    		doc.getElementsByClass( "auc" ).removeClass( "hidden" );
    		ChartDataElement chart = aucPic.getDataElement(ChartDataElement.class);
    		exportChart(chart, "ROC-curves.png");
    		reportAucCurves( doc );
    	}
    	
    }
    
    private void reportFncmAndFpcm( Document doc ) 
    {
    	qualityMetrics = DataElementPath.create( outFolder.toString() + "/quality_metrics" );
    	LinkedHashMap<String, String> tableMap = readTwoRowsTableFromFile( qualityMetrics );
    	tableMap.remove( "Estimated_number_of_sites" );
    	fillAndAddColumns( doc, tableMap, "FPCM_and_FNCM" );
    	fillInFCMValues(doc, tableMap);
    }
    
    private void fillInFCMValues(Document doc, LinkedHashMap<String, String> tableMap) {
    	String fpcm = tableMap.get("FPCM");
    	
    	String fpcmThresh = Double.toString(this.parameters.getQCAnalysisParameters().getFpcmThreshold());
    	doc.getElementById("FPCM").text(fpcm);
    	doc.getElementById("FPCM_Th").text(fpcmThresh);
    	tableMap.remove("FPCM");
    	String datasetWithMaxFncm = "";
    	double fncm = 0.0;
    	List<String> fncms = new ArrayList<>();
    	for(Entry<String, String> entry : tableMap.entrySet())
    	{
    		fncms.add(entry.getValue());
    		if(fncm < Double.parseDouble(entry.getValue()))
    		{
    			fncm = Double.parseDouble(entry.getValue());
    			datasetWithMaxFncm = entry.getKey();
    		}
    	}
    	doc.getElementById("Dataset_name").text(datasetWithMaxFncm);
    	doc.getElementById("FNCM_max").text(Double.toString(fncm));
    	doc.getElementById("FNCMs").text(String.join(", ", fncms));
    	
	}

	private void reportPeaksNum( Document doc ) 
    {
    	qualityMetrics = DataElementPath.create( outFolder.toString() + "/quality_metrics" );
    	LinkedHashMap<String, String> tableMap = readTwoRowsTableFromFile( qualityMetrics );
    	LinkedHashMap<String, String> peaksNumMap = new LinkedHashMap<>();
    	peaksNumMap.put( "Estimated number of peaks", tableMap.get( "Estimated_number_of_sites" ));
    	
    	Track track = mergedPeaks.getDataElement(Track.class);
    	String numMerge = Integer.toString(track.getAllSites().getSize());
    	track = mergedNoOrphsPeaks.getDataElement(Track.class);
    	String numMergeNoOrphs = Integer.toString(track.getAllSites().getSize());
    	peaksNumMap.put( mergedPeaks.getName(), numMerge);
    	peaksNumMap.put( mergedNoOrphsPeaks.getName(), numMergeNoOrphs);
    	
    	if(this.parameters.getDataType().equals(ChIPseqQCAnalysisParameters.RAW_CHIPSEQ_DATA)) 
    	{
    		track = macsPeaks.getDataElement(Track.class);
    		String numMacs = Integer.toString(track.getAllSites().getSize());
    		track = gemPeaks.getDataElement(Track.class);
    		String numGem = Integer.toString(track.getAllSites().getSize());
    		track = sissrsPeaks.getDataElement(Track.class);
    		String numSissrs = Integer.toString(track.getAllSites().getSize());
    		track = picsPeaks.getDataElement(Track.class);
    		String numPics = Integer.toString(track.getAllSites().getSize());
    		peaksNumMap.put( macsPeaks.getName(), numMacs );
    		peaksNumMap.put( gemPeaks.getName(), numGem );
    		peaksNumMap.put( sissrsPeaks.getName(), numSissrs );
    		peaksNumMap.put( picsPeaks.getName(), numPics );
    	} 
    	else
    	{
    		PathToDataSet[] pathToDataSets = this.parameters.getPathsToDataSets();
    		for(PathToDataSet pathToDataSet : pathToDataSets)
    		{
    			track = pathToDataSet.getPathToDataSet().getDataElement(Track.class);
    			String peaksNum = Integer.toString(track.getAllSites().getSize());
    			peaksNumMap.put( pathToDataSet.getPathToDataSet().getName(), peaksNum );
    		}
    	}
    	fillAndAddColumns(doc, peaksNumMap, "N_PEAKS" );
    }
    
    private void fillAndAddColumns( Document doc, LinkedHashMap<String, String> tableMap, String elementIdCore) 
    {
    	int i = 0;
    	for(Entry<String, String> entry : tableMap.entrySet()) 
    	{
    		Element td;
    		if(i < 4)
    		{
    			td = doc.getElementById(elementIdCore + "_header").getElementsByTag("td").get(i);
    			td.getElementsByTag("span").get(0).text(entry.getKey());
    			td = doc.getElementById(elementIdCore + "_content").getElementsByTag("td").get(i);
    			td.getElementsByTag("span").get(0).text(entry.getValue());
    		}
    		else
    		{
    			Element cellHeader = doc.getElementById(elementIdCore + "_header").getElementsByTag("td").get(3).clone();
    			Element cellContent = doc.getElementById(elementIdCore + "_content").getElementsByTag("td").get(3).clone();
    			cellHeader.getElementsByTag("span").get(0).text(entry.getKey());
    			cellContent.getElementsByTag("span").get(0).text(entry.getValue());
    			cellHeader.appendTo(doc.getElementById(elementIdCore + "_header"));
    			cellContent.appendTo(doc.getElementById(elementIdCore + "_content"));
    		}
    		i++;
    	}
    }
    
    private void fillAndCloneRow( Document doc, List<String[]> rows, String tableId ) 
    {
    	for(int i = 0; i < rows.size(); i++) {
    		Element cells = doc.getElementById(tableId).getElementsByTag("tr").get(1).clone();
    		Element td;
    		if(i == 0)
    		{
    			for( int j = 0; j < rows.get(i).length; j++)
    			{
    				td = doc.getElementById(tableId).getElementsByTag("tr").get(1).getElementsByTag("td").get(j);
    				td.getElementsByTag("span").get(0).text("");
    				td.getElementsByTag("span").get(0).html(rows.get(i)[j]);
    			}
    		}
    		else
    		{
    			for( int j = 0; j < rows.get(i).length; j++)
    			{
    				cells.getElementsByTag("td").get(j).getElementsByTag("span").get(0).text("");
    				cells.getElementsByTag("td").get(j).getElementsByTag("span").get(0).html(rows.get(i)[j]);
    			}
    			cells.appendTo(doc.getElementById(tableId));
    		}
    	}
    }
    
    private List<String[]> createOutputFilesList() 
    {
    	List<String[]> result = new ArrayList<>();
    	String filePath;
    	if( this.parameters.getDataType().equals(ChIPseqQCAnalysisParameters.RAW_CHIPSEQ_DATA) ) 
    	{
    		filePath = outFolder.toString() + "/alignment_exp";
    		result.add(new String[] {makeFileHyperLink(filePath), getFileSize(filePath, "bam"), "Experiment raw data alignment"});
    		if( !this.parameters.getInputDataParameters().isCtrlTypeSelectorHidden() ) 
    		{
    			filePath = outFolder.toString() + "/alignment_ctrl";
    			result.add(new String[] {makeFileHyperLink(filePath), getFileSize(filePath, "bam"), "Input control raw data alignment"});
    		}
    		filePath = macsPeaks.toString();
    		result.add(new String[] {makeFileHyperLink(filePath), "~ 1 Mb", "MACS peaks"});
    		filePath = gemPeaks.toString();
    		result.add(new String[] {makeFileHyperLink(filePath), "~ 1 Mb", "GEM peaks"});
    		filePath = sissrsPeaks.toString();
    		result.add(new String[] {makeFileHyperLink(filePath), "~ 1 Mb", "SISSRS peaks"});
    		filePath = picsPeaks.toString();
    		result.add(new String[] {makeFileHyperLink(filePath), "~ 1 Mb", "PICS peaks"});
    		filePath = encodeQualityMetrics.toString();
    		result.add(new String[] {makeFileHyperLink(filePath), "~ 1 Mb", "Encode quality metrics"});
    	}
    	filePath = mergedPeaks.toString();
    	result.add(new String[] {makeFileHyperLink(filePath), "~ 1 Mb", "Merged peaks"});
    	filePath = mergedNoOrphsPeaks.toString();
    	result.add(new String[] {makeFileHyperLink(filePath), "~ 1 Mb", "Merged peaks without orphans"});
    	filePath = qualityMetrics.toString();
    	result.add(new String[] {makeFileHyperLink(filePath), "< 1 Mb", "FPCM and FNCM values and estimated site number"});
    	
    	if( this.parameters.getQCAnalysisParameters().getDoAucs()) 
    	{
    		filePath = aucTable.toString();
    		result.add(new String[] {makeFileHyperLink(filePath), "< 1 Mb", "AUC values"});
    		filePath = aucPic.toString();
    		result.add(new String[] {makeFileHyperLink(filePath), "< 1 Mb", "Chart with ROC curves"});
    	}
    	//result.add(new String[] {makeFileHyperLink(resultView.toString()), " - ", "result view"});
    	return result;
    }
    
    private List<String[]> createInputFilesList() 
    {
    	List<String[]> result = new ArrayList<>();
    	InputDataParameters inputParameters = this.parameters.getInputDataParameters();
    	String filePath;
    	if( this.parameters.getDataType().equals(ChIPseqQCAnalysisParameters.RAW_CHIPSEQ_DATA) ) 
    	{

    		if( inputParameters.getExpTypeSelector().equals(InputDataParameters.EXP_TYPE_SINGLE_END)) 
    		{
    			filePath = this.parameters.getInputDataParameters().getPathToInputExp1().toString();
    			result.add(new String[] {makeFileHyperLink(filePath), getFileSize(filePath, "fastq"), "Experiment raw data"});
    		} 
    		else 
    		{
    			filePath = this.parameters.getInputDataParameters().getPathToInputExp1().toString();
    			result.add(new String[] {makeFileHyperLink(filePath), getFileSize(filePath, "fastq"), "Experiment raw data (mate1)"});
    			filePath = this.parameters.getInputDataParameters().getPathToInputExp2().toString();
    			result.add(new String[] {makeFileHyperLink(filePath), getFileSize(filePath, "fastq"), "Experiment raw data (mate2)"});
    		}
    		if( !inputParameters.isCtrlTypeSelectorHidden() ) 
    		{
    			if( inputParameters.getCtrlTypeSelector().equals(InputDataParameters.EXP_TYPE_SINGLE_END)) 
    			{
    				filePath = this.parameters.getInputDataParameters().getPathToInputCtrl1().toString();
    				result.add(new String[] {makeFileHyperLink(filePath), getFileSize(filePath, "fastq"), "Input control raw data"});
    			} 
    			else 
    			{
    				filePath = this.parameters.getInputDataParameters().getPathToInputCtrl1().toString();
    				result.add(new String[] {makeFileHyperLink(filePath), getFileSize(filePath, "fastq"), "Input control raw data (mate1)"});
    				filePath = this.parameters.getInputDataParameters().getPathToInputCtrl2().toString();
    				result.add(new String[] {makeFileHyperLink(filePath), getFileSize(filePath, "fastq"), "Input control raw data (mate2)"});
    			}

    		}
    	} else 
    	{
    		for(PathToDataSet pathToDataSet : this.parameters.getPathsToDataSets()) 
    		{
    			filePath = pathToDataSet.getPathToDataSet().toString();
    			result.add(new String[] {makeFileHyperLink(filePath), " - ", "BED-file"});
    		}
    	}
    	
    	return result;
    }
    
    private void reportEncodeMetrics( Document doc ) 
    {
    	TableDataCollection table = encodeQualityMetrics.optDataElement(TableDataCollection.class);
    	RowDataElement row = table.getAt(0);
		
    	double nrf = Double.parseDouble(row.getValues()[0].toString());
    	double pbc1 = Double.parseDouble(row.getValues()[1].toString());
    	double pbc2 = Double.parseDouble(row.getValues()[2].toString());
    	double nsc = Double.parseDouble(row.getValues()[3].toString());
    	double rsc = Double.parseDouble(row.getValues()[4].toString());
    	double fripMacs = Double.parseDouble(row.getValues()[5].toString());
    	double fripGem = Double.parseDouble(row.getValues()[6].toString());
    	double fripSissrs = Double.parseDouble(row.getValues()[7].toString());
    	double fripPics = Double.parseDouble(row.getValues()[8].toString());
    	
    	NumberFormat formatter = new DecimalFormat("#0.0000");
    	
    	doc.getElementById("NRF").text(formatter.format(nrf));
    	doc.getElementById("PBC1").text(formatter.format(pbc1));
    	doc.getElementById("PBC2").text(formatter.format(pbc2));
    	doc.getElementById("NSC").text(formatter.format(nsc));
    	doc.getElementById("RSC").text(formatter.format(rsc));
    	doc.getElementById("FRIP_MACS").text(formatter.format(fripMacs));
    	doc.getElementById("FRIP_GEM").text(formatter.format(fripGem));
    	doc.getElementById("FRIP_SISSRS").text(formatter.format(fripSissrs));
    	doc.getElementById("FRIP_PICS").text(formatter.format(fripPics));
    }
    
    private void reportAucCurves( Document doc ) 
    {
    	TableDataCollection table = aucTable.optDataElement(TableDataCollection.class);
    	double withoutOrphans = Double.parseDouble(table.getAt(0).getValues()[0].toString());
    	double whole = Double.parseDouble(table.getAt(1).getValues()[0].toString());
    	withoutOrphans = Math.abs(withoutOrphans);
    	whole = Math.abs(whole);
    	NumberFormat formatter = new DecimalFormat("#0.00000");
    	
    	doc.getElementById("AUC_NO_ORPHS").text(formatter.format(withoutOrphans).toString());
    	doc.getElementById("AUC_WHOLE").text(formatter.format(whole).toString());
    	
    	doc.getElementById("AUC_matrix").text(this.parameters.getQCAnalysisParameters().getAUCParameters().getSiteModelName());
    }
    
    private LinkedHashMap<String, String> readTwoRowsTableFromFile(DataElementPath pathToTable) 
    {
    	
    	try {
    		LinkedHashMap<String, String> result = new LinkedHashMap<>();
    		TableDataCollection table = pathToTable.optDataElement(TableDataCollection.class);
    		RowDataElement row = table.getAt(0);
    		ColumnModel columnModel = table.getColumnModel();
			int size = columnModel.getColumnCount();
    		for(int i=0; i< size; i++ ) {
    			String cName = columnModel.getColumn(i).getName();
    			if(row.getValueAsString(cName).contains("."))
    			{
    				Double value = Double.parseDouble(row.getValueAsString(cName));
    				NumberFormat formatter = new DecimalFormat("#0.00000");
    				result.put(cName, formatter.format(value));
    			}
    			else 
    			{
    				result.put(cName, row.getValueAsString(cName));
    			}
    		}
    	
    		return result;
    	} 
    	catch(Exception e) {
    		return null;
    	}
    }
   /* private String fillInTable(LinkedHashMap<String, String> map) 
    {
    	StringBuilder header = new StringBuilder("<tr>");
    	StringBuilder values = new StringBuilder("<tr>");
    	for(Map.Entry<String,String> entry : map.entrySet()) 
    	{
    		header.append("<th><td>").append(entry.getKey()).append("</td></th>");
    		values.append("<th><td>").append(entry.getValue()).append("</td></th>");
    	}
    	header.append("</tr>");
    	values.append("</tr>");
    	return header.toString() + values.toString();
    }
    
    private String fillInTable(List<String[]> list) 
    {
    	StringBuilder table = new StringBuilder("<tr>");
    	for(String[] row : list) 
    	{
    		for(String cell : row) 
    		{
    			table.append("<th><td>" + cell + "</td></tr>");
    		}
    		table.append("</tr>");
    	}
    	return table.toString();
    }*/
    
    private String getFileSize(File file) 
    {
    	if(file.getName().equals("exception"))
    		return " - ";
    	NumberFormat formatter = new DecimalFormat("#0.00");
    	float size = (float) file.length() / (1024 * 1024);
    	if(size >= 1000)
    		return formatter.format( (double) file.length() / (1024 * 1024 * 1024) ) + " Gb";
    	else if(size >= 1)
    		return formatter.format( (double) file.length() / (1024 * 1024) ) + " Mb";
    	else 
    		return formatter.format( (double) file.length() / (1024) ) + " Kb";
    }

    private String makeFileHyperLink(String filePath) 
    {
    	File file = new File(filePath);
    	if(file.isDirectory()) 
    	{
    		return "<a href=\"/bioumlweb/#de=" + filePath + "\" target=\"_parent\">" + filePath + "</a>";
    	}
    	return "<a href=\"/bioumlweb/#de=" + filePath + "\" target=\"_parent\">" + file.getName() + "</a>";
    }
    
    private String makeDirHyperLink(String dirPath) 
    { 	
    	return "<a href=\"/bioumlweb/#de=" + dirPath + "\" target=\"_parent\">" + dirPath + "</a>";
    }
    
    private String getFileSize(String filePath, String fileType ) 
    {
    	switch(fileType) {
    	case "fastq":
    		return getFileSize(DataElementPath.create(filePath).getDataElement(FileDataElement.class).getFile());
    	case "bam":
    		return getFileSize(DataElementPath.create(filePath).getDataElement(BAMTrack.class).getBAMFile());
    	default:
    		return getFileSize(new File("exception"));
    	}
    }
    
    
}
