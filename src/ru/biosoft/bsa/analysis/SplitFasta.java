package ru.biosoft.bsa.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SplitFasta extends AnalysisMethodSupport<SplitFasta.Parameters>
{
    public SplitFasta(DataCollection<?> origin, String name)
    {
        super(origin, name, new Parameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        File inputFile = parameters.getFasta().getDataElement(FileDataElement.class).getFile();
        if( !parameters.getOutFolder().exists() )
            DataCollectionUtils.createSubCollection(parameters.getOutFolder());
        DataCollection<DataElement> outCollection = parameters.getOutFolder().getDataCollection();
        List<File> files = new ArrayList<>();

        String line;
        long sequences = 0, letters = 0;
        File outFile = DataCollectionUtils.getChildFile( outCollection, "part1.fa");
        files.add(outFile);
        BufferedWriter writer = ApplicationUtils.utfWriter( outFile );
        try(BufferedReader reader = ApplicationUtils.utfReader( inputFile ))
        {
            line = reader.readLine();
            while( line != null )
            {
                if( sequences >= parameters.getSequencesPerFile() || letters >= parameters.getLettersPerFile() )
                {
                    writer.close();
                    outFile = DataCollectionUtils.getChildFile( outCollection, "part" + ( files.size() + 1 ) + ".fa");
                    files.add(outFile);
                    writer = ApplicationUtils.utfWriter( outFile );
                    sequences = 0;
                    letters = 0;
                }

                if( !line.startsWith(">") )
                    throw new Exception("Expecting '>' in fasta file " + parameters.getFasta());
                writer.write(line);
                writer.newLine();

                while( ( line = reader.readLine() ) != null && !line.startsWith(">") )
                {
                    writer.write(line);
                    writer.newLine();
                    letters += line.length();
                }
                sequences++;
            }

            writer.close();

            for( File f : files )
                outCollection.put(new FileDataElement(f.getName(), outCollection, f));
        }
        catch( Exception e )
        {
            for( File f : files )
            {
                outCollection.remove(f.getName());
                f.delete();
            }
            throw e;
        }

        return outCollection;
    }


    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath fasta, outFolder;
        private int sequencesPerFile, lettersPerFile;
        public DataElementPath getFasta()
        {
            return fasta;
        }
        public void setFasta(DataElementPath fasta)
        {
            this.fasta = fasta;
        }
        public DataElementPath getOutFolder()
        {
            return outFolder;
        }
        public void setOutFolder(DataElementPath outFolder)
        {
            this.outFolder = outFolder;
        }
        public int getSequencesPerFile()
        {
            return sequencesPerFile;
        }
        public void setSequencesPerFile(int sequencesPerFile)
        {
            this.sequencesPerFile = sequencesPerFile;
        }
        public int getLettersPerFile()
        {
            return lettersPerFile;
        }
        public void setLettersPerFile(int lettersPerFile)
        {
            this.lettersPerFile = lettersPerFile;
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super(Parameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            property( "fasta" ).inputElement( FileDataElement.class ).add();
            add("sequencesPerFile");
            add("lettersPerFile");
            property( "outFolder" ).outputElement( FolderCollection.class ).add();
        }
    }
}
