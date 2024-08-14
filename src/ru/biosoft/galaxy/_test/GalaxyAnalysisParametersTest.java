package ru.biosoft.galaxy._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.galaxy.GalaxyAnalysisParameters.SelectorOption;
import ru.biosoft.galaxy.GalaxyMethod;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

public class GalaxyAnalysisParametersTest extends TestCase
{

    public GalaxyAnalysisParametersTest(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( GalaxyAnalysisParametersTest.class.getName() );
        suite.addTest( new GalaxyAnalysisParametersTest( "testTophat" ) );
        return suite;
    }

    public void testTophat() throws Exception
    {
        DataElement de = DataElementPath.create( "analyses/Galaxy/ngs-rna-tools/tophat" ).optDataElement();
        assertNotNull( "Can't find tophat analysis", de );
        assertTrue( "Data element is not analysis method info", ( de instanceof AnalysisMethodInfo ) );

        AnalysisMethod method = ( (AnalysisMethodInfo)de ).createAnalysisMethod();
        assertNotNull( "Can't create analysis", method );
        assertTrue( "Analysis is not Galaxy method", ( method instanceof GalaxyMethod ) );
        AnalysisParameters parameters = method.getParameters();
        ComponentModel model = ComponentFactory.getModel( parameters );
        assertEquals( "indexed", ( (SelectorOption)model.findProperty( "refGenomeSource|genomeSource" ).getValue() ).getName() );
        assertTrue( "when indexed, index property not visible",
                model.findProperty( "refGenomeSource|indexed|index" ).isVisible( Property.SHOW_EXPERT ) );
        assertFalse( "when indexed, ownFile property visible",
                model.findProperty( "refGenomeSource|history|ownFile" ).isVisible( Property.SHOW_EXPERT ) );
        model.findProperty( "refGenomeSource|genomeSource" ).setValue( new SelectorOption( "history", "Use one from the history" ) );
        assertEquals( "history", ( (SelectorOption)model.findProperty( "refGenomeSource|genomeSource" ).getValue() ).getName() );
        assertFalse( "when history, index property visible",
                model.findProperty( "refGenomeSource|indexed|index" ).isVisible( Property.SHOW_EXPERT ) );
        assertTrue( "when history, ownFile property not visible",
                model.findProperty( "refGenomeSource|history|ownFile" ).isVisible( Property.SHOW_EXPERT ) );
    }
}
