package ru.biosoft.galaxy._test;

import java.io.File;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import junit.framework.Test;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.galaxy.FormatRegistry;
import ru.biosoft.galaxy.GalaxyAnalysisParameters;
import ru.biosoft.galaxy.GalaxyAnalysisParameters.SelectorOption;
import ru.biosoft.galaxy.GalaxyDataCollection;
import ru.biosoft.galaxy.GalaxyMethod;
import ru.biosoft.galaxy.GalaxyMethodInfo;
import ru.biosoft.galaxy.GalaxyMethodTest;
import ru.biosoft.galaxy.ParametersContainer;
import ru.biosoft.galaxy.ResultComparator;
import ru.biosoft.galaxy.parameters.ArrayParameter;
import ru.biosoft.galaxy.parameters.BooleanParameter;
import ru.biosoft.galaxy.parameters.ConditionalParameter;
import ru.biosoft.galaxy.parameters.ConfigParameter;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.Parameter;
import ru.biosoft.galaxy.parameters.SelectParameter;
import ru.biosoft.galaxy.parameters.StringParameter;
import ru.biosoft.util.SuppressHuntBugsWarning;
import ru.biosoft.util.TempFiles;

/**
 * Run all functional tests for Galaxy methods importing input files to repository,
 * exporting produced data elements to files and comparing files with expected files.
 */
public class ImportExecuteExportTest extends AbstractBioUMLTest
{
    private static final Logger log = Logger.getLogger(ImportExecuteExportTest.class.getName());
    private static final String analysesDir = "../analyses";
    private static final String galaxyPath = "analyses/Galaxy";
    private static File galaxyTestDataDir;

    /**
     * If not null run single test, otherwise run all tests
     */
    @SuppressHuntBugsWarning({"FieldIsAlwaysNull"})
    private static final String test = null;

    /**
     * Galaxy method to test
     */
    private final GalaxyMethodInfo galaxyMethodInfo;
    /**
     * DataCollection where test data (input and output DataElements) will be stored
     */
    private DataCollection workingDC;

    /**
     * Folder with output files (exported output DataElements)
     */
    private File outputDir;

    private boolean testSuccess;

    public static Test suite() throws Exception
    {
        CollectionFactory.createRepository("../data_resources");


        CollectionFactory.createRepository(analysesDir);
        DataElementPath galaxyRoot = DataElementPath.create(galaxyPath);

        TestSuite suite = new TestSuite();
        if( test != null )
        {
            suite.addTest(new ImportExecuteExportTest((GalaxyMethodInfo)CollectionFactory.getDataElement(galaxyPath + "/" + test)));
        }
        else
        {
            for( DataElementPath methodGroup : galaxyRoot.getChildren() )
                for( DataElementPath method : methodGroup.getChildren() )
                {
                    DataElement child = method.optDataElement();
                    if(child instanceof GalaxyMethodInfo)
                        suite.addTest(new ImportExecuteExportTest((GalaxyMethodInfo)child));
                }
        }

        galaxyTestDataDir = GalaxyDataCollection.getGalaxyDistFiles().getTestDataFolder();
        return suite;
    }
    public ImportExecuteExportTest(GalaxyMethodInfo galaxyMethodInfo)
    {
        super("doTest");
        this.galaxyMethodInfo = galaxyMethodInfo;
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        String id = galaxyMethodInfo.getOrigin().getName() + "_" + galaxyMethodInfo.getName();

        workingDC = DataCollectionUtils.createSubCollection(DataElementPath.create("data/galaxy_test").getChildPath(id));

        outputDir = TempFiles.dir("galaxy_" + id + "_test_output");
        
        log.info( "Running tests in " + id );
    }

    @Override
    public void tearDown() throws Exception
    {
        log.info( "Test " + (testSuccess ? "success" : "failed") );
        if( !testSuccess )//Allow to inspect intermediate results
            return;
        FileUtils.deleteDirectory(outputDir);
        DataElementPath.create(workingDC).remove();
        super.tearDown();
    }


    public void doTest() throws Exception
    {
        AnalysisMethod method = galaxyMethodInfo.createAnalysisMethod();
        assertNotNull("Can't create analysis", method);
        assertTrue("Analysis is not Galaxy method", ( method instanceof GalaxyMethod ));
        GalaxyMethod galaxyMethod = (GalaxyMethod)method;

        for( GalaxyMethodTest test : galaxyMethodInfo.getTests() )
        {
            GalaxyAnalysisParameters parameters = (GalaxyAnalysisParameters)galaxyMethod.getParameters();
            setParametersFromTest(test.getParameters(), parameters, "");
            galaxyMethod.setParameters(parameters);
            galaxyMethod.justAnalyzeAndPut();
            checkResult(test);
        }
        testSuccess = true;
    }

