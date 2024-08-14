package biouml.workbench.module._test;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import biouml.model.Module;
import biouml.model.util.ModulePackager;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.Repository;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.util.ExProperties;

public class ImportExportModuleTest extends AbstractBioUMLTest
{
    private final static String repositoryPath = "../data";

    public ImportExportModuleTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ImportExportModuleTest.class.getName());
        suite.addTest(new ImportExportModuleTest("testInitFramework"));
        suite.addTest(new ImportExportModuleTest("testExportSpecialTranspath"));
        suite.addTest(new ImportExportModuleTest("testImportTranspath"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public static final String geneNetTestModuleName = "GeneNetTest";
    public static final String geneNetTestModuleVersion = "1.0 test";
    public static final String geneNetTestModuleDescription = "Test description";
    public static final String geneNetModuleFileName = "genenet_test";

    public void testInitFramework() throws Exception
    {
        CollectionFactory.createRepository( repositoryPath );
        assertNotNull( "Repository was not initalized", CollectionFactoryUtils.getDatabases() );
    }

    private void testModuleNameChangedInConfigFile(JarFile moduleJarFile, String configFileName, String expectedValue) throws Exception
    {
        JarEntry jarEntry = moduleJarFile.getJarEntry(configFileName);
        assertNotNull("The file " + configFileName + " is not found in " + moduleJarFile.getName(), jarEntry);
        File tmpFile = new File(configFileName);
        if (tmpFile.exists())
        {
            tmpFile.delete();
        }
        ModulePackager.extractFile(new File("."), configFileName, moduleJarFile, jarEntry);
        assertTrue("The " + configFileName + " is not extracted", tmpFile.exists());

        Properties properties = new ExProperties(tmpFile);

        String value = properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY);
        assertNotNull("The property " + DataCollectionConfigConstants.NAME_PROPERTY + " is not set in " + configFileName + " file.", value);
        assertEquals("The name is not changed", expectedValue, value);
        tmpFile.delete();
    }

    private void testRemoveModule(String moduleName) throws Exception
    {
        DataCollection repository = CollectionFactoryUtils.getDatabases();
        repository.remove(moduleName);
    }

    private void removeTestBMDFile(String moduleFileName) throws Exception
    {
        String oldModuleFileName = moduleFileName + ModulePackager.BMD_FILE_EXTENTION;
        File oldModuleFile = new File(oldModuleFileName);
        if (oldModuleFile.exists())
        {
            oldModuleFile.delete();
        }
    }


    public static final String transpathTestModuleName = "TranspathTest";
    public static final String transpathTestModuleVersion = "1.0 test ";
    public static final String transpathTestModuleDescription = "Transpath test description";
    public static final String transpathTestModuleFileName = "transpath_test";

    public void testExportSpecialTranspath() throws Exception
    {
        removeTestBMDFile(transpathTestModuleFileName);

        CollectionFactory.createRepository( repositoryPath );

        String modulePath = "databases/transpath";
        Module module = (Module)CollectionFactory.getDataCollection("databases/Transpath");

        assertNotNull("Transpath module is null", module);

        Set<String> excludedFiles = new HashSet<>();
        excludedFiles.add("molecule.dat");
        excludedFiles.add("reaction.dat");
        ModulePackager.exportModule(module, transpathTestModuleName, transpathTestModuleVersion, transpathTestModuleDescription,
                                    transpathTestModuleFileName, null, excludedFiles);
        File moduleFile = new File(transpathTestModuleFileName + ModulePackager.BMD_FILE_EXTENTION);
        assertTrue("The " + transpathTestModuleFileName + ModulePackager.BMD_FILE_EXTENTION + " is not created", moduleFile.exists());

        JarFile moduleJarFile = new JarFile(moduleFile);
        Manifest manifest = moduleJarFile.getManifest();
        assertNotNull("Transpath jar file manifest is null", manifest);

        Attributes attr = manifest.getMainAttributes();
        assertEquals("The module names are not the same",       transpathTestModuleName,         attr.getValue(ModulePackager.MF_DATABASE_NAME));
        assertEquals("The module versions are not the same",    transpathTestModuleVersion,      attr.getValue(ModulePackager.MF_DATABASE_VERSION));
        assertEquals("The module descriptions are not the same",transpathTestModuleDescription,  attr.getValue(ModulePackager.MF_DATABASE_DESCRIPTION));

        testModuleNameChangedInConfigFile(moduleJarFile, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE, transpathTestModuleName);
        testModuleNameChangedInConfigFile(moduleJarFile, DataCollectionConfigConstants.DEFAULT_REPOSITORY, transpathTestModuleName);
    }

    public void testImportTranspath() throws Exception
    {
        String moduleName = transpathTestModuleName;
        CollectionFactory.createRepository( repositoryPath );
        testRemoveModule(moduleName);

        File moduleFile = new File(transpathTestModuleFileName + ModulePackager.BMD_FILE_EXTENTION);
        assertTrue("The " + transpathTestModuleFileName + ModulePackager.BMD_FILE_EXTENTION + " is not exist", moduleFile.exists());

        JarFile moduleJarFile = new JarFile(moduleFile);
        Repository repository = (Repository)CollectionFactoryUtils.getDatabases();
        ModulePackager.importModule(null, moduleJarFile, repository, null);
/*
        TranspathModuleActivator moduleActivator = (TranspathModuleActivator)get(new ModulePackager(), "moduleActivator");
        assertNotNull("TranspathModuleActivator is not created", moduleActivator);

        TranspathModuleSetupDialog moduleSetupDialog = (TranspathModuleSetupDialog)get(moduleActivator, "dialog");
        assertNotNull("TranspathModuleSetupDialog is not initialized", moduleSetupDialog);

        JTextField moleculePathTextField = (JTextField)get(moduleSetupDialog, "moleculePathTextField");
        assertNotNull("moleculePathTextField is null", moleculePathTextField);

        JTextField reactionPathTextField = (JTextField)get(moduleSetupDialog, "reactionPathTextField");
        assertNotNull("reactionPathTextField is null", reactionPathTextField);

        String originalTranspathDataDirName = "databases/Transpath/" + Module.DATA ;
        File originalTranspathDataDir = new File(originalTranspathDataDirName);
        assertTrue(originalTranspathDataDir + " is not exist", originalTranspathDataDir.exists());

        File moleculeFile = new File(originalTranspathDataDir, "molecule.dat");
        assertTrue(moleculeFile.getPath() + " is not exist", moleculeFile.exists());
        moleculePathTextField.setText(moleculeFile.getPath());

        File reactionFile = new File(originalTranspathDataDir, "reaction.dat");
        assertTrue(reactionFile.getPath() + " is not exist", reactionFile.exists());
        reactionPathTextField.setText(reactionFile.getPath());

        JButton okButton = (JButton)get(moduleSetupDialog, "okButton");
        assertNotNull("okButton is null", okButton);

        okButton.setEnabled(true);
        okButton.doClick();
*/
        String moduleDirName = repositoryPath + "/" + moduleName;
        File moduleDir = new File(moduleDirName);
        File moduleInfoFile = new File(moduleDir, ModulePackager.INFO_FILENAME);
        assertTrue("The " + moduleInfoFile.getPath() + " is not exist", moduleInfoFile.exists());

        assertNotNull(moduleName + " data collection was not created", CollectionFactoryUtils.getDatabases().get(moduleName));
        testRemoveModule(transpathTestModuleName);
    }

}
