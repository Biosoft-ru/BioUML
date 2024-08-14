package biouml.plugins.gtrd.master.meta.json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.Experiment;
import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.master.sites.json.ListSerializer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public class Experiments {
	
	public Map<String, ChIPseqExperiment> chipSeqExps = new HashMap<>();
	public Map<String, ChIPexoExperiment> chipExoExps = new HashMap<>();
	public Map<String, HistonesExperiment> histonesExps = new HashMap<>();
	public Map<String, DNaseExperiment> dnaseExps = new HashMap<>();
	public Map<String, ATACExperiment> atacExps = new HashMap<>();
	public Map<String, MNaseExperiment> mnaseExps = new HashMap<>();
	public Map<String, FAIREExperiment> faireExps = new HashMap<>();
	public Map<String, CellLine> cells = new HashMap<>();
	
	public static Experiments loadFromJson(Path folder) throws IOException
	{
		Experiments res = new Experiments();
		
		res.loadChIPseq(folder);
		res.loadChIPexo(folder);
		res.loadHistones(folder);
		res.loadDNase(folder);
		res.loadATAC(folder);
		res.loadFAIRE(folder);
		res.loadMNase(folder);
		res.loadCells(folder);
		res.linkCellsToExperiments();
		
		return res;
	}
	
	public static void exportFromSQLToJSON(Path outFolder) throws IOException
	{
		Files.createDirectories(outFolder);
		exportChIPseq(outFolder);
		exportChIPexo(outFolder);
		exportHistones(outFolder);
		exportDNase(outFolder);
		exportATAC(outFolder);
		exportFaire(outFolder);
		exportMNase(outFolder);
		exportCells(outFolder);
	}

	private void loadChIPseq(Path folder) throws IOException
	{
		ListSerializer<ChIPseqExperiment> s = new ListSerializer<>(new ChIPSeqExperimentSerializer());
		String jsonStr = new String(Files.readAllBytes(folder.resolve("chipseq.json")));
		for(ChIPseqExperiment exp : s.fromJSON(jsonStr))
			chipSeqExps.put(exp.getName(), exp);
	}
	
	private void loadChIPexo(Path folder) throws IOException
	{
		ListSerializer<ChIPexoExperiment> s = new ListSerializer<>(new ChIPExoExperimentSerializer());
		String jsonStr = new String(Files.readAllBytes(folder.resolve("chipexo.json")));
		for(ChIPexoExperiment exp : s.fromJSON(jsonStr))
			chipExoExps.put(exp.getName(), exp);
	}
	
	private void loadHistones(Path folder) throws IOException
	{
		ListSerializer<HistonesExperiment> s = new ListSerializer<>(new HistoneExperimentSerializer());
		String jsonStr = new String(Files.readAllBytes(folder.resolve("histones.json")));
		for(HistonesExperiment exp : s.fromJSON(jsonStr))
			histonesExps.put(exp.getName(), exp);
	}
	
	private void loadDNase(Path folder) throws IOException
	{
		ListSerializer<DNaseExperiment> s = new ListSerializer<>(new DNaseExperimentSerializer());
		String jsonStr = new String(Files.readAllBytes(folder.resolve("dnase.json")));
		for(DNaseExperiment exp : s.fromJSON(jsonStr))
			dnaseExps.put(exp.getName(), exp);
	}
	
	private void loadATAC(Path folder) throws IOException
	{
		ListSerializer<ATACExperiment> s = new ListSerializer<>(new ATACExperimentSerializer());
		String jsonStr = new String(Files.readAllBytes(folder.resolve("atac.json")));
		for(ATACExperiment exp : s.fromJSON(jsonStr))
			atacExps.put(exp.getName(), exp);
	}
	
	private void loadFAIRE(Path folder) throws IOException
	{
		ListSerializer<FAIREExperiment> s = new ListSerializer<>(new FAIREExperimentSerializer());
		String jsonStr = new String(Files.readAllBytes(folder.resolve("faire.json")));
		for(FAIREExperiment exp : s.fromJSON(jsonStr))
			faireExps.put(exp.getName(), exp);
	}
	
	private void loadMNase(Path folder) throws IOException
	{
		ListSerializer<MNaseExperiment> s = new ListSerializer<>(new MNaseExperimentSerializer());
		String jsonStr = new String(Files.readAllBytes(folder.resolve("mnase.json")));
		for(MNaseExperiment exp : s.fromJSON(jsonStr))
			mnaseExps.put(exp.getName(), exp);
	}

	private void loadCells(Path folder) throws IOException
	{
		ListSerializer<CellLine> s = new ListSerializer<>(new CellSerializer());
		String jsonStr = new String(Files.readAllBytes(folder.resolve("cells.json")));
		for(CellLine exp : s.fromJSON(jsonStr))
			cells.put(exp.getName(), exp);
	}
	
	private void linkCellsToExperiments() {
		linkCellsToExperiments(chipSeqExps.values());
		linkCellsToExperiments(chipExoExps.values());
		linkCellsToExperiments(dnaseExps.values());
		linkCellsToExperiments(histonesExps.values());
		linkCellsToExperiments(mnaseExps.values());
		linkCellsToExperiments(atacExps.values());
		linkCellsToExperiments(faireExps.values());
	}

	private void linkCellsToExperiments(Collection<? extends Experiment> c) {
		for (Experiment exp : c) {
			String cellId = exp.getCell().getName();
			CellLine cell = cells.get(cellId);
			if (cell != null) {
				exp.setCell(cell);
				exp.setSpecie(cell.getSpecies());
			}
			// if not found leave stub cell
		}
	}
	
	
	//Export to JSON

	public static void exportChIPseq(Path outFolder) throws IOException
	{
		DataCollection<ChIPseqExperiment> expDC = DataElementPath.create("databases/GTRD/Data/experiments").getDataCollection(ChIPseqExperiment.class);
		List<ChIPseqExperiment> expList = new ArrayList<>();
		for(ChIPseqExperiment exp : expDC)
			expList.add(exp);
		ListSerializer<ChIPseqExperiment> expListSerializer = new ListSerializer<>(new ChIPSeqExperimentSerializer());
		String jsonStr = expListSerializer.toPrettyJSON(expList);
		Files.write(outFolder.resolve("chipseq.json"), jsonStr.getBytes(StandardCharsets.UTF_8));
	}
	
	public static void exportChIPexo(Path outFolder) throws IOException
	{
		DataCollection<ChIPexoExperiment> expDC = DataElementPath.create("databases/GTRD/Data/ChIP-exo experiments").getDataCollection(ChIPexoExperiment.class);
		List<ChIPexoExperiment> expList = new ArrayList<>();
		for(ChIPexoExperiment exp : expDC)
			expList.add(exp);
		ListSerializer<ChIPexoExperiment> expListSerializer = new ListSerializer<>(new ChIPExoExperimentSerializer());
		String jsonStr = expListSerializer.toPrettyJSON(expList);
		Files.write(outFolder.resolve("chipexo.json"), jsonStr.getBytes(StandardCharsets.UTF_8));
	}
	
	public static void exportHistones(Path outFolder) throws IOException
	{
		DataCollection<HistonesExperiment> expDC = DataElementPath.create("databases/GTRD/Data/ChIP-seq HM experiments").getDataCollection(HistonesExperiment.class);
		List<HistonesExperiment> expList = new ArrayList<>();
		for(HistonesExperiment exp : expDC)
			expList.add(exp);
		ListSerializer<HistonesExperiment> expListSerializer = new ListSerializer<>(new HistoneExperimentSerializer());
		String jsonStr = expListSerializer.toPrettyJSON(expList);
		Files.write(outFolder.resolve("histones.json"), jsonStr.getBytes(StandardCharsets.UTF_8));
	}
	
	public static void exportDNase(Path outFolder) throws IOException
	{
		DataCollection<DNaseExperiment> expDC = DataElementPath.create("databases/GTRD/Data/DNase experiments").getDataCollection(DNaseExperiment.class);
		List<DNaseExperiment> expList = new ArrayList<>();
		for(DNaseExperiment exp : expDC)
			expList.add(exp);
		ListSerializer<DNaseExperiment> expListSerializer = new ListSerializer<>(new DNaseExperimentSerializer());
		String jsonStr = expListSerializer.toPrettyJSON(expList);
		Files.write(outFolder.resolve("dnase.json"), jsonStr.getBytes(StandardCharsets.UTF_8));
	}
	
	public static void exportATAC(Path outFolder) throws IOException
	{
		DataCollection<ATACExperiment> expDC = DataElementPath.create("databases/GTRD/Data/ATAC-seq experiments").getDataCollection(ATACExperiment.class);
		List<ATACExperiment> expList = new ArrayList<>();
		for(ATACExperiment exp : expDC)
			expList.add(exp);
		ListSerializer<ATACExperiment> expListSerializer = new ListSerializer<>(new ATACExperimentSerializer());
		String jsonStr = expListSerializer.toPrettyJSON(expList);
		Files.write(outFolder.resolve("atac.json"), jsonStr.getBytes(StandardCharsets.UTF_8));
	}
	
	public static void exportFaire(Path outFolder) throws IOException
	{
		DataCollection<FAIREExperiment> expDC = DataElementPath.create("databases/GTRD/Data/FAIRE-seq experiments").getDataCollection(FAIREExperiment.class);
		List<FAIREExperiment> expList = new ArrayList<>();
		for(FAIREExperiment exp : expDC)
			expList.add(exp);
		ListSerializer<FAIREExperiment> expListSerializer = new ListSerializer<>(new FAIREExperimentSerializer());
		String jsonStr = expListSerializer.toPrettyJSON(expList);
		Files.write(outFolder.resolve("faire.json"), jsonStr.getBytes(StandardCharsets.UTF_8));
	}
	
	public static void exportMNase(Path outFolder) throws IOException
	{
		DataCollection<MNaseExperiment> expDC = DataElementPath.create("databases/GTRD/Data/MNase-seq experiments").getDataCollection(MNaseExperiment.class);
		List<MNaseExperiment> expList = new ArrayList<>();
		for(MNaseExperiment exp : expDC)
			expList.add(exp);
		ListSerializer<MNaseExperiment> expListSerializer = new ListSerializer<>(new MNaseExperimentSerializer());
		String jsonStr = expListSerializer.toPrettyJSON(expList);
		Files.write(outFolder.resolve("mnase.json"), jsonStr.getBytes(StandardCharsets.UTF_8));
	}
	
	public static void exportCells(Path outFolder) throws IOException
	{
		DataCollection<CellLine> cellDC = DataElementPath.create("databases/GTRD/Dictionaries/cells").getDataCollection(CellLine.class);
		List<CellLine> cellList = new ArrayList<>();
		for(CellLine exp : cellDC)
			cellList.add(exp);
		ListSerializer<CellLine> expListSerializer = new ListSerializer<>(new CellSerializer());
		String jsonStr = expListSerializer.toPrettyJSON(cellList);
		Files.write(outFolder.resolve("cells.json"), jsonStr.getBytes(StandardCharsets.UTF_8));
	}
}
