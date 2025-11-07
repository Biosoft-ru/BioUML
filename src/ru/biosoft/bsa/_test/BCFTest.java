package ru.biosoft.bsa._test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.tribble.readers.PositionalBufferedStream;
import htsjdk.variant.bcf2.BCF2Codec;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFFormatHeaderLine;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;
import htsjdk.variant.vcf.VCFHeaderLineType;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import ru.biosoft.util.TempFiles;

/**
 * Test class for BCF file track processing with htsjdk library.
 * Partially copied from htsjdk.variant.bcf2.BCF2WriterUnitTest
 * 
 * @author anna
 *
 */
public class BCFTest extends AbstractTrackTest
{

    public static final String filesPath = "../data/test/ru/biosoft/bsa/vcf";
    private static VCFHeader createFakeHeader()
    {
        final SAMSequenceDictionary sequenceDict = createArtificialSequenceDictionary();
        final Set<VCFHeaderLine> metaData = new HashSet<>();
        final Set<String> additionalColumns = new HashSet<>();
        metaData.add( new VCFHeaderLine( "two", "2" ) );
        additionalColumns.add( "extra1" );
        additionalColumns.add( "extra2" );
        final VCFHeader header = new VCFHeader( metaData, additionalColumns );
        header.addMetaDataLine( new VCFInfoHeaderLine( "DP", 1, VCFHeaderLineType.String, "x" ) );
        header.addMetaDataLine( new VCFFormatHeaderLine( "GT", 1, VCFHeaderLineType.String, "x" ) );
        header.addMetaDataLine( new VCFFormatHeaderLine( "BB", 1, VCFHeaderLineType.String, "x" ) );
        header.addMetaDataLine( new VCFFormatHeaderLine( "GQ", 1, VCFHeaderLineType.String, "x" ) );
        header.setSequenceDictionary( sequenceDict );
        return header;
    }

    public static SAMSequenceDictionary createArtificialSequenceDictionary()
    {
        final int[] contigLengths = { 249250621, 243199373, 198022430, 191154276, 180915260, 171115067, 159138663, 146364022, 141213431, 135534747, 135006516, 133851895, 115169878,
                107349540, 102531392, 90354753, 81195210, 78077248, 59128983, 63025520, 48129895, 51304566, 155270560, 59373566, 16569 };
        List<SAMSequenceRecord> contigs = new ArrayList<SAMSequenceRecord>();

        for ( int contig = 1; contig <= 22; contig++ )
        {
            contigs.add( new SAMSequenceRecord( Integer.toString( contig ), contigLengths[contig - 1] ) );
        }

        int position = 22;
        for ( String contigName : Arrays.asList( "X", "Y", "MT" ) )
        {
            contigs.add( new SAMSequenceRecord( contigName, contigLengths[position] ) );
            position++;
        }

        return new SAMSequenceDictionary( contigs );
    }


    /**
     * Test writing uncompressed BCF and then reading
     */
    public void testWriteAndReadUncompressedBCF() throws IOException
    {
        final File bcfOutputFile = TempFiles.file( "test_4sites_uncompressed.bcf" );
        bcfOutputFile.deleteOnExit();
        final VCFHeader header = createFakeHeader();
        try (final VariantContextWriter writer = new VariantContextWriterBuilder().setOutputFile( bcfOutputFile ).setReferenceDictionary( header.getSequenceDictionary() )
                .unsetOption( Options.INDEX_ON_THE_FLY ).build())
        {
            writer.writeHeader( header );
            writer.add( createVC( header, 5, 5 ) );
            writer.add( createVC( header, 20, 20 ) );
            writer.add( createVC( header, 50, 50 ) );
            writer.add( createVC( header, 55, 55 ) );
        }

        VariantContextTestProvider.VariantContextContainer container = VariantContextTestProvider.readAllVCs( bcfOutputFile, new BCF2Codec(), false );
        int counter = 0;
        final Iterator<VariantContext> it = container.getVCs().iterator();
        while ( it.hasNext() )
        {
            it.next();
            counter++;
        }
        assertEquals( counter, 4 );
    }

