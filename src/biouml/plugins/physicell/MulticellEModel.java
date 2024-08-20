package biouml.plugins.physicell;

import java.util.List;
import java.util.logging.Logger;

import biouml.model.DiagramElement;
import biouml.model.dynamics.EModel;

public class MulticellEModel extends EModel
{
    private static final Logger log = Logger.getLogger(MulticellEModel.class.getName());
    
    public static final String MULTICELLULAR_EMODEL_TYPE = "Multicellular Model";

    private DomainOptions domain = new DomainOptions();
    private UserParameters userParmeters = new UserParameters();
    private InitialCondition initialCondition = new InitialCondition();
    private ReportProperties reportProperties = new ReportProperties();

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
        doClone( emodel );
        emodel.updateCellDefinitions();
        return emodel;
    }

    /**
     * It adds parts of Cell Definition stored in edges. Nodes are cloned before edges and thus edge parts are not present yet.
     */
    public void updateCellDefinitions()
    {
        log.info("Update");
        for( CellDefinitionProperties cdp : getCellDefinitions() )
        {
            log.info( cdp.getName()+" updating. Edges: "+cdp.getDiagramElement().getEdges().length );
            cdp.update();
        }
    }
}