package biouml.plugins.brain.diagram;

import java.util.List;
import java.util.logging.Level;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.dynamics.TableElement;
import biouml.plugins.brain.model.BrainCellularModelDeployer;
import biouml.plugins.brain.model.BrainReceptorModelDeployer;
import biouml.plugins.brain.model.BrainRegionalModelDeployer;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.subaction.BackgroundDynamicAction;
import ru.biosoft.access.subaction.DynamicAction;
import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@SuppressWarnings ("serial")
public class BrainGenerateEquationsAction extends BackgroundDynamicAction
{
    public BrainGenerateEquationsAction()
    {
        setNumSelected( DynamicAction.SELECTED_ZERO_OR_ANY );
    }
    @Override
    public void validateParameters(Object model, List<DataElement> selectedItems)
    {
        if (!isApplicable(model))
            throw new ParameterNotAcceptableException("Document", String.valueOf(model));
    }

    @Override
    public boolean isApplicable(Object object)
    {
        if (!(object instanceof Diagram))
        {
        	return false;
        }
        
        Diagram diagram = (Diagram)object;
        DiagramType type = diagram.getType();
        if (type == null)
            return false;
        return type instanceof BrainDiagramType;
    }

    @Override
    public JobControl getJobControl(Object model, List<DataElement> selectedItems, Object properties) throws Exception
    {
        return new AbstractJobControl( log )
        {
            @Override
            protected void doRun() throws JobControlException
            {
                try
                {
                	Diagram diagram = ((Diagram)model);
                 
                    int regionalModelCount = BrainUtils.getRegionalModelNodes(diagram).size();
                    int cellularModelCount = BrainUtils.getCellularModelNodes(diagram).size();
                    int receptorModelCount = BrainUtils.getReceptorModelNodes(diagram).size();
                    
                    if (regionalModelCount + cellularModelCount + receptorModelCount == 0)
                    {
                    	log.log(Level.WARNING, "BrainGenerateEquations: please, specify regional, cellular or receptor model.");
                    	return;
                    }
                    else if (regionalModelCount + cellularModelCount +  receptorModelCount > 1)
                    {
                       	log.log(Level.WARNING, "BrainGenerateEquations: only one regional, cellular or receptor model should be provided.");
                    	return;
                    }
                    else
                    {
                    	if (regionalModelCount == 1)
                    	{
                    		int connectivityMatrixCount = BrainUtils.getConnectivityMatrixNodes(diagram).size();                    		
                    		
                            if (connectivityMatrixCount == 0)
                            {
                            	log.log(Level.WARNING, "BrainGenerateEquations: please, specify connectivity matrix for regional model generation.");
                            	return;
                            }
                            else if (connectivityMatrixCount > 1)
                            {
                            	log.log(Level.WARNING, "BrainGenerateEquations: only one connectivity matrix should be provided for regional model generation.");
                            	return;
                            }
                            else
                            {
                              	TableElement connectivityTe = BrainUtils.getConnectivityMatrixNodes(diagram).get(0).getRole(TableElement.class);
                        	    int connectivitySizeRows = connectivityTe.getTable().getSize();
                        	    int connectivitySizeColumns = connectivityTe.getVariables().length;
                        	    
                            	if (connectivitySizeRows == 0) 
                            	{
                            		log.log(Level.WARNING, "BrainGenerateEquations: connectivity matrix must have at least one region.");
                                	return;
                            	}
                                else if (connectivitySizeRows != connectivitySizeColumns)
                            	{
                            		log.log(Level.WARNING, "BrainGenerateEquations: connectivity matrix must be square.");
                                	return;
                            	}
                            	
                            	int delayMatrixCount = BrainUtils.getDelayMatrixNodes(diagram).size();
                            	if (delayMatrixCount > 1)
                            	{
                                	log.log(Level.WARNING, "BrainGenerateEquations: only one delay matrix should be provided for regional model generation.");
                                	return;
                            	}
                            	else if (delayMatrixCount == 1)
                            	{
                                  	TableElement delayTe = BrainUtils.getDelayMatrixNodes(diagram).get(0).getRole(TableElement.class);
                            	    int delaySizeRows = delayTe.getTable().getSize();
                            	    int delaySizeColumns = delayTe.getVariables().length;
                            	    
                                    if (delaySizeRows != connectivitySizeRows || delaySizeColumns != connectivitySizeColumns)
                                	{
                                		log.log(Level.WARNING, "BrainGenerateEquations: connectivity matrix and delay matrix must be the same size.");
                                    	return;
                                	}
                            	}
                            	
                                log.log(Level.INFO, "BrainGenerateEquations: starting regional model equations generation.");
                                Diagram diagramModel = BrainRegionalModelDeployer.deployBrainRegionalModel(diagram, ""); 
                                CollectionFactoryUtils.save(diagramModel);
                                log.log(Level.INFO, "BrainGenerateEquations: regional model equations generation is completed.");
                            }
                    	}
                    	else if (cellularModelCount == 1)
                    	{
                            log.log(Level.INFO, "BrainGenerateEquations: starting cellular model equations generation.");
                            Diagram diagramModel = BrainCellularModelDeployer.deployBrainCellularModel(diagram, ""); 
                            CollectionFactoryUtils.save(diagramModel);
                            log.log(Level.INFO, "BrainGenerateEquations: cellular model equations generation is completed.");
                    	}
                    	else if (receptorModelCount == 1)
                    	{
                            log.log(Level.INFO, "BrainGenerateEquations: starting receptor model equations generation.");
                            Diagram diagramModel = BrainReceptorModelDeployer.deployBrainReceptorModel(diagram, ""); 
                            CollectionFactoryUtils.save(diagramModel);
                            log.log(Level.INFO, "BrainGenerateEquations: receptor model equations generation is completed.");
                    	}
                    }
                }
                catch(Exception e)
                {
                    throw new JobControlException(e);
                }
            }
        };
    }

}