    /**
     * Test writing compressed BCF and then reading
     */
    public void testWriteAndReadCompressedBCF() throws IOException
    {
        final File bcfOutputFile = TempFiles.file( "test_4sites_compressed.bcf" );
        bcfOutputFile.deleteOnExit();
        final VCFHeader header = createFakeHeader();
        BlockCompressedOutputStream bcos = new BlockCompressedOutputStream( bcfOutputFile );
        try (final VariantContextWriter writer = new VariantContextWriterBuilder().setOutputBCFStream( bcos ).setReferenceDictionary( header.getSequenceDictionary() )
                .unsetOption( Options.INDEX_ON_THE_FLY ).build())
        {
            writer.writeHeader( header );
            writer.add( createVC( header, 5, 5 ) );
            writer.add( createVC( header, 20, 20 ) );
            writer.add( createVC( header, 50, 50 ) );
            writer.add( createVC( header, 55, 55 ) );
        }

        VariantContextTestProvider.VariantContextContainer container = VariantContextTestProvider.readAllVCs( bcfOutputFile, new BCF2Codec(), true );
        int counter = 0;
        final Iterator<VariantContext> it = container.getVCs().iterator();
        while ( it.hasNext() )
        {
            it.next();
            counter++;
        }
        assertEquals( counter, 4 );

    }

    // Reading the VCF and writing it to a compressed BCF
    public void testReadVCFWriteCompressedBCF() throws IOException
    {
        File vcfInputFile = new File( filesPath, "vcf_track.vcf" );
        final File bcfOutputFile = TempFiles.file( "bcf_compressed.bcf" );
        bcfOutputFile.deleteOnExit();

        BlockCompressedOutputStream bcos = new BlockCompressedOutputStream( bcfOutputFile );
        try (VCFFileReader vcfFile = new VCFFileReader( vcfInputFile, false );
                VariantContextWriter bcfWriter = new VariantContextWriterBuilder().setOutputBCFStream( bcos )
                        .setReferenceDictionary( vcfFile.getFileHeader().getSequenceDictionary() ).build();
        )
        {
            bcfWriter.writeHeader( vcfFile.getFileHeader() );

            for ( VariantContext vc : vcfFile.iterator().toList() )
            {
                bcfWriter.add( vc );
            }
        }

        int counter = 0;
        try (final PositionalBufferedStream headerPbs = new PositionalBufferedStream( new BlockCompressedInputStream( bcfOutputFile ) ))
        {

            BCF2Codec codec = new BCF2Codec();
            codec.readHeader( headerPbs );

            while ( !headerPbs.isDone() )
            {
                VariantContext vc = codec.decode( headerPbs );
                counter++;
            }
            assertEquals( "Wrong number of variants", counter, 221 );
        }

    }