    /**
     * Sets GalaxyAnalysisParameters from test parameter values
     * @param parameters         test parameters
     * @param dpsParameters      galaxy analysis parameters
     * @param prefix             prefix for test parameter names
     * @throws Exception
     */
    private void setParametersFromTest(Map<String, Parameter> parameters, DynamicPropertySet dpsParameters, String prefix) throws Exception
    {
        for( Map.Entry<String, Parameter> entry : parameters.entrySet() )
        {
            String paramName = entry.getKey();
            Parameter param = entry.getValue();

            if( param instanceof ConfigParameter )
                continue;

            if( param instanceof ArrayParameter )
            {
                ArrayParameter arrayParam = (ArrayParameter)param;
                List<ParametersContainer> childs = arrayParam.getValues();
                DynamicPropertySet[] dpsChilds = new DynamicPropertySet[childs.size()];
                for( int i = 0; i < childs.size(); i++ )
                {
                    dpsChilds[i] = new DynamicPropertySetAsMap();
                    setParametersFromTest(childs.get(i), dpsChilds[i], prefix);
                }
                dpsParameters.setValue(prefix + paramName, dpsChilds);
            }
            else if( param instanceof ConditionalParameter )
            {
                ConditionalParameter condParam = (ConditionalParameter)param;
                Map<String, Parameter> childs = new LinkedHashMap<>();
                childs.put(condParam.getKeyParameterName(), condParam.getKeyParameter());
                childs.putAll(condParam.getWhenParameters(condParam.getKeyParameter().toString()));
                setParametersFromTest(childs, dpsParameters, prefix + paramName + ".");
            }
            else
            {
                dpsParameters.setValue(prefix + paramName, getParameterValue(param));
            }
        }
    }

    /**
     * @param parameter
     * @return parameter value as represented in DynamicProperty
     * @throws Exception
     */
    private Object getParameterValue(Parameter parameter) throws Exception
    {
        if( parameter instanceof StringParameter )
        {
            Object type = parameter.getAttributes().get("type");
            if( type != null )
            {
                if( type.equals("integer") )
                    return Integer.parseInt(parameter.toString());
                if( type.equals("float") )
                    return Float.parseFloat(parameter.toString());
            }
            return parameter.toString();
        }
        if( parameter instanceof BooleanParameter )
            return ( (BooleanParameter)parameter ).getValue();
        if( parameter instanceof FileParameter )
        {
            FileParameter fileParam = (FileParameter)parameter;
            if( !fileParam.isOutput() )
            {
                fileParam.setPath(galaxyTestDataDir);
                importFile(fileParam);
            }
            return DataElementPath.create(workingDC, fileParam.getName());
        }
        if( parameter instanceof SelectParameter )
        {
            SelectParameter select = (SelectParameter)parameter;
            if( select.isMultiple() )
            {
                return StreamEx.split(select.toString(), ',')
                        .mapToEntry( select.getOptions()::get )
                        .mapKeyValue( SelectorOption::new )
                        .toArray( SelectorOption[]::new );
            }
            return new SelectorOption(select.toString(), select.getOptions().get(select.toString()));
        }
        throw new Exception("Unknown parameter type " + parameter.getClass().getName());
    }
    /**
     * Import file to "workingDC/fileName"
     * @param fileParameter
     */
    private void importFile(FileParameter fileParameter) throws Exception
    {
        DataElementPath dePath = DataElementPath.create(workingDC, fileParameter.getName());
        if( dePath.exists() )
            dePath.remove();

        File file = fileParameter.getFile();
        assertTrue("Test file does not exists", file.exists());

        Object format = fileParameter.getAttributes().get("format");
        assertNotNull("Format not specified for file" + fileParameter.getName(), format);

        DataElementImporter importer = FormatRegistry.getImporter(format.toString(), dePath, file).importer;
        assertNotNull("Importer not found for" + format + " format", importer);

        importer.doImport( dePath.getParentCollection(), file, dePath.getName(), null, log );
    }

    public void checkResult(GalaxyMethodTest test) throws Exception
    {
        for( Map.Entry<String, Parameter> entry : test.getParameters().entrySet() )
        {
            String paramName = entry.getKey();
            Parameter param = entry.getValue();
            if( param.isOutput() && param instanceof FileParameter )
            {
                String elementName = ( (FileParameter)param ).getName();
                if( elementName == null )
                    continue;

                DataElementPath dePath = DataElementPath.create(workingDC, elementName);
                assertTrue("Output data element '" + dePath + "' for " + paramName + " does not exists", dePath.exists());

                Object format = param.getAttributes().get("format");
                assertNotNull("Format not specified for " + paramName, format);

                DataElementExporter exporter = FormatRegistry.getExporter(format.toString(), dePath).exporter;
                assertNotNull("Exporter not found for " + paramName + ", format is " + format, exporter);

                File outputFile = new File(outputDir, elementName);
                exporter.doExport(dePath.getDataElement(), outputFile);

                ResultComparator comparator = test.getComparator(paramName);
                StringWriter errors = new StringWriter();
                boolean correctResult = comparator.compare(new File(galaxyTestDataDir, elementName), outputFile, errors);

                assertTrue("Incorrect result for " + paramName + ". " + errors.toString(), correctResult);
            }

        }
    }

    @Override
    public String toString()
    {
        return galaxyMethodInfo.getName();
    }
}
