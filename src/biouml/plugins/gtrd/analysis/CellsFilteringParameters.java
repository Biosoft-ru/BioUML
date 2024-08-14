package biouml.plugins.gtrd.analysis;

import java.util.ArrayList;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.analysis.CellsFiltering.NodeInfo;
import biouml.plugins.gtrd.analysis.CellsFilteringParametersBeanInfo.ExpFactorsLocal;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class CellsFilteringParameters extends AbstractAnalysisParameters

{
	private String organism = "Homo sapiens";
	private String prefix = "result_table";
	
	private DataElementPath pathToNode = DataElementPath.create( "databases/GTRD/Dictionaries/source" );
	private DataElementPath pathToExperiments = DataElementPath.create("databases/GTRD/Data/experiments");
	private DataElementPath pathToChipExoExperiments = DataElementPath.create("databases/GTRD/Data/ChIP-exo experiments");
	private DataElementPath pathToDnaseExperiments = DataElementPath.create("databases/GTRD/Data/DNase experiments");
	private DataElementPath pathToMnaseExperiments = DataElementPath.create("databases/GTRD/Data/MNase-seq experiments");
	private DataElementPath pathToHistonesExperiments = DataElementPath.create("databases/GTRD/Data/ChIP-seq HM experiments");
	private DataElementPath pathToExpFactors = DataElementPath.create("databases/GTRD/Dictionaries/exp factors");
	private DataElementPath pathToTables = DataElementPath.create("data/Collaboration/Demo/tmp");
	
	private NodeInfo cluster = new NodeInfo(null, null, null);
	private NodeInfo source = new NodeInfo(null, null, null); 
	private NodeInfo level2 = new NodeInfo(null, null, null);
	private NodeInfo level3 = new NodeInfo(null, null, null);
	private NodeInfo level4 = new NodeInfo(null, null, null);
	private NodeInfo level5 = new NodeInfo(null, null, null);
	private NodeInfo chipseq = new NodeInfo("ChIP-seq (TF) experiments", "chip", pathToExperiments);
	private NodeInfo chipsexo = new NodeInfo("ChIP-exo (TF) experiments", "chip_exo", pathToChipExoExperiments);
	private NodeInfo mnase = new NodeInfo("MNase-seq-experiments", "mnase", pathToMnaseExperiments);
	private NodeInfo dnase = new NodeInfo("DNase-seq-experiments", "dnase", pathToDnaseExperiments);
	private NodeInfo histones = new NodeInfo("Histones modifications ChIP-seq experiments", "hist", pathToHistonesExperiments);
	
	private NodeInfo[] availableCellTypes = new NodeInfo[]{new NodeInfo(null, null, null)};
	private ArrayList<NodeInfo> avExperimentsTypes;
	
	private NodeInfo[] choosedExperimentsTypes = new NodeInfo[]{};
	private ExpFactorsLocal[] factorSources = new ExpFactorsLocal[] {new ExpFactorsLocal()};
	
	private boolean selectExpFactors = false;
	private boolean hiddenExperimentalFactors = true;
	
	public CellsFilteringParameters() 
	{
		avExperimentsTypes = new ArrayList<CellsFiltering.NodeInfo>();
		avExperimentsTypes.add(this.chipseq);
		avExperimentsTypes.add(this.chipsexo);
		avExperimentsTypes.add(this.mnase);
		avExperimentsTypes.add(this.dnase);
		avExperimentsTypes.add(this.histones);
	}
	
	@PropertyName ( "Choose available experiment types" )
    @PropertyDescription ( "Select one or more experiment types using combination of ctrl + left mouse bottom" )
	public NodeInfo[] getChoosedExperimentsTypes() {
		return choosedExperimentsTypes;
	}

	public void setChoosedExperimentsTypes(NodeInfo[] choosedExperimentsTypes) {
		this.choosedExperimentsTypes = choosedExperimentsTypes;
	}


	public ArrayList<NodeInfo> getAvExperimentsTypes() 
	{
		return avExperimentsTypes;
	}

	public void setExperimentsTypes(ArrayList<NodeInfo> experimentsTypes) {
		this.avExperimentsTypes = experimentsTypes;
	}

	@PropertyName ( "Name of output table" )
    @PropertyDescription ( "specify table name" )
	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@PropertyName ( "Add experimental factors" )
    @PropertyDescription ( "Open parameters for experimental factors" )
	public boolean isSelectExpFactors() {
		return selectExpFactors;
	}

	public void setSelectExpFactors(boolean selectExpFactors) 
	{
		Object oldValue = this.selectExpFactors;
		this.selectExpFactors = selectExpFactors;
		setHiddenExperimentalFactors(!isSelectExpFactors());
		firePropertyChange("selectExpFactors", oldValue, selectExpFactors);
	}

	public boolean isHiddenExperimentalFactors() {
		return hiddenExperimentalFactors;
	}

	public void setHiddenExperimentalFactors(boolean hiddenExperimentalFactors) 
	{
		this.hiddenExperimentalFactors = hiddenExperimentalFactors;
	}




	@PropertyName ( "Experimental factors" )
    @PropertyDescription ( "Select list  of experimental factors" )
    public ExpFactorsLocal[] getFactorSources()
    {
        return factorSources;
    }

    public void setFactorSources(ExpFactorsLocal[] factorSources)
    {
    	ExpFactorsLocal[] oldValue = this.factorSources;
        this.factorSources = factorSources;
        if(oldValue != null)
            for(ExpFactorsLocal a : oldValue)
                a.setParent( null );
        if(factorSources != null)
            for(ExpFactorsLocal a : factorSources)
                a.setParent( this );
        firePropertyChange( "factorSources", oldValue, factorSources );
    }


	public DataElementPath getPathToExpFactors() {
		return pathToExpFactors;
	}



	public void setPathToExpFactors(DataElementPath pathToExpFactors) {
		this.pathToExpFactors = pathToExpFactors;
		
	}


	@PropertyName("Path to output folder")
	@PropertyDescription("Select path to result tables")
	public DataElementPath getPathToTables() {
		return pathToTables;
	}

	public void setPathToTables(DataElementPath pathToTables) {
		Object oldValue = this.pathToTables;
		this.pathToTables = pathToTables;
		firePropertyChange("pathToTables", oldValue, pathToTables);
	}



	public boolean isEmptyLevel(DataElementPath choosedPath) {

		int choosed = 0;
		if ( choosedPath != null )
		{
			ru.biosoft.access.core.DataElementPath[] childrens = choosedPath.getChildrenArray();
			for (DataElementPath el : childrens)
			{
				String elId = el.getName();
				if (!elId.startsWith("GTRD"))
					choosed++;
			}

			if ( choosed == 0 )
				return true; 

			else 
				return false;
		}
		else
			return true;
	}

	public DataElementPath  getCheckLevel() {

		ru.biosoft.access.core.DataElementPath path = getCluster().getPath();

		if ( getSource().getPath() != null )
			path = getSource().getPath();

		if ( getLevel2().getPath() != null )
			path = getLevel2().getPath();
		
		if ( getLevel3().getPath() != null )
			path = getLevel3().getPath();
		
		if ( getLevel4().getPath() != null )
			path = getLevel4().getPath();
		
		if ( getLevel5().getPath() != null )
			path = getLevel5().getPath();
		
		return path;
	}

	public DataElementPath getPathToChipExoExperiments() {
		return pathToChipExoExperiments;
	}

	public void setPathToChipExoExperiments(DataElementPath pathToChipExoExperiments) {
		this.pathToChipExoExperiments = pathToChipExoExperiments;
	}

	public DataElementPath getPathToDnaseExperiments() {
		return pathToDnaseExperiments;
	}

	public void setPathToDnaseExperiments(DataElementPath pathToDnaseExperiments) {
		this.pathToDnaseExperiments = pathToDnaseExperiments;
	}

	public DataElementPath getPathToMnaseExperiments() {
		return pathToMnaseExperiments;
	}

	public void setPathToMnaseExperiments(DataElementPath pathToMnaseExperiments) {
		this.pathToMnaseExperiments = pathToMnaseExperiments;
	}

	public DataElementPath getPathToHistonesExperiments() {
		return pathToHistonesExperiments;
	}


	public void setPathToHistonesExperiments(DataElementPath pathToHistonesExperiments) {
		this.pathToHistonesExperiments = pathToHistonesExperiments;
	}


	@PropertyName("Source level 3")
	@PropertyDescription ( "Subsource of Source level 3" )
	public NodeInfo getLevel3() {
		return level3;
	}

	public void setLevel3(NodeInfo level3) {
		Object oldValue = this.level3;
		this.level3 = level3;
		firePropertyChange("*", oldValue, level3);
	}
	
	@PropertyName("Source level 4")
	@PropertyDescription ( "Subsource of Source level 4" )
	public NodeInfo getLevel4() {
		return level4;
	}

	public void setLevel4(NodeInfo level4) {
		Object oldValue = this.level4;
		this.level4 = level4;
		firePropertyChange("*", oldValue, level4);
	}
	
	@PropertyName("Source level 5")
	@PropertyDescription ( "Subsource of Source level 5" )
	public NodeInfo getLevel5() {
		return level5;
	}

	public void setLevel5(NodeInfo level5) {
		Object oldValue = this.level5;
		this.level5 = level5;
		firePropertyChange("*", oldValue, level5);
	}



	public DataElementPath getPathToExperiments() {
		return pathToExperiments;
	}


	public void setPathToExperiments(DataElementPath pathToExperiments) {
		this.pathToExperiments = pathToExperiments;
	}

	@PropertyName("Cell type")
	public NodeInfo[] getAvailableCellTypes() {
		return availableCellTypes;
	}

	public void setAvailableCellTypes(NodeInfo[] availableCellTypes) {
		Object oldValue = this.availableCellTypes;
		this.availableCellTypes = availableCellTypes;
		firePropertyChange("availableCellTypes", oldValue, availableCellTypes);
	}

	@PropertyName("Source level 2")
	@PropertyDescription ( "Subsource of Source" )
	public NodeInfo getLevel2() {
		return level2;
	}


	public void setLevel2(NodeInfo level2) {
		Object oldValue = this.level2;
		if (this.level2.getName() != level2.getName())
		{
			setLevel3(new NodeInfo(null, null, null));
		}
		this.level2 = level2;
		firePropertyChange("*",oldValue,level2);
	}

	public boolean isHiddenSource() 
	{
		if (getCluster().getPath() != null)
		{
			ru.biosoft.access.core.DataElementPath path = getCluster().getPath();  
			if (isEmptyLevel(path))
				return true;
			else
				return false;
		}
		else
			return true;
	}

	public boolean isHiddenLevel2() {
		if (!isHiddenSource())
		{
			ru.biosoft.access.core.DataElementPath path = getSource().getPath();  
			if (isEmptyLevel(path))
				return true;
			else
				return false;
		}
		else
			return true;
	}


	public boolean isHiddenLevel3() {

		if (!isHiddenLevel2())
		{
			ru.biosoft.access.core.DataElementPath path = getLevel2().getPath(); 
			if (isEmptyLevel(path))
				return true;
			else
				return false;
		}
		else
			return true;
	}
	
	public boolean isHiddenLevel4() {

		if (!isHiddenLevel3())
		{
			ru.biosoft.access.core.DataElementPath path = getLevel3().getPath(); 
			if (isEmptyLevel(path))
				return true;
			else
				return false;
		}
		else
			return true;
	}
	
	public boolean isHiddenLevel5() {

		if (!isHiddenLevel4())
		{
			ru.biosoft.access.core.DataElementPath path = getLevel4().getPath(); 
			if (isEmptyLevel(path))
				return true;
			else
				return false;
		}
		else
			return true;
	}


	@PropertyName("Cluster's source")
	@PropertyDescription ( "Subsources of choosed cluster" )
	public NodeInfo getSource() {
		return source;
	}


	public void setSource(NodeInfo source) {
		Object oldValue = this.source;
		if (this.source.getName() != source.getName())
		{
			setLevel2(new NodeInfo(null, null, null));
			setLevel3(new NodeInfo(null, null, null));
		}
		this.source = source;
		firePropertyChange("source", oldValue, source);
	}

	public DataElementPath getPathToNode() {
		return pathToNode;
	}

	public void setPathToNode(DataElementPath pathToNode) {
		Object oldvalue = this.pathToNode;
		this.pathToNode = pathToNode;
		firePropertyChange("pathToNode", oldvalue, pathToNode);
	}

	@PropertyName("Organism")
	public String getOrganism() {
		return organism;
	}


	public void setOrganism(String organism) {
		Object oldValue = this.organism;
		this.organism = organism;
		firePropertyChange("organism", oldValue, organism);
	}

	@PropertyName("Cluster")
	public NodeInfo getCluster() {
		return cluster;
	}


	public void setCluster(NodeInfo cluster) {

		Object oldValue = this.cluster;
		if (this.cluster.getName() != cluster.getName())
		{
			setSource(new NodeInfo(null, null, null));
			setLevel2(new NodeInfo(null, null, null));
			setLevel3(new NodeInfo(null, null, null));
		}
		this.cluster = cluster;
		firePropertyChange("cluster", oldValue, cluster);
	}


}
