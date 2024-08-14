package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

//import biouml.plugins.genomeenhancer.analysis.FindRegulatoryRegionsWithMutations.Parameters;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

@ClassIcon ( "resources/SiteSearchReport.gif" )
public class SiteSearchReportOnMatrices extends AnalysisMethodSupport<SiteSearchReportOnMatrices.Parameters>
{
    public SiteSearchReportOnMatrices(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkGreater( "topModels", 1 );
        //TODO: validate
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        TableDataCollection summary = parameters.getMatrixTable().getDataElement( TableDataCollection.class );
        Track track = getParameters().getTrack().getDataElement( Track.class );
        String column1 = getParameters().isAscending() ? getParameters().getPValueColumn() : "";
        String column2 = getParameters().isAscending() ? "" : getParameters().getPValueColumn();
        String title = parameters.getTitleProperty().equals( TrackPropertiesSelector.NO_TITLE ) ? null : parameters.getTitleProperty();
        Object result = SiteSearchReportAnalysis.generateReport( summary, column1, column2, track, parameters.getTopModels(),
                parameters.isAddPositions(), parameters.getTarget(), jobControl, title, parameters.isUseOriginalIds() );
        if( result instanceof TableDataCollection && parameters.isSitesColumnHidden() )
        {
            ( (TableDataCollection)result ).getColumnModel().getColumn( "Sites view" ).setHidden( true );
        }
        return result;
    }


    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath matrixTable;
        private DataElementPath track;
        private DataElementPath target;
        private String pValueColumn;
        private boolean ascending = true;
        private boolean sitesColumnHidden = false;
        private String titleProperty = TrackPropertiesSelector.NO_TITLE;
        int topModels = 3;
        boolean addPositions = false;
        boolean useOriginalIds = false;

        @PropertyName ( "Table of matrices" )
        @PropertyDescription ( "Summary table obtained as result of site search, site search optimization, or search for enriched TFBSs. Must contain p-value column" )
        public DataElementPath getMatrixTable()
        {
            return matrixTable;
        }

        public void setMatrixTable(DataElementPath matrixTable)
        {
            Object oldValue = this.matrixTable;
            this.matrixTable = matrixTable;
            firePropertyChange( "result", oldValue, this.matrixTable );
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

        @PropertyName ( "Track result of site search analysis" )
        @PropertyDescription ( "Track with sites found by site search analysis" )
        public DataElementPath getTrack()
        {
            return track;
        }

        public void setTrack(DataElementPath track)
        {
            Object oldValue = this.track;
            this.track = track;
            this.titleProperty = TrackPropertiesSelector.NO_TITLE;
            firePropertyChange( "track", oldValue, track );
        }

        @PropertyName ( "Sorting column" )
        @PropertyDescription ( "Use sorting column to order table for top matrices selection." )
        public String getPValueColumn()
        {
            return pValueColumn;
        }

        public void setPValueColumn(String pValueColumn)
        {
            Object oldValue = this.pValueColumn;
            this.pValueColumn = pValueColumn;
            firePropertyChange( "pValueColumn", oldValue, pValueColumn );
        }

        @PropertyName ( "Sort order ascending" )
        @PropertyDescription ( "If checked, column will be sorted in ascending order." )
        public boolean isAscending()
        {
            return ascending;
        }

        public void setAscending(boolean isAscending)
        {
            Object oldValue = this.ascending;
            this.ascending = isAscending;
            firePropertyChange( "ascending", oldValue, isAscending );
        }

        @PropertyName ( "Hide sites column" )
        @PropertyDescription ( "For table optimization sites column can be hidden. To show it use Columns viewpart." )
        public boolean isSitesColumnHidden()
        {
            return sitesColumnHidden;
        }

        public void setSitesColumnHidden(boolean sitesColumnHidden)
        {
            Object oldValue = this.sitesColumnHidden;
            this.sitesColumnHidden = sitesColumnHidden;
            firePropertyChange( "sitesColumnHidden", oldValue, sitesColumnHidden );
        }

        @PropertyName ( "Title property" )
        @PropertyDescription ( "Use this property from promoters track to generate output rows titles" )
        public String getTitleProperty()
        {
            return titleProperty;
        }

        public void setTitleProperty(String titleProperty)
        {
            Object oldValue = this.titleProperty;
            this.titleProperty = titleProperty;
            firePropertyChange( "titleProperty", oldValue, titleProperty );
        }

        public boolean isTitlePropertyHidden()
        {
            return track == null;
        }

        @PropertyName ( "Use original sequence IDs" )
        public boolean isUseOriginalIds()
        {
            return useOriginalIds;
        }

        public void setUseOriginalIds(boolean useOriginalIds)
        {
            Object oldValue = this.useOriginalIds;
            this.useOriginalIds = useOriginalIds;
            firePropertyChange( "useOriginalIds", oldValue, useOriginalIds );
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
            property( "matrixTable" ).inputElement( TableDataCollection.class ).add();
            add( ColumnNameSelector.registerNumericSelector( "pValueColumn", beanClass, "matrixTable", false ) );
            add( "ascending" );
            property( "track" ).inputElement( Track.class ).add();
            add( "topModels" );
            add( "addPositions" );
            addExpert( "sitesColumnHidden" );
            property( "titleProperty" ).hidden( "isTitlePropertyHidden" ).expert().editor( TrackPropertiesSelector.class ).canBeNull()
                    .add();
            addExpert( "useOriginalIds" );
            property( "target" ).outputElement( TableDataCollection.class ).auto( "$matrixTable$ report top $topModels$" ).add();
        }
    }

    public static class TrackPropertiesSelector extends StringTagEditor
    {
        DataElementPath lastPath;
        String[] lastResult;
        public static String NO_TITLE = "(no selection)";

        @Override
        public String[] getTags()
        {
            DataElementPath path = null;
            try
            {
                path = ( (Parameters)getBean() ).getTrack();
                if( lastPath != null && path != null && path.equals( lastPath ) )
                    return lastResult;
            }
            catch( Exception e )
            {
                return new String[0];
            }
            List<String> result = new ArrayList<>();
            try
            {
                if( path != null )
                {
                    DataCollection trackDC = path.optDataCollection();
                    String intervals = trackDC.getInfo().getProperty( SiteSearchAnalysis.INTERVALS_COLLECTION_PROPERTY );
                    if( intervals != null )
                    {
                        DataElement de = DataElementPath.create( intervals ).optDataElement();
                        if( de != null && de instanceof Track )
                        {
                            DataCollection<Site> allSites = ( (Track)de ).getAllSites();
                            Iterator<String> nameIterator = allSites.iterator().next().getProperties().nameIterator();
                            while( nameIterator.hasNext() )
                            {
                                result.add( nameIterator.next() );
                            }
                        }
                    }
                }
            }
            catch( Exception e )
            {
            }
            lastPath = path;
            result.add( 0, NO_TITLE );
            lastResult = result.toArray( new String[result.size()] );
            return lastResult;
        }
    }
}