    //    /**
    //     * test, with index-on-the-fly option, that we can output and input BCF without problems
    //     */
    //    @Test
    //    public void testWriteAndReadBCFWithIndex() throws IOException {
    //        final File bcfOutputFile = File.createTempFile("testWriteAndReadVCF.", ".bcf", tempDir);
    //        bcfOutputFile.deleteOnExit();
    //        Tribble.indexFile(bcfOutputFile).deleteOnExit();
    //        final VCFHeader header = createFakeHeader();
    //        try (final VariantContextWriter writer = new VariantContextWriterBuilder()
    //                .setOutputFile(bcfOutputFile).setReferenceDictionary(header.getSequenceDictionary())
    //                .setOptions(EnumSet.of(Options.INDEX_ON_THE_FLY))
    //                .build()) {
    //            writer.writeHeader(header);
    //            writer.add(createVC(header));
    //            writer.add(createVC(header));
    //        }
    //        VariantContextTestProvider.VariantContextContainer container = VariantContextTestProvider
    //                .readAllVCs(bcfOutputFile, new BCF2Codec());
    //        int counter = 0;
    //        final Iterator<VariantContext> it = container.getVCs().iterator();
    //        while (it.hasNext()) {
    //            it.next();
    //            counter++;
    //        }
    //        Assert.assertEquals(counter, 2);
    //    }
    //
    //    /**
    //     * test, using the writer and reader, that we can output and input a BCF body without header
    //     */
    //    @Test
    //    public void testWriteAndReadBCFHeaderless() throws IOException {
    //        final File bcfOutputFile = File.createTempFile("testWriteAndReadBCFWithHeader.", ".bcf", tempDir);
    //        bcfOutputFile.deleteOnExit();
    //        final File bcfOutputHeaderlessFile = File.createTempFile("testWriteAndReadBCFHeaderless.", ".bcf", tempDir);
    //        bcfOutputHeaderlessFile.deleteOnExit();
    //
    //        final VCFHeader header = createFakeHeader();
    //        // we write two files, bcfOutputFile with the header, and bcfOutputHeaderlessFile with just the body
    //        try (final VariantContextWriter fakeBCFFileWriter = new VariantContextWriterBuilder()
    //                .setOutputFile(bcfOutputFile).setReferenceDictionary(header.getSequenceDictionary())
    //                .unsetOption(Options.INDEX_ON_THE_FLY)
    //                .build()) {
    //            fakeBCFFileWriter.writeHeader(header); // writes header
    //        }
    //
    //        try (final VariantContextWriter fakeBCFBodyFileWriter = new VariantContextWriterBuilder()
    //                .setOutputFile(bcfOutputHeaderlessFile).setReferenceDictionary(header.getSequenceDictionary())
    //                .unsetOption(Options.INDEX_ON_THE_FLY)
    //                .build()) {
    //            fakeBCFBodyFileWriter.setHeader(header); // does not write header
    //            fakeBCFBodyFileWriter.add(createVC(header));
    //            fakeBCFBodyFileWriter.add(createVC(header));
    //        }
    //
    //        VariantContextTestProvider.VariantContextContainer container;
    //
    //        try (final PositionalBufferedStream headerPbs = new PositionalBufferedStream(new FileInputStream(bcfOutputFile));
    //        final PositionalBufferedStream bodyPbs = new PositionalBufferedStream(new FileInputStream(bcfOutputHeaderlessFile))) {
    //
    //            BCF2Codec codec = new BCF2Codec();
    //            codec.readHeader(headerPbs);
    //            // we use the header information read from identical file with header+body to read just the body of second file
    //
    //            int counter = 0;
    //            while (!bodyPbs.isDone()) {
    //                VariantContext vc = codec.decode(bodyPbs);
    //                counter++;
    //            }
    //            Assert.assertEquals(counter, 2);
    //        }
    //
    //    }
    //
    //    /**
    //     * test, using the writer and reader, that phased information is preserved in a round trip
    //     */
    //    @Test
    //    public void testReadAndWritePhasedBCF() throws IOException
    //    {
    //        final File vcfInputFile = new File( "src/test/resources/htsjdk/variant/phased.vcf" );
    //        final File bcfOutputFile = TempFiles.file( "testWriteAndReadBCFHeaderless.bcf" );
    //        bcfOutputFile.deleteOnExit();
    //
    //        try (VCFFileReader vcfFile = new VCFFileReader( vcfInputFile );
    //
    //                VariantContextWriter bcfWriter = new VariantContextWriterBuilder().setOutputFile( bcfOutputFile )
    //                        .setReferenceDictionary( vcfFile.getFileHeader().getSequenceDictionary() ).build();
    //
    //        )
    //        {
    //            bcfWriter.writeHeader( vcfFile.getFileHeader() );
    //
    //            for ( VariantContext vc : vcfFile.iterator().toList() )
    //            {
    //                assertEquals( vc.getGenotypes().stream().filter( Genotype::isPhased ).count(), 2 );
    //                bcfWriter.add( vc );
    //            }
    //            bcfWriter.close();
    //
    //            // Reading the VCF and writing it to a BCF
    //            final File vcfOutputFile = TempFiles.file( "testWriteAndReadBCFHeaderless.vcf" );
    //            vcfOutputFile.deleteOnExit();
    //
    //            try (final PositionalBufferedStream headerPbs = new PositionalBufferedStream( new FileInputStream( bcfOutputFile ) );
    //                    VariantContextWriter vcfWriter = new VariantContextWriterBuilder().setOutputFile( vcfOutputFile )
    //                            .setReferenceDictionary( vcfFile.getFileHeader().getSequenceDictionary() ).build();)
    //            {
    //                vcfWriter.writeHeader( vcfFile.getFileHeader() );
    //
    //                BCF2Codec codec = new BCF2Codec();
    //                codec.readHeader( headerPbs );
    //                // we use the header information read from identical file with header+body to read just the body of second file
    //
    //                while ( !headerPbs.isDone() )
    //                {
    //                    VariantContext vc = codec.decode( headerPbs );
    //                    assertEquals( vc.getGenotypes().stream().filter( Genotype::isPhased ).count(), 2 );
    //                    vcfWriter.add( vc );
    //                }
    //                vcfWriter.close();
    //            }
    //
    //            try (VCFFileReader vcfOutput = new VCFFileReader( vcfInputFile );)
    //            {
    //                for ( VariantContext vc : vcfOutput.iterator().toList() )
    //                {
    //                    assertEquals( vc.getGenotypes().stream().filter( Genotype::isPhased ).count(), 2 );
    //                }
    //            }
    //        }
    //    }
    //
    //    @Test(expectedExceptions = IllegalStateException.class)
    //    public void testWriteHeaderTwice() throws IOException {
    //        final File bcfOutputFile = File.createTempFile("testWriteAndReadVCF.", ".bcf", tempDir);
    //        bcfOutputFile.deleteOnExit();
    //
    //        final VCFHeader header = createFakeHeader();
    //        // prevent writing header twice
    //        try (final VariantContextWriter writer = new VariantContextWriterBuilder()
    //                .setOutputFile(bcfOutputFile).setReferenceDictionary(header.getSequenceDictionary())
    //                .unsetOption(Options.INDEX_ON_THE_FLY)
    //                .build()) {
    //            writer.writeHeader(header);
    //            writer.writeHeader(header);
    //        }
    //    }
    //
    //    @Test(expectedExceptions = IllegalStateException.class)
    //    public void testChangeHeaderAfterWritingHeader() throws IOException {
    //        final File bcfOutputFile = File.createTempFile("testWriteAndReadVCF.", ".bcf", tempDir);
    //        bcfOutputFile.deleteOnExit();
    //
    //        final VCFHeader header = createFakeHeader();
    //        // prevent changing header if it's already written
    //        try (final VariantContextWriter writer = new VariantContextWriterBuilder()
    //                .setOutputFile(bcfOutputFile).setReferenceDictionary(header.getSequenceDictionary())
    //                .unsetOption(Options.INDEX_ON_THE_FLY)
    //                .build()) {
    //            writer.writeHeader(header);
    //            writer.setHeader(header);
    //        }
    //    }
    //
    //    @Test(expectedExceptions = IllegalStateException.class)
    //    public void testChangeHeaderAfterWritingBody() throws IOException {
    //        final File bcfOutputFile = File.createTempFile("testWriteAndReadVCF.", ".bcf", tempDir);
    //        bcfOutputFile.deleteOnExit();
    //
    //        final VCFHeader header = createFakeHeader();
    //        // prevent changing header if part of body is already written
    //        try (final VariantContextWriter writer = new VariantContextWriterBuilder()
    //                .setOutputFile(bcfOutputFile).setReferenceDictionary(header.getSequenceDictionary())
    //                .unsetOption(Options.INDEX_ON_THE_FLY)
    //                .build()) {
    //            writer.setHeader(header);
    //            writer.add(createVC(header));
    //            writer.setHeader(header);
    //        }
    //    }

    /**
     * create a fake VCF record
     *
     * @param header the VCF header
     * @return a VCFRecord
     */
    private VariantContext createVC(final VCFHeader header, int start, int end)
    {
        final List<Allele> alleles = new ArrayList<>();
        final Map<String, Object> attributes = new HashMap<>();
        final GenotypesContext genotypes = GenotypesContext.create( header.getGenotypeSamples().size() );

        alleles.add( Allele.create( "A", true ) );
        alleles.add( Allele.create( "ACC", false ) );

        attributes.put( "DP", "50" );
        for ( final String name : header.getGenotypeSamples() )
        {
            final Genotype gt = new GenotypeBuilder( name, alleles.subList( 1, 2 ) ).GQ( 0 ).attribute( "BB", "1" ).phased( true ).make();
            genotypes.add( gt );
        }
        return new VariantContextBuilder( "RANDOM", "1", start, end, alleles ).genotypes( genotypes ).attributes( attributes ).make();
    }

}
