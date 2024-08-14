package ru.biosoft.bsa._test;

import java.io.File;

import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.DataElementExporterRegistry.ExporterInfo;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.bsa.exporter.DefaultTrackExporter;
import ru.biosoft.bsa.exporter.DefaultTrackExporter.TrackExporterProperties;
import ru.biosoft.bsa.exporter.TrackExporter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.TempFiles;

public class ExportTest extends AbstractBioUMLTest
{
    public void testExport() throws Exception
    {
        BSATestUtils.createRepository();
        DataCollection<?> dcEns = CollectionFactory.getDataCollection( "databases/Ensembl" );
        assertNotNull("Ensembl data collection", dcEns);
        DataElement deSeq = CollectionFactory.getDataElement("databases/Ensembl/Sequences/chromosomes NCBI36/21");
        assertTrue("Chromosome databases/Ensembl/Sequences/chromosomes NCBI36/21", deSeq instanceof AnnotatedSequence);
        DataElement deTrack = CollectionFactory.getDataElement("databases/Ensembl/Tracks/Genes");
        assertTrue("Track databases/Ensembl/Tracks/Genes", deTrack instanceof Track);

        TrackRegion tr = new TrackRegion((Track)deTrack, "databases/Ensembl/Sequences/chromosomes NCBI36/21", 10000000, 20000000);
        ExporterInfo[] exporterInfo = DataElementExporterRegistry.getExporterInfo("BED format (*.bed)", tr);
        assertTrue("BED exporter exists", exporterInfo != null && exporterInfo.length > 0);
        DataElementExporter bedExporter = exporterInfo[0].cloneExporter();
        TrackExporter gffExporter = new DefaultTrackExporter();
        assertTrue("GFF exporter exists", gffExporter.init("GFF format", "gff"));
        TrackExporter strangeExporter = new DefaultTrackExporter();
        assertFalse("Some strange exporter doesn't exist", strangeExporter.init("AYHVIHDKJAHJKLASDHKJA format", "AYHVIHDKJAHJKLASDHKJA"));
        assertTrue("GFF exporter accepts TrackRegion", gffExporter.accept(tr));
        File bedFile = TempFiles.file("testExport.bed");
        modifyProperties( bedExporter, tr, bedFile );
        bedExporter.doExport(tr, bedFile);
        File gffFile = TempFiles.file("testExport.gff");
        FunctionJobControl fjc = new FunctionJobControl(null);
        modifyProperties( gffExporter, tr, gffFile );
        gffExporter.doExport(tr, gffFile, fjc);
        assertEquals(100, fjc.getPreparedness());
        assertFileEquals( "bedFile", new File( "ru/biosoft/bsa/_test/resources/21.GeneTrack.10000000-20000000.bed" ), bedFile );
        assertFileEquals( "gffFile", new File( "ru/biosoft/bsa/_test/resources/21.GeneTrack.10000000-20000000.gff" ), gffFile );
        bedFile.delete();
        gffFile.delete();
    }

    private void modifyProperties(DataElementExporter exporter, DataElement de, File file)
    {
        Object properties = exporter.getProperties( de, file );
        if( properties instanceof TrackExporterProperties )
        {
            ( (TrackExporterProperties)properties ).setPrependChrPrefix( true );
        }
    }
}
