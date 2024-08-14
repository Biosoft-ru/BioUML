package ru.biosoft.bsa;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.developmentontheedge.beans.PropertiesDPS;
import com.developmentontheedge.beans.annot.PropertyName;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndex;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;

@ClassIcon ( "resources/trackvcf.png" )
@PropertyName ( "track" )
public class VCFFileTrack extends AbstractDataCollection<DataElement> implements Track
{
    public static final String INDEX_PATH_PROPERTY="indexPath";
    
    private File vcfFile;
    private File indexFile;
    private VCFFileReader reader;
    private ChrCache chrCache;
    
    
    public VCFFileTrack(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
        
        String vcfFilePath = properties.getProperty( DataCollectionConfigConstants.FILE_PROPERTY );
        if(vcfFilePath == null)
            throw new IllegalArgumentException();
        vcfFile = new File(vcfFilePath);
        if(!vcfFile.exists())
            throw new FileNotFoundException();
        
        String indexPath = properties.getProperty( INDEX_PATH_PROPERTY );
        if(indexPath == null)
        {
            indexPath = vcfFilePath + ".tbi";
            if(!Files.exists( Paths.get( indexPath )))
            {
                String vcfFileName = new File(vcfFilePath).getName();
                String configFolder = properties.getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
                indexPath = new File(configFolder, vcfFileName + ".tbi").getAbsolutePath();
            }
        }
        indexFile = new File(indexPath);
        
        DataElementPath seqBase = DataElementPath.create( properties.getProperty( Track.SEQUENCES_COLLECTION_PROPERTY ) );
        chrCache = new ChrCache( seqBase );
        
        
        if(!hasIndex())
            buildIndex();
        
        reader = new VCFFileReader( vcfFile, indexFile );
    }

    
    private boolean hasIndex()
    {
        return indexFile.exists();
    }
    
    private Object indexLock = new Object();
    
    private void buildIndex() throws IOException
    {
        synchronized( indexLock )
        {
            if(hasIndex())
                return;
            VCFFileReader reader = new VCFFileReader( vcfFile, false );
            SAMSequenceDictionary chromosomes = reader.getFileHeader().getSequenceDictionary();
            reader.close();

            TabixIndex index = IndexFactory.createTabixIndex( vcfFile, new VCFCodec(), TabixFormat.VCF, chromosomes );
            index.write( indexFile );
        }
    }

    @Override
    public List<String> getNameList()
    {
        return Collections.emptyList();
    }
    
    @Override
    protected DataElement doGet(String name) throws Exception
    {
        return null;
    }

    private Object readerLock = new Object();
    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        String chrName = DataElementPath.create( sequence ).getName();
        
        //tabix index always return empty results for coords <= 0
        if(from <= 0)
            from = 1;

        synchronized(readerLock)
        {
            CloseableIterator<VariantContext> it = reader.query( chrName, from, to );
            return convertToDC( it );
        }
    }

    @Override
    public int countSites(String sequence, int from, int to) throws Exception
    {
        return getSites( sequence, from, to ).getSize();
    }

    @Override
    public Site getSite(String sequence, String siteName, int from, int to) throws Exception
    {
        return getSites(sequence, from ,to).get( siteName );
    }

    @Override
    public DataCollection<Site> getAllSites()
    {
        synchronized(readerLock)
        {
            return convertToDC( reader.iterator() );
        }
    }
    
    private VectorDataCollection<Site> convertToDC(CloseableIterator<VariantContext> it)
    {
        VectorDataCollection<Site> result = new VectorDataCollection<>( "sites" );
        int i = 1;
        while(it.hasNext())
        {
            String siteName = String.valueOf(i++);
            Site site = variantToSite(it.next(), siteName);
            result.put(site);
        }
        it.close();
        return result;
    }
    
    private Site variantToSite(VariantContext v, String siteName)
    {
        Properties parameters = new Properties();
        if(v.hasID())
            parameters.put( "name", v.getID() );
        
        Allele altAllele = v.getAltAlleleWithHighestAlleleCount();
        String altAlleleStr = altAllele == null ? "" : altAllele.getBaseString();
        if(altAlleleStr.equals( "." ))
            altAlleleStr = "";
        parameters.put( "AltAllele", altAlleleStr );
        
        String refAlleleStr = v.getReference().getBaseString();
        if(refAlleleStr.equals( "." ))
            refAlleleStr = "";
        parameters.put( "RefAllele", refAlleleStr );
        
        String type = SiteType.TYPE_VARIATION;
        
        Sequence seq = chrCache.getSequence( v.getContig() );
        
        return new SiteImpl(null,  siteName, type, Basis.BASIS_USER, v.getStart(), v.getEnd() - v.getStart() + 1, Precision.PRECISION_EXACTLY,
                StrandType.STRAND_NOT_APPLICABLE, seq, new PropertiesDPS(parameters));
    }
    
    private TrackViewBuilder viewBuilder = new DefaultTrackViewBuilder();
    @Override
    public TrackViewBuilder getViewBuilder()
    {
        return viewBuilder;
    }
}
