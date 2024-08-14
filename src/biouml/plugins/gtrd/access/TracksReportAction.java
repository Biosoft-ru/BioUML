package biouml.plugins.gtrd.access;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biouml.plugins.ensembl.analysis.CountSiteAggregator;
import biouml.plugins.ensembl.analysis.SiteAggregator;
import biouml.plugins.ensembl.analysis.StructureSiteAggregator;
import biouml.plugins.ensembl.analysis.TrackToGeneSet;
import biouml.plugins.ensembl.analysis.TrackToGeneSetParameters;
import biouml.plugins.ensembl.analysis.TrackToGeneSetParametersBeanInfo;

import ru.biosoft.jobcontrol.JobControl;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.exception.InvalidSelectionException;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.bsa.classification.ClassificationUnit;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.treetable.TreeTableElement;

/**
 * @author lan
 *
 */
public class TracksReportAction extends BackgroundDynamicAction
{

    /**
     * Table column property to store path to tracks
     */
    private static final String TRACKS_PATH = "tracksPath";
    private static final String TABLE_REPORTS_PATH = "databases/GTRD/Results/report";
    
    protected DataElementPathSet getTracks(TreeTableElement model, List<DataElement> selectedItems)
    {
        TableDataCollection table = (TableDataCollection)model.getTable();
        for(TableColumn column : table.getColumnModel())
        {
            if(column.getValue(TRACKS_PATH) != null)
            {
                Set<String> names = new HashSet<>();
                for(DataElement element: selectedItems)
                {
                    try
                    {
                        Object val = (table.get(element.getName())).getValue(column.getName());
                        if(val != null && !val.toString().matches("\\d+")) names.addAll(Arrays.asList(val.toString().split(",\\s*")));
                    }
                    catch( Exception e )
                    {
                    }
                }
                return new DataElementPathSet(DataElementPath.create(column.getValue(TRACKS_PATH)), names);
            }
        }
        return null;
    }

    @Override
    public JobControl getJobControl(Object model, List<DataElement> selectedItems, Object properties) throws Exception
    {
        TracksReportActionParameters parameters = (TracksReportActionParameters)properties;
        DataElementPathSet tracks = getTracks((TreeTableElement)model, selectedItems);
        TrackToGeneSet analysis = new TrackToGeneSet(null, "");
        parameters.setSourcePaths(tracks);
        analysis.setParameters(parameters);
        analysis.validateParameters();
        return analysis.getJobControl();
    }

    @Override
    public boolean isApplicable(Object model)
    {
        if(!(model instanceof TreeTableElement)) return false;
        TreeTableElement viewModel = (TreeTableElement)model;
        if(!(viewModel.getTree() instanceof ClassificationUnit)) return false;
        if(!(viewModel.getTable() instanceof TableDataCollection)) return false;
        TableDataCollection table = (TableDataCollection)viewModel.getTable();
        for(TableColumn column: table.getColumnModel())
        {
            if(column.getValue(TRACKS_PATH) != null) return true;
        }
        return false;
    }

    @Override
    public void validateParameters(Object model, List<DataElement> selectedItems) throws LoggedException
    {
        DataElementPathSet tracks = getTracks((TreeTableElement)model, selectedItems);
        if(tracks == null || tracks.isEmpty()) 
            throw new InvalidSelectionException(InvalidSelectionException.SELECTED_ANY, "row with tracks", "rows with tracks");
    }
    
    @Override
    public Object getProperties(Object model, List<DataElement> selectedItems)
    {
        return new TracksReportActionParameters();
    }

    public static class TracksReportActionParameters extends TrackToGeneSetParameters
    {
        public TracksReportActionParameters()
        {
            setFrom(10000);
            setTo(10000);
            setResultTypes(new SiteAggregator[] {new CountSiteAggregator(), new StructureSiteAggregator()});
            setDestPath(DataElementPath.create(TABLE_REPORTS_PATH));
        }
    }
    
    public static class TracksReportActionParametersBeanInfo extends TrackToGeneSetParametersBeanInfo
    {
        public TracksReportActionParametersBeanInfo()
        {
            super(TracksReportActionParameters.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            findPropertyDescriptor("sourcePaths").setHidden(true);
        }
    }

}
