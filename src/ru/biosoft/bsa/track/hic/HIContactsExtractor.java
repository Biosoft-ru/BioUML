package ru.biosoft.bsa.track.hic;

import java.util.Iterator;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.javastraw.reader.Dataset;
import ru.biosoft.javastraw.reader.basics.Chromosome;
import ru.biosoft.javastraw.reader.block.ContactRecord;
import ru.biosoft.javastraw.reader.mzd.Matrix;
import ru.biosoft.javastraw.reader.mzd.MatrixZoomData;
import ru.biosoft.javastraw.reader.type.HiCZoom;
import ru.biosoft.javastraw.reader.type.NormalizationType;
import ru.biosoft.javastraw.tools.HiCFileTools;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class HIContactsExtractor extends AnalysisMethodSupport<HIContactsExtractor.Parameters>
{

    public HIContactsExtractor(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        HICTrack de = parameters.getHicPath().getDataElement( HICTrack.class );
        Dataset ds = HiCFileTools.extractDatasetForCLT( de.getFilePath(), false, true, false );
        String resolution = parameters.getResolution();
        HiCZoom zoom = null;
        for ( HiCZoom cur : ds.getAllPossibleResolutions() )
            if( resolution.equals( cur.toString() ) )
                zoom = cur;
        if( zoom == null )
            return null;

        int binSize = zoom.getBinSize();

        String normStr = parameters.getNormalization();
        if( normStr == null )
            return null;
        NormalizationType norm = ds.getNormalizationTypesMap().get( normStr );

        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getResultTablePath() );
        ColumnModel cm = result.getColumnModel();
        //<in_chrom><in_from><in_to><out1_crom><out1_from><out1_to><out1_score><out2_crom><out2_from><out2_to><out2_score>......
        cm.addColumn( "int1 chrom", DataType.Text );
        cm.addColumn( "int1 from", DataType.Integer );
        cm.addColumn( "int1 to", DataType.Integer );
        cm.addColumn( "int2 chrom", DataType.Text );
        cm.addColumn( "int2 from", DataType.Integer );
        cm.addColumn( "int2 to", DataType.Integer );

        cm.addColumn( "Score", DataType.Float );

        Chromosome[] chromosomes = ds.getChromosomeHandler().getChromosomeArrayWithoutAllByAll();
        // to iterate over the whole genome
        for ( int i = 0; i < chromosomes.length; i++ )
        {
            for ( int j = i; i < chromosomes.length; i++ )
            {
                Matrix matrix = ds.getMatrix( chromosomes[i], chromosomes[j] );
                if( matrix == null )
                    continue;
                MatrixZoomData zd = matrix.getZoomData( zoom );
                if( zd == null )
                    continue;

                Iterator<ContactRecord> iterator = zd.getNormalizedIterator( norm );
                while ( iterator.hasNext() )
                {
                    ContactRecord record = iterator.next();
                    // now do whatever you want with the contact record
                    int binX = record.getBinX();
                    int binY = record.getBinY();
                    float counts = record.getCounts();

                    // binX and binY are in BIN coordinates, not genome coordinates
                    // to switch, we can just multiply by the resolution
                    int genomeX = binX * binSize;
                    int genomeY = binY * binSize;


                    if( counts > 0 )
                    { // will skip NaNs

                        // do task
                        //System.out.println( genomeX + " " + genomeY + " " + counts );

                        Object[] values = new Object[] { chromosomes[i].getName(), genomeX, genomeX + binSize, chromosomes[j].getName(), genomeY, genomeY + binSize, counts };
                        TableDataCollectionUtils.addRow( result, record.getKey( norm ), values );

                    }
                }

                if( i == j )
                {
                    // intra-chromosomal region
                }
                else
                {
                    // inter-chromosomal region
                }
            }
        }

        result.finalizeAddition();
        parameters.getResultTablePath().save( result );
        return result;
    }


    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath hicPath;
        private String resolution;
        private String[] allZooms = new String[0];
        private String normalization;
        private String[] allNorms = new String[0];
        private DataElementPath resultTablePath;

        @PropertyName(".hic file")
        public DataElementPath getHicPath()
        {
            return hicPath;
        }

        public void setHicPath(DataElementPath hic)
        {
            Object oldValue = this.hicPath;
            if( (oldValue == null && hic == null) || (hic != null && hic.equals( this.hicPath )) )
                return;
            this.hicPath = hic;
            resetResolution();
            firePropertyChange( "hicPath", oldValue, this.hicPath );
        }

        public String getResolution()
        {
            return resolution;
        }

        @PropertyName("Resolution")
        public void setResolution(String resolution)
        {
            String oldValue = this.resolution;
            this.resolution = resolution;
            firePropertyChange( "this.resolution", oldValue, this.resolution );
        }

        private void resetResolution()
        {
            try
            {
                HICTrack de = hicPath.getDataElement( HICTrack.class );
                Dataset ds = HiCFileTools.extractDatasetForCLT( de.getFilePath(), false, true, false );
                List<HiCZoom> zooms = ds.getAllPossibleResolutions();
                allZooms = new String[zooms.size()];
                for ( int i = 0; i < zooms.size(); i++ )
                    allZooms[i] = zooms.get( i ).toString();
                allNorms = ds.getNormalizationTypesMap().keySet().toArray( new String[] {} );

            }
            catch (Exception e)
            {
                allZooms = new String[0];
                allNorms = new String[0];
            }
        }

        public String[] getZooms()
        {
            return allZooms;
        }

        @PropertyName("Result table")
        public DataElementPath getResultTablePath()
        {
            return resultTablePath;
        }

        public void setResultTablePath(DataElementPath resultTablePath)
        {
            Object oldValue = this.resultTablePath;
            this.resultTablePath = resultTablePath;
            firePropertyChange( "resultTablePath", oldValue, this.resultTablePath );
        }

        @PropertyName("Normalization")
        public String getNormalization()
        {
            return normalization;
        }

        public void setNormalization(String normalization)
        {
            String oldValue = this.normalization;
            this.normalization = normalization;
            firePropertyChange( "normalization", oldValue, this.normalization );
        }

        public String[] getNorms()
        {
            return allNorms;
        }

    }

    public static class ResolutionSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ((Parameters) getBean()).getZooms();
        }
    }

    public static class NormalizationSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ((Parameters) getBean()).getNorms();
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "hicPath" ).inputElement( HICTrack.class ).structureChanging().add();
            property( "resolution" ).editor( ResolutionSelector.class ).simple().add();
            property( "normalization" ).editor( NormalizationSelector.class ).simple().add();
            property( "resultTablePath" ).outputElement( TableDataCollection.class ).add();
        }
    }


}
