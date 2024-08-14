package biouml.plugins.gtrd.access;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import biouml.plugins.gtrd.analysis.CellsFiltering.NodeInfo;
import biouml.plugins.gtrd.analysis.CellsFilteringParametersBeanInfo.SourceSelector;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.classification.ClassificationUnitAsSQL;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class GTRDTrackFinderBeanInfo extends BeanInfoEx2<GTRDTrackFinder>
{

    public GTRDTrackFinderBeanInfo()
    {
        super( GTRDTrackFinder.class );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        property( "dataType" ).editor( DataTypeSelector.class ).add();
        property("cellClusters").hidden( "isCellClusterHidden" ).simple().editor( ClusterTypeSelector.class ).add();
        property("cellSource").hidden( "isCellSourceHidden" ).simple().editor( SourceTypeSelector.class ).add();
        property("cellSourceLvl2").hidden( "isCellSourceLvl2Hidden" ).simple().editor( SourceTypeSelectorLvl2.class ).add();
        property("cellName").hidden( "isCellHidden" ).simple().editor( CellTypeSelectorTrF.class ).add();
        property( "dnaseCellName" ).hidden( "isDnaseCellHidden" ).editor( DnaseCellNameSelector.class ).add();
        property( "mnaseCellName" ).hidden( "isMnaseCellHidden" ).editor( MnaseCellNameSelector.class ).add();
        property( "histCellName" ).hidden( "isHistCellHidden" ).editor( HistCellNameSelector.class ).add();
        property( "chipexoCellName" ).hidden( "isChipexoCellHidden" ).editor( ChipexoCellNameSelector.class ).add();
        property("transcriptionFactor").hidden( "isTranscriptionFactorChIPexoHidden" ).editor( ChipexoTFSelector.class ).add();
        property("transcriptionFactor").hidden( "isTranscriptionFactorHidden" ).editor( TFSelector.class ).add();
    }
    
    public static class DataTypeSelector extends GenericComboBoxEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            GTRDTrackFinder bean = (GTRDTrackFinder)getBean();
            String organism = bean.getSpeciesLatinName();
            boolean dnaseAvailable = isDNaseAvailable( bean, organism );
            boolean hocomocoAvailable = isHOCOMOCOAvailable( organism );
            List<String> result = new ArrayList<>();
            for( String type : GTRDTrackFinder.DATA_TYPES )
            {
                if( type.equals( GTRDTrackFinder.DATA_TYPE_DNASE_OPEN_CHROMATIN_MACS2 )
                        || type.equals( GTRDTrackFinder.DATA_TYPE_DNASE_OPEN_CHROMATIN_HOTSPOT2 )
                        || type.equals( GTRDTrackFinder.DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2 )
                        || type.equals( GTRDTrackFinder.DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2 ) )
                {
                    if( dnaseAvailable )
                        result.add( type );
                }
                else if( type.equals( GTRDTrackFinder.DATA_TYPE_HOCOMOCO_STRONG_SITES )
                        || type.equals( GTRDTrackFinder.DATA_TYPE_HOCOMOCO_WEAK_SITES ) )
                {
                    //weak sites are not supported for now
                    if( type.equals( GTRDTrackFinder.DATA_TYPE_HOCOMOCO_WEAK_SITES ) )
                        continue;
                    if( hocomocoAvailable )
                        result.add( type );
                }
                else
                    result.add( type );
            }
            return result.toArray( new String[0] );
        }

        private boolean isDNaseAvailable(GTRDTrackFinder bean, String species)
        {
            Connection con = DataCollectionUtils.getSqlConnection( bean.getDatabasePath().getDataElement() );
            return SqlUtil.hasResult( con,
                    "SELECT id FROM dnase_experiments WHERE organism=" + SqlUtil.quoteString( species ) );
        }

        private boolean isHOCOMOCOAvailable(String organism)
        {
            return "Homo sapiens".equals( organism ) || "Mus musculus".equals( organism );
        }
    }
    
    public static class ClusterTypeSelector extends GenericComboBoxEditor
	{
		public Object[] getAvailableValues()
		{
			String organism = ((GTRDTrackFinder)getBean()).getSpeciesLatinName();
			ru.biosoft.access.core.DataElementPath pathToSourceTable = ((GTRDTrackFinder)getBean()).getPathToSource();
			ru.biosoft.access.core.DataElementPath[] clustersPaths = pathToSourceTable.getChildrenArray();
			ArrayList<NodeInfo> clustersNames = new ArrayList<>();

			for (int i = 0; i < clustersPaths.length; i++)
			{
				ru.biosoft.access.core.DataElementPath clusterPath = clustersPaths[i];
				ClassificationUnitAsSQL node = clusterPath.getDataElement(ClassificationUnitAsSQL.class);
				String clusterName = node.getClassName();
				String clusterId = clusterPath.getName();
				NodeInfo cluster;
				switch ( organism )
				{
				case "Schizosaccharomyces pombe":
					if (clusterId.equalsIgnoreCase("BTO:0002307"))
					{
						cluster = new NodeInfo(clusterName, clusterId, clusterPath);
						clustersNames.add(cluster);
					}
					break;
				case "Saccharomyces cerevisiae":
					if (clusterId.equalsIgnoreCase("BTO:0002307"))
					{
						cluster = new NodeInfo(clusterName, clusterId, clusterPath);
						clustersNames.add(cluster);
					}
					break;
				case "Arabidopsis thaliana":
					if (!clusterId.contentEquals("EFO:0000311"))
					{
						if (!clusterId.contentEquals("BTO:0002307"))
						{
							cluster = new NodeInfo(clusterName, clusterId, clusterPath);
							clustersNames.add(cluster);
						}
					}
					break;

				default:
					if (!clusterId.contentEquals("BTO:0002307"))
					{
						cluster = new NodeInfo(clusterName, clusterId, clusterPath);
						clustersNames.add(cluster);
					}
					break;
				}
			}
			clustersNames.sort(Comparator.comparing(NodeInfo::toString));
			return clustersNames.toArray(new NodeInfo[clustersNames.size()]);
		}
	}
    
    public static class SourceTypeSelector extends SourceSelector
	{

		@Override
		public Object[] getAvailableValues()
		{
			try {
				ru.biosoft.access.core.DataElementPath pathToSourceTable = ((GTRDTrackFinder)getBean()).getCellClusters().getPath();
				NodeInfo[] clustersNames = findTags(pathToSourceTable);
				return clustersNames;
			}

			catch (RepositoryException e) {
				return new NodeInfo[]{new NodeInfo("Is empty", "Error", null)};
			}
			catch (Exception e) {
				return new NodeInfo[]{new NodeInfo("No folder", "Error", null)};
			}

		}

	}
    
    public static class SourceTypeSelectorLvl2 extends SourceSelector
	{

		@Override
		public Object[] getAvailableValues()
		{
			try {
				ru.biosoft.access.core.DataElementPath pathToSourceTable = ((GTRDTrackFinder)getBean()).getCellSource().getPath();
				NodeInfo[] clustersNames = findTags(pathToSourceTable);
				return clustersNames;
			}

			catch (RepositoryException e) {
				return new NodeInfo[]{new NodeInfo("Is empty", "Error", null)};
			}
			catch (Exception e) {
				return new NodeInfo[]{new NodeInfo("No folder", "Error", null)};
			}

		}

	}

