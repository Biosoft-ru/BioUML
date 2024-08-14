package biouml.plugins.gtrd.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.ObjectUtils;

import biouml.plugins.gtrd.ChIPseqExperiment;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CompareExperiments extends AnalysisMethodSupport<CompareExperiments.Parameters>
{
    public CompareExperiments(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath experimentCollection1, experimentCollection2, unmatchedTable;

        public DataElementPath getExperimentCollection1()
        {
            return experimentCollection1;
        }

        public void setExperimentCollection1(DataElementPath experimentCollection1)
        {
            Object oldValue = this.experimentCollection1;
            this.experimentCollection1 = experimentCollection1;
            firePropertyChange( "experimentCollection1", oldValue, experimentCollection1 );
        }

        public DataElementPath getExperimentCollection2()
        {
            return experimentCollection2;
        }

        public void setExperimentCollection2(DataElementPath experimentCollection2)
        {
            Object oldValue = this.experimentCollection2;
            this.experimentCollection2 = experimentCollection2;
            firePropertyChange( "experimentCollection2", oldValue, experimentCollection2 );
        }

        public DataElementPath getUnmatchedTable()
        {
            return unmatchedTable;
        }

        public void setUnmatchedTable(DataElementPath unmatchedTable)
        {
            Object oldValue = this.unmatchedTable;
            this.unmatchedTable = unmatchedTable;
            firePropertyChange( "unmatchedTable", oldValue, unmatchedTable );
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
            add( DataElementPathEditor.registerInputChild( "experimentCollection1", beanClass, ChIPseqExperiment.class ) );
            add( DataElementPathEditor.registerInputChild( "experimentCollection2", beanClass, ChIPseqExperiment.class ) );
            property( "unmatchedTable" ).outputElement( TableDataCollection.class ).add();
        }

    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getUnmatchedTable() );
        result.getColumnModel().addColumn( "path", String.class );

        List<ChIPseqExperiment> c1List = new ArrayList<>(DataCollectionUtils.asCollection(parameters.getExperimentCollection1(),
                ChIPseqExperiment.class));
        List<ChIPseqExperiment> c2List = new ArrayList<>(DataCollectionUtils.asCollection(parameters.getExperimentCollection2(),
                ChIPseqExperiment.class));


        if( checkDuplicates( c1List ) || checkDuplicates( c2List ) )
            throw new Exception( "Duplicates found" );


        Iterator<ChIPseqExperiment> it1 = c1List.iterator();

        while( it1.hasNext() )
        {
            ChIPseqExperiment e1 = it1.next();
            Iterator<ChIPseqExperiment> it2 = c2List.iterator();
            while( it2.hasNext() )
            {
                ChIPseqExperiment e2 = it2.next();
                if( sameExperiments( e1, e2 ) )
                {
                    it1.remove();
                    it2.remove();
                    break;
                }
            }
        }

        int i = 0;
        for( List<ChIPseqExperiment> list : Arrays.asList( c1List, c2List ) )
            for( ChIPseqExperiment e : list )
                TableDataCollectionUtils.addRow( result, String.valueOf( i++ ), new Object[] {DataElementPath.create( e ).toString()} );

        return result;
    }


    private boolean checkDuplicates(List<ChIPseqExperiment> experiments) throws Exception
    {
        boolean anyDuplicates = false;
        for( ChIPseqExperiment e1 : experiments )
            for( ChIPseqExperiment e2 : experiments )
                if( !e1.getName().equals( e2.getName() ) && sameExperiments( e1, e2 ) )
                {
                    log.log(Level.SEVERE,  DataElementPath.create( e1 ) + " is duplicate of " + DataElementPath.create( e2 ) );
                    anyDuplicates = true;
                }
        return anyDuplicates;
    }

    private static boolean sameExperiments(ChIPseqExperiment e1, ChIPseqExperiment e2)
    {
        if( !ObjectUtils.equals( e1.getAntibody(), e2.getAntibody() ) )
            return false;
        if( !ObjectUtils.equals( e1.getCell().getName(), e2.getCell().getName() ) )
            return false;
        if( !ObjectUtils.equals( e1.getTfUniprotId(), e2.getTfUniprotId() ) )
            return false;
        if( !ObjectUtils.equals( e1.getTreatment(), e2.getTreatment() ) )
            return false;
        if( !ObjectUtils.equals( e1.getSpecie().getLatinName(), e2.getSpecie().getLatinName() ) )
            return false;

        ChIPseqExperiment ctrl1 = null;
        if( e1.getControl() != null )
            ctrl1 = e1.getControl().optDataElement(ChIPseqExperiment.class);

        ChIPseqExperiment ctrl2 = null;
        if( e2.getControl() != null )
            ctrl2 = e2.getControl().optDataElement(ChIPseqExperiment.class);

        if( ctrl1 == null )
        {
            if( ctrl2 != null )
                return false;
        }
        else if( ctrl2 == null )
        {
            return false;
        }
        else if( !sameExperiments( ctrl1, ctrl2 ) )
            return false;


        if( !new HashSet<>( e1.getExternalRefs() )
                .equals( new HashSet<>( e2.getExternalRefs() ) ) )
            return false;

        if( !new HashSet<>( e1.getReadsURLs() ).equals( new HashSet<>( e2.getReadsURLs() ) ) )
            return false;

        return true;
    }
}
