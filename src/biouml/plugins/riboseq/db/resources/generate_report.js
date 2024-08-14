importClass(Packages.org.jfree.data.general.DefaultPieDataset);
importClass(Packages.org.jfree.chart.ChartFactory);

var r = report.create("RiboSeqDB");
r.addHeader("Statistics");

var experiments = data.get("databases/RiboSeqDB/Data/experiments");
var totalExperiments = experiments.getSize();
var bySpecies = {};
var byPlatforms = {};
var it = experiments.iterator();
var datasets = {};
while(it.hasNext())
{
  var e = it.next();
  var s = e.getSpecies().getCommonName();
  if(bySpecies[s]===undefined) bySpecies[s] = 1; else bySpecies[s] = bySpecies[s] + 1;
  var p = e.getSequencingPlatform().getTitle();
  if(byPlatforms[p]===undefined) byPlatforms[p] = 1; else byPlatforms[p] = byPlatforms[p] + 1;
  var srp = e.getSraProjectId();
  datasets[srp]=true;
}

var mexps = data.get("databases/RiboSeqDB/Data/mRNA experiments");
it = mexps.iterator();
var mDatasets = {};
while(it.hasNext())
{
  var e = it.next();
  var srp = e.getSraProjectId();
  mDatasets[srp] = true;
}

var allDatasets = {};
for(var srp in mDatasets) allDatasets[srp] = true;
for(var srp in datasets) allDatasets[srp] = true;


var dataset = new DefaultPieDataset();
for(var s in bySpecies)
{
  dataset.setValue(new java.lang.String(s + " (" + bySpecies[s] + ")"), bySpecies[s]);  
}
chart = ChartFactory.createPieChart( "", dataset, false, true, false );
var img = chart.createBufferedImage(400,400);
r.addSubHeader("RiboSeq experiments by species");
r.addImage(img, "by-species", "RiboSeq experiments by species" );

dataset = new DefaultPieDataset();
for(var p in byPlatforms)
{
  dataset.setValue( p, byPlatforms[p]);  
}
chart = ChartFactory.createPieChart( "", dataset, false, true, false );
chart.getPlot().setToolTipGenerator(null);
var img = chart.createBufferedImage(600,600);
r.addSubHeader("RiboSeq experiments by sequencing platforms");
r.addImage(img, "by-platforms", "RiboSeq experiments by sequencing platforms" );

r.addSubHeader("Summary table");
r.addTable(["", "Count"],[
  ['<a href="#de=databases/RiboSeqDB/Data/experiments">RiboSeq experiments</a>', data.get("databases/RiboSeqDB/Data/experiments").getSize()],
  ['RiboSeq data sets', Object.keys(datasets).length],
  ['<a href="#de=databases/RiboSeqDB/Data/cells">Distinct cells</a>', data.get("databases/RiboSeqDB/Data/cells").getSize() ],
  ['<a href="#de=databases/RiboSeqDB/Data/conditions">Distinct experimental conditions</a>', data.get("databases/RiboSeqDB/Data/conditions").getSize() ],
  ['<a href="#de=databases/RiboSeqDB/Data/sequencing%20platforms">Sequencing platforms</a>', data.get("databases/RiboSeqDB/Data/sequencing platforms").getSize() ],
  ['<a href="#de=databases/RiboSeqDB/Data/mRNA%20experiments">mRNA-seq experiments</a>', data.get("databases/RiboSeqDB/Data/mRNA experiments").getSize()],
  ['mRNA-seq data sets', Object.keys(mDatasets).length],
  ['Total data sets', Object.keys(allDatasets).length]
], "", false);

r.store("data/Collaboration/test/Data/temp/statistics");