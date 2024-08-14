package ru.biosoft.galaxy;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckForNull;

import org.w3c.dom.Element;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.galaxy.parameters.Parameter;

import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Extension of {@link AnalysisMethodInfo} with Galaxy specifics, parsed Galaxy tool's config file.
 */
@ClassIcon ( "resources/galaxy.gif" )
@PropertyName ("galaxy method")
public class GalaxyMethodInfo extends AnalysisMethodInfo
{
    protected Command command;
    protected ParametersContainer parameters;
    
    protected List<GalaxyMethodTest> tests;
    private final List<Element> testElements;
    
    protected String title;
    protected String extendedTitle;
    
    private final List<Requirement> requirements = new ArrayList<>();
    private ToolShedElement toolShedElement;
    /**
     * id is XML file name like "fastx_toolkit/fasta_formatter.xml"
     * It's used by AnalysesOverridesRegistry to identify the analysis as single XML may be included several times,
     * thus identifying it by repository path is ineffective
     */
    protected String id;
    public GalaxyMethodInfo(String name, String title, String description, DataCollection parent)
    {
        super(name, description, parent, GalaxyMethod.class);

        this.title = title;
        this.tests = new ArrayList<>();
        this.testElements = new ArrayList<>();
    }

    @Override
    public AnalysisMethod createAnalysisMethod()
    {
        GalaxyMethod method = (GalaxyMethod)super.createAnalysisMethod();
        method.setMethodInfo(this);
        return method;
    }

    public Command getCommand()
    {
        return command;
    }

    public void setCommand(Command command)
    {
        this.command = command;
    }

    public void addTest(GalaxyMethodTest test)
    {
        tests.add(test);
    }
    
    public void addTest(Element element)
    {
        testElements.add(element);
    }

    public List<GalaxyMethodTest> getTests()
    {
        for( Element e : testElements )
            addTest(MethodInfoParser.parseTest(e, this));
        testElements.clear();
        return tests;
    }
    
    public Iterable<Requirement> getRequirements()
    {
        return requirements;
    }
    
    public void addRequirement(Requirement requirement)
    {
        requirements.add( requirement );
    }

    public ParametersContainer getParameters()
    {
        if( parameters == null )
        {
            parameters = new ParametersContainer();
        }
        return parameters;
    }

    public Parameter getParameter(String name)
    {
        return getParameters().getParameter(name);
    }

    @Override
    public String getDisplayName()
    {
        return title;
    }

    protected String descriptionHTML = null;
    @Override
    public String getDescriptionHTML()
    {
        if( descriptionHTML == null )
        {
            descriptionHTML = GalaxyFactory.convertRstToHtml(description);
        }
        return descriptionHTML;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    @Override
    public String getBaseId()
    {
        return ClassLoading.getPluginForClass( getClass() )+":"+GalaxyImageLoader.class.getName()+"?";
    }

    @Override
    public URL getBase()
    {
        try
        {
            return GalaxyDataCollection.getGalaxyDistFiles().getRootFolder().toURI().toURL();
        }
        catch( MalformedURLException e )
        {
            return null;
        }
    }

    @CheckForNull
    public ToolShedElement getToolShedElement()
    {
        return toolShedElement;
    }
    
    public void setToolShedElement(ToolShedElement toolShedElement)
    {
        this.toolShedElement = toolShedElement;
    }
}
