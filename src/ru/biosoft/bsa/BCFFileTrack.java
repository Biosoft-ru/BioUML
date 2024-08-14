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
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.tribble.Feature;
import htsjdk.tribble.FeatureCodecHeader;
import htsjdk.tribble.SimpleFeature;
import htsjdk.tribble.index.Block;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndex;
import htsjdk.tribble.index.tabix.TabixIndexCreator;
import htsjdk.variant.bcf2.BCF2Codec;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.exception.ExceptionRegistry;

@ClassIcon ( "resources/trackvcf.png" )
@PropertyName ( "track" )
//BCF file (binary VCF) indexed with tabix index
public class BCFFileTrack extends AbstractDataCollection<DataElement> implements Track
{
    public static final String INDEX_PATH_PROPERTY="indexPath";
    
    private File bcfFile;
    private File indexFile;
    private ChrCache chrCache;
    private TabixIndex tabixIndex;
    private BCF2Codec codec = new BCF2Codec();
    
    public BCFFileTrack(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
        
        String bcfFilePath = properties.getProperty( DataCollectionConfigConstants.FILE_PROPERTY );
        if(bcfFilePath == null)
            throw new IllegalArgumentException();
        bcfFile = new File(bcfFilePath);
        if(!bcfFile.exists())
            throw new FileNotFoundException();
        
        String indexPath = properties.getProperty( INDEX_PATH_PROPERTY );
        if(indexPath == null)
        {
            indexPath = bcfFilePath + ".tbi";
            if(!Files.exists( Paths.get( indexPath )))
            {
                String bcfFileName = new File(bcfFilePath).getName();
                String configFolder = properties.getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
                indexPath = new File(configFolder, bcfFileName + ".tbi").getAbsolutePath();
            }
        }
        indexFile = new File(indexPath);
        
        DataElementPath seqBase = DataElementPath.create( properties.getProperty( Track.SEQUENCES_COLLECTION_PROPERTY ) );
        chrCache = new ChrCache( seqBase );
        
        
        if(!hasIndex())
            buildIndex();
        open();
    }

    
    private boolean hasIndex()
    {
        return indexFile.exists();
    }
    
    private void open() throws IOException
    {
        tabixIndex = new TabixIndex( indexFile );
        try(BlockCompressedInputStream bc = new BlockCompressedInputStream(bcfFile))
        {
            codec.readHeaderFromInputStream(bc);
        }
    }

    
    private Object indexLock = new Object();
    
    private void buildIndex() throws IOException
    {
        synchronized( indexLock )
        {
            if(hasIndex())
                return;
            BlockCompressedInputStream bc = new BlockCompressedInputStream(bcfFile);
            BCF2Codec codec = new BCF2Codec();
            FeatureCodecHeader header = codec.readHeaderFromInputStream(bc);
            VCFHeader vcfHeader = (VCFHeader) header.getHeaderValue();
            TabixIndexCreator indexCreator = new TabixIndexCreator(vcfHeader.getSequenceDictionary(), TabixFormat.VCF);
            while(bc.available() > 0)
            {
                long position = bc.getPosition();
                VariantContext feature = codec.decodeFromInputStream(bc);
                indexCreator.addFeature(feature, position);
            }
            Index index = indexCreator.finalizeIndex(bc.getPosition());
            index.write( indexFile );
            bc.close();
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
    
    private Object readLock = new Object();

    @Override
    public DataCollection<Site> getSites(String sequence, int from, int to)
    {
        String chrName = DataElementPath.create( sequence ).getName();
        
        //tabix index always return empty results for coords <= 0
        if(from <= 0)
            from = 1;

        synchronized( readLock )
        {

            Feature query = new SimpleFeature( chrName, from, to );
            List<Block> blocks = tabixIndex.getBlocks( query.getContig(), query.getStart(), query.getEnd() );

            VectorDataCollection<Site> result = new VectorDataCollection<>( "sites" );

            int siteId = 1;


            try (BlockCompressedInputStream bc = new BlockCompressedInputStream( bcfFile ))
            {
                for( Block b : blocks )
                {
                    bc.seek( b.getStartPosition() );
                    while( bc.getPosition() < b.getEndPosition() )
                    {
                        VariantContext variant = codec.decodeFromInputStream( bc );
                        if( variant.overlaps( query ) )
                        {
                            String siteName = String.valueOf( siteId++ );
                            Site site = variantToSite( variant, siteName );
                            result.put( site );
                        }
                    }
                }
            }
            catch( IOException e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            return result;
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
        throw new UnsupportedOperationException();
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
