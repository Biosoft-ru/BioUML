package ru.biosoft.bsa.transformer;

import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import one.util.streamex.StreamEx;

import ru.biosoft.access.Entry;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.StaticDescriptor;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Publication;

import com.developmentontheedge.beans.DynamicProperty;

/**
 * Reads genes from TRANSFAC gene.dat file
 */
public class SiteTransformer extends TransfacTransformerSupport<Site>
{
    


    private static final PropertyDescriptor MATRICES_PD = StaticDescriptor.create("matrices", "Weight matrices");
    
    public static final String FACTORS_PROPERTY = "factors";
    private static final PropertyDescriptor FACTORS_PD = StaticDescriptor.create(FACTORS_PROPERTY, "Binding factors");
    
    private static final PropertyDescriptor METHODS_PD = StaticDescriptor.create("methods", "Identification methods");
    private static final PropertyDescriptor PUBLICATIONS_PD = StaticDescriptor.create("publications", "Publications");
    private static final PropertyDescriptor REGION_PD = StaticDescriptor.create("region", "Gene region");
    private static final PropertyDescriptor SEQUENCE_PD = StaticDescriptor.create("sequence", "Sequence");
    private static final PropertyDescriptor ELEMENT_PD = StaticDescriptor.create("element", "Element");
    private static final PropertyDescriptor ID_PD = StaticDescriptor.create("ID", "ID");
    private static final PropertyDescriptor SPECIES_PD = StaticDescriptor.create("species", "Species");
    private static final PropertyDescriptor DESCRIPTION_PD = StaticDescriptor.create("description", "Description");
    private static final PropertyDescriptor GENE_PD = StaticDescriptor.create("gene", "Gene name");
    
    public static final String GENOMIC_COORDINATES_PROPERTY = "coords";
    private static final PropertyDescriptor GENOMIC_COORDINATES_PD = StaticDescriptor.create( GENOMIC_COORDINATES_PROPERTY, "Genomic coordinates" );
    
    public static final String SOURCES_PROPERTY = "sources";
    public static final PropertyDescriptor SOURCES_PD = StaticDescriptor.create( SOURCES_PROPERTY, "Sources" );

    @Override
    public Class<Site> getOutputType()
    {
        return Site.class;
    }

