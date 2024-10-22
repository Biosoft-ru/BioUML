package biouml.plugins.physicell;

import java.util.List;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.dynamics.EModelRoleSupport;

public class MulticellEModel extends EModelRoleSupport
{
    public static final String MULTICELLULAR_EMODEL_TYPE = "Multicellular Model";

    private DomainOptions domain = new DomainOptions();
    private UserParameters userParmeters = new UserParameters();
    private InitialCondition initialCondition = new InitialCondition();
    private ReportProperties reportProperties = new ReportProperties();
    private ModelOptions options = new ModelOptions();
    private VisualizerProperties visualizerProperties = new VisualizerProperties();
    private ColorScheme[] schemes = new ColorScheme[0];

    public MulticellEModel()
    {
        reportProperties.setModel( this );
        visualizerProperties.setEModel( this );
    }

    public ModelOptions getOptions()
    {
        return options;
    }

    public ReportProperties getReportProperties()
    {
        return reportProperties;
    }

    public VisualizerProperties getVisualizerProperties()
    {
        return visualizerProperties;
    }

    public ColorScheme[] getColorSchemes()
    {
        return schemes;
    }
    public void setColorSchemes(ColorScheme[] schemes)
    {
        this.schemes = schemes;
    }

    public void addColorScheme(ColorScheme scheme)
    {
        int l = schemes.length;
        ColorScheme[] newSchemes = new ColorScheme[l + 1];
        newSchemes[l] = scheme;
        System.arraycopy( schemes, 0, newSchemes, 0, l );
        setColorSchemes( newSchemes );
    }
    
    public void addColorScheme()
    {
        int l = schemes.length;
        addColorScheme( new ColorScheme(String.valueOf( l )));
    }
    
    public void removeColorScheme(int index)
    {
        int l = schemes.length;
        ColorScheme[] newSchemes = new ColorScheme[l - 1];
        if( index == 0 )
            System.arraycopy( schemes, 1, newSchemes, 0, l - 1 );
        else if( index == l - 1 )
            System.arraycopy( schemes, 0, newSchemes, 0, l - 1 );
        else
        {
            System.arraycopy( schemes, 0, newSchemes, 0, index );
            System.arraycopy( schemes, index + 1, newSchemes, index, l - index - 1 );
        }
        this.setColorSchemes( newSchemes );
    }

    public UserParameters getUserParmeters()
    {
        return userParmeters;
    }

    public void addUserParameter(UserParameter parameter)
    {
        userParmeters.addUserParameter( parameter );
    }

    public InitialCondition getInitialCondition()
    {
        return initialCondition;
    }

    public void setInitialCondition(InitialCondition condition)
    {
        this.initialCondition = condition;
    }

    public MulticellEModel(DiagramElement diagramElement)
    {
        super( diagramElement );
        reportProperties.setModel( this );
        visualizerProperties.setEModel( this );
    }

    @Override
    public String getType()
    {
        return MULTICELLULAR_EMODEL_TYPE;
    }

    public DomainOptions getDomain()
    {
        return domain;
    }

    @Override
    public Diagram getDiagramElement()
    {
        return (Diagram)super.getDiagramElement();
    }

    @Override
    public Diagram getParent()
    {
        return (Diagram)super.getParent();
    }

    public List<EventProperties> getEvents()
    {
        return this.getDiagramElement().stream().map( n -> n.getRole() ).select( EventProperties.class ).toList();
    }


    public List<SubstrateProperties> getSubstrates()
    {
        return this.getDiagramElement().stream().map( n -> n.getRole() ).select( SubstrateProperties.class ).toList();
    }

    public List<CellDefinitionProperties> getCellDefinitions()
    {
        return this.getDiagramElement().stream().map( n -> n.getRole() ).select( CellDefinitionProperties.class ).toList();
    }

    public List<SecretionProperties> getSecretions()
    {
        return this.getDiagramElement().stream().map( n -> n.getRole() ).select( SecretionProperties.class ).toList();
    }

    public List<ChemotaxisProperties> getChemotaxis()
    {
        return this.getDiagramElement().stream().map( n -> n.getRole() ).select( ChemotaxisProperties.class ).toList();
    }

    @Override
    public MulticellEModel clone(DiagramElement de)
    {
        MulticellEModel emodel = new MulticellEModel( de );
        emodel.userParmeters = this.userParmeters.clone();
        emodel.domain = domain.clone();
        emodel.initialCondition = initialCondition.clone();
        emodel.reportProperties = reportProperties.clone();
        reportProperties.setModel( emodel );
        emodel.userParmeters = userParmeters.clone();
        emodel.options = options.clone();
        emodel.comment = comment;
        emodel.updateCellDefinitions();
        return emodel;
    }

    /**
     * It adds parts of Cell Definition stored in edges. Nodes are cloned before edges and thus edge parts are not present yet.
     */
    public void updateCellDefinitions()
    {
        for( CellDefinitionProperties cdp : getCellDefinitions() )
            cdp.update();
    }
}