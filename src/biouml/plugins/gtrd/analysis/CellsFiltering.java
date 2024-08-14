package biouml.plugins.gtrd.analysis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPexoExperimentGTRDType;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.DnaseExperimentGTRDType;
import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.ExperimentGTRDType;
import biouml.plugins.gtrd.ExperimentalFactor;
import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.HistonesExperimentGTRDType;
import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.MNaseExperimentGTRDType;
import biouml.plugins.gtrd.analysis.CellsFilteringParametersBeanInfo.ExpFactorsLocal;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysis.FakeProgress;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.classification.ClassificationUnitAsSQL;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;

public class CellsFiltering extends AnalysisMethodSupport<CellsFilteringParameters> 
{
	private static SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss ");

	public CellsFiltering (DataCollection<?> origin, String name)

	{
		super( origin, name, new CellsFilteringParameters() );

	}

	@Override
	public Object justAnalyzeAndPut() throws Exception
	{


		ru.biosoft.access.core.DataElementPath pathToOutTable = parameters.getPathToTables();
		NodeInfo[] choosedCellType = parameters.getAvailableCellTypes();
		ArrayList<NodeInfo> expPaths = getChoosedExperiments();
		ArrayList<Experiment> chipExp = new ArrayList<>();
		ArrayList<Experiment> chipExoExp = new ArrayList<>();
		ArrayList<Experiment> dnaseExp = new ArrayList<>();
		ArrayList<Experiment> atacExp = new ArrayList<>();
		ArrayList<Experiment> faireExp = new ArrayList<>();
		ArrayList<Experiment> mnaseExp = new ArrayList<>();
		ArrayList<Experiment> histExp = new ArrayList<>();
		ArrayList<TableDataCollection> result = new ArrayList<TableDataCollection>();


		//TODO refactor duplicating lines to one method
		
		FakeProgress progress = new FakeProgress( jobControl, 10000 );
        progress.start();
		for (NodeInfo cT : choosedCellType)
		{
			if (cT.getName() == "All")
			{
				String[] allCellTypes = takeAllCellTypes(cT.getPath());
				log.info(formatter.format(new Date()) + "Found " + allCellTypes.length + " сell types and tissues" );
				for (String cell : allCellTypes)
				{
					for (NodeInfo extype : expPaths)
					{
						if (extype.getId().equals("chip"))
						{
							chipExp.addAll(getExperimentsForCell(cell, extype.getPath()));
							log.info(formatter.format(new Date()) + "Found " + chipExp.size() + " ChIP-seq experiments" );
						}
						else if (extype.getId().equals("chip_exo"))
						{
							chipExoExp.addAll(getExperimentsForCell(cell, extype.getPath()));
							log.info(formatter.format(new Date()) + "Found " + chipExoExp.size() + " ChIP-exo experiments" );
						}
						else if (extype.getId().equals("mnase"))
						{
							mnaseExp.addAll(getExperimentsForCell(cell, extype.getPath()));
							log.info(formatter.format(new Date()) + "Found " + mnaseExp.size() + " MNase-seq experiments" );
						}
						else if (extype.getId().equals("dnase"))
						{
							dnaseExp.addAll(getExperimentsForCell(cell, extype.getPath()));
							log.info(formatter.format(new Date()) + "Found " + dnaseExp.size() + " DNase-seq experiments" );
						}
						else if (extype.getId().equals("atac"))
						{
							atacExp.addAll(getExperimentsForCell(cell, extype.getPath()));
							log.info(formatter.format(new Date()) + "Found " + atacExp.size() + " ATAC-seq experiments" );
						}
						else if (extype.getId().equals("faire"))
						{
							faireExp.addAll(getExperimentsForCell(cell, extype.getPath()));
							log.info(formatter.format(new Date()) + "Found " + faireExp.size() + " FAIRE-seq experiments" );
						}
						else if (extype.getId().equals("hist"))
						{
							histExp.addAll(getExperimentsForCell(cell, extype.getPath()));
							log.info(formatter.format(new Date()) + "Found " + histExp.size() + " Histones modifications ChIP-seq experiments" );
						}
					}
				}
				break;

			}
			else 
			{

				for (NodeInfo extype : expPaths)
				{
					if (extype.getId().equals("chip"))
					{
						chipExp.addAll(getExperimentsForCell(cT.getId(), extype.getPath()));
						log.info(formatter.format(new Date()) + "Found " + chipExp.size() + " ChIP-seq experiments" );
					}
					else if (extype.getId().equals("chip_exo"))
					{
						chipExoExp.addAll(getExperimentsForCell(cT.getId(), extype.getPath()));
						log.info(formatter.format(new Date()) + "Found " + chipExoExp.size() + " ChIP-exo experiments" );
					}
					else if (extype.getId().equals("mnase"))
					{
						mnaseExp.addAll(getExperimentsForCell(cT.getId(), extype.getPath()));
						log.info(formatter.format(new Date()) + "Found " + mnaseExp.size() + " MNase-seq experiments" );
					}
					else if (extype.getId().equals("dnase"))
					{
						dnaseExp.addAll(getExperimentsForCell(cT.getId(), extype.getPath()));
						log.info(formatter.format(new Date()) + "Found " + dnaseExp.size() + " DNase-seq experiments" );
					}
					else if (extype.getId().equals("atac"))
					{
						dnaseExp.addAll(getExperimentsForCell(cT.getId(), extype.getPath()));
						log.info(formatter.format(new Date()) + "Found " + atacExp.size() + " ATAC-seq experiments" );
					}
					else if (extype.getId().equals("faire"))
					{
						dnaseExp.addAll(getExperimentsForCell(cT.getId(), extype.getPath()));
						log.info(formatter.format(new Date()) + "Found " + faireExp.size() + " FAIRE-seq experiments" );
					}
					else if (extype.getId().equals("hist"))
					{
						histExp.addAll(getExperimentsForCell(cT.getId(), extype.getPath()));
						log.info(formatter.format(new Date()) + "Found " + histExp.size() + " Histones modifications ChIP-seq experiments" );
					}
				}
			}
		}

		if (!chipExp.isEmpty())
		{
			ru.biosoft.access.core.DataElementPath chipTablePath = DataElementPath.create( pathToOutTable + "/" + "ChIP-seq" );
			if ( chipTablePath.exists() )
				chipTablePath.remove();
			TableDataCollection chTable = prepareTable(chipExp, chipTablePath, "chip");
			result.add(chTable);
		}
		if (!chipExoExp.isEmpty())
		{
			ru.biosoft.access.core.DataElementPath chExoTablePath = DataElementPath.create( pathToOutTable + "/" + "ChIP-exo" );
			if ( chExoTablePath.exists() )
				chExoTablePath.remove();
			TableDataCollection chExoTable = prepareTable(chipExoExp, chExoTablePath, "chip_exo");
			result.add(chExoTable);
		}

		if (!dnaseExp.isEmpty())
		{
			ru.biosoft.access.core.DataElementPath dnTablePath = DataElementPath.create( pathToOutTable + "/" + "DNase-seq" );
			if ( dnTablePath.exists() )
				dnTablePath.remove();
			TableDataCollection dnTable = prepareTable(dnaseExp, dnTablePath, "dnase");
			result.add(dnTable);
		}
		if (!dnaseExp.isEmpty())
		{
			ru.biosoft.access.core.DataElementPath dnTablePath = DataElementPath.create( pathToOutTable + "/" + "ATAC-seq" );
			if ( dnTablePath.exists() )
				dnTablePath.remove();
			TableDataCollection atacTable = prepareTable(dnaseExp, dnTablePath, "atac");
			result.add(atacTable);
		}
		if (!dnaseExp.isEmpty())
		{
			ru.biosoft.access.core.DataElementPath dnTablePath = DataElementPath.create( pathToOutTable + "/" + "FAIRE-seq" );
			if ( dnTablePath.exists() )
				dnTablePath.remove();
			TableDataCollection faireTable = prepareTable(dnaseExp, dnTablePath, "faire");
			result.add(faireTable);
		}
		if (!mnaseExp.isEmpty())
		{
			ru.biosoft.access.core.DataElementPath mnTablePath = DataElementPath.create( pathToOutTable + "/" + "MNase-seq" );
			if ( mnTablePath.exists() )
				mnTablePath.remove();
			TableDataCollection mnTable = prepareTable(mnaseExp, mnTablePath, "mnase");
			result.add(mnTable);
		}

		if (!histExp.isEmpty())
		{
			ru.biosoft.access.core.DataElementPath hisTablePath = DataElementPath.create( pathToOutTable + "/" + "ChIP-seq HM" );
			if ( hisTablePath.exists() )
				hisTablePath.remove();
			TableDataCollection hisTable = prepareTable(histExp, hisTablePath, "hist");
			result.add(hisTable);
		}

		log.info(formatter.format(new Date()) + "Finished" );
		progress.stop();

		return result.toArray();

	}

	public String[] takeAllCellTypes(DataElementPath allCellTypesPath)
	{
		ru.biosoft.access.core.DataElementPath level = allCellTypesPath;
		ru.biosoft.access.core.DataElementPath[] cellsPaths = level.getChildrenArray();
		ArrayList<String> result = new ArrayList<>();
		String organism = parameters.getOrganism();
		for (DataElementPath cellPath : cellsPaths)
		{
			String description = cellPath.getDataElement(ClassificationUnitAsSQL.class).getDescription();
			String cellId = cellPath.getDataElement(ClassificationUnitAsSQL.class).getName();

			if (cellId.startsWith( "GTRD" ))
			{
				String [] splitedDescr = description.split(";");
				String cellOrganism = splitedDescr[0].split(":")[1];
				if (cellOrganism.equals(organism))
				{
					result.add(cellId);
				}
			}
			else
			{
				result.addAll(takeAllCellTypesFromNodes(cellPath, organism)); 
			}
		}
		
		HashSet<String> uniqueCells = new HashSet<String>(result);
		String[] us = uniqueCells.toArray(new String [uniqueCells.size()]);
		return us;
	}

	public ArrayList<String> takeAllCellTypesFromNodes(DataElementPath pathToLevel, String org)
	{
		ArrayList<String> nodes = new ArrayList<String>();
		ArrayList<String> cells = new ArrayList<String>();

		for ( DataElementPath pth : pathToLevel.getChildrenArray() )
		{
			ArrayList<ArrayList<String>>  fC = parsingNode(pth, org);
			nodes.addAll(fC.get( 0 ));
			cells.addAll(fC.get( 1 ));
		}

		while ( true )
		{
			ArrayList<String> nd = new ArrayList<String>();

			for (String sPath : nodes)
			{
				ArrayList<ArrayList<String>> search = parsingNode(DataElementPath.create( sPath ), org);
				cells.addAll(search.get( 1 ));
				nd.addAll(search.get( 0 ));
			}
			if ( nd.isEmpty() )
				break;
			else
			{
				nodes.clear();
				nodes.addAll( nd );
			}
		}

		return cells;
	}

	public ArrayList<ArrayList<String>> parsingNode (DataElementPath pathToNode, String org)
	{

		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		ArrayList<String> cells = new ArrayList<String>();
		ArrayList<String> nodes = new ArrayList<String>();
		String pathToNodeCell = pathToNode.getDataElement(ClassificationUnitAsSQL.class).getName();
		
		if ( pathToNodeCell.startsWith( "GTRD" ) )
		{
			String description = pathToNode.getDataElement(ClassificationUnitAsSQL.class).getDescription();
			String [] splitedDescr = description.split(";");
			String cellOrganism = splitedDescr[0].split(":")[1];
			if (cellOrganism.equals( org ))
				cells.add( pathToNodeCell );
		}
		
		else
		{
			ru.biosoft.access.core.DataElementPath[] nodeChildren = pathToNode.getChildrenArray();
			for (DataElementPath cellPath : nodeChildren)
			{
	
				String description = cellPath.getDataElement(ClassificationUnitAsSQL.class).getDescription();
				String cellId = cellPath.getDataElement(ClassificationUnitAsSQL.class).getName();
	
				if (cellId.startsWith( "GTRD" ))
				{
					String [] splitedDescr = description.split(";");
					String cellOrganism = splitedDescr[0].split(":")[1];
					if ( cellOrganism.equals( org ) )
						cells.add( cellId );
				}
				else
					nodes.add( cellPath.toString() );
			}
		}

		result.add(nodes);
		result.add(cells);

		return result;
	}

