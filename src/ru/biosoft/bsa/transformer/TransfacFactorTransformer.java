package ru.biosoft.bsa.transformer;

import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Entry;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.TransfacTranscriptionFactor;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Publication;

/**
 * Converts {@link Entry} to the {@link TransfacTranscriptionFactor} and back
 * @see ru.biosoft.access.core.TransformedDataCollection
 *
 * @PENDING TransfacFactorTransformerTest
 */
public class TransfacFactorTransformer extends TransfacTransformerSupport<TransfacTranscriptionFactor>
{
    private static final PropertyDescriptor GENES_PD = StaticDescriptor.create("Genes with sites");

    /**
     * Returns TranscriptionFactor.class
     *
     * @return TranscriptionFactor.class
     */
    @Override
    public Class<TransfacTranscriptionFactor> getOutputType()
    {
        return TransfacTranscriptionFactor.class;
    }

    /**
     * Converts Entry to the TranscriptionFactor
     *
     * @param input  Entry of profile  FileEntryCollection
     * @return TranscriptionFactor data collection
     * @exception Exception If any error
     */
    @Override
    public TransfacTranscriptionFactor transformInput(Entry entry) throws Exception
    {
        String name = null;
        String taxon = null;
        String species = null;
        String displayName = null;
        String dnaBindingDomain = null;
        String generalClass = null;
        StringBuilder cpBuilder = new StringBuilder();
        StringBuilder cnBuilder = new StringBuilder();
        String CP = "";
        String CN = "";
        String synonyms = "";
        ArrayList<Publication> publications = new ArrayList<>();
        DataElementPath classifications = getClassificationsPath();
        List<DatabaseReference> references = new ArrayList<>();
        Set<String> genes = new TreeSet<>();
        //        Publication publication=new Publication(null, null);
        try( BufferedReader reader = new BufferedReader(entry.getReader()) )
        {
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                String[] fields = TextUtil2.splitPos(line, 2);

                if( fields[0].equals("//") )
                    break;
                if( fields[0].equals("OS") )
                {
                    species = TextUtil2.split( fields[1], '/' )[0];
                }
                if( fields[0].equals("OC") )
                {
                    String leafName = fields[1];
                    taxon = Const.TAXON_CLASSIFICATION + "/" + leafName.replaceAll("; ", "/").toLowerCase();
                }
                else if( fields[0].equals("AC") )
                    name = fields[1];
                else if( fields[0].equals("DR") )
                {
                    DatabaseReference ref = parseDatabaseReference(fields[1]);
                    references.add(ref);
                }
                else if( fields[0].equals("FA") )
                    displayName = fields[1];
                else if( fields[0].equals("CP") )
                    cpBuilder.append(" ").append(fields[1]);
                else if( fields[0].equals("CN") )
                    cnBuilder.append(" ").append(fields[1]);
                else if( fields[0].equals("CL") ) //CL  C0001; CH; 2.3.3.0.1.
                {
                    StringTokenizer strTok = new StringTokenizer(fields[1], ";");

                    if( strTok.hasMoreTokens() )
                        dnaBindingDomain = Const.DNA_BINDING_DOMAIN_CLASSIFICATION + "/" + strTok.nextToken().trim();

                    if( strTok.hasMoreTokens() )
                        strTok.nextToken();

                    if( strTok.hasMoreTokens() )
                        generalClass = strTok.nextToken().trim();
                }
                else if( fields[0].equals("SY") )
                {
                    synonyms = fields[1];
                }
                else if( fields[0].equals("BS") )
                {
                    String[] bsFields = fields[1].split("; ");
                    if(bsFields.length>=5)
                        genes.add(bsFields[3]+" ("+bsFields[4].split(", ")[0]+")");
                }
                else updatePublications(fields[0], fields[1], publications);
            }
        }

        if( generalClass != null ) // 1.2.3.4 -> 1/1.2/1.2.3/1.2.3.4
        {
            if( generalClass.endsWith(".") ) // remove traling '.'
                generalClass = generalClass.substring(0, generalClass.length() - 1);

            StringTokenizer strTok = new StringTokenizer(generalClass, ".");
            StringBuilder complName = new StringBuilder();
            StringBuilder tmpName = new StringBuilder();

            while( strTok.hasMoreTokens() )
            {
                String token = strTok.nextToken();
                if( tmpName.length() > 0 )
                    tmpName.append('.');
                tmpName.append(token);

                if( !token.equals("0") )
                {
                    complName.append("/");
                    complName.append(tmpName);
                }
            }

            if( tmpName.length() == 0 )
                generalClass = null;
            else
                generalClass = Const.TRANSCRIPTION_FACTOR_CLASSIFICATION + "/" + complName.toString();
        }

        CP = cpBuilder.toString().trim();
        if( CP.isEmpty() )
            CP = null;
        CN = cnBuilder.toString().trim();
        if( CN.isEmpty() )
            CN = null;

        TransfacTranscriptionFactor tf = new TransfacTranscriptionFactor(name, getPrimaryCollection(), displayName, classifications, taxon
                + "/" + species, dnaBindingDomain, generalClass, CP, CN);
        tf.setDatabaseReferences(references.toArray(new DatabaseReference[references.size()]));
        tf.setSynonyms(synonyms);
        tf.setPublications(publications);
        if(!genes.isEmpty())
        {
            tf.getAttributes().add(new DynamicProperty(GENES_PD, String[].class, genes.toArray(new String[genes.size()])));
        }
        return tf;
    }
}
