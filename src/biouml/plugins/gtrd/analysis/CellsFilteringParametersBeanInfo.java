package biouml.plugins.gtrd.analysis;

import java.util.ArrayList;
import java.util.Comparator;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.gtrd.analysis.CellsFiltering.NodeInfo;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.bsa.classification.ClassificationUnitAsSQL;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class CellsFilteringParametersBeanInfo extends BeanInfoEx2<CellsFilteringParameters> 
{
	public CellsFilteringParametersBeanInfo()
	{
		super(CellsFilteringParameters.class);
	}
	
	@Override
	protected void initProperties() throws Exception
	{
		add(new PropertyDescriptorEx("organism", beanClass), OrganismEditor.class);
		property("cluster").simple().editor(ClusterSelector.class).add();
		property("source").simple().editor(SourceSelector.class).hidden("isHiddenSource").add();
		property("level2").simple().editor(Level2Selector.class).hidden("isHiddenLevel2").add();
		property("level3").simple().editor(Level3Selector.class).hidden("isHiddenLevel3").add();
		property("level4").simple().editor(Level4Selector.class).hidden("isHiddenLevel4").add();
		property("level5").simple().editor(Level5Selector.class).hidden("isHiddenLevel5").add();
		property("availableCellTypes").simple().editor(CellTypeSelector.class).add();
		//property("selectExpFactors").add();
		//property("factorSources").hidden("isHiddenExperimentalFactors").add();
		property("choosedExperimentsTypes").simple().editor(ExperimentsTypeSelector.class).add();
		//property("prefix").add();
		//property("pathToTables").inputElement( FolderCollection.class ).add();
	}

	public static class OrganismEditor extends StringTagEditor
	{
		@Override
		public String[] getTags()
		{
			ru.biosoft.access.core.DataElementPath pathToSpecies = DataElementPath.create( "databases/Utils/Species" );
			DataCollection<Species> species = pathToSpecies.getDataCollection(Species.class);
			String [] organisms = new String[species.getSize()];
			int i = 0;
			for (Species sp : species)
			{
				organisms[i] = sp.getLatinName();
				i++;
			}
			return organisms;

		}
	}

	public static class ClusterSelector extends GenericComboBoxEditor
	{
		public Object[] getAvailableValues()
		{
			String organism = ((CellsFilteringParameters) getBean()).getOrganism();
			ru.biosoft.access.core.DataElementPath pathToSourceTable = ((CellsFilteringParameters)getBean()).getPathToNode();
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

	public static class SourceSelector extends GenericComboBoxEditor
	{

		@Override
		public Object[] getAvailableValues()
		{
			try {
				ru.biosoft.access.core.DataElementPath pathToSourceTable = ((CellsFilteringParameters)getBean()).getCluster().getPath();
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
		public NodeInfo[] findTags(DataElementPath pathToSourceTable)
		{
			ArrayList<NodeInfo> clustersNames = new ArrayList<NodeInfo>();
			if (pathToSourceTable.getDataElement(ClassificationUnitAsSQL.class).getSize() > 0)
			{
				ru.biosoft.access.core.DataElementPath[] clustersPaths = pathToSourceTable.getChildrenArray();
				for (int i = 0; i < clustersPaths.length; i++)
				{
					ru.biosoft.access.core.DataElementPath clusterPath = clustersPaths[i];
					String clusterName = clusterPath.getDataElement(ClassificationUnitAsSQL.class).getClassName();
					String clusterId = clusterPath.getName();
					if (!clusterId.startsWith("GTRD"))
					{
						NodeInfo source = new NodeInfo(clusterName, clusterId, clusterPath);
						clustersNames.add(source);
					}
				}
				clustersNames.sort(Comparator.comparing(NodeInfo::toString));
				//clustersNames.add(0, new NodeInfo("all", pathToSourceTable.getName(), pathToSourceTable));
			}
			return clustersNames.toArray(new NodeInfo[clustersNames.size()]);
		}

	}

	public static class Level2Selector extends SourceSelector
	{
		@Override
		public Object[] getAvailableValues()
		{
			try 
			{
				ru.biosoft.access.core.DataElementPath pathToTable = ((CellsFilteringParameters)getBean()).getSource().getPath();
				NodeInfo[] clustersNames = findTags(pathToTable); 
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

	public static class Level3Selector extends SourceSelector
	{
		@Override
		public Object[] getAvailableValues()
		{
			try 
			{
				ru.biosoft.access.core.DataElementPath pathToSourceTable = ((CellsFilteringParameters)getBean()).getLevel2().getPath();
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
	
	public static class Level4Selector extends SourceSelector
	{
		@Override
		public Object[] getAvailableValues()
		{
			try 
			{
				ru.biosoft.access.core.DataElementPath pathToSourceTable = ((CellsFilteringParameters)getBean()).getLevel3().getPath();
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
	
	public static class Level5Selector extends SourceSelector
	{
		@Override
		public Object[] getAvailableValues()
		{
			try 
			{
				ru.biosoft.access.core.DataElementPath pathToSourceTable = ((CellsFilteringParameters)getBean()).getLevel4().getPath();
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

	public static class CellTypeSelector extends GenericMultiSelectEditor
	{
		@Override
		public Object[] getAvailableValues()
		{
			try 
			{
				ru.biosoft.access.core.DataElementPath pathToLevel = ((CellsFilteringParameters)getBean()).getCheckLevel();
				ru.biosoft.access.core.DataElementPath[] pathToCellTypes = pathToLevel.getChildrenArray();
				String organism = ((CellsFilteringParameters)getBean()).getOrganism();
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
						if (cellOrganism.equals(organism))
						{
							NodeInfo cell = new NodeInfo(cellName, cellId, cellPath);
							cellTypes.add(cell);
						}
					}
				}
				cellTypes.sort(Comparator.comparing(NodeInfo::toString));
				cellTypes.add(0, new NodeInfo("All", pathToLevel.getName(), pathToLevel));
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
	public static class ExperimentsTypeSelector extends GenericMultiSelectEditor
	{
		@Override
		public Object[] getAvailableValues()
		{
			try 
			{
				ArrayList<NodeInfo> experiments = (ArrayList<NodeInfo>)((CellsFilteringParameters)getBean()).getAvExperimentsTypes().clone();
				experiments.sort(Comparator.comparing(NodeInfo::toString));
				experiments.add(0, new NodeInfo("All", null, null));
				
				return experiments.toArray(new NodeInfo[experiments.size()]);
			}
			catch (RepositoryException e) {
				return new NodeInfo[]{new NodeInfo("No avalable experimental types", "Error", null)};
			}
			catch (Exception e) {
				return new NodeInfo[]{new NodeInfo("No avalable experimental types", "Error", null)};
			}
		}
	}
	public static class ExpFactorsLocal extends OptionEx implements JSONBean
	{
		private DataElementPath pathToExpFactors = DataElementPath.create("databases/GTRD/Dictionaries/exp factors");
		private NodeInfo expFactors = new NodeInfo(null, null, null);
		private NodeInfo expFactorsLvl2 = new NodeInfo(null, null, null);
		private NodeInfo expFactorsLvl3 = new NodeInfo(null, null, null);
		
		public ExpFactorsLocal()
		{
			
		}
		
		@PropertyName ( "Experimental factor" )
        @PropertyDescription ( "Choose experimental factor" )
		public NodeInfo getExpFactors() {
			return expFactors;
		}


		public void setExpFactors(NodeInfo expFactors) 
		{
			Object oldValue = this.expFactors;
			this.expFactors = expFactors;
			firePropertyChange("*", expFactors, oldValue);
		}
		
		@PropertyName ( "Experimental factors level 2" )
        @PropertyDescription ( "Choose experimental factor type" )
		public NodeInfo getExpFactorsLvl2() {
			return expFactorsLvl2;
		}

		public void setExpFactorsLvl2(NodeInfo expFactorsLvl2) {
			Object oldValue = this.expFactorsLvl2;
			this.expFactorsLvl2 = expFactorsLvl2;
			firePropertyChange("*", expFactorsLvl2, oldValue);
		}

		@PropertyName ( "Experimental factor level 3" )
        @PropertyDescription ( "Choose experimental factor type" )
		public NodeInfo getExpFactorsLvl3() 
		{
			return expFactorsLvl3;
		}

		public void setExpFactorsLvl3(NodeInfo expFactorsLvl3) {
			Object oldValue = this.expFactorsLvl3;
			this.expFactorsLvl3 = expFactorsLvl3;
			firePropertyChange("*", expFactorsLvl3, oldValue);
		}

		public DataElementPath getPathToExpFactors() 
		{
			return pathToExpFactors;
		}
		
		public void setPathToExpFactors(DataElementPath pathToExpFactors) 
		{
			this.pathToExpFactors = pathToExpFactors;
		}
		
		public boolean isHiddenLevel2()
		{
			ru.biosoft.access.core.DataElementPath path = getExpFactors().getPath();
			if (path != null)
			{
				int sourceSize = path.getDataElement(ClassificationUnitAsSQL.class).getSize();
				if (sourceSize > 0)
					return false;
				else
					return true;
		
			}
			else
				return true;
		}
		
		public boolean isHiddenLevel3()
		{
			ru.biosoft.access.core.DataElementPath path = getExpFactorsLvl2().getPath();
			if (path != null)
			{
				int sourceSize = path.getDataElement(ClassificationUnitAsSQL.class).getSize();
				if (sourceSize > 0)
					return false;
				else
					return true;
		
			}
			else
				return true;
		}
		
		public NodeInfo checkLevelOfExpFac()
		{
			if (getExpFactorsLvl3().getName() != null)
				return getExpFactorsLvl3();
			else if (getExpFactorsLvl2().getName() != null)
				return getExpFactorsLvl2();
			else
				return getExpFactors();
		}
			

	}
	
	
	public static class ExpFactorsLocalBeanInfo extends BeanInfoEx2<ExpFactorsLocal>
    {
        public ExpFactorsLocalBeanInfo()
        {
            super( ExpFactorsLocal.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
        	property("expFactors").simple().editor(ExpFactorsSelector.class).add();
			property("expFactorsLvl2").simple().editor(ExpFactorL2Selector.class).hidden("isHiddenLevel2").add();
			property("expFactorsLvl3").simple().editor(ExpFactorL3Selector.class).hidden("isHiddenLevel3").add();
        
        }
    }
	
	public static class ExpFactorsSelector extends GenericComboBoxEditor
	{
		
		@Override
		public Object[] getAvailableValues()
		{
			try 
			{
				ru.biosoft.access.core.DataElementPath pathToExpTable = ((ExpFactorsLocal)getBean()).getPathToExpFactors();
				NodeInfo[] clustersNames = findTags(pathToExpTable);
				return clustersNames;
			}

			catch (RepositoryException e) {
				return new NodeInfo[]{new NodeInfo("Is empty", "Error", null)};
			}
			catch (Exception e) {
				return new NodeInfo[]{new NodeInfo("No folder", "Error", null)};
			}

		}
		public NodeInfo[] findTags(DataElementPath pathToExpTable)
		{
			ArrayList<NodeInfo> clustersNames = new ArrayList<NodeInfo>();
			ru.biosoft.access.core.DataElementPath[] expFactorsPaths = pathToExpTable.getChildrenArray();
			for (DataElementPath expFPath : expFactorsPaths)
			{
				String id = expFPath.getName();
				String name = expFPath.getDataElement(ClassificationUnitAsSQL.class).getClassName();
				ru.biosoft.access.core.DataElementPath path = expFPath;
				NodeInfo factor = new NodeInfo(name, id, path);
				clustersNames.add(factor);
				
			}
			clustersNames.sort(Comparator.comparing(NodeInfo::toString));
			return clustersNames.toArray(new NodeInfo[clustersNames.size()]);
		}

	}
	
	public static class ExpFactorL2Selector extends ExpFactorsSelector
	{
		public Object[] getAvailableValues()
		{
			try 
			{
				ru.biosoft.access.core.DataElementPath pathToExpTable = ((ExpFactorsLocal)getBean()).getExpFactors().getPath();
				NodeInfo[] selectedFactors = findTags(pathToExpTable);
				return selectedFactors;
			}
			catch (RepositoryException e) {
				return new NodeInfo[]{new NodeInfo("Is empty", "Error", null)};
			}
			catch (Exception e) {
				return new NodeInfo[]{new NodeInfo("No folder", "Error", null)};
			}
		}
		
	}
	
	public static class ExpFactorL3Selector extends ExpFactorsSelector
	{
		public Object[] getAvailableValues()
		{
			try 
			{
				ru.biosoft.access.core.DataElementPath pathToExpTable = ((ExpFactorsLocal)getBean()).getExpFactorsLvl2().getPath();
				NodeInfo[] selectedFactors = findTags(pathToExpTable);
				return selectedFactors;
			}
			catch (RepositoryException e) {
				return new NodeInfo[]{new NodeInfo("Is empty", "Error", null)};
			}
			catch (Exception e) {
				return new NodeInfo[]{new NodeInfo("No folder", "Error", null)};
			}
		}
		
	}

}
