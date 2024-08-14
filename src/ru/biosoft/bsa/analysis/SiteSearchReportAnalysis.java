package ru.biosoft.bsa.analysis;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon("resources/SiteSearchReport.gif")
public class SiteSearchReportAnalysis extends AnalysisMethodSupport<SiteSearchReportAnalysis.SiteSearchReportParameters>
{
    public SiteSearchReportAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new SiteSearchReportParameters() );
    }
    
    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkGreater( "topModels", 1 );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection summary = parameters.getResult().getChildPath( SiteSearchResult.SUMMARY ).getDataElement( TableDataCollection.class );
        Track track = DataElementPath.create( DataCollectionUtils.getPropertyStrict( summary, SiteSearchReport.YES_TRACK_PROPERTY ) )
                .getDataElement( Track.class );
        return generateReport( summary, SiteSearchSummary.P_VALUE_COLUMN, SiteSearchSummary.SITES_DENSITY_PER_1000BP, track,
                parameters.getTopModels(), parameters.isAddPositions(),
                parameters.getTarget(), jobControl, null, false );
    }

    protected static Object generateReport(TableDataCollection modelsTable, String sortColumn, String sortColumn2, Track sitesTrack,
            int numModels,
            boolean isAddPositions, DataElementPath resultPath, JobControl jobControl, String promoterTitle, boolean useOriginalIds)
    {
        int pvalueCol = modelsTable.getColumnModel().optColumnIndex( sortColumn );
        ToDoubleFunction<RowDataElement> sortFn;
        if( pvalueCol == -1 )
        {
            int sitesCol = modelsTable.getColumnModel().getColumnIndex( sortColumn2 );
            sortFn = row -> - ( (Number)row.getValues()[sitesCol] ).doubleValue();
        }
        else
        {
            sortFn = row -> ( (Number)row.getValues()[pvalueCol] ).doubleValue();
        }
        List<String> models = modelsTable.stream().sorted( Comparator.comparingDouble( sortFn ) ).limit( numModels )
                .map( RowDataElement::getName ).collect( Collectors.toList() );
        SiteSearchReport ssr = new SiteSearchReport( resultPath, sitesTrack, models, isAddPositions, useOriginalIds );
        if( promoterTitle != null )
            ssr.setTitleProperty( promoterTitle );
        ssr.addListener( new JobControlListenerAdapter()
        {
            @Override
            public void valueChanged(JobControlEvent event)
            {
                jobControl.setPreparedness( ssr.getPreparedness() );
            }
        } );
        ssr.run();
        return ssr.getResult();
    }

    @SuppressWarnings ( "serial" )
    public static class SiteSearchReportParameters extends AbstractAnalysisParameters
    {
        DataElementPath result;
        DataElementPath target;
        int topModels = 3;
        boolean addPositions = true;

        @PropertyName ( "Result of site search analysis" )
        @PropertyDescription ( "Previously obtained result of site search; must contain summary table with p-value column" )
        public DataElementPath getResult()
        {
            return result;
        }

        public void setResult(DataElementPath result)
        {
            Object oldValue = this.result;
            this.result = result;
            firePropertyChange( "result", oldValue, this.result );
        }

        @PropertyName ( "Number of best models" )
        @PropertyDescription ( "Number of best models (according to p-value) to include to the report" )
        public int getTopModels()
        {
            return topModels;
        }

        public void setTopModels(int topModels)
        {
            Object oldValue = this.topModels;
            this.topModels = topModels;
            firePropertyChange( "topModels", oldValue, this.topModels );
        }

        @PropertyName ( "Target report path" )
        @PropertyDescription ( "Path to save report" )
        public DataElementPath getTarget()
        {
            return target;
        }

        public void setTarget(DataElementPath target)
        {
            Object oldValue = this.target;
            this.target = target;
            firePropertyChange( "target", oldValue, this.target );
        }

        @PropertyName("Add columns with site positions")
        @PropertyDescription ( "If checked, site position columns will be added" )
        public boolean isAddPositions()
        {
            return addPositions;
        }

        public void setAddPositions(boolean addPositions)
        {
            Object oldValue = this.addPositions;
            this.addPositions = addPositions;
            firePropertyChange( "addPositions", oldValue, this.addPositions );
        }
    }

    public static class SiteSearchReportParametersBeanInfo extends BeanInfoEx2<SiteSearchReportParameters>
    {
        public SiteSearchReportParametersBeanInfo()
        {
            super( SiteSearchReportParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "result" ).inputElement( SiteSearchResult.class ).add();
            property( "topModels" ).add();
            property( "addPositions" ).add();
            property( "target" ).outputElement( TableDataCollection.class ).auto( "$result$/Top $topModels$" ).add();
        }
    }
}
