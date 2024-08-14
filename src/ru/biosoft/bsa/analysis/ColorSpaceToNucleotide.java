package ru.biosoft.bsa.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ColorSpaceToNucleotide extends AnalysisMethodSupport<ColorSpaceToNucleotide.Parameters>
{
    public ColorSpaceToNucleotide(DataCollection<?> origin, String name)
    {
        super(origin, name, new Parameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        FileDataElement result = createFileDataElement(parameters.getFasta());

        File csFastaFile = parameters.getCsFasta().getDataElement(FileDataElement.class).getFile();

        try(BufferedReader reader = ApplicationUtils.utfReader( csFastaFile ))
        {
            String line;
            while( ( line = reader.readLine() ) != null && line.startsWith("#") );

            try (BufferedWriter writer = ApplicationUtils.utfWriter( result.getFile() ))
            {
                while( line != null )
                {
                    String name = line;
                    
                    String sequence = reader.readLine();
                    if( sequence == null )
                        throw new Exception("Unexpected end of file " + parameters.getCsFasta());
                    String colors = colorToNucleotide(sequence);
                    if( !colors.isEmpty() )
                        writer.write(name + "\n" + colors + "\n");
                    
                    line = reader.readLine();
                }
            }
        }

        parameters.getFasta().save(result);
        return result;
    }

    private FileDataElement createFileDataElement(DataElementPath path)
    {
        DataCollection<? extends DataElement> parent = path.optParentCollection();
        String name = path.getName();

        File file = DataCollectionUtils.getChildFile(parent, name);
        FileDataElement result = new FileDataElement(name, parent, file);
        return result;
    }

    private static String colorToNucleotide(String sequence)
    {
        Nucleotide5LetterAlphabet alphabet = Nucleotide5LetterAlphabet.getInstance();
        int base = alphabet.letterToCodeMatrix()[sequence.charAt(0)];
        StringBuilder sb = new StringBuilder(sequence.length());
        for( int i = 1; i < sequence.length(); i++ )
        {
            int color = sequence.charAt(i) - '0';
            if( color < 0 || color > 3 )
                break;
            base = base ^ color;
            sb.append((char)alphabet.codeToLetterMatrix()[base]);
        }
        return sb.toString();
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath csFasta, fasta;

        public DataElementPath getCsFasta()
        {
            return csFasta;
        }

        public void setCsFasta(DataElementPath csFasta)
        {
            this.csFasta = csFasta;
        }

        public DataElementPath getFasta()
        {
            return fasta;
        }

        public void setFasta(DataElementPath fasta)
        {
            this.fasta = fasta;
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
            property( "csFasta" ).inputElement( FileDataElement.class ).add();
            property( "fasta" ).outputElement( FileDataElement.class ).add();
        }
    }
}
