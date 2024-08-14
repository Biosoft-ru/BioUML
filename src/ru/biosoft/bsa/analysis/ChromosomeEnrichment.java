package ru.biosoft.bsa.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.Gene;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.GenomeSelector;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WithSite;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ChromosomeEnrichment extends AnalysisMethodSupport<ChromosomeEnrichment.Parameters>
{
    public ChromosomeEnrichment(DataCollection<?> origin, String name)
    {
        super(origin, name, new Parameters());
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        DataCollection<Gene> genes = TrackUtils.getGenesCollection(parameters.getGenome().getDbSelector().getBasePath());
        Set<String> ensIds = new HashSet<>( parameters.getInputGeneSet().getDataCollection().getNameList() );

        Map<String, Integer> targetCountByChrom = new HashMap<>();
        Map<String, Integer> totalCountByChrom = new HashMap<>();

        for( Gene gene : genes )
        {
            String id = gene.getName();

            if ( gene instanceof WithSite )
            {
                String chr = ((WithSite) gene).getSite().getOriginalSequence().getName();

                if ( !totalCountByChrom.containsKey(chr) )
                    totalCountByChrom.put(chr, 0);
                totalCountByChrom.put(chr, totalCountByChrom.get(chr) + 1);

                if ( !targetCountByChrom.containsKey(chr) )
                    targetCountByChrom.put(chr, 0);
                if ( ensIds.contains(id) )
                    targetCountByChrom.put(chr, targetCountByChrom.get(chr) + 1);
            }
        }
        
        TableDataCollection resultTable = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        resultTable.getColumnModel().addColumn( "Target count", Integer.class );
        resultTable.getColumnModel().addColumn( "Total", Integer.class );
        resultTable.getColumnModel().addColumn( "Percent", Double.class );
        
        for( Map.Entry<String, Integer> entry : totalCountByChrom.entrySet() )
        {
            String chr = entry.getKey();
            int total = entry.getValue();
            int target = targetCountByChrom.get( chr );
            double percent = ( target * 100.0 ) / total;
            TableDataCollectionUtils.addRow( resultTable, chr, new Object[] { target, total, percent } );
        }
        parameters.getOutputTable().save( resultTable );

        return resultTable;
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputGeneSet, outputImage, outputTable;
        private GenomeSelector genome = new GenomeSelector();

        @PropertyName( "Input genes" )
        @PropertyDescription( "Input genes" )
        public DataElementPath getInputGeneSet()
        {
            return inputGeneSet;
        }

        public void setInputGeneSet(DataElementPath inputGeneSet)
        {
            Object oldValue = this.inputGeneSet;
            this.inputGeneSet = inputGeneSet;
            firePropertyChange( "inputGeneSet", oldValue, inputGeneSet );
        }

        @PropertyName( "Genome" )
        @PropertyDescription( "Genome" )
        public GenomeSelector getGenome()
        {
            return genome;
        }

        public void setGenome(GenomeSelector genome)
        {
            Object oldValue = this.genome;
            this.genome = genome;
            firePropertyChange( "genome", oldValue, genome );
        }

        @PropertyName( "Output image" )
        @PropertyDescription( "Output image" )
        public DataElementPath getOutputImage()
        {
            return outputImage;
        }

        public void setOutputImage(DataElementPath outputImage)
        {
            Object oldValue = this.outputImage;
            this.outputImage = outputImage;
            firePropertyChange( "outputImage", oldValue, outputImage );
        }

        @PropertyName( "Output table" )
        @PropertyDescription( "Output table" )
        public DataElementPath getOutputTable()
        {
            return outputTable;
        }

        public void setOutputTable(DataElementPath outputTable)
        {
            Object oldValue = this.outputTable;
            this.outputTable = outputTable;
            firePropertyChange( "outputTable", oldValue, outputTable );
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
            add( DataElementPathEditor.registerInput( "inputGeneSet", beanClass, TableDataCollection.class, ReferenceTypeRegistry
                    .getReferenceType( "Genes: Ensembl" ).getClass() ) );
            add( "genome" );
            property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$inputGeneSet$ chromosomes" ).add();
            property( "outputImage" ).outputElement( ImageDataElement.class ).auto( "$outputTable$ image" ).add();
        }
    }

}
