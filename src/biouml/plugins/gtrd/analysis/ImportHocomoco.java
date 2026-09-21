package biouml.plugins.gtrd.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;
import gnu.trove.list.array.TDoubleArrayList;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.PValueCutoff;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.bsa.analysis.CustomWeightsModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.bsa.transformer.SiteModelTransformer;
import ru.biosoft.bsa.transformer.TranscriptionFactorTransformer;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ImportHocomoco extends AnalysisMethodSupport<ImportHocomoco.Parameters>
{
    public ImportHocomoco(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {

    	if(!parameters.getOutPath().exists())
    	{
    		DataCollectionUtils.createFoldersForPath(parameters.getOutPath());
    		DataCollectionUtils.createSubCollection(parameters.getOutPath());
    	}
    	DataElementPath factorsPath = parameters.getOutPath().getChildPath("factors");
    	
        if(!factorsPath.exists())
            TranscriptionFactorTransformer.createCollection( factorsPath );
        DataCollection<TranscriptionFactor> factors = factorsPath.getDataCollection( TranscriptionFactor.class );
        
        Map<String,List<TranscriptionFactor>> matrixNameToFactor = new HashMap<>();
        Map<String,String> organisms = new HashMap<>();//Homo sapiens->HUMAN

		FileDataElement jsonMetadata = parameters.getJsonMetadataPath().getDataElement(FileDataElement.class);
		
		try (BufferedReader reader = new BufferedReader(new FileReader(jsonMetadata.getFile()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				JsonObject json = Json.parse(line).asObject();
				String matrixName = json.get("name").asString();
				JsonObject speciesJson = json.get("masterlist_info").asObject().get("species").asObject();
				for (String speciesName : speciesJson.names()) {
					String latinName = getLatinName(speciesName);
					organisms.put(latinName, speciesName);
					JsonObject organismInfo = speciesJson.get(speciesName).asObject();
					String uniprotId = organismInfo.getString("uniprot_ac", null);
					String uniprotName = organismInfo.getString("uniprot_id", null);
					if (uniprotId == null || uniprotName == null)
						throw new IllegalArgumentException(line);
					TranscriptionFactor tf = new TranscriptionFactor(uniprotId, factors, uniprotName,
							ReferenceTypeRegistry.getReferenceType(UniprotProteinTableType.class), latinName);
					factors.put(tf);
					matrixNameToFactor.computeIfAbsent(matrixName, k -> new ArrayList<>()).add(tf);
				}
			}
		}        
        Map<String, DataCollection<FrequencyMatrix>> matrixLibForOrganism = new HashMap<>();
        for(String latinName : organisms.keySet())
        {
        	String commonName = organisms.get(latinName);
        	DataElementPath matrixPath = parameters.getOutPath().getChildPath("PCM_" + commonName);
        	if(matrixPath.exists())
        		matrixPath.remove();
        	WeightMatrixCollection.createMatrixLibrary( matrixPath, log );
        	DataCollection<FrequencyMatrix> library = matrixPath.getDataCollection(FrequencyMatrix.class);
        	matrixLibForOrganism.put(latinName, library);
        }
        
        File inputFile = parameters.getPcmFlatFile().getDataElement( FileDataElement.class ).getFile();
        
        try(BufferedReader reader = ApplicationUtils.asciiReader( inputFile ))
        {
            String line = reader.readLine();
            while(line != null)
            {
                String header = line;
                if(!header.startsWith( ">" ))
                    throw new Exception("Illegal line: " + line);
                String id = header.substring( 1 );
                
                List<double[]> counts = new ArrayList<>();
                while((line = reader.readLine()) != null && !line.startsWith( ">" ))
                {
                    String[] fields = line.split( "\t" );
                    if(fields.length != 4)
                        throw new Exception("Illegal line: " + line);
                    double[] row = new double[4];
                    for(int i = 0; i < 4; i++)
                        row[i] = Double.parseDouble( fields[i] );
                    counts.add( row );
                }

                List<TranscriptionFactor> tfList = matrixNameToFactor.get(id);
                
                if( tfList == null )
                {
                    log.warning( "No uniprot matching for " + id );
                    continue;
                }
                
                for(TranscriptionFactor factor : tfList)
                {
                	String uniprotId = factor.getName();
                	String uniprotName = factor.getDisplayName();
                	BindingElement bindingElement = new BindingElement( uniprotName, Collections.singletonList( factor ) );
                	String latinName = factor.getSpeciesName();
                	DataCollection<FrequencyMatrix> library = matrixLibForOrganism.get(latinName);
                	FrequencyMatrix pcm = new FrequencyMatrix( library, id, Nucleotide15LetterAlphabet.getInstance(), bindingElement, counts.toArray( new double[counts.size()][] ), false );
                    library.put( pcm );
                }
               
            }
        }
      
        
        Map<String, DataCollection<SiteModel>> profileByOrganism = new HashMap<>();
        for(String latinName : organisms.keySet())
        {
        	String commonName = organisms.get(latinName);
        	DataElementPath profilePath = parameters.getOutPath().getChildPath("PWM_"+commonName);
            if(profilePath.exists())
                profilePath.remove();
            DataCollection<SiteModel> profile = SiteModelTransformer.createCollection(profilePath);
            profileByOrganism.put(latinName, profile);
            
        }
        
        inputFile = parameters.getPwmFlatFile().getDataElement(FileDataElement.class).getFile();
        try(BufferedReader reader = ApplicationUtils.asciiReader( inputFile ))
        {
            String line = reader.readLine();
            while(line != null)
            {
                String header = line;
                if(!header.startsWith( ">" ))
                    throw new Exception("Illegal line: " + line);
                String id = header.substring( 1 );
                int idx = id.indexOf( '.' );
                if(idx < 0)
                    throw new Exception("Illegal matrix id: " + id);
                List<double[]> weights = new ArrayList<>();
                while((line = reader.readLine()) != null && !line.startsWith( ">" ))
                {
                    String[] fields = line.split( "\t" );
                    if(fields.length != 4)
                        throw new Exception("Illegal line: " + line);
                    double[] row = new double[4];
                    for(int i = 0; i < 4; i++)
                        row[i] = Double.parseDouble( fields[i] );
                    weights.add( row );
                }
                
                DataElementPath thresholdsFolder = parameters.getThresholdsFolder();
                File file = thresholdsFolder.getChildPath( id + ".thr" ).getDataElement( FileDataElement.class ).getFile();
                PValueCutoff pvalueCutoff = loadThresholds(file);
                
                Map<String, String> thresholdTemplates = new LinkedHashMap<>();
				String[] predefinedPvals = { "0.001", "0.0005", "0.0001" };
				for (String pval : predefinedPvals) {
					double cutoff = pvalueCutoff.getCutoff(Double.parseDouble(pval));
					thresholdTemplates.put(pval, String.valueOf(cutoff));
				}
                
                
                List<TranscriptionFactor> tfList = matrixNameToFactor.get(id);
                
                if( tfList == null )
                {
                    log.warning( "No uniprot matching for " + id );
                    continue;
                }
                
                for(TranscriptionFactor factor : tfList)
                {
                	String latinName = factor.getSpeciesName();
                	
                	DataCollection<FrequencyMatrix> library = matrixLibForOrganism.get(latinName);
                	DataCollection<SiteModel> profile = profileByOrganism.get(latinName);
                	
                	FrequencyMatrix pcm = library.get( id );
                    if(pcm == null)
                        throw new Exception("PCM for " + id + " not found");
                    
                    SiteModel pwm = new CustomWeightsModel( id, profile, pcm, 0, weights.toArray( new double[weights.size()][] ) );
                    pwm.setPValueCutoff( pvalueCutoff );
                    pwm.setThresholdTemplates( thresholdTemplates );
                    pwm.setThresholdTemplate( thresholdTemplates.keySet().iterator().next() );
                    profile.put( pwm );
                }
              
            }
        }
        
        for(DataCollection<FrequencyMatrix> lib : matrixLibForOrganism.values())
        	lib.getCompletePath().save(lib);
        
        for(DataCollection<SiteModel> profile : profileByOrganism.values())
        	profile.getCompletePath().save(profile);
        
        
        return new Object[] {parameters.getOutPath().getDataCollection()};
    }

    private String getLatinName(String speciesName) {
    	if(speciesName.equals("HUMAN"))
    		return "Homo sapiens";
    	if(speciesName.equals("MOUSE"))
    		return "Mus musculus";
    	throw new IllegalArgumentException(speciesName);
    }

	private PValueCutoff loadThresholds(File file) throws IOException
    {
        TDoubleArrayList cutoffs = new TDoubleArrayList();
        TDoubleArrayList pvalues = new TDoubleArrayList();
        try(BufferedReader reader = new BufferedReader(new FileReader( file )))
        {
            String line;
            
            while( (line = reader.readLine()) != null)
            {
                String[] parts = line.split( "\t", 2 );
                double cutoff = Double.parseDouble( parts[0] );
                double pval = Double.parseDouble( parts[1] );
                cutoffs.add( cutoff );
                pvalues.add( pval );
            }
        }
        cutoffs.reverse();
        pvalues.reverse();
        return new PValueCutoff( cutoffs.toArray(), pvalues.toArray() );
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        DataElementPath pcmFlatFile;
        DataElementPath pwmFlatFile;
        DataElementPath thresholdsFolder;
        DataElementPath jsonMetadataPath;
        DataElementPath outPath;
        
        @PropertyName("PCM flat file")
        @PropertyDescription("Position count matrix flat file")
        public DataElementPath getPcmFlatFile()
        {
            return pcmFlatFile;
        }
        public void setPcmFlatFile(DataElementPath pcmFlatFile)
        {
            DataElementPath oldValue = this.pcmFlatFile;
            this.pcmFlatFile = pcmFlatFile;
            firePropertyChange( "pcmFlatFile", oldValue, pcmFlatFile );
        }
        
        @PropertyName("PWM flat file")
        @PropertyDescription("Position weight matrix flat file")
        public DataElementPath getPwmFlatFile()
        {
            return pwmFlatFile;
        }
        public void setPwmFlatFile(DataElementPath pwmFlatFile)
        {
            DataElementPath oldValue = this.pwmFlatFile;
            this.pwmFlatFile = pwmFlatFile;
            firePropertyChange( "pwmFlatFile", oldValue, pwmFlatFile );
        }

       
        @PropertyName("Thresholds folder")
        public DataElementPath getThresholdsFolder()
        {
            return thresholdsFolder;
        }
        public void setThresholdsFolder(DataElementPath thresholdsFolder)
        {
            Object oldValue = this.thresholdsFolder;
            this.thresholdsFolder = thresholdsFolder;
            firePropertyChange( "thresholdsFolder", oldValue, thresholdsFolder );
        }
        
        
        @PropertyName("Json metadata")
        public DataElementPath getJsonMetadataPath() {
			return jsonMetadataPath;
		}
		public void setJsonMetadataPath(DataElementPath jsonMetadataPath) {
		    DataElementPath oldValue = this.jsonMetadataPath;
		    this.jsonMetadataPath = jsonMetadataPath;
            firePropertyChange( "jsonMetadataPath", oldValue, jsonMetadataPath );
		}
		public DataElementPath getOutPath() {
			return outPath;
		}
		public void setOutPath(DataElementPath outPath) {
			DataElementPath oldValue = this.outPath;
			this.outPath = outPath;
            firePropertyChange( "outPath", oldValue, outPath );
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
            property( "pcmFlatFile" ).inputElement( FileDataElement.class ).add();
            property( "pwmFlatFile" ).inputElement( FileDataElement.class ).add();
            property( "thresholdsFolder" ).inputElement( FolderCollection.class ).add();
            property( "jsonMetadataPath" ).inputElement(FileDataElement.class).add(); 
            property( "outPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
