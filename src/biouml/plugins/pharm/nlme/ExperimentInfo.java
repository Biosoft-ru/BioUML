package biouml.plugins.pharm.nlme;

import java.util.ArrayList;
import java.util.List;

public class ExperimentInfo
{
    public double[][] getTableData()
    {
        return tableData;
    }
    
    public String[] getColumnNames()
    {
        return columnNames;
    }
    
    public String[] parameterNames()
    {
        return parameterNames;
    }

    protected double[][] tableData;
    protected String[] columnNames;
    protected String[] parameterNames;
    
    protected int doseIndex;
    protected int timeIndex;
    protected int subjectIndex;
    
    protected double[][] doseTimes;
    protected double[][] doseVals;
    
    public double[][] getDoseTimes()
    {
        return doseTimes;
    }
    
    public double[][] getDoseVals()
    {
        return doseVals;
    }
    
    public void initDosing()
    {
        if( tableData != null )
        {
            List<double[]> doseTimeArray = new ArrayList<>();
            List<double[]> doseValArray = new ArrayList<>();
            double[] subject = tableData[subjectIndex];
            double[] dose = tableData[doseIndex];
            double[] times = tableData[timeIndex];

            int startOfSubject = 0;
            int endOfSubject = 0;
            while( startOfSubject < subject.length )
            {
                double currentSubject = subject[startOfSubject];
                while( currentSubject == subject[endOfSubject] )
                {
                    endOfSubject++;

                    if( endOfSubject >= subject.length )
                        break;
                }

                double[] nextDoseTime = new double[endOfSubject - startOfSubject];
                double[] nextDoseVal = new double[endOfSubject - startOfSubject];
                System.arraycopy(times, startOfSubject, nextDoseTime, 0, nextDoseTime.length);
                System.arraycopy(dose, startOfSubject, nextDoseVal, 0, nextDoseVal.length);
                doseTimeArray.add(nextDoseTime);
                doseValArray.add(nextDoseVal);
                startOfSubject = endOfSubject;
            }

            doseTimes = doseTimeArray.toArray(new double[doseTimeArray.size()][]);
            doseVals = doseValArray.toArray(new double[doseValArray.size()][]);
        }
    }
}
