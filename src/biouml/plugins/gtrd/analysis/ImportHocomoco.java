package biouml.plugins.gtrd.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;
import gnu.trove.list.array.TDoubleArrayList;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.PValueCutoff;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.bsa.analysis.CustomWeightsModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.bsa.transformer.SiteModelTransformer;
import ru.biosoft.bsa.transformer.TranscriptionFactorTransformer;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
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
        TableDataCollection uniprotTable = parameters.getUniprotMatchingTable().getDataElement( TableDataCollection.class );
        if(!parameters.getResultingFactorCollection().exists())
            TranscriptionFactorTransformer.createCollection( parameters.getResultingFactorCollection() );
        DataCollection<TranscriptionFactor> factors = parameters.getResultingFactorCollection().getDataCollection( TranscriptionFactor.class );
        for(String uniprotName : uniprotTable.getNameList())
        {
            String uniprotId = (String)uniprotTable.get( uniprotName ).getValues()[0];
            String speciesName = null;
            if(uniprotName.endsWith( "_HUMAN" ))
                speciesName = "Homo sapiens";
            else if(uniprotName.endsWith( "_MOUSE" ))
                speciesName = "Mus musculus";
            else if(uniprotName.endsWith( "_RAT" ))
                speciesName = "Rattus norvegicus";
            else
                throw new Exception("Unknown specie for " + uniprotName);
            TranscriptionFactor tf = new TranscriptionFactor( uniprotId, factors, uniprotName, ReferenceTypeRegistry.getReferenceType( UniprotProteinTableType.class ), speciesName );
            factors.put( tf );
        }
        
        
        if(!parameters.getResultingMatrixCollection().exists())
            WeightMatrixCollection.createMatrixLibrary( parameters.getResultingMatrixCollection(), log );
        DataCollection<FrequencyMatrix> library = parameters.getResultingMatrixCollection().getDataCollection( FrequencyMatrix.class );
        
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
                int idx = id.indexOf( '.' );
                if(idx < 0)
                    throw new Exception("Illegal matrix id: " + id);
                String uniprotName = id.substring( 0, idx );
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


                
                if( !uniprotTable.contains( uniprotName ) )
                {
                    log.warning( "No matching for " + uniprotName );
                    continue;
                }
                String uniprotId = (String)uniprotTable.get( uniprotName ).getValues()[0];
                TranscriptionFactor factor = factors.get( uniprotId );
                BindingElement bindingElement = new BindingElement( uniprotName, Collections.singletonList( factor ) );
                
                FrequencyMatrix pcm = new FrequencyMatrix( library, id, Nucleotide15LetterAlphabet.getInstance(), bindingElement, counts.toArray( new double[counts.size()][] ), false );
                library.put( pcm );
            }
        }
        parameters.getResultingMatrixCollection().save( library );
        
        TableDataCollection thresholdsTable = parameters.getThresholdsTable().getDataElement( TableDataCollection.class );
        
        
        DataElementPath profilePath = parameters.getResultingSiteModelCollection();
        if(profilePath.exists())
            profilePath.remove();
        
        DataCollection<SiteModel> profile = SiteModelTransformer.createCollection(profilePath);

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
                FrequencyMatrix pcm = library.get( id );
                if(pcm == null)
                    throw new Exception("PCM for " + id + " not found");
                
                Map<String, String> thresholdTemplates = new HashMap<>();
                ColumnModel columnModel = thresholdsTable.getColumnModel();
                for(int i = 0; i < columnModel.getColumnCount(); i++)
                {
                    String name = columnModel.getColumn( i ).getName();
                    RowDataElement row = thresholdsTable.get( id );
                    if(row == null)
                        throw new Exception("No thresholds for " + id);
                    String value = row.getValueAsString( name );
                    thresholdTemplates.put( name, value );
                }
                SiteModel pwm = new CustomWeightsModel( id, profile, pcm, 0, weights.toArray( new double[weights.size()][] ) );
                pwm.setThresholdTemplates( thresholdTemplates );
                pwm.setThresholdTemplate( columnModel.getColumn( 0 ).getName() );
                
                DataElementPath thresholdsFolder = parameters.getThresholdsFolder();
                if(thresholdsFolder != null)
                {
                    File file = thresholdsFolder.getChildPath( id + ".thr" ).getDataElement( FileDataElement.class ).getFile();
                    PValueCutoff pvalueCutoff = loadThresholds(file);
                    pwm.setPValueCutoff( pvalueCutoff );
                }
                
                profile.put( pwm );
            }
        }
        
        profilePath.save( profile );
        
        return new Object[] {library, profile};
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
        DataElementPath thresholdsTable;
        DataElementPath uniprotMatchingTable;
        DataElementPath resultingMatrixCollection;
        DataElementPath resultingSiteModelCollection;
        DataElementPath resultingFactorCollection;
        DataElementPath thresholdsFolder;
        
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

        @PropertyName("Thresholds table")
        @PropertyDescription("Table with model thresholds")
        public DataElementPath getThresholdsTable()
        {
            return thresholdsTable;
        }
        public void setThresholdsTable(DataElementPath thresholdsTable)
        {
            DataElementPath oldValue = this.thresholdsTable;
            this.thresholdsTable = thresholdsTable;
            firePropertyChange( "thresholdsTable", oldValue, thresholdsTable );
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
        
        @PropertyName("Uniprot matching table")
        @PropertyDescription("Table witih ID corresponding to uniprot name and first columnt corresponding to uniprot id")
        public DataElementPath getUniprotMatchingTable()
        {
            return uniprotMatchingTable;
        }
        public void setUniprotMatchingTable(DataElementPath uniprotMatchingTable)
        {
            DataElementPath oldValue = this.uniprotMatchingTable;
            this.uniprotMatchingTable = uniprotMatchingTable;
            firePropertyChange( "uniprotMatchingTable", oldValue, uniprotMatchingTable );
        }
        
        @PropertyName("Resulting matrix collection")
        @PropertyDescription("Path to resulting matrix collection")
        public DataElementPath getResultingMatrixCollection()
        {
            return resultingMatrixCollection;
        }
        
        public void setResultingMatrixCollection(DataElementPath resultingMatrixCollection)
        {
            DataElementPath oldValue = this.resultingMatrixCollection;
            this.resultingMatrixCollection = resultingMatrixCollection;
            firePropertyChange( "resultingMatrixCollection", oldValue, resultingMatrixCollection );
        }
        
        @PropertyName("Resulitng site model collection")
        @PropertyDescription("Path to resulting site model collection")
        public DataElementPath getResultingSiteModelCollection()
        {
            return resultingSiteModelCollection;
        }
        public void setResultingSiteModelCollection(DataElementPath resultingSiteModelCollection)
        {
            DataElementPath oldValue = this.resultingSiteModelCollection;
            this.resultingSiteModelCollection = resultingSiteModelCollection;
            firePropertyChange( "resultingSiteModelCollection", oldValue, resultingSiteModelCollection );
        }
        
        @PropertyName("Resulting factor collection")
        public DataElementPath getResultingFactorCollection()
        {
            return resultingFactorCollection;
        }
        public void setResultingFactorCollection(DataElementPath resultingFactorCollection)
        {
            DataElementPath oldValue = this.resultingFactorCollection;
            this.resultingFactorCollection = resultingFactorCollection;
            firePropertyChange( "resultingFactorCollection", oldValue, resultingFactorCollection );
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
            property( "thresholdsTable" ).inputElement( TableDataCollection.class ).add();
            property( "thresholdsFolder" ).inputElement( FolderCollection.class ).add();
            property( "uniprotMatchingTable" ).inputElement( TableDataCollection.class ).add();
            property( "resultingMatrixCollection" ).outputElement( WeightMatrixCollection.class ).add();
            property( "resultingSiteModelCollection" ).outputElement( SiteModelCollection.class ).add();
            property( "resultingFactorCollection" ).outputElement( TransformedDataCollection.class ).add();
        }
    }
}
