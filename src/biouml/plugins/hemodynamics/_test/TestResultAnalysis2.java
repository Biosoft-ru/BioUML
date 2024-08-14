package biouml.plugins.hemodynamics._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.analysis.Stat;
import ru.biosoft.util.Util;
import biouml.plugins.hemodynamics._test.PressureTest.PatientInfo;

public class TestResultAnalysis2 extends TestCase
{

    public TestResultAnalysis2(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TestResultAnalysis2.class.getName());
        suite.addTest(new TestResultAnalysis2("test"));
        return suite;
    }

    private static final int REFERENCED_ID = 1396;
    //    private static String DIR_PATH = "C:/VP/Ready";
    //    C:\Users\Ilya\Desktop\Arterial tree results\Simulation results\Resistance\Ready
    private static String DIR_PATH = "C:/Arterial tree results/Simulation Results with resistance/Final/R";//Selected";///R different";///R groups";
    //        private static String DIR_PATH = "C:/Users/Ilya/Desktop/Arterial tree results/Simulation results/Resistance (RH)";

    private static Censor ageCensor = new Censor("age", 30, 50);
    private static Censor hrCensor = new Censor("heartRate", 60, 100);
    private static Censor bmiCensor = new Censor("bmi", 18.5, 25);
    private static Censor psCensor = new Censor("originalPS", 90, 130);
    private static Censor pdCensor = new Censor("originalPD", 60, 90);

    private static PercentCensor a0PercentCensor = new PercentCensor("a0", 5);
    private static PercentCensor betaPercentCensor = new PercentCensor("beta", 5);
    
    private static PercentCensor a0PercentCensor10 = new PercentCensor("a0", 10);
    private static PercentCensor betaPercentCensor10 = new PercentCensor("beta", 10);
    
    private static PercentCensor agePercentCensor = new PercentCensor("age", 5);
    private static PercentCensor hrPercentCensor = new PercentCensor("heartRate", 5);
    private static PercentCensor bmiPercentCensor = new PercentCensor("bmi", 5);
    private static PercentCensor psPercentCensor = new PercentCensor("originalPS", 5);
    private static PercentCensor pdPercentCensor = new PercentCensor("originalPD", 5);

    public void test() throws IOException
    {
//        doTest(ReportType.LEFT_HAND, new Censor[0]);
//        doTest(ReportType.LEFT_HAND, a0PercentCensor);
//        doTest(ReportType.LEFT_HAND, betaPercentCensor);
//        doTest(ReportType.LEFT_HAND, a0PercentCensor, betaPercentCensor);
//        doTest(ReportType.RIGHT_HAND, a0PercentCensor, betaPercentCensor);
//        doTest(ReportType.LEFT_LEG, a0PercentCensor, betaPercentCensor);
//        doTest(ReportType.RIGHT_LEG, a0PercentCensor, betaPercentCensor);
//        doTest(ReportType.LEFT_HAND, a0PercentCensor10, betaPercentCensor10);
//        doTest(ReportType.RIGHT_HAND, new Censor[0]);
//
//        doTest(ReportType.LEFT_LEG, new Censor[0]);
        doTest(new Censor("age", 0, 30));
        doTest(new Censor("age", 30, 40));
        doTest(new Censor("age", 40, 50));
        doTest(new Censor("age", 50, 60));
        doTest(new Censor("age", 60, 80));
        doTest(new Censor("age", 80, 200));
        //        doTest(agePercentCensor, hrPercentCensor, bmiPercentCensor, psPercentCensor, pdPercentCensor);
        //        doTest(agePercentCensor);
        //        doTest(hrPercentCensor);
        //        doTest(bmiPercentCensor);
        //        doTest(psPercentCensor, pdPercentCensor);

        //        doTest(ageCensor, hrCensor, bmiCensor, psCensor, pdCensor);
        //        doTest(ageCensor);
        //        doTest(hrCensor);
        //        doTest(bmiCensor);
        //        doTest(psCensor, pdCensor);
    }


    public void doTest(PercentCensor ... censors) throws IOException
    {
        doTest(ReportType.LEFT_HAND, censors);
    }
    
    public void doTest(Censor ... censors) throws IOException
    {
        doTest(ReportType.LEFT_HAND, censors);
    }
    
    public void doTest(ReportType type, PercentCensor ... censors) throws IOException
    {
        File dir = new File(DIR_PATH);

        List<Report> reports = new ArrayList<>();

        readExpectedData(new File("C:/Arterial tree results/expectedResults.txt"));
        
        for( File f : dir.listFiles() )
        {
            if (f.isDirectory())
                continue;

            List<PatientInfo> infos = readResult(f, (Censor[])null);

            Set<PatientInfo> censoredInfos = new HashSet<>();
            for( PercentCensor censor : censors )
                censoredInfos.addAll(censor.getFiltered(infos));

            infos = StreamEx.of(infos).filter(info -> info.isValid && !censoredInfos.contains(info)).toList();
            reports.add(calculateData(infos, f.getName(), type));
        }

        String result = generateGlobalReport(reports, censors);
        System.out.println(result);
    }


    public void doTest(ReportType type, Censor ... censors) throws IOException
    {
        File dir = new File(DIR_PATH);

        List<Report> reports = new ArrayList<>();
        readExpectedData(new File("C:/Arterial tree results/expectedResults.txt"));
        for( File f : dir.listFiles() )
        {
            if (f.isDirectory())
                continue;
            
            List<PatientInfo> infos = readResult(f, censors);
            reports.add(calculateData(infos, f.getName(), type));//f, censors));
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


    protected enum ReportType
    {
        LEFT_HAND, RIGHT_HAND, LEFT_LEG, RIGHT_LEG;
    }

    public Report calculateData(List<PatientInfo> infos, String name, ReportType type)
    {
        Report report = new Report();

       
        List<Double> originalPs = new ArrayList<>();
        List<Double> simulatedPs = new ArrayList<>();

        List<Double> originalPd = new ArrayList<>();
        List<Double> simulatedPd = new ArrayList<>();

        List<Double> originalPp = new ArrayList<>();
        List<Double> simulatedPp = new ArrayList<>();

//        int size = 0;
        for( PatientInfo patientInfo : infos )
        {
            ExpectedInfo expected = this.expectedResults.get(patientInfo.patId);
//
            if (!expected.isValid || !patientInfo.isValid)
            {
//                System.out.println("patient "+patientInfo.patId +" skipped.");
                continue;
            }
//            size++;
            switch( type )
            {
                case LEFT_HAND:
                {

                    originalPs.add(expected.ps0);
                    simulatedPs.add(patientInfo.ps);

                    originalPd.add(expected.pd0);
                    simulatedPd.add(patientInfo.pd);

                    originalPp.add(expected.pp0);
                    simulatedPp.add(patientInfo.pp);
                    break;
                }
                case RIGHT_HAND:
                {
                    originalPs.add(expected.ps1);
                    simulatedPs.add(patientInfo.ps_right);

                    originalPd.add(expected.pd1);
                    simulatedPd.add(patientInfo.pd_right);

                    originalPp.add(expected.pp1);
                    simulatedPp.add(patientInfo.pp_right);
                    break;
                }
                case LEFT_LEG:
                {
                    originalPs.add(expected.ps2);
                    simulatedPs.add(patientInfo.ps_ptibial_left);

                    originalPd.add(expected.pd2);
                    simulatedPd.add(patientInfo.pd_ptibial_left);

                    originalPp.add(expected.pp2);
                    simulatedPp.add(patientInfo.pp_ptibial_left);
                    break;
                }
                case RIGHT_LEG:
                {
                    originalPs.add(expected.ps3);
                    simulatedPs.add(patientInfo.ps_ptibial_left);

                    originalPd.add(expected.pd3);
                    simulatedPd.add(patientInfo.pd_ptibial_left);

                    originalPp.add(expected.pp3);
                    simulatedPp.add(patientInfo.pp_ptibial_left);
                    break;
                }
            }


            if( patientInfo.patId == REFERENCED_ID )
            {
                report.errorPdRef = Math.abs(patientInfo.ps - patientInfo.originalPS) / patientInfo.originalPS;
                report.errorPsRef = Math.abs(patientInfo.pd - patientInfo.originalPD) / patientInfo.originalPD;
                report.errorPpRef = Math.abs(patientInfo.pp - patientInfo.originalPP) / patientInfo.originalPP;

                //                double pdyn = 1d/3*patientInfo.ps + 2d/3*patientInfo.pd;
                //                double originalPdyn = 1d/3*patientInfo.originalPS + 2d/3*patientInfo.originalPD;
                //                report.errorPdynRef = Math.abs(pdyn - originalPdyn) / originalPdyn;
            }
        }

        //        List<Double> originalPdyn_left = sum(1d/3, originalPs_Left, 2d/3, originalPd_Left);
        //        List<Double> simulatedPdyn_left = sum(1d/3, simulatedPs_Left, 2d/3, simulatedPd_Left);

        report.fileName = name;//file.getName();
        report.patients = originalPs.size();
        report.correlationPs = correlation(originalPs, simulatedPs);
        report.correlationPd = correlation(originalPd, simulatedPd);
        report.correlationPp = correlation(originalPp, simulatedPp);
        //            report.correlationPdyn = correlation(originalPdyn_left, simulatedPdyn_left);

        //            report.errorPdyn = relativeErrorMed(originalPdyn_left, simulatedPdyn_left);
        report.errorPp = relativeErrorMean(originalPp, simulatedPp);
        report.errorPs = relativeErrorMean(originalPs, simulatedPs);
        report.errorPd = relativeErrorMean(originalPd, simulatedPd);
        
//        report.errorPp = relativeErrorMed(originalPp, simulatedPp);
//        report.errorPs = relativeErrorMed(originalPs, simulatedPs);
//        report.errorPd = relativeErrorMed(originalPd, simulatedPd);
        return report;

    }

    public static List<Double> difference(List<Double> list1, List<Double> list2)
    {
        return StreamEx.zip(list1, list2, (val1, val2) -> val1 - val2).toList();
    }

    public static List<Double> sum(double factor1, List<Double> list1, double factor2, List<Double> list2)
    {
        return StreamEx.zip(list1, list2, (val1, val2) -> factor1 * val1 + factor2 * val2).toList();
    }

    public static double correlation(List<Double> list1, List<Double> list2)
    {
        double[] arr1 = ArrayUtils.toPrimitive(list1.toArray(new Double[list1.size()]));
        double[] arr2 = ArrayUtils.toPrimitive(list2.toArray(new Double[list2.size()]));

        try
        {
            return Stat.pearsonCorrelation(arr1, arr2);
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
            return Double.NaN;
        }
    }
    
    public static StreamEx<Double> relativeError(List<Double> list1, List<Double> list2)
    {
        return StreamEx.zip(list1, list2, (val1, val2) -> Math.abs(val1 - val2) / val1);
    }
    
    public static double relativeErrorMed(List<Double> list1, List<Double> list2)
    {
        return relativeError(list1, list2).mapToDouble(Double::doubleValue)
                .collect(Util.median()).getAsDouble();
    }

    public static double relativeErrorMean(List<Double> list1, List<Double> list2)
    {
        return Stat.mean(StreamEx.zip(list1, list2, (val1, val2) -> Math.abs(val1 - val2) / val1).toList());
    }


    private static class Report
    {
        String fileName;
        double correlationPs;
        double correlationPd;
        double correlationPp;
        //        double correlationPdyn;

        double errorPp;
        //        double errorPdyn;
        double errorPs;
        double errorPd;

        double errorPpRef;
        //        double errorPdynRef;
        double errorPsRef;
        double errorPdRef;

        int patients;

        public static String getDescription()
        {
            return StringUtils
                    .join(new Object[] {"name", "#", "cor PS", "cor PD", "cor PP", /*"cor Pdyn", */ "significane PS", "significance PD","significance PD",
                            "error PS", "error PD", "errorPP", /*"errorPdyn", " ref PS", "ref PD", "ref PP", "ref Pdyn"*/}, "\t");
        }

        @Override
        public String toString()
        {
            return StringUtils.join(new Object[] {fileName, patients, correlationPs, correlationPd, correlationPp, //correlationPdyn,
                    Stat.pearsonSignificance(correlationPs, patients), Stat.pearsonSignificance(correlationPd, patients), Stat.pearsonSignificance(correlationPp, patients), errorPs,
                    errorPd, errorPp, ///*errorPdyn,*/ errorPsRef//, //errorPdRef,
                    /*errorPpRef, /*errorPdynRef, referencedErrorPs, referencedErrorPd*/}, "\t");
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
                return Double.compare(f.getDouble(o1), f.getDouble(o2));
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
                return val >= lower && val < upper;
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

    public void readExpectedData(File f) throws IOException
    {
        expectedResults = new HashMap<>();
        try (BufferedReader br = ApplicationUtils.utfReader(f))
        {
            String line = br.readLine();
            line = br.readLine();

            while( line != null )
            {
                ExpectedInfo expectedInfo = new ExpectedInfo(line);
                expectedResults.put(expectedInfo.patId, expectedInfo);
                line = br.readLine();
            }
        }
    }
    
    Map<Double, ExpectedInfo> expectedResults = new HashMap<>();
    
    private static class ExpectedInfo
    {
        double patId;
        double pd0;
        double ps0;
        double pd1;
        double ps1;
        double pd2;
        double ps2;
        double pd3;
        double ps3;
        double pp0;
        double pp1;
        double pp2;
        double pp3;
        
        boolean isValid = true;

        public ExpectedInfo(String str)
        {
            String[] data = str.split("\t");
            int i = 0;
            patId = read(data[i++]);
            pd0 = read(data[i++]);
            ps0 = read(data[i++]);
            pd1 = read(data[i++]);
            ps1 = read(data[i++]);
            pd2 = read(data[i++]);
            ps2 = read(data[i++]);
            pd3 = read(data[i++]);
            ps3 = read(data[i++]);
            
            pp0 = ps0-pd0;
            pp1 = ps1-pd1;
            pp2 = ps2-pd2;
            pp3 = ps3-pd3;
        }

        private double read(String str)
        {
            try
            {
                str = str.replaceAll(",", ".");
                double val = Double.parseDouble(str);
                return val;
            }
            catch( Exception ex )
            {
                isValid = false;
                return Double.NaN;

            }

        }
    }
}
