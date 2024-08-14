package ru.biosoft.bsastats.processors;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.logging.Logger;
import org.mozilla.javascript.NativeArray;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.IntArray;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("Overrepresented K-mers")
@PropertyDescription("Search for K-mers which are represented 3x times per sequence or 5x times per position")
public class OverrepresentedKmers extends AbstractStatisticsProcessor
{
    private static final int KMER_COUNT = 3125;
    private static final byte[] LETTER_TO_CODE = Nucleotide5LetterAlphabet.getInstance().letterToCodeMatrix();
    private static final byte[] CODE_TO_LETTER = Nucleotide5LetterAlphabet.getInstance().codeToLetterMatrix();
    
    private final boolean nKmers[] = new boolean[KMER_COUNT]; // Kmers containing 'N'
    private final IntArray[] kmerCounts = new IntArray[3125]; // 5*5*5*5*5
    private final long[] nucleotideCounts = new long[5];
    private final IntArray positionCounts = new IntArray();
    private long totalKmers = 0;
    private final double[] probs = new double[5];

    @Override
    public void update(byte[] sequence, byte[] qualities)
    {
        if(sequence.length < 5) return;
        int kmerCode = (((LETTER_TO_CODE[sequence[0]]*5)+LETTER_TO_CODE[sequence[1]])*5+LETTER_TO_CODE[sequence[2]])*5+LETTER_TO_CODE[sequence[3]];
        positionCounts.growTo(sequence.length-4);
        for(int i=4; i<sequence.length; i++)
        {
            kmerCode = (kmerCode%625)*5+LETTER_TO_CODE[sequence[i]];
            if(nKmers[kmerCode]) continue;
            IntArray counts = kmerCounts[kmerCode];
            if(counts == null)
            {
                counts = new IntArray();
                kmerCounts[kmerCode] = counts;
            }
            counts.growTo(i-3);
            counts.data()[i-4]++;
            positionCounts.data()[i-4]++;
            totalKmers++;
        }
        for(int i=0; i<sequence.length; i++) nucleotideCounts[LETTER_TO_CODE[sequence[i]]]++;
    }
    
    /**
     * @author lan
     *
     */
    private static final class DoubleValueComparator implements Comparator<String>
    {
        private final int index;
        private final Map<String, double[]> curKmers;
        
        private DoubleValueComparator(Map<String, double[]> curKmers, int index)
        {
            this.curKmers = curKmers;
            this.index = index;
        }
        @Override
        public int compare(String o1, String o2)
        {
            return Double.compare( curKmers.get(o2)[index], curKmers.get(o1)[index] );
        }
    }

    private static class KmerInfo implements Comparable<KmerInfo>
    {
        int kmer;
        double ratio;
        double probability;

        public KmerInfo(int kmer, double ratio, double bestRatio, double probability)
        {
            super();
            this.kmer = kmer;
            this.ratio = ratio;
            this.probability = probability;
        }

        @Override
        public int compareTo(KmerInfo o)
        {
            return Double.compare( o.ratio, ratio );
        }
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        long totalLength = 0;
        for(int i=0; i<4; i++)
        {
            totalLength+=nucleotideCounts[i];
        }
        for(int i=0; i<4; i++)
        {
            probs[i] = ((double)nucleotideCounts[i])/totalLength;
        }
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(resultsFolder, getName());
        ColumnModel columnModel = table.getColumnModel();
        columnModel.addColumn("Count", Integer.class);
        columnModel.addColumn("Observed to expected", Double.class);
        columnModel.addColumn("Max observed to expected", Double.class);
        columnModel.addColumn("Max position", Integer.class);
        List<KmerInfo> kmerInfos = new ArrayList<>();
        for(int i=0; i<kmerCounts.length; i++)
        {
            IntArray counts = kmerCounts[i];
            if(counts == null) continue;
            double probability = getKmerProbability(i);
            int totalKmerCount = 0;
            boolean found = false;
            for(int pos=0; pos<counts.size(); pos++)
            {
                totalKmerCount+=counts.get(pos);
                if(counts.get(pos)>probability*positionCounts.get(pos)*5) found = true;
            }
            double totalRatio = totalKmerCount/probability/totalKmers;
            if(!found && totalRatio<3) continue;
            raiseWarning();
            int bestPos = 0;
            double bestRatio = 0;
            for(int pos=0; pos<counts.size(); pos++)
            {
                double ratio = counts.get(pos)/probability/positionCounts.get(pos);
                if(ratio > bestRatio)
                {
                    bestRatio = ratio;
                    bestPos = pos;
                }
            }
            if(bestRatio>10) raiseError();
            kmerInfos.add(new KmerInfo(i, totalRatio, bestRatio, probability));
            TableDataCollectionUtils.addRow(table, getKmerString(i), new Object[] {
                    totalKmerCount,
                    totalRatio,
                    bestRatio,
                    (bestPos+1)
            }, true);
        }
        table.finalizeAddition();
        if(kmerInfos.isEmpty())
        {
            while(columnModel.getColumnCount() > 0)
                columnModel.removeColumn(0);
            TableDataCollectionUtils.addRow(table, "No overrepresented K-mers found", new Object[0]);
        } else
        {
            TableDataCollectionUtils.setSortOrder(table, "Observed to expected", false);
        }
        resultsFolder.put(table);
        if(kmerInfos.isEmpty()) return;
        
