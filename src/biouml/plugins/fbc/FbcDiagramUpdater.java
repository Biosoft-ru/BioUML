package biouml.plugins.fbc;

import java.beans.PropertyChangeEvent;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxBounds;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxObjFunc;
import biouml.standard.type.Reaction;
import ru.biosoft.table.RowDataElement;

public class FbcDiagramUpdater implements FbcConstant
{
    public static void update(Diagram diagram, PropertyChangeEvent evt)
    {
        String name = ((RowDataElement)evt.getSource()).getName();
        String propertyName = evt.getPropertyName();
        update( diagram, name, propertyName, evt.getNewValue().toString() );
    }

    public static void update(Diagram diagram, String name, String propertyName, String value)
    {
        try
        {
            switch( propertyName )
            {
                case "Formula":
                    updateFormula( name, value, diagram );
                    break;
                case "Coefficient Objective Function":
                    updateCoefObjFunc( name, Double.parseDouble( value ), diagram );
                    break;
                case "Greater":
                    updateBounds( FBC_GREATER_EQUAL, name, value, diagram );
                    break;
                case "Equal":
                    updateBounds( FBC_EQUAL, name, value, diagram );
                    break;
                case "Less":
                    updateBounds( FBC_LESS_EQUAL, name, value, diagram );
                    break;
                default:
                    break;
            }
        }
        catch( Exception e )
        {
        }
    }

    private static void updateCoefObjFunc(String name, Double value, Diagram diagram) throws Exception
    {
        Node reaction = diagram.findNode(name);
        FluxObjFunc fluxObjective = SbmlModelFBCReader.getFluxObjFunc(reaction);
        DynamicPropertySet dps = diagram.getAttributes();
        String activObj = dps.getValueAsString(FBC_ACTIVE_OBJECTIVE);
        if(activObj == null)
            throw new Exception("Active object function isn't defined");

        int index = fluxObjective.idObj.indexOf(activObj);
        if(index != -1)
            fluxObjective.coefficient.set(index, value);
        else
            fluxObjective.addObjectiveCoefficient(null, null, activObj, null, value);
    }

    private static void updateBounds(String sign, String name, String value, Diagram diagram)
    {
        Node reaction = diagram.findNode(name);

        FluxBounds bounds = SbmlModelFBCReader.getFluxBounds(reaction);
        int index = bounds.sign.indexOf(sign);
        if(index != -1)
            bounds.value.set(index, value);
        else
            bounds.addBound(sign, value);
    }

    private static void updateFormula(String name, String formula,Diagram diagram)
    {
        Node reaction = diagram.findNode(name);
        Reaction r = (Reaction)reaction.getKernel();
        r.setFormula(formula);
    }
}