	public TableDataCollection prepareTable(ArrayList<Experiment> filredExp, DataElementPath tPath, String type)
	{
		TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(tPath);

		if (type.equals("chip"))
		{
			table.getColumnModel().addColumn("Cell type", String.class);
			table.getColumnModel().addColumn("Experimental factors", String.class);
			table.getColumnModel().addColumn("Antibody", String.class);
			table.getColumnModel().addColumn("Target class", String.class);
			table.getColumnModel().addColumn("Target name", String.class);
			table.getColumnModel().addColumn("Peaks", DataElementPath.class);
			table.getColumnModel().addColumn("Control", DataElementPath.class);
			table.getColumnModel().addColumn("Specie", String.class);
			table.getColumnModel().addColumn("External References", String.class);
			for (Experiment exp : filredExp)
			{
				ChIPseqExperiment chex = (ChIPseqExperiment) exp;
				String exNm = chex.getName();
				String cell = chex.getCell().getTitle();
				String tr = chex.getTreatment();
				String ab = chex.getAntibody();
				String tgCl = chex.getTfClassId();
				String tg = chex.getTfTitle();
				ru.biosoft.access.core.DataElementPath pk = chex.getPeak();
				ru.biosoft.access.core.DataElementPath cont = chex.getControl();
				String sp = chex.getSpecie().getLatinName();
				String exRef = chex.getExternalReferences();

				TableDataCollectionUtils.addRow(table, exNm, new Object[] {cell, tr, ab, tgCl, tg, pk, cont, sp, exRef });
			}
			table.setReferenceType(ReferenceTypeRegistry.getReferenceType(ExperimentGTRDType.class).toString());
			tPath.save(table);
			//table.finalizeAddition();
			//CollectionFactory.save(table);
		}

		if (type.equals("chip_exo"))
		{
			table.getColumnModel().addColumn("Cell type", String.class);
			table.getColumnModel().addColumn("Experimental factors", String.class);
			table.getColumnModel().addColumn("Antibody", String.class);
			table.getColumnModel().addColumn("Target class", String.class);
			table.getColumnModel().addColumn("Target name", String.class);
			table.getColumnModel().addColumn("Peaks", DataElementPath.class);
			table.getColumnModel().addColumn("Control", DataElementPath.class);
			table.getColumnModel().addColumn("Specie", String.class);
			table.getColumnModel().addColumn("External References", String.class);
			for (Experiment exp : filredExp)
			{
				ChIPexoExperiment chex = (ChIPexoExperiment) exp;
				String exNm = chex.getName();
				String cell = chex.getCell().getTitle();
				String tr = chex.getTreatment();
				String ab = chex.getAntibody();
				String tgCl = chex.getTfClassId();
				String tg = chex.getTfTitle();
				ru.biosoft.access.core.DataElementPath pk = chex.getPeaksByPeakCaller("gem");
				ru.biosoft.access.core.DataElementPath cont = chex.getControl();
				String sp = chex.getSpecie().getLatinName();
				String exRef = chex.getExternalReferences();

				TableDataCollectionUtils.addRow(table, exNm, new Object[] {cell, tr, ab, tgCl, tg, pk, cont, sp, exRef });
			}
			table.setReferenceType(ReferenceTypeRegistry.getReferenceType(ChIPexoExperimentGTRDType.class).toString());
			tPath.save(table);
			//table.finalizeAddition();
			//CollectionFactory.save(table);
		}

		else if (type.equals("dnase"))
		{
			table.getColumnModel().addColumn("Cell type", String.class);
			table.getColumnModel().addColumn("Experimental factors", String.class);
			table.getColumnModel().addColumn("Peaks", DataElementPathSet.class);
			table.getColumnModel().addColumn("Specie", String.class);
			table.getColumnModel().addColumn("External References", String.class);
			for (Experiment exp : filredExp)
			{
				DNaseExperiment dex = (DNaseExperiment) exp;
				String exNm = dex.getName();
				String cell = dex.getCell().getTitle();
				String tr = dex.getTreatment();
				DataElementPathSet pk = dex.getPeaksByPeakCaller("macs2");
				String sp = dex.getSpecie().getLatinName();
				String exRef = dex.getExternalReferences();

				TableDataCollectionUtils.addRow(table, exNm, new Object[] {cell, tr, pk, sp, exRef });
			}
			table.setReferenceType(ReferenceTypeRegistry.getReferenceType(DnaseExperimentGTRDType.class).toString());
			tPath.save(table);
			//table.finalizeAddition();
			//CollectionFactory.save(table);
		}
		else if (type.equals("atac"))
		{
			table.getColumnModel().addColumn("Cell type", String.class);
			table.getColumnModel().addColumn("Experimental factors", String.class);
			table.getColumnModel().addColumn("Peaks", DataElementPathSet.class);
			table.getColumnModel().addColumn("Specie", String.class);
			table.getColumnModel().addColumn("External References", String.class);
			for (Experiment exp : filredExp)
			{
				ATACExperiment aex = (ATACExperiment) exp;
				String exNm = aex.getName();
				String cell = aex.getCell().getTitle();
				String tr = aex.getTreatment();
				ru.biosoft.access.core.DataElementPath pk = aex.getPeaksByPeakCaller("macs2");
				String sp = aex.getSpecie().getLatinName();
				String exRef = aex.getExternalReferences();

				TableDataCollectionUtils.addRow(table, exNm, new Object[] {cell, tr, pk, sp, exRef });
			}
			table.setReferenceType(ReferenceTypeRegistry.getReferenceType(DnaseExperimentGTRDType.class).toString());
			tPath.save(table);
			//table.finalizeAddition();
			//CollectionFactory.save(table);
		}
		else if (type.equals("faire"))
		{
			table.getColumnModel().addColumn("Cell type", String.class);
			table.getColumnModel().addColumn("Experimental factors", String.class);
			table.getColumnModel().addColumn("Peaks", DataElementPathSet.class);
			table.getColumnModel().addColumn("Specie", String.class);
			table.getColumnModel().addColumn("External References", String.class);
			for (Experiment exp : filredExp)
			{
				FAIREExperiment fex = (FAIREExperiment) exp;
				String exNm = fex.getName();
				String cell = fex.getCell().getTitle();
				String tr = fex.getTreatment();
				ru.biosoft.access.core.DataElementPath pk = fex.getPeaksByPeakCaller("macs2");
				String sp = fex.getSpecie().getLatinName();
				String exRef = fex.getExternalReferences();

				TableDataCollectionUtils.addRow(table, exNm, new Object[] {cell, tr, pk, sp, exRef });
			}
			table.setReferenceType(ReferenceTypeRegistry.getReferenceType(DnaseExperimentGTRDType.class).toString());
			tPath.save(table);
			//table.finalizeAddition();
			//CollectionFactory.save(table);
		}
		else if (type.equals("mnase"))
		{
			table.getColumnModel().addColumn("Cell type", String.class);
			table.getColumnModel().addColumn("Experimental factors", String.class);
			table.getColumnModel().addColumn("Peaks", DataElementPath.class);
			table.getColumnModel().addColumn("Specie", String.class);
			table.getColumnModel().addColumn("External References", String.class);
			for (Experiment exp : filredExp)
			{
				MNaseExperiment chex = (MNaseExperiment) exp;
				String exNm = chex.getName();
				String cell = chex.getCell().getTitle();
				String tr = chex.getTreatment();
				ru.biosoft.access.core.DataElementPath pk = chex.getPeaksByPeakCaller("danpos2");
				String sp = chex.getSpecie().getLatinName();
				String exRef = chex.getExternalReferences();

				TableDataCollectionUtils.addRow(table, exNm, new Object[] {cell, tr, pk, sp, exRef });
			}
			table.setReferenceType(ReferenceTypeRegistry.getReferenceType(MNaseExperimentGTRDType.class).toString());
			tPath.save(table);
			//table.finalizeAddition();
			//CollectionFactory.save(table);
		}
		if (type.equals("hist"))
		{
			table.getColumnModel().addColumn("Cell type", String.class);
			table.getColumnModel().addColumn("Experimental factors", String.class);
			table.getColumnModel().addColumn("Antibody", String.class);
			table.getColumnModel().addColumn("Target name", String.class);
			table.getColumnModel().addColumn("Peaks", DataElementPath.class);
			table.getColumnModel().addColumn("Control", DataElementPath.class);
			table.getColumnModel().addColumn("Specie", String.class);
			table.getColumnModel().addColumn("External References", String.class);
			for (Experiment exp : filredExp)
			{
				HistonesExperiment chex = (HistonesExperiment) exp;
				String exNm = chex.getName();
				String cell = chex.getCell().getTitle();
				String tr = chex.getTreatment();
				String ab = chex.getAntibody();
				String tg = chex.getTarget();
				ru.biosoft.access.core.DataElementPath pk = chex.getPeakByPeakCaller("macs2");
				ru.biosoft.access.core.DataElementPath cont = chex.getControl();
				String sp = chex.getSpecie().getLatinName();
				String exRef = chex.getExternalReferences();

				TableDataCollectionUtils.addRow(table, exNm, new Object[] {cell, tr, ab, tg, pk, cont, sp, exRef });
			}
			table.setReferenceType(ReferenceTypeRegistry.getReferenceType(HistonesExperimentGTRDType.class).toString());
			tPath.save(table);
			//table.finalizeAddition();
			//CollectionFactory.save(table);
		}
		return table;
	}