    /**
     * Converts Entry to the Gene
     */
    @Override
    public Site transformInput(Entry entry) throws Exception
    {
        String name = null;
        String taxon = null;
        StringBuilder sequenceBuilder = new StringBuilder();
        String species = null;
        String type = null;
        String description = null;
        String comment = null;
        String displayName = null;
        String location = null;
        String geneName = null;
        String element = null;
        Integer start = null, end = null;
        List<String> bindingFactors = new ArrayList<>();
        List<String> matrices = new ArrayList<>();
        List<String> methods = new ArrayList<>();
        List<Publication> publications = new ArrayList<>();
        List<DatabaseReference> references = new ArrayList<>();
        String genomicCoordinates = null;
        List<String> sourceList = new ArrayList<>();
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
                    if( species.contains(", ") )
                        species = species.split(", ")[1];
                }
                else if( fields[0].equals("OC") )
                {
                    String leafName = fields[1];
                    taxon = Const.TAXON_CLASSIFICATION + "/" + leafName.replaceAll("; ", "/").toLowerCase();
                }
                else if( fields[0].equals("AC") )
                {
                    name = fields[1];
                }
                else if( fields[0].equals("TY") )
                {
                    type = fields[1];
                }
                else if( fields[0].equals("DE") )
                {
                    description = fields[1];
                    int pos = description.lastIndexOf("Gene: ");
                    if( pos >= 0 )
                        geneName = description.substring(pos + 6, description.length() - 1);
                }
                else if( fields[0].equals("CC") )
                {
                    comment = fields[1];
                }
                else if( fields[0].equals("SQ") )
                {
                    String s = fields[1].trim();
                    sequenceBuilder.append( s );
                }
                else if( fields[0].equals("EL") )
                {
                    element = fields[1];
                }
                else if( fields[0].equals("BF") )
                {
                    bindingFactors.add(fields[1]);
                }
                else if( fields[0].equals("MX") )
                {
                    matrices.add(fields[1]);
                }
                else if( fields[0].equals("MM") )
                {
                    methods.add(fields[1]);
                }
                else if( fields[0].equals("RE") )
                {
                    location = fields[1];
                }
                else if( fields[0].equals("SF") )
                {
                    start = Integer.parseInt(fields[1]);
                }
                else if( fields[0].equals("ST") )
                {
                    end = Integer.parseInt(fields[1]);
                }
                else if( fields[0].equals("DR") )
                {
                    DatabaseReference ref = parseDatabaseReference(fields[1]);
                    references.add(ref);
                }
                else if( fields[0].equals("ID") )
                {
                    displayName = fields[1];
                }
                else if(fields[0].equals("SC"))
                {
                    genomicCoordinates = fields[1];
                }
                else if(fields[0].equals("SO"))
                {
                    sourceList.add( fields[1] );
                }
                else
                    updatePublications(fields[0], fields[1], publications);
            }
        }
        
        String sequence = sequenceBuilder.toString();
        if(sequence.endsWith( "." ))
            sequence = sequence.substring( 0, sequence.length() - 1 );
        sequence = sequence.replace( '.', ',' );//multiple sequences possible
        if(sequence.isEmpty())
            sequence = "?";

        if( end == null || start == null )
        {
            end = start = 0;
        }

        Sequence seq;
        if( sequence.length() == end - start + 1 )
            seq = new LinearSequence(geneName, start, sequence.getBytes(), Nucleotide5LetterAlphabet.getInstance());
        else
            seq = new UnknownSequence(geneName, start, end);
        Site site = new SiteImpl(getTransformedCollection(), name, type, Basis.BASIS_ANNOTATED, start, end - start + 1,
                Precision.PRECISION_NOT_KNOWN, StrandType.STRAND_NOT_KNOWN, seq, comment, null);
        if( !matrices.isEmpty() )
            site.getProperties().add(new DynamicProperty(MATRICES_PD, String[].class, matrices.toArray(new String[matrices.size()])));
        if( !methods.isEmpty() )
            site.getProperties().add(new DynamicProperty(METHODS_PD, String[].class, methods.toArray(new String[methods.size()])));
        if( !bindingFactors.isEmpty() )
            site.getProperties().add(
                    new DynamicProperty(FACTORS_PD, String[].class, bindingFactors.toArray(new String[bindingFactors.size()])));
        if( !publications.isEmpty() )
        {
            String[] stringPublications = StreamEx.of(publications).map(Publication::getReference).toArray( String[]::new );
            site.getProperties().add(new DynamicProperty(PUBLICATIONS_PD, String[].class, stringPublications));
        }
        if( location != null )
            site.getProperties().add(new DynamicProperty(REGION_PD, String.class, location));
        if( description != null )
            site.getProperties().add(new DynamicProperty(DESCRIPTION_PD, String.class, description));
        if( geneName != null )
            site.getProperties().add(new DynamicProperty(GENE_PD, String.class, geneName));
        if( displayName != null )
            site.getProperties().add(new DynamicProperty(ID_PD, String.class, displayName));
        if( element != null )
            site.getProperties().add(new DynamicProperty(ELEMENT_PD, String.class, element));
        if( !sequence.isEmpty() )
            site.getProperties().add(new DynamicProperty(SEQUENCE_PD, String.class, sequence));
        if( species != null )
            site.getProperties().add(new DynamicProperty(SPECIES_PD, String.class, species));
        if(genomicCoordinates != null)
            site.getProperties().add( new DynamicProperty( GENOMIC_COORDINATES_PD, String.class, genomicCoordinates ) );
        if(!sourceList.isEmpty())
            site.getProperties().add( new DynamicProperty(SOURCES_PD, String[].class, sourceList.toArray(new String[0])) );
            
        return site;
    }
}