//TODO refactor class to extend from cell filtering
    public static class CellTypeSelectorTrF extends GenericComboBoxEditor
	{
		@Override
		public Object[] getAvailableValues()
		{
			try 
			{
				ru.biosoft.access.core.DataElementPath pathToLevel = ((GTRDTrackFinder)getBean()).checkSourceLevel().getPath();
				ru.biosoft.access.core.DataElementPath[] pathToCellTypes = pathToLevel.getChildrenArray();
				String organism = ((GTRDTrackFinder)getBean()).getSpeciesLatinName();
				ArrayList<NodeInfo> cellTypes = new ArrayList<>();
				for (DataElementPath cellPath : pathToCellTypes)
				{

					String description = cellPath.getDataElement(ClassificationUnitAsSQL.class).getDescription();
					String cellName = cellPath.getDataElement(ClassificationUnitAsSQL.class).getClassName();
					String cellId = cellPath.getDataElement(ClassificationUnitAsSQL.class).getName();
					if (cellId.startsWith("GTRD"))
					{
						String [] splitedDescr = description.split(";");
						String cellOrganism = splitedDescr[0].split(":")[1];
						String parsCellId = cellId.split(":")[1];
						if (cellOrganism.equals(organism))
						{
							NodeInfo cell = new NodeInfo(cellName, parsCellId, cellPath);
							cellTypes.add(cell);
						}
					}
				}
				cellTypes.sort(Comparator.comparing(NodeInfo::toString));
				return cellTypes.toArray(new NodeInfo[cellTypes.size()]);
			}
			catch (RepositoryException e) {
				return new NodeInfo[]{new NodeInfo("Is empty", "Error", null)};
			}
			catch (Exception e) {
				return new NodeInfo[]{new NodeInfo("No folder", "Error", null)};
			}
		}
	}

    public static class CellSourceSelector extends GenericComboBoxEditor
    {
        protected String getQuery()
        {
            return "SELECT DISTINCT source FROM cells "
                    + "JOIN chip_experiments ce on(cells.id=ce.cell_id) "
                    + "JOIN peaks_finished pf on(ce.id=pf.exp_id) "
                    + "WHERE ce.specie=? ORDER BY 1";
        }
        
        @Override
        protected Object[] getAvailableValues()
        {
            GTRDTrackFinder bean = (GTRDTrackFinder)getBean();
            Connection con = DataCollectionUtils.getSqlConnection( bean.getDatabasePath().getDataElement() );
            String organism = bean.getSpeciesLatinName();
            if(organism == null)
                return new Object[] {GTRDTrackFinder.ALL_STRING_OPTION};

            String query = getQuery();

            List<String> result = new ArrayList<>();
            result.add( GTRDTrackFinder.ALL_STRING_OPTION );
            try(PreparedStatement ps = con.prepareStatement( query ))
            {
                ps.setString( 1, organism );
                try( ResultSet rs = ps.executeQuery() )
                {
                    while( rs.next() )
                    {
                        String name = rs.getString( 1 );
                        result.add( name );
                    }
                }
            }
            catch( SQLException e )
            {
                throw new RuntimeException(e);
            }

            return result.toArray();
        }
    }


    //TODO: rework
    public static class DnaseCellNameSelector extends CellSourceSelector
    {
        @Override
        protected String getQuery()
        {
            GTRDTrackFinder bean = (GTRDTrackFinder)getBean();
            String dataType = bean.getDataType();
            String peakType = GTRDTrackFinder.getPeakCallerByDataType( dataType );
            return "SELECT DISTINCT cells.title FROM dnase_experiments e JOIN cells on(e.cell_id=cells.id)"
                    + " JOIN dnase_peaks_finished ON(e.id=exp_id "
                    + ( peakType.isEmpty() ? "" : "AND peak_type=" + SqlUtil.quoteString( peakType ) ) + ")"
                    + " WHERE e.organism=? ORDER BY 1";
        }
    }
    
    public static class MnaseCellNameSelector extends CellSourceSelector
    {
        @Override
        protected String getQuery()
        {
            GTRDTrackFinder bean = (GTRDTrackFinder)getBean();
            String dataType = bean.getDataType();
            String peakType = GTRDTrackFinder.getPeakCallerByDataType( dataType );
            return "SELECT DISTINCT cells.title FROM mnase_experiments e JOIN cells on(e.cell_id=cells.id)"
                    + " JOIN mnase_peaks_finished ON(e.id=exp_id "
                    + ( peakType.isEmpty() ? "" : "AND peak_type=" + SqlUtil.quoteString( peakType ) ) + ")"
                    + " WHERE e.organism=? ORDER BY 1";
        }
    }
    
    public static class HistCellNameSelector extends CellSourceSelector
    {
        @Override
        protected String getQuery()
        {
            GTRDTrackFinder bean = (GTRDTrackFinder)getBean();
            String dataType = bean.getDataType();
            String peakType = GTRDTrackFinder.getPeakCallerByDataType( dataType );
            return "SELECT DISTINCT cells.title FROM hist_experiments e JOIN cells on(e.cell_id=cells.id)"
                    + " JOIN hist_peaks_finished ON(e.id=exp_id "
                    + ( peakType.isEmpty() ? "" : "AND peak_type=" + SqlUtil.quoteString( peakType ) ) + ")"
                    + " WHERE e.specie=? ORDER BY 1";
        }
    }
    
    public static class ChipexoCellNameSelector extends CellSourceSelector
    {
        @Override
        protected String getQuery()
        {
            GTRDTrackFinder bean = (GTRDTrackFinder)getBean();
            String dataType = bean.getDataType();
            String peakType = GTRDTrackFinder.getPeakCallerByDataType( dataType );
            return "SELECT DISTINCT cells.title FROM chipexo_experiments e JOIN cells on(e.cell_id=cells.id)"
                    + " JOIN chipexo_peaks_finished ON(e.id=exp_id "
                    + ( peakType.isEmpty() ? "" : "AND peak_type=" + SqlUtil.quoteString( peakType ) ) + ")"
                    + " WHERE e.specie=? ORDER BY 1";
        }
    }
    
    public static class TFSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            GTRDTrackFinder bean = (GTRDTrackFinder)getBean();
            Connection con = DataCollectionUtils.getSqlConnection( bean.getDatabasePath().getDataElement() );
            String organism = bean.getSpeciesLatinName();
            if(organism == null)
                return new Object[] {GTRDTrackFinder.ALL_STRING_OPTION};
            
            String cell = bean.getCellName().getName();

            String query = "SELECT DISTINCT uniprot.id, uniprot.gene_name FROM uniprot "
                    + "JOIN chip_experiments ce on(uniprot.id=ce.tf_uniprot_id) "
                    + "JOIN clusters_finished cf on(uniprot.id=cf.uniprot_id AND ce.specie=cf.specie) "
                    + " JOIN cells on(ce.cell_id=cells.id)"
                    + "WHERE ce.specie=?"
                    + ( cell == null || GTRDTrackFinder.ALL_STRING_OPTION.equals( cell ) ? "" : " AND cells.title=?" )
                    + " ORDER BY 2";
            
            List<String> result = new ArrayList<>();
            result.add( GTRDTrackFinder.ALL_STRING_OPTION );
            try(PreparedStatement ps = con.prepareStatement( query ))
            {
                ps.setString( 1, organism );
                if( cell != null && !GTRDTrackFinder.ALL_STRING_OPTION.equals( cell ) )
                    ps.setString( 2, cell );
                try( ResultSet rs = ps.executeQuery() )
                {
                    while( rs.next() )
                    {
                        String uniprotId = rs.getString( 1 );
                        String geneName = rs.getString( 2 );
                        result.add( geneName + " " + uniprotId );
                    }
                }
            }
            catch( SQLException e )
            {
                throw new RuntimeException(e);
            }

            return result.toArray();
        }
    }
    
    public static class ChipexoTFSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            GTRDTrackFinder bean = (GTRDTrackFinder)getBean();
            Connection con = DataCollectionUtils.getSqlConnection( bean.getDatabasePath().getDataElement() );
            String organism = bean.getSpeciesLatinName();
            if(organism == null)
                return new Object[] {GTRDTrackFinder.ALL_STRING_OPTION};
            
            String cell = bean.getCellName().getName();

            String query = "SELECT DISTINCT uniprot.id, uniprot.gene_name FROM uniprot "
                    + "JOIN chipexo_experiments ce on(uniprot.id=ce.tf_uniprot_id) "
                    + "JOIN clusters_finished cf on(uniprot.id=cf.uniprot_id AND ce.specie=cf.specie) "
                    + " JOIN cells on(ce.cell_id=cells.id)"
                    + "WHERE ce.specie=?"
                    + ( cell == null || GTRDTrackFinder.ALL_STRING_OPTION.equals( cell ) ? "" : " AND cells.title=?" )
                    + " ORDER BY 2";
            
            List<String> result = new ArrayList<>();
            result.add( GTRDTrackFinder.ALL_STRING_OPTION );
            try(PreparedStatement ps = con.prepareStatement( query ))
            {
                ps.setString( 1, organism );
                if( cell != null && !GTRDTrackFinder.ALL_STRING_OPTION.equals( cell ) )
                    ps.setString( 2, cell );
                try( ResultSet rs = ps.executeQuery() )
                {
                    while( rs.next() )
                    {
                        String uniprotId = rs.getString( 1 );
                        String geneName = rs.getString( 2 );
                        result.add( geneName + " " + uniprotId );
                    }
                }
            }
            catch( SQLException e )
            {
                throw new RuntimeException(e);
            }

            return result.toArray();
        }
    }
}
