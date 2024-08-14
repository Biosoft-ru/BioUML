package biouml.plugins.simulation_test;

import java.io.File;
import java.util.List;
import java.util.Properties;

import ru.biosoft.util.ExProperties;

public class CalculateBiomodelsStatistics
{
    protected static final String outDirectory = "C:/SBML_tests/biomodels/biomodels/results_test/";
    protected static final String otherCsvDirectory = "C:/SBML_tests/biomodels/2007-11-12_CSV-Files/";
    protected static final String testDirectory = "../../test_subset/";

    //private static final String outDirectory = "F:/SBML_tests/biomodels/results_test_new/";
    //private static final String otherCsvDirectory = "F:/SBML_tests/biomodels/2007-11-12_CSV-Files/";
    //protected static final String testDirectory = "../../biomodels/test_subset2/";

    protected static final String bioumlCsvDirectory = outDirectory + "csvResults/";

    public static final int CONTROL_POINTS_NUMBER = 50;
    public static final double CONTROL_POINTS_COMPLETION = 10.0;
    public static final double CONTROL_POINTS_INITIAL = 0.0;

    static final String[] SOLVER_EXTENSIONS = new String[] {"ByoDyn", "copasi", "CVODE", "edu.kgi.roadRunner", "Jarnac", "jsim", "Oscill8 Core",
            "SBWOdeSolver", "MathSBML", "SBToolbox2", "VCell"};

    public static final String BIOUML_EXTENSION = "BioUML";

    static final String[] SOLVER_COLORS = new String[] {"#77aa77", "#ffff77", "#ffaa77", "#77ffff", "#aaffaa", "#ffaaaa", "#aaaaff", "#aa77ff",
            "#ff77aa", "#ffffaa", "#aaffff"};

    public void startStatisticCalculation()
    {
        generateStatistics(bioumlCsvDirectory, otherCsvDirectory, outDirectory);
    }

    public void generateStatistics(String bioumlCsvDirectory, String otherCsvDirectory, String outputDirectory)
    {
        try
        {
            File dir = new File(bioumlCsvDirectory);
            if( dir.isDirectory() )
            {
                StatisticsLogger logger = new StatisticsLogger(outputDirectory, "csvResults/", testDirectory, "java/");
                logger.start();
                File[] files = dir.listFiles();
                for( File file : files )
                {
                    if( file.getName().endsWith(".info") )
                    {
                        Properties properties = new ExProperties(file);
                        String testName = file.getName();
                        testName = testName.substring(0, testName.lastIndexOf("."));

                        System.out.println(testName);
                        TestStatistics testStatistics = new TestStatistics(properties, BIOUML_EXTENSION);
                        testStatistics.setControlPoints(getControlPoints());
                        loadResults(testName, bioumlCsvDirectory, otherCsvDirectory, testStatistics);
                        logger.addTestStatistics(testStatistics, testName, properties);
                    }
                }
                logger.end();
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    protected void loadResults(String testName, String bioumlCsvDirectory, String otherCsvDirectory, TestStatistics testStatistics)
            throws Exception
    {
        File csvFile = new File(bioumlCsvDirectory + testName + "." + BIOUML_EXTENSION + ".csv");
        if( csvFile.exists() )
        {
            loadHandler(new SbmlCSVHandler(csvFile), BIOUML_EXTENSION, testStatistics);
        }

        SbmlCSVHandler[] handlers = getCSVHandlers(otherCsvDirectory, testName);

        for( int i = 0; i < handlers.length; i++ )
        {
            if( handlers[i] != null )
            {
                loadHandler(handlers[i], SOLVER_EXTENSIONS[i], testStatistics);
            }
        }
    }

    protected void loadHandler(SbmlCSVHandler handler, String solverName, TestStatistics testStatistics) throws Exception
    {
        List<String> variableNames = handler.getVariableNames();
        List<double[]> variableValues = handler.getVariableValues();

        boolean hasVariables = true;
        if( variableNames == null || variableNames.size() == 0 || ( variableNames.size() == 1 && variableNames.get(0).equals("time") ) )
        {
            hasVariables = false;
        }

        if( hasVariables )
        {
            if( variableValues.size() != 0 )
            {
                testStatistics.addResult(solverName, variableNames, variableValues);
            }
        }
    }

    private double[] getControlPoints()
    {
        double[] p = new double[CONTROL_POINTS_NUMBER];
        double t1 = CONTROL_POINTS_COMPLETION;
        double t0 = CONTROL_POINTS_INITIAL;
        double delta = ( t1 - t0 ) / CONTROL_POINTS_NUMBER;
        for( int i = 0; i < CONTROL_POINTS_NUMBER; i++ )
            p[i] = t0 + i * delta;

        return p;
    }

    private static SbmlCSVHandler[] getCSVHandlers(String csvDirectory, String testName) throws Exception
    {
        SbmlCSVHandler[] handlers = new SbmlCSVHandler[SOLVER_EXTENSIONS.length];
        for( int i = 0; i < SOLVER_EXTENSIONS.length; i++ )
        {
            File csvFile = new File(csvDirectory + testName + "." + SOLVER_EXTENSIONS[i] + ".csv");
            if( !csvFile.exists() )
            {
                //logger.error(Status.CSV_ERROR, "CSV file absents, test=" + testName + ", file=" + csvFile.getName());
            }
            else
            {
                handlers[i] = new SbmlCSVHandler(csvFile);
            }
        }
        return handlers;
    }
}
