package ru.biosoft.bsa.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class DoubleEncodeSOLiD extends AnalysisMethodSupport<DoubleEncodeSOLiD.Parameters>
{
    public DoubleEncodeSOLiD(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath path = parameters.getOutputFastqPath();
        DataCollection<? extends DataElement> parent = path.optParentCollection();
        String name = path.getName();
        File file = DataCollectionUtils.getChildFile(parent, name);
        FileDataElement result = new FileDataElement(name, parent, file);
        
        File inFile = parameters.getInputFastqPath().getDataElement( FileDataElement.class ).getFile();
        try ( BufferedReader reader = new BufferedReader( new FileReader( inFile ) );
              Writer writer = new BufferedWriter(new FileWriter( file )) )
        {
            String line;
            while((line = reader.readLine()) != null)
            {
                if(line.startsWith( "#" ))
                    continue;
                String h1 = line;
                String seq = reader.readLine();
                if(seq == null)
                    break;
                String h2 = reader.readLine();
                if(h2 == null)
                    break;
                String qual = reader.readLine();
                if(qual == null)
                    break;
                writer.write( h1 );
                writer.write( '\n' );
                writer.write( doubleEncode(seq.substring( 2 )) );
                writer.write( '\n' );
                writer.write( h2 );
                writer.write( '\n' );
                writer.write( qual.substring( 2 ) );
                writer.write( '\n' );
            }
        }
        path.save( result );
        return result;
    }

    private char[] doubleEncode(String seq)
    {
        char[] result = new char[seq.length()];
        for(int i = 0; i < seq.length(); i++)
        {
            char c = seq.charAt( i );
            char e;
            switch(c)
            {
                case '0': e = 'A'; break;
                case '1': e = 'C'; break;
                case '2': e = 'G'; break;
                case '3': e = 'T'; break;
                case '.': e = 'N'; break;
                default:
                    throw new RuntimeException( "Invalid char '" + c + "' in color sequence" );
            }
            result[i] = e;
        }
        return result;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputFastqPath;
        @PropertyName("Input fastq path")
        @PropertyDescription("Path to fastq file with color space reads")
        public DataElementPath getInputFastqPath()
        {
            return inputFastqPath;
        }
        public void setInputFastqPath(DataElementPath inputFastqPath)
        {
            DataElementPath oldValue = this.inputFastqPath;
            this.inputFastqPath = inputFastqPath;
            firePropertyChange( "inputFastqPath", oldValue, inputFastqPath );
        }
        
        private DataElementPath outputFastqPath;
        @PropertyName("Output double encoded fastq")
        @PropertyDescription("Output double encoded fastq")
        public DataElementPath getOutputFastqPath()
        {
            return outputFastqPath;
        }
        public void setOutputFastqPath(DataElementPath outputFastqPath)
        {
            DataElementPath oldValue = this.outputFastqPath;
            this.outputFastqPath = outputFastqPath;
            firePropertyChange( "outputFastqPath", oldValue, outputFastqPath );
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
            property( "inputFastqPath" ).inputElement( FileDataElement.class ).add();
            property( "outputFastqPath" ).outputElement( FileDataElement.class ).auto( "$inputFastqPath$ double encoded" ).add();
        }
    }
}
