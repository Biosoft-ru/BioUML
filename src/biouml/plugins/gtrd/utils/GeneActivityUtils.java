/* $Id$ */

package biouml.plugins.gtrd.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.DataMatrixString.DataMatrixChar;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author yura
 *
 */

public class GeneActivityUtils
{
    /********************** MessageBundle : start *************************/ 
    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_START_POSITION = "Start position of promoter region";
        public static final String PD_START_POSITION = "Start position of promoter region";
        
        public static final String PN_FINISH_POSITION = "Finish position of promoter region";
        public static final String PD_FINISH_POSITION = "Finish position of promoter region";
    }
    /********************** MessageBundle : end *************************/ 

    /********************** PromoterRegion : start **********************/ 
    public static class PromoterRegion extends OptionEx
    {
        private int startPosition;
        private int finishPosition;

        @PropertyName(MessageBundle.PN_START_POSITION)
        @PropertyDescription(MessageBundle.PD_START_POSITION)
        public int getStartPosition()
        {
            return startPosition;
        }
        public void setStartPosition(int startPosition)
        {
            Object oldValue = this.startPosition;
            this.startPosition = startPosition;
            firePropertyChange("startPosition", oldValue, startPosition);
        }
        
        @PropertyName(MessageBundle.PN_FINISH_POSITION)
        @PropertyDescription(MessageBundle.PD_FINISH_POSITION)
        public int getFinishPosition()
        {
            return finishPosition;
        }
        public void setFinishPosition(int finishPosition)
        {
            Object oldValue = this.finishPosition;
            this.finishPosition = finishPosition;
            firePropertyChange("finishPosition", oldValue, finishPosition);
        }
    }
    /********************** PromoterRegion : end **********************/
    
    /********************** PromoterRegionBeanInfo : start ************/

    public static class PromoterRegionBeanInfo extends BeanInfoEx2<PromoterRegion>
    {
        public PromoterRegionBeanInfo()
        {
            super(PromoterRegion.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("startPosition");
            add("finishPosition");
        }
    }
    /********************** PromoterRegionBeanInfo : end **************/
   
    /********************** IndicatorMatrixConstructor : start ********/ 
    public static class IndicatorMatrixConstructor
    {
        private String[] tssNames, chromosomeNames;
        private int[] tssPositions, tssStrands;
        private int boundaryMin, boundaryMax;
        private Interval[] promoterIntervals, promoterIntervalsForInverseStrand;
        private DataElementPath pathToSequences;
        
        public IndicatorMatrixConstructor(DataElementPath pathToSequences, DataElementPath pathToFileWithTsss, Interval[] promoterIntervals)
        {
            this.pathToSequences = pathToSequences;
            this.promoterIntervals = promoterIntervals;
            readTsssInFile(pathToFileWithTsss);
            getInternalParameters();
        }
        
        public DataMatrixChar getIndicatorMatrix(DataElementPath[] pathsToTracks, String[] siteSetNames, JobControl jobControl, int from, int to)
        {
            DataMatrixChar[] dmcs = new DataMatrixChar[pathsToTracks.length];
            int difference = to - from;
            for( int i = 0; i < pathsToTracks.length; i++ )
            {
                if( jobControl != null )
                    jobControl.setPreparedness(from + (i + 1) * difference / pathsToTracks.length);
                dmcs[i] = getIndicatorMatrix(pathsToTracks[i].getDataElement(Track.class), siteSetNames[i]);
            }
            return DataMatrixChar.concatinateDataMatricesColumnWise(dmcs);
        }

        private DataMatrixChar getIndicatorMatrix(Track track, String siteSetName)
        {
            // 1. Initialization of indicator matrix.
            int n = chromosomeNames.length, m = promoterIntervals.length;
            char[][] matrix = new char[n][m];
            for( int i = 0; i < n; i++ )
                matrix[i] = UtilsForArray.getConstantArray(m, '0');
            
            // 2. Calculation of columnNames.
            String[] columnNames = new String[promoterIntervals.length];
            for( int i = 0; i < promoterIntervals.length; i++ )
                columnNames[i] = siteSetName + "_" + Integer.toString(promoterIntervals[i].getFrom()) + "_" + Integer.toString(promoterIntervals[i].getTo());

            // 3. Calculation of indicator matrix.
            String oldValue = "--", sequence = null;
            for( int i = 0; i < n; i++ )
            {
                if( ! chromosomeNames[i].equals(oldValue) )
                {
                    sequence = pathToSequences.getChildPath(chromosomeNames[i]).toString();
                    oldValue = chromosomeNames[i];
                }
                DataCollection<Site> dc = tssStrands[i] == StrandType.STRAND_PLUS ? track.getSites(sequence, tssPositions[i] + boundaryMin, tssPositions[i] + boundaryMax) : track.getSites(sequence, tssPositions[i] - boundaryMax, tssPositions[i] - boundaryMin);
                if( dc.getSize() < 1 ) continue;
                Interval[] intervals = tssStrands[i] == StrandType.STRAND_PLUS ? promoterIntervals : promoterIntervalsForInverseStrand;
                for( Site site : dc )
                {
                    Interval interval = new Interval(site.getFrom(), site.getTo());
                    for( int j = 0; j < intervals.length; j++ )
                        if( new Interval(tssPositions[i] + intervals[j].getFrom(), tssPositions[i] + intervals[j].getTo()).intersect(interval) != null )
                            matrix[i][j] = '1';
                }
            }
            return new DataMatrixChar(tssNames, columnNames, matrix);
        }
        
        private void readTsssInFile(DataElementPath pathToFileWithTsss)
        {
            Object[] objects = TableAndFileUtils.readMatrixOrSubmatixInFile(pathToFileWithTsss, new String[]{"chromosome"}, TableAndFileUtils.STRING_TYPE);
            tssNames = (String[])objects[0];
            chromosomeNames = MatrixUtils.getColumn((String[][])objects[2], 0);
            objects = TableAndFileUtils.readMatrixOrSubmatixInFile(pathToFileWithTsss, new String[]{"TSS_start", "strand"}, TableAndFileUtils.INT_TYPE);
            tssPositions = MatrixUtils.getColumn((int[][])objects[2], 0);
            tssStrands = MatrixUtils.getColumn((int[][])objects[2], 1);
        }
        
        // Calculation of internal parameters: promoterIntervalsForInverseStrand, boundaryMin, boundaryMax);
        private void getInternalParameters()
        {
            promoterIntervalsForInverseStrand = new Interval[promoterIntervals.length];
            for( int i = 0; i < promoterIntervals.length; i++ )
                promoterIntervalsForInverseStrand[i] = new Interval(-promoterIntervals[i].getTo(), -promoterIntervals[i].getFrom());
            boundaryMin = promoterIntervals[0].getFrom();
            boundaryMax = promoterIntervals[0].getTo();
            for( int i = 1; i < promoterIntervals.length; i++ )
            {
                boundaryMin = Math.min(boundaryMin, promoterIntervals[i].getFrom());
                boundaryMax = Math.max(boundaryMax, promoterIntervals[i].getTo());
            }
        }
    }
    /********************** IndicatorMatrixConstructor : end ********/ 
    
    public static Object[] calculateIndicatorAndFrequencyMatrices(DataElementPath pathToSequences, DataElementPath pathToFolderWithTracks, String[] trackNames, PromoterRegion[] promoterRegions, DataElementPath pathToFileWithTsss, JobControl jobControl)
    {
        // 1. Define promoterIntervals.
        Interval[] promoterIntervals = new Interval[promoterRegions.length];
        for( int i = 0; i < promoterIntervals.length; i++ )
            promoterIntervals[i] = new Interval(promoterRegions[i].getStartPosition(), promoterRegions[i].getFinishPosition());

        // 2. Define pathsToTracks.
        ru.biosoft.access.core.DataElementPath[] pathsToTracks = new ru.biosoft.access.core.DataElementPath[trackNames.length];
        for( int i = 0; i < trackNames.length; i++ )
            pathsToTracks[i] = pathToFolderWithTracks.getChildPath(trackNames[i]);

        // 3. Calculate indicator matrix and frequency matrix and write them into files.
        IndicatorMatrixConstructor imc = new IndicatorMatrixConstructor(pathToSequences, pathToFileWithTsss, promoterIntervals);
        DataMatrixChar indicatorMatrix = imc.getIndicatorMatrix(pathsToTracks, trackNames, jobControl, 0, 100);
        DataMatrix frequencyMatrix = getFrequenciesInPromoterRegions(indicatorMatrix);
        return new Object[]{indicatorMatrix, frequencyMatrix};
    }
    
    private static DataMatrix getFrequenciesInPromoterRegions(DataMatrixChar indicatorMatrix)
    {
        // 1. Define rowNames and columnNames.
        String[] array = indicatorMatrix.getColumnNames(), rowNames = indicatorMatrix.getRowNames();
        Set<String> set = new HashSet<>(); 
        for( String s : array )
        {
            String[] ss = s.split("_");
            if( ss.length < 3 ) continue;
            set.add(ss[ss.length - 2] + "_" + ss[ss.length - 1]);
        }
        String[] columnNames = set.toArray(new String[0]);
        
        //2. Calculate frequencies in matrix and in counts.
        char[][] indicators = indicatorMatrix.getMatrix();
        double[][] matrix = new double[rowNames.length][columnNames.length];
        int[] counts = new int[columnNames.length];
        for( int j = 0; j < array.length; j++ )
        {
            String[] ss = array[j].split("_");
            if( ss.length < 3 ) continue;
            int index = ArrayUtils.indexOf(columnNames, ss[ss.length - 2] + "_" + ss[ss.length - 1]);
            counts[index]++;
            for( int i = 0; i < rowNames.length; i++ )
                if( indicators[i][j] == '1' )
                    matrix[i][index] += 1.0;
        }
        
        // 3. Normalize columns of matrix.
        for( int i = 0; i < rowNames.length; i++ )
            for( int j = 0; j < columnNames.length; j++ )
                matrix[i][j] /= (double)counts[j];
        return new DataMatrix(rowNames, columnNames, matrix);
    }
    
    static Logger log = Logger.getLogger(GeneActivityUtils.class.getName());
}
