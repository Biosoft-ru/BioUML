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
    
    public ModelOptions getOptions()
    {
        return options;
    }
    
    public ReportProperties getReportProperties()
    {
        return reportProperties;
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