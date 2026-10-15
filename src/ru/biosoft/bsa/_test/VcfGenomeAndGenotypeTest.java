package ru.biosoft.bsa._test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import junit.framework.TestCase;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.analysis.FilterVCFAnalysis;
import ru.biosoft.bsa.analysis.FilterVCFAnalysisParameters;

/**
 * Genome build detection from a VCF header and genotype classes of "Filter VCF by genotype".
 */
public class VcfGenomeAndGenotypeTest extends TestCase
{
    public void testNormalizeGenomeBuild()
    {
        assertEquals( "hg19", TrackUtils.normalizeGenomeBuild( "hg19" ) );
        assertEquals( "hg19", TrackUtils.normalizeGenomeBuild( "GRCh37.p13" ) );
        assertEquals( "hg19", TrackUtils.normalizeGenomeBuild( "file:///ref/human_g1k_v37.fasta" ) );
        assertEquals( "hg19", TrackUtils.normalizeGenomeBuild( "hs37d5" ) );
        assertEquals( "hg38", TrackUtils.normalizeGenomeBuild( "GRCh38" ) );
        assertEquals( "hg38", TrackUtils.normalizeGenomeBuild( "/data/Homo_sapiens_assembly38_hg38.fa" ) );
        assertEquals( "hg18", TrackUtils.normalizeGenomeBuild( "NCBI36" ) );
        assertEquals( "mm10", TrackUtils.normalizeGenomeBuild( "GRCm38" ) );
        assertNull( TrackUtils.normalizeGenomeBuild( "hg190" ) );
        assertNull( TrackUtils.normalizeGenomeBuild( "unknown" ) );
        assertNull( TrackUtils.normalizeGenomeBuild( null ) );
    }

    public void testDetectVcfGenomeBuild() throws Exception
    {
        assertEquals( "hg19", detect( "##fileformat=VCFv4.2\n##source=Genotek\n##reference=hg19\n#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\n" ) );
        assertEquals( "hg38", detect( "##fileformat=VCFv4.2\n##assembly=GRCh38\n#CHROM\tPOS\n" ) );
        assertEquals( "hg19", detect( "##fileformat=VCFv4.1\n##contig=<ID=1,length=249250621,assembly=b37>\n#CHROM\tPOS\n" ) );
        // ##reference wins over the contig assembly
        assertEquals( "hg38", detect( "##contig=<ID=1,assembly=b37>\n##reference=GRCh38\n#CHROM\tPOS\n" ) );
        assertNull( detect( "##fileformat=VCFv4.2\n#CHROM\tPOS\n1\t752721\trs3131972\tA\tG\t.\t.\t.\n" ) );
    }

    private static String detect(String content) throws Exception
    {
        File file = File.createTempFile( "genome", ".vcf" );
        try
        {
            Files.write( file.toPath(), content.getBytes( StandardCharsets.UTF_8 ) );
            return TrackUtils.detectVcfGenomeBuild( file );
        }
        finally
        {
            file.delete();
        }
    }

    public void testClassifyGenotype()
    {
        for( String gt : new String[] {"0/1", "1/0", "0|1", "1|0", "1/2", "0/2"} )
            assertEquals( gt, FilterVCFAnalysis.GT_HETEROZYGOTE, FilterVCFAnalysis.classifyGenotype( gt ) );
        for( String gt : new String[] {"1/1", "1|1", "2/2", "1"} )
            assertEquals( gt, FilterVCFAnalysis.GT_HOMOZYGOTE, FilterVCFAnalysis.classifyGenotype( gt ) );
        for( String gt : new String[] {"0/0", "0|0", "0", "./.", ".", "0/.", "1/.", "", null, "A/G"} )
            assertEquals( String.valueOf( gt ), FilterVCFAnalysis.GT_OTHER, FilterVCFAnalysis.classifyGenotype( gt ) );
    }

    public void testModes()
    {
        assertTrue( FilterVCFAnalysis.matches( FilterVCFAnalysisParameters.MODE_BOTH, "1" ) );
        assertTrue( FilterVCFAnalysis.matches( FilterVCFAnalysisParameters.MODE_BOTH, "0|1" ) );
        assertFalse( FilterVCFAnalysis.matches( FilterVCFAnalysisParameters.MODE_BOTH, "0" ) );
        assertTrue( FilterVCFAnalysis.matches( FilterVCFAnalysisParameters.MODE_HOMOZYGOTE, "1" ) );
        assertFalse( FilterVCFAnalysis.matches( FilterVCFAnalysisParameters.MODE_HOMOZYGOTE, "0/1" ) );
        assertTrue( FilterVCFAnalysis.matches( FilterVCFAnalysisParameters.MODE_HETEROZYGOTE, "1|0" ) );
        assertFalse( FilterVCFAnalysis.matches( FilterVCFAnalysisParameters.MODE_HETEROZYGOTE, "1/1" ) );
    }
}