        Collections.sort(kmerInfos);
        kmerInfos = kmerInfos.subList(0, Math.min(6, kmerInfos.size()));
        
        Chart chart = new Chart();
        ChartOptions options = new ChartOptions();
        options.getXAxis().setLabel("Position in read (bp)");
        options.getXAxis().setMin(1.0);
        options.getXAxis().setMax((double)positionCounts.size());
        options.getYAxis().setLabel("Observed to expected");
        chart.setOptions(options);
        
        Color[] colors = new Color[] {Color.RED, Color.GREEN, Color.BLUE, Color.GRAY, Color.CYAN, Color.BLACK};
        
        for(int i=0; i<kmerInfos.size(); i++)
        {
            KmerInfo kmerInfo = kmerInfos.get(i);
            IntArray counts = kmerCounts[kmerInfo.kmer];
            double[][] data = new double[counts.size()][];
            for(int pos=0; pos<counts.size(); pos++)
            {
                data[pos] = new double[] {pos + 1, counts.get(pos) / kmerInfo.probability / positionCounts.get(pos)};
            }
            ChartSeries series = new ChartSeries(data);
            series.setColor(colors[i]);
            series.setLabel(getKmerString(kmerInfo.kmer));
            chart.addSeries(series);
        }
        ChartDataElement image = new ChartDataElement(getName()+" chart", resultsFolder, chart);
        resultsFolder.put(image);
    }

    private String getKmerString(int kmer)
    {
        byte[] result = new byte[5];
        for(int i=4; i>=0; i--)
        {
            result[i] = CODE_TO_LETTER[kmer%5];
            kmer/=5;
        }
        return new String(result).toUpperCase();
    }

    private double getKmerProbability(int kmer)
    {
        double prob = 1;
        for(int i=0; i<5; i++)
        {
            prob*=probs[kmer%5];
            kmer/=5;
        }
        return prob;
    }

    @Override
    public void init(Logger log)
    {
        super.init(log);
        byte n = LETTER_TO_CODE['n'];
        for(int i=0; i<nKmers.length; i++)
        {
            nKmers[i] = (i%5==n) || ((i/5)%5==n) || ((i/25)%5==n) || ((i/125)%5==n) || ((i/625)%5==n);
        }
    }

    @Override
    public String[] getReportItemNames()
    {
        if(getQuality()==Quality.OK) return new String[] {getName()};
        return new String[] {getName(), getName()+" chart"};
    }

    @Override
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws Exception
    {
        Map<ru.biosoft.access.core.DataElementPath, Map<String, double[]>> kmers = new TreeMap<>();
        Set<String> selectedRows = new TreeSet<>();
        for(DataElementPath path: inputReports)
        {
            TableDataCollection table = path.getChildPath(getName()).optDataElement(TableDataCollection.class);
            if(table == null) continue;
            final Map<String, double[]> curKmers = new HashMap<>();
            for(RowDataElement row: table)
            {
                try
                {
                    curKmers.put(row.getName(), new double[] {((Number)row.getValue("Observed to expected")).doubleValue(),
                            ((Number)row.getValue("Max observed to expected")).doubleValue()});
                }
                catch( Exception e )
                {
                }
            }
            List<String> kmerNames = new ArrayList<>(curKmers.keySet());
            if(kmerNames.size() <= 5) selectedRows.addAll(kmerNames);
            else
            {
                Collections.sort(kmerNames, new DoubleValueComparator(curKmers, 0));
                selectedRows.addAll(kmerNames.subList(0, 5));
                Collections.sort(kmerNames, new DoubleValueComparator(curKmers, 1));
                selectedRows.addAll(kmerNames.subList(0, 5));
            }
            kmers.put(path, curKmers);
        }
        String[] columnNames = new String[kmers.size()+1];
        int i=0;
        columnNames[i++] = "5-mer";
        for(DataElementPath path: kmers.keySet()) columnNames[i++] = path.getName();
        NativeArray[] data = new NativeArray[selectedRows.size()];
        int j=0;
        for(String kmer: selectedRows)
        {
            String[] row = new String[kmers.size()+1];
            i=0;
            row[i++] = kmer;
            for(Map<String, double[]> curKmers: kmers.values())
            {
                double[] values = curKmers.get(kmer);
                row[i++] = values == null ? "-" : String.format("%.3f/%.3f", values[0], values[1]);
            }
            data[j++] = new NativeArray(row);
        }
        if(data.length > 0)
        {
            outputReport.addSubHeader(getName());
            outputReport.addTable(new NativeArray(columnNames), new NativeArray(data), "data");
        }
    }
}
