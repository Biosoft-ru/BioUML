package biouml.plugins.simulation_test.fbc;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import biouml.model.Diagram;
import biouml.plugins.fbc.ApacheModelCreator;
import biouml.plugins.fbc.FbcConstant;
import biouml.plugins.fbc.FbcModel;
import biouml.plugins.simulation_test.DefaultTestLogger;
import biouml.plugins.simulation_test.SbmlCSVHandler;
import biouml.plugins.simulation_test.Status;
import biouml.plugins.simulation_test.TestDescription;
import biouml.plugins.simulation_test.TestLogger;
import biouml.standard.simulation.SimulationResult;


public class FbcSbmlTest implements FbcConstant
{
    private String testDir;
    private TestDescription statistics;

    public void setStatistics(TestDescription statistics)
    {
        this.statistics = statistics;
    }

    public void setTestDir(String testDir)
    {
        this.testDir = testDir;
    }


    public void testDiagram(Diagram diagram, String testName, TestLogger logger) throws Exception
    {
        ApacheModelCreator creator = new ApacheModelCreator();
        FbcModel model = creator.createModel(diagram);
        model.optimize();

        File csvFile = new File(testDir + testName + "-results.csv");
        SbmlCSVHandler handler = new SbmlCSVHandler(csvFile, false);
        List<String> namesVariable = handler.getVariableNames();
        double[] valuesVariable = new double[namesVariable.size()];
        String activObj = (String)diagram.getAttributes().getProperty(FBC_ACTIVE_OBJECTIVE).getValue();
        int i = 0;
        for( String name : namesVariable )
        {
            if( name.equals(activObj) )
                valuesVariable[i] = model.getValueObjFunc();
            else
                valuesVariable[i] = model.getOptimValue(name);
            i++;
        }
        if( logger instanceof FBCLogger )
        {
            ( (FBCLogger)logger ).setVariables(namesVariable);
            ( (FBCLogger)logger ).setValues(valuesVariable);
        }
    }



    public static class FBCLogger extends DefaultTestLogger
    {
        public FBCLogger(String outputPath, String name)
        {
            super(outputPath, name);
        }
        private List<String> varNames;
        private double[] values;
        public void setVariables(List<String> varNames)
        {
            this.varNames = varNames;
        }

        public void setValues(double[] values)
        {
            this.values = values;
        }

        @Override
        protected void writeCSVFile(String fileName, SimulationResult simulationResult)
        {
            if( status == Status.SUCCESSFULL )
            {
                File outDir = new File(outputPath);
                if( !outDir.exists() )
                    outDir.mkdirs();

                if( statistics != null )
                {
                    File outputFile = new File(outDir, fileName);
                    SbmlCSVHandler handler = new SbmlCSVHandler();

                    //                    Arrays.sort(vars);

                    handler.setVariableNames(varNames);

                    List<double[]> valuesList = new ArrayList<>();
                    valuesList.add(values);
                    handler.setVariableValues(valuesList);

                    handler.writeCSVFile(outputFile);
                }
            }
        }

        @Override
        protected void writeInfoFile()
        {
            File outDir = new File(outputPath);
            if( !outDir.exists() )
                outDir.mkdirs();
            name = name.replaceAll("\\\\", "/");
            new File(outDir, name).getParentFile().mkdirs();

            try(PrintWriter pw = new PrintWriter(new File(outDir, name + ".info"), "UTF-8"))
            {
                pw.println(DefaultTestLogger.STATUS + "=" + status);
                pw.println(DefaultTestLogger.SIMULATIONTIME + "=" + this.getSimulationTime());
                if( statistics != null )
                {
                    pw.println(SIMULATOR + "=" + statistics.getSolverName());
                    pw.println(SBML_LEVEL + "=" + statistics.getSbmlLevel());
                    pw.println(ZERO + "=" + statistics.getZero());
                    pw.println(ATOL + "=" + statistics.getAtol());
                    pw.println(RTOL + "=" + statistics.getRtol());
                    pw.println(STEP + "=");
                    pw.println(MESSAGES + "=");
                    pw.println(SCRIPT_NAME + "=");
                    pw.println(TIME_COURSE_TEST + "= false");
                }
            }
            catch( Exception e )
            {
                System.out.println("*** " + e.getMessage());
            }
        }
    }
}