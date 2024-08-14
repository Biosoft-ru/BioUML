package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import one.util.streamex.EntryStream;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.aggregate.NumericAggregatorEditor;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.SiteSearchReport.SiteInfo;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.Maps;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.ReferenceTypeSelector;

public class SiteSearchModelSummary extends SiteModelsToProteinsSupport<SiteSearchModelSummary.SiteSearchModelSummaryParameters>
{
    public SiteSearchModelSummary(DataCollection<?> origin, String name)
    {
        super( origin, name, new SiteSearchModelSummaryParameters() );
    }
    
    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        Set<String> nameList = new HashSet<>(getModels());
        String[] models = getParameters().getModels();
        if(models != null)
            nameList.retainAll(Arrays.asList(models));
        String[] matrixNames = nameList.toArray(new String[nameList.size()]);
        TableDataCollection src = getParameters().getSitesCollection().getDataElement(TableDataCollection.class);

        Track yesTrack = DataElementPath.create( DataCollectionUtils
                .getPropertyStrict( src, SiteSearchReport.YES_TRACK_PROPERTY ) ).getDataElement( Track.class );
        SiteSearchTrackInfo trackInfo = new SiteSearchTrackInfo(yesTrack);
        Track promotersTrack = trackInfo.getIntervals();
        if(promotersTrack == null)
        {
            throw new IllegalArgumentException( "Unable to find original sequences track: probably it was deleted" );
        }
        int offset = trackInfo.getIntervalsOffset();
        
        DataCollection<SiteModel> sitesLibrary = getParameters().getSiteModelsCollection().getDataCollection(SiteModel.class);
        
        jobControl.pushProgress(0, 20);
        log.info("Fetching factors...");
        Map<String, Set<Link>> factors = getFactors(sitesLibrary, matrixNames, getParameters().getSpecies());
        if(jobControl.isStopped()) return null;
        jobControl.popProgress();

        jobControl.pushProgress(20, 70);
        log.info("Converting...");
        ReferenceType referenceType = ReferenceTypeRegistry.getReferenceType(getParameters().getTargetType());
        Map<String, String[]> molecules = getMolecules(factors, referenceType, getParameters().getSpecies());
        if(jobControl.isStopped()) return null;
        jobControl.popProgress();
        
        Map<String, Set<String>> references = revertReferences(molecules);
        
        jobControl.pushProgress(70, 80);
        log.info("Generating result...");
        TableDataCollection intermediate = new StandardTableDataCollection( null, "" );
        ColumnModel cm = intermediate.getColumnModel();
        cm.addColumn( "Name", String.class );
        cm.addColumn( "Total count", Integer.class );
        cm.addColumn( "Positions", StringSet.class );
        int idx = src.getColumnModel().optColumnIndex( parameters.getColumnName() );
        if( idx > -1 )
        {
            TableColumn col = src.getColumnModel().getColumn( idx );
            cm.addColumn( col.getName(), col.getValueClass() );
        }
        
        List<SiteInfo<String>> promoterList = new ArrayList<>();
        for(Site site : promotersTrack.getAllSites())
        {
            String name = StreamEx.of( GeneSetToTrack.GENE_NAME_PROPERTY, GeneSetToTrack.GENE_ID_PROPERTY )
                    .map( site.getProperties()::getValueAsString ).nonNull().findFirst().orElseGet( site::getName );
            promoterList.add(new SiteInfo<>(site, name));
        }
        Collections.sort( promoterList );
        
