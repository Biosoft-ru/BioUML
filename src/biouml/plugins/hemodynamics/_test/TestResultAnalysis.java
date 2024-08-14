package biouml.plugins.hemodynamics._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import one.util.streamex.StreamEx;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import ru.biosoft.analysis.Stat;
import ru.biosoft.util.Util;
import biouml.plugins.hemodynamics._test.PressureTest.PatientInfo;

public class TestResultAnalysis extends TestCase
{

    public TestResultAnalysis(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TestResultAnalysis.class.getName());
        suite.addTest(new TestResultAnalysis("test"));
        return suite;
    }

        private static final int REFERENCED_ID = 1396;
//    private static String DIR_PATH = "C:/VP/Ready";
//    C:\Users\Ilya\Desktop\Arterial tree results\Simulation results\Resistance\Ready
    private static String DIR_PATH = "C:/Users/Ilya/Desktop/Arterial tree results/Simulation Results with resistance";
//        private static String DIR_PATH = "C:/Users/Ilya/Desktop/Arterial tree results/Simulation results/Resistance (RH)";

    private static Censor ageCensor = new Censor("age", 30, 50);
    private static Censor hrCensor = new Censor("heartRate", 60, 100);
    private static Censor bmiCensor = new Censor("bmi", 18.5, 25);
    private static Censor psCensor = new Censor("originalPS", 90, 130);
    private static Censor pdCensor = new Censor("originalPD", 60, 90);

    private static PercentCensor agePercentCensor = new PercentCensor("age", 5);
    private static PercentCensor hrPercentCensor = new PercentCensor("heartRate", 5);
    private static PercentCensor bmiPercentCensor = new PercentCensor("bmi", 5);
    private static PercentCensor psPercentCensor = new PercentCensor("originalPS", 5);
    private static PercentCensor pdPercentCensor = new PercentCensor("originalPD", 5);

    public void test()
    {
        doTest(new Censor[0]);
        doTest(agePercentCensor, hrPercentCensor, bmiPercentCensor, psPercentCensor, pdPercentCensor);
        doTest(agePercentCensor);
        doTest(hrPercentCensor);
        doTest(bmiPercentCensor);
        doTest(psPercentCensor, pdPercentCensor);

        //        doTest(ageCensor, hrCensor, bmiCensor, psCensor, pdCensor);
        //        doTest(ageCensor);
        //        doTest(hrCensor);
        //        doTest(bmiCensor);
        //        doTest(psCensor, pdCensor);
    }


    public void doTest(PercentCensor ... censors)
    {
        File dir = new File(DIR_PATH);

        List<Report> reports = new ArrayList<>();

        for( File f : dir.listFiles() )
        {
            List<PatientInfo> infos = readResult(f, (Censor[])null);

            Set<PatientInfo> censoredInfos = new HashSet<>();
            for( PercentCensor censor : censors )
                censoredInfos.addAll(censor.getFiltered(infos));

            infos = StreamEx.of(infos).filter(info-> !censoredInfos.contains(info)).toList();
            reports.add(calculateData(infos, f.getName()));
        }

        String result = generateGlobalReport(reports, censors);
        System.out.println(result);
    }


    public void doTest(Censor ... censors)
    {
        File dir = new File(DIR_PATH);

        List<Report> reports = new ArrayList<>();

        for( File f : dir.listFiles() )
        {
            List<PatientInfo> infos = readResult(f, censors);
            reports.add(calculateData(infos, f.getName()));//f, censors));
        }
        String result = generateGlobalReport(reports, censors);
        System.out.println(result);
    }

    public String generateGlobalReport(List<Report> reports, Object[] censors)
    {
        StringBuffer result = new StringBuffer();

        for( Object censor : censors )
            result.append(censor.toString() + "\n");

        result.append(Report.getDescription() + "\n");
        for( Report report : reports )
            result.append(report.toString() + "\n");
        return result.toString().replaceAll("\\.", ",").replaceAll(".txt", "");
    }


    public Report calculateData(List<PatientInfo> infos, String name)
    {
        Report report = new Report();

        //leftHand
        List<Double> originalPs_Left = new ArrayList<>();
        List<Double> simulatedPs_Left = new ArrayList<>();

        List<Double> originalPd_Left = new ArrayList<>();
        List<Double> simulatedPd_Left = new ArrayList<>();
        
        List<Double> originalPp_left = new ArrayList<>();
        List<Double> simulatedPp_left = new ArrayList<>();
        
        for( PatientInfo patientInfo : infos )
        {
            originalPs_Left.add(patientInfo.ps);
            simulatedPs_Left.add(patientInfo.originalPS);

            originalPd_Left.add(patientInfo.pd);
            simulatedPd_Left.add(patientInfo.originalPD);
            
            originalPp_left.add(patientInfo.pp);
            simulatedPp_left.add(patientInfo.originalPP);
            
            if (patientInfo.patId == REFERENCED_ID)
            {
                report.errorPdRef = Math.abs(patientInfo.ps - patientInfo.originalPS) / patientInfo.originalPS;
                report.errorPsRef = Math.abs(patientInfo.pd - patientInfo.originalPD) / patientInfo.originalPD;
                report.errorPpRef = Math.abs(patientInfo.pp - patientInfo.originalPP) / patientInfo.originalPP;
                
                double pdyn = 1d/3*patientInfo.ps + 2d/3*patientInfo.pd;
                double originalPdyn = 1d/3*patientInfo.originalPS + 2d/3*patientInfo.originalPD;
                report.errorPdynRef = Math.abs(pdyn - originalPdyn) / originalPdyn;
            }
        }
        
        List<Double> originalPdyn_left = sum(1d/3, originalPs_Left, 2d/3, originalPd_Left);
        List<Double> simulatedPdyn_left = sum(1d/3, simulatedPs_Left, 2d/3, simulatedPd_Left);

        try
        {
            report.fileName = name;//file.getName();
            report.patients = originalPs_Left.size();
            
            report.correlationPs = correlation(originalPs_Left, simulatedPs_Left);
            report.correlationPd = correlation(originalPd_Left, simulatedPd_Left);
            report.correlationPp = correlation(originalPp_left, simulatedPp_left);
            report.correlationPdyn = correlation(originalPdyn_left, simulatedPdyn_left);
            
            report.errorPdyn = relativeErrorMed(originalPdyn_left, simulatedPdyn_left);
            report.errorPp = relativeErrorMed(originalPp_left, simulatedPp_left);
            report.errorPs = relativeErrorMed(originalPs_Left, simulatedPs_Left);
            report.errorPd = relativeErrorMed(originalPd_Left, simulatedPd_Left);
        }
        catch( Exception ex )
        {

        }
        return report;

    }
    
    public static List<Double> difference(List<Double> list1, List<Double> list2)
    {
        return StreamEx.zip(list1, list2,  (val1, val2) -> val1 - val2).toList();
    }
    
    public static List<Double> sum(double factor1, List<Double> list1, double factor2, List<Double> list2)
    {
        return StreamEx.zip(list1, list2,  (val1, val2) -> factor1 * val1 + factor2 * val2).toList();
    }

    public static double correlation(List<Double> list1, List<Double> list2) throws Exception
    {
        double[] arr1 = ArrayUtils.toPrimitive(list1.toArray(new Double[list1.size()]));
        double[] arr2 = ArrayUtils.toPrimitive(list2.toArray(new Double[list2.size()]));

        return Stat.pearsonCorrelation(arr1, arr2);
    }

    public static double relativeErrorMed(List<Double> list1, List<Double> list2) throws Exception
    {
        return StreamEx.zip(list1, list2, (val1, val2) -> Math.abs(val1 - val2) / val1).mapToDouble(Double::doubleValue)
                .collect(Util.median()).getAsDouble();
    }


    private static class Report
    {
        String fileName;
        double correlationPs;
        double correlationPd;
        double correlationPp;
        double correlationPdyn;

        double errorPp;
        double errorPdyn;
        double errorPs;
        double errorPd;
        
        double errorPpRef;
        double errorPdynRef;
        double errorPsRef;
        double errorPdRef;

        int patients;

        public static String getDescription()
        {
            return StringUtils.join(new Object[] {"name", "#", "cor PS", "cor PD", "cor PP", "cor Pdyn",/*, "significane PS", "significance PD",*/
                    "error PS", "error PD", "errorPP", "errorPdyn", " ref PS", "ref PD", "ref PP", "ref Pdyn"}, "\t");
        }

        @Override
        public String toString()
        {
            return StringUtils
                    .join(new Object[] {fileName, patients, correlationPs, correlationPd, correlationPp, correlationPdyn,
                            /*Stat.pearsonSignificance(correlationPs, patients), Stat.pearsonSignificance(correlationPd, patients)*/ errorPs,
                            errorPd, errorPp, errorPdyn, errorPsRef,
                            errorPdRef, errorPpRef, errorPdynRef/*, referencedErrorPs, referencedErrorPd*/}, "\t");
        }
    }


    public List<PatientInfo> readResult(File file, Censor ... censors)
    {
        List<PatientInfo> infos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            br.readLine(); //header
            String line = br.readLine();
            while( line != null )
            {
                PatientInfo patInfo = new PatientInfo(line);
                if( patInfo.isValid && isPermited(patInfo, censors) )
                    infos.add(new PatientInfo(line));
                line = br.readLine();
            }
        }
        catch( Exception ex )
        {
            return null;
        }
        return infos;
    }


    public boolean isPermited(PatientInfo patInfo, Censor ... censors)
    {
        if( censors == null )
            return true;

        for( Censor censor : censors )
        {
            if( !censor.permit(patInfo) )
                return false;
        }
        return true;
    }


    private static class PercentCensor
    {
        Field f;
        double percent;

        public PercentCensor(String fieldName, double percent)
        {
            try
            {
                f = PatientInfo.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                this.percent = percent;
            }
            catch( Exception e )
            {
            }
        }

        public Set<PatientInfo> getFiltered(List<PatientInfo> patInfos)
        {
            Set<PatientInfo> result = new HashSet<>();
            patInfos.sort(new PatientComparator(f));
            int numberToFilter = patInfos.size() / 20;
            for( int i = 0; i < patInfos.size(); i++ )
            {
                if( i <= numberToFilter || i >= patInfos.size() - numberToFilter )
                    result.add(patInfos.get(i));
            }
            return result;
        }

        public List<PatientInfo> censor(List<PatientInfo> patInfos)
        {
            List<PatientInfo> result = new ArrayList<>();
            patInfos.sort(new PatientComparator(f));
            int numberToFilter = patInfos.size() / 20;
            for( int i = numberToFilter; i < patInfos.size() - numberToFilter; i++ )
                result.add(patInfos.get(i));
            return result;
        }

        @Override
        public String toString()
        {
            return f.getName() + " without " + percent + " %";
        }

    }

    protected static class PatientComparator implements Comparator<PatientInfo>
    {

        Field f;

        public PatientComparator(Field f)
        {
            this.f = f;
        }

        @Override
        public int compare(PatientInfo o1, PatientInfo o2)
        {
            try
            {
                return Double.compare(f.getDouble(o1),f.getDouble(o2));
            }
            catch( Exception ex )
            {
                ex.printStackTrace();
                return 0;
            }
        }

    }

    private static class Censor
    {
        Field f;
        double lower;
        double upper;
        public Censor(String fieldName, double lower, double upper)
        {
            try
            {
                f = PatientInfo.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                this.lower = lower;
                this.upper = upper;
            }
            catch( Exception ex )
            {

            }
        }

        public boolean permit(PatientInfo patInfo)
        {
            try
            {
                double val = f.getDouble(patInfo);
                return val >= lower && val <= upper;
            }
            catch( Exception ex )
            {
                System.out.println(ex.getMessage());
                return false;
            }
        }

        @Override
        public String toString()
        {
            return f.getName() + " in [ " + lower + " , " + upper + " ]";
        }
    }
}
