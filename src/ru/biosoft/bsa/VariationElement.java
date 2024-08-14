package ru.biosoft.bsa;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.StaticDescriptor;

public class VariationElement extends DataElementSupport implements WithSite
{
    public static final PropertyDescriptor NAME_DESCRIPTOR = StaticDescriptor.create("name");
    public static final PropertyDescriptor REF_ALLELE_DESCRIPTOR = StaticDescriptor.create("RefAllele");
    public static final PropertyDescriptor ALT_ALLELE_DESCRIPTOR = StaticDescriptor.create("AltAllele");

    private String allele;
    private Site site;

    public VariationElement(DataCollection origin, String name, Sequence chromosome, int from, int to, int strand, String allele)
    {
        super(name, origin);
        int start = strand == StrandType.STRAND_PLUS ? from : to;
        int length = to - from + 1;
        this.site = createVariationSite( chromosome, name, name, start, length, strand, allele );
        this.allele = allele;
    }

    public String getAllele()
    {
        return allele;
    }

    @Override
    public Site getSite()
    {
        return site;
    }
    
    @Override
    public String toString()
    {
        return getName()+"/chr."+site.getOriginalSequence().getName()+" "+site.getFrom()+"-"+site.getTo();
    }
    
    public static Site createVariationSite(Sequence sequence, String id, String name, int start, int length, int strand, String alleleStr)
    {
        String[] allele = TextUtil.split( alleleStr, '/' );
        if(allele[0].equals("-")) allele[0] = "";
        if(allele[1].equals("-")) allele[1] = "";
        
        //workaround for bug in Ensembl  
        if( allele[0].isEmpty() )
            length = 0;

        DynamicPropertySet propertySet = new DynamicPropertySetAsMap();
        propertySet.add(new DynamicProperty(REF_ALLELE_DESCRIPTOR, String.class, allele[0]));
        propertySet.add(new DynamicProperty(ALT_ALLELE_DESCRIPTOR, String.class, allele[1]));
        propertySet.add(new DynamicProperty(NAME_DESCRIPTOR, String.class, name));
        
        Site site = new SiteImpl(null, id, allele[0].length() < allele[1].length() ? SiteType.TYPE_INSERTION
                : allele[0].length() > allele[1].length() ? SiteType.TYPE_DELETION : SiteType.TYPE_VARIATION, Basis.BASIS_USER, start,
                length, Precision.PRECISION_EXACTLY, strand, sequence, propertySet);
        return site;
    }

    public static Site extendInsertionSite(Site site)
    {
        if( site.getType().equals( SiteType.TYPE_INSERTION ) && site.getLength() == 0 )
        {
            DynamicPropertySet propertySet = new DynamicPropertySetAsMap();
            String refAllele = site.getProperties().getValueAsString( "RefAllele" );
            String altAllele = site.getProperties().getValueAsString( "AltAllele" );
            String startLetter = String.valueOf( (char) ( site.getSequence().getLetterAt( site.getStart() ) ) ).toUpperCase();
            propertySet.add( new DynamicProperty( VariationElement.REF_ALLELE_DESCRIPTOR, String.class, startLetter + refAllele ) );
            propertySet.add( new DynamicProperty( VariationElement.ALT_ALLELE_DESCRIPTOR, String.class, startLetter + altAllele ) );
            propertySet.add( new DynamicProperty( VariationElement.NAME_DESCRIPTOR, String.class,
                    site.getProperties().getValueAsString( "name" ) ) );
            Site newSite = new SiteImpl( null, site.getName(), site.getType(), site.getBasis(), site.getStart(), site.getLength() + 1,
                    site.getPrecision(), site.getStrand(), site.getSequence(), propertySet );
            return newSite;
        }
        return site;
    }

}