        Map<String, Integer> counts = StreamEx.of( yesTrack.getAllSites().stream() ).select( SiteImpl.class ).map( SiteImpl::getModel )
                .groupingBy( Function.identity(), MoreCollectors.countingInt() );
        Collector<Entry<SiteImpl, SiteInfo<String>>, ?, Map<String, List<Integer>>> internalCollector =
                Collectors.groupingBy( (Entry<SiteImpl, SiteInfo<String>> e) -> e.getValue().rowId,
                        Collectors.mapping( (Entry<SiteImpl, SiteInfo<String>> e) -> e.getKey().getFrom() - e.getValue().from + offset,
                                Collectors.toList() ));
        Collector<Entry<SiteImpl, SiteInfo<String>>, ?, Map<String, Map<String, List<Integer>>>> collector =
                Collectors.groupingBy( (Entry<SiteImpl, SiteInfo<String>> e) -> e.getKey().getModel(),
                        internalCollector );
        Map<String, Map<String, List<Integer>>> positions = StreamEx.of( yesTrack.getAllSites().stream() ).select( SiteImpl.class )
                .cross( s -> SiteInfo.binSearch( promoterList, s ) ).collect( collector );
        Map<String, StringSet> finalPositions = Maps.transformValues( positions,
                map -> EntryStream.of( map ).mapKeyValue( (name, pos) -> name + "(" + StreamEx.of( pos ).sorted().joining( ", " ) + ")" )
                        .sorted().toCollection( StringSet::new ) );
        for(RowDataElement row : src)
        {
            SiteModel sm = sitesLibrary.get( row.getName() );
            String name = sm == null ? "" : sm.getBindingElement().getName();
            Integer count = counts.getOrDefault( row.getName(), 0 );
            StringSet pos = finalPositions.get( row.getName() );
            StreamEx<Object> stream = StreamEx.of( name, count, pos );
            if(idx > -1)
                stream = stream.append( row.getValues()[idx] );
            TableDataCollectionUtils.addRow( intermediate, row.getName(), stream.toArray() );
        }
        
        jobControl.pushProgress(80, 99);
        TableDataCollection resTable = TableDataCollectionUtils.createTableDataCollection(getParameters().getOutputTable());
        fillTable(intermediate, references, resTable);
        if(jobControl.isStopped())
        {
            resTable.getOrigin().remove(resTable.getName());
            return null;
        }
        jobControl.popProgress();
        resTable.finalizeAddition();
        ReferenceTypeRegistry.setCollectionReferenceType(resTable, referenceType.getClass());
        getParameters().getOutputTable().save(resTable);
        return resTable;
    }


    @SuppressWarnings ( "serial" )
    public static class SiteSearchModelSummaryParameters extends SiteModelsToProteinsParameters
    {
        DataElementPath siteSearchResult;

        @PropertyName ( "Result of site search analysis" )
        @PropertyDescription ( "Previously obtained result of site search; must contain summary table with p-value column" )
        public DataElementPath getSiteSearchResult()
        {
            return siteSearchResult;
        }

        public void setSiteSearchResult(DataElementPath siteSearchResult)
        {
            Object oldValue = this.siteSearchResult;
            this.siteSearchResult = siteSearchResult;
            setSitesCollection( siteSearchResult.getChildPath( SiteSearchResult.SUMMARY ) );
            firePropertyChange( "siteSearchResult", oldValue, this.siteSearchResult );
        }
    }

    public static class SiteSearchModelSummaryParametersBeanInfo extends BeanInfoEx2<SiteSearchModelSummaryParameters>
    {
        public SiteSearchModelSummaryParametersBeanInfo()
        {
            super( SiteSearchModelSummaryParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property("siteSearchResult").inputElement( SiteSearchResult.class ).add();
            property("sitesCollection").inputElement( TableDataCollection.class ).hidden().add();
            addHidden(new PropertyDescriptor("defaultProfile", beanClass, "getDefaultProfile", null));
            property("siteModelsCollection").inputElement( SiteModelCollection.class ).hidden().auto( "$defaultProfile$" ).add();
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            add(ReferenceTypeSelector.registerSelector("targetType", beanClass));
            addHidden(new PropertyDescriptorEx("shortTargetType", beanClass, "getShortTargetType", null));
            property( "ignoreNaNInAggregator" ).titleRaw( "Ignore empty values" )
                    .descriptionRaw( "Ignore empty values during aggregator work" ).add();
            property( "aggregator" ).simple().editor( NumericAggregatorEditor.class ).add();
            property( ColumnNameSelector.registerSelector( "columnName", beanClass, "sitesCollection" ) ).hidden( "isAggregatorColumnHidden" )
                    .add();
            property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$sitesCollection$ TFs $shortTargetType$" )
                    .value( DataElementPathEditor.ICON_ID, SiteSearchModelSummaryParameters.class.getMethod( "getIcon" ) ).add();
        }
    }
}
