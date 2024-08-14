package biouml.plugins.brain.diagram;

import java.awt.Point;
import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.plugins.brain.model.regional.BrainMatrixProperties;
import biouml.standard.diagram.CreatorElementWithName;
import biouml.standard.diagram.MathDiagramSemanticController;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;

public class BrainDiagramSemanticController extends MathDiagramSemanticController implements CreatorElementWithName
{
	@Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
		if (type.equals(BrainType.TYPE_CONNECTIVITY_MATRIX) || type.equals(BrainType.TYPE_DELAY_MATRIX) 
				|| type.equals(BrainRegionalModel.class) || type.equals(BrainCellularModel.class) || type.equals(BrainReceptorModel.class)) 
		{
			try
			{
				Object properties = getPropertiesByType(parent, type, point);

				PropertiesDialog dialog = new PropertiesDialog(Application.getApplicationFrame(), "New element", properties);
				if (dialog.doModal())
				{
					if (properties instanceof InitialElementProperties)
					{
						((InitialElementProperties)properties).createElements(parent, point, viewEditor);
					}
					return null;
				}
			}
			catch( Throwable t )
			{
				//throw ExceptionRegistry.translateException( t );
				return null;
			}
			return DiagramElementGroup.EMPTY_EG;
		}
		
		return super.createInstance( parent, type, point, viewEditor );
    }
	
    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        try
        {
            Object bean = super.getPropertiesByType(compartment, type, point);
            if (bean != null)
                return bean;

            if (type.equals(BrainType.TYPE_CONNECTIVITY_MATRIX))
            	return new BrainMatrixProperties(generateUniqueNodeName(compartment, "Connectivity_matrix", false), BrainType.TYPE_CONNECTIVITY_MATRIX);
            else if (type.equals(BrainType.TYPE_DELAY_MATRIX))
            	return new BrainMatrixProperties(generateUniqueNodeName(compartment, "Delay_matrix", false), BrainType.TYPE_DELAY_MATRIX);
            else if (type.equals(BrainRegionalModel.class))
                return new BrainRegionalModel(generateUniqueNodeName(compartment, "Regional_model", false), false);
            else if (type.equals(BrainCellularModel.class))
                return new BrainCellularModel(generateUniqueNodeName(compartment, "Cellular_Model", false), false);
            else if (type.equals(BrainReceptorModel.class))
                return new BrainReceptorModel(generateUniqueNodeName(compartment, "Receptor_Model", false), false);
        }
        catch( Exception e )
        {
            //throw ExceptionRegistry.translateException( e );
        	return null;
        }
        return null;
    }
    
    @Override
    public boolean canAccept(Compartment parent, DiagramElement de)
    {
        if (de.getKernel() == null) 
        {
        	return false;
        }

        if (de instanceof Node)
        {
            Node node = (Node)de;

            if (BrainUtils.isConnectivityMatrix(node) || BrainUtils.isDelayMatrix(node)
            		|| BrainUtils.isRegionalModel(node) || BrainUtils.isCellularModel(node) || BrainUtils.isReceptorModel(node))
            {
                return parent instanceof Diagram;
            }
        }
        
        return super.canAccept(parent, de);
    }
    
    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement)
    {
        Base kernel = de.getKernel();
       
        if (kernel != null && kernel instanceof Stub && BrainType.TYPE_REGIONAL_MODEL.equals(kernel.getType()))
        {
            if (de.getRole() == null)
            {
                DynamicProperty dp = de.getAttributes().getProperty("regionalModel");
                BrainRegionalModel regionalModel = (BrainRegionalModel)dp.getValue();
                regionalModel.setDiagramElement(de);
                de.setRole(regionalModel);
            }
        } 
        else if (kernel != null && kernel instanceof Stub && BrainType.TYPE_CELLULAR_MODEL.equals(kernel.getType()))
        {
            if (de.getRole() == null)
            {
                DynamicProperty dp = de.getAttributes().getProperty("cellularModel");
                BrainCellularModel cellularModel = (BrainCellularModel)dp.getValue();
                cellularModel.setDiagramElement(de);
                de.setRole(cellularModel);
            }
        }
        else if (kernel != null && kernel instanceof Stub && BrainType.TYPE_RECEPTOR_MODEL.equals(kernel.getType()))
        {
            if (de.getRole() == null)
            {
                DynamicProperty dp = de.getAttributes().getProperty("receptorModel");
                BrainReceptorModel receptorModel = (BrainReceptorModel)dp.getValue();
                receptorModel.setDiagramElement(de);
                de.setRole(receptorModel);
            }
        } 
        else 
        {
        	try 
        	{
        	    super.validate(compartment, de, newElement);
        	}
        	catch (Exception e) 
        	{
        		// TODO Auto-generated catch block
        		e.printStackTrace();
        	}
        }

        return de;
    }
}