	public  ArrayList<NodeInfo> getChoosedExperiments()
	{

		ArrayList<NodeInfo> choosedExpeimentsTypes = new ArrayList<>();

		NodeInfo[] types = parameters.getChoosedExperimentsTypes();
		for (NodeInfo type : types )
		{
			if (type.getName().equals("All"))
			{
				choosedExpeimentsTypes.addAll(parameters.getAvExperimentsTypes());
				break;
			}
			choosedExpeimentsTypes.add(type);
		}


		return choosedExpeimentsTypes;

	}

	public List<String> gteChoosedExperimentalFactors()
	{
		ExpFactorsLocal[] choosedExpFactors = parameters.getFactorSources();
		List<String> result = new ArrayList<String>();
		for (ExpFactorsLocal expF : choosedExpFactors)
		{
			NodeInfo factor = expF.checkLevelOfExpFac();
			result.add(factor.getId());

		}


		return result;
	}

	public ArrayList<Experiment> getExperimentsForCell(String cellId, DataElementPath expType)
	{

		ArrayList<Experiment> correctExperiments = new ArrayList<Experiment>();
		String gtrdCell = cellId.split(":")[1];
		ru.biosoft.access.core.DataElementPath[] experiments = expType.getChildrenArray();
		for (DataElementPath pathToexperiment : experiments)
		{
			Experiment experiment = pathToexperiment.getDataElement(Experiment.class);
			String expCellId = experiment.getCell().getName();
			List<ExperimentalFactor> expfactors = experiment.getExperimentalFactors();
			if (expCellId.equals(gtrdCell))
			{
				if (parameters.isSelectExpFactors())
				{
					List<String> choosedFactors = gteChoosedExperimentalFactors();
					for (ExperimentalFactor exF : expfactors)
					{
						if (choosedFactors.contains(exF.getFactorId()))
						{
							correctExperiments.add(experiment);
							break;
						}
					}
				}
				else
					correctExperiments.add(experiment);
			}
		}

		return correctExperiments;
	}


	public static class NodeInfo implements JSONBean
	{
		private String name;
		private String id;
		private DataElementPath path;

		public NodeInfo(String name, String id, DataElementPath path)
		{
			this.name = name;
			this.id = id;
			this.path = path;
		}
		
		public NodeInfo()
		{
			
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}

		public DataElementPath getPath(){
			return path;
		}

		public void setPath(DataElementPath path) {
			this.path = path;
		}

		public String toString() 
		{
			if ( name == null)
				return "empty";
			return name;
		}

	}
	
	public static class NodeInfoBeanInfo extends BeanInfoEx2<NodeInfo>
	{
		public NodeInfoBeanInfo() 
		{
			super(NodeInfo.class);
		}
		@Override
        protected void initProperties() throws Exception
        {
        	property("name").add();
			property("id").add();
			property("path").add();
        
        }
	}
	

}
