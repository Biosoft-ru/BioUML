package ru.biosoft.bsa.transformer;

import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Gene;
import biouml.standard.type.Publication;
import ru.biosoft.access.Entry;
import ru.biosoft.bsa.Const;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.StaticDescriptor;

/**
 * Reads genes from TRANSFAC gene.dat file
 */
public class GeneTransformer extends TransfacTransformerSupport<Gene>
{
    private static final PropertyDescriptor CLASSIFICATION_PD = StaticDescriptor.create("class", "TF class");
    private static final PropertyDescriptor BINDING_SITES_PD = StaticDescriptor.create("bindingSites", "Binding sites");
    private static final PropertyDescriptor FACTOR_NAME_PD = StaticDescriptor.create("factorName", "Factor name");

    @Override
    public Class<Gene> getOutputType()
    {
        return Gene.class;
    }

    /**
     * Converts Entry to the Gene
     */
    @Override
    public Gene transformInput(Entry entry) throws Exception
    {
        String name = null;
        String taxon = null;
        String factorName = null;
        String promoterClass = null;
        String species = null;
        String displayName = null;
        String synonyms = null;
        String location = null;
        List<String> bindingSites = new ArrayList<>();
        List<Publication> publications = new ArrayList<>();
        List<DatabaseReference> references = new ArrayList<>();
        //        Publication publication=new Publication(null, null);
        try( BufferedReader reader = new BufferedReader(entry.getReader()) )
        {
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                String[] fields = TextUtil.splitPos(line, 2);

                if( fields[0].equals("//") )
                    break;
                if( fields[0].equals("OS") )
                {
                    species = TextUtil.split(fields[1], '/')[0];
                }
                else if( fields[0].equals("BS"))
                {
                    bindingSites.add(fields[1]);
                }
                else if( fields[0].equals("OC") )
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
                else if( fields[0].equals("BC") ) //BC  1.1.5.1
                {
                    promoterClass = fields[1];
                }
                else if( fields[0].equals("SY") )
                {
                    synonyms = fields[1];
                }
                else if( fields[0].equals("CH") )
                {
                    location = fields[1];
                }
                else if( fields[0].equals("FA") )
                {
                    factorName = fields[1];
                }
                else if( fields[0].equals("SD") )
                {
                    displayName = fields[1];
                }
                else
                    updatePublications(fields[0], fields[1], publications);
            }
        }
        Gene gene = new Gene(getTransformedCollection(), name);
        gene.setTitle(displayName);
        gene.setDatabaseReferences(references.toArray(new DatabaseReference[references.size()]));
        gene.setLiteratureReferences( StreamEx.of( publications ).map( Publication::getReference ).toArray( String[]::new ) );
        gene.setSynonyms(synonyms);
        gene.setSpecies(species);
        gene.getAttributes().add(new DynamicProperty(Gene.LOCATION_PD, String.class, location));
        if(promoterClass != null)
            gene.getAttributes().add(new DynamicProperty(CLASSIFICATION_PD, String.class, promoterClass));
        if(factorName != null)
            gene.getAttributes().add(new DynamicProperty(FACTOR_NAME_PD, String.class, factorName));
        if(!bindingSites.isEmpty())
            gene.getAttributes().add(new DynamicProperty(BINDING_SITES_PD, String[].class, bindingSites.toArray(new String[bindingSites.size()])));

        return gene;
    }
}
