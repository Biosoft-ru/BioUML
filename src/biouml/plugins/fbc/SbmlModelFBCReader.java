package biouml.plugins.fbc;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.XmlUtil;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.sbml.SbmlPackageReader;


public class SbmlModelFBCReader extends SbmlPackageReader implements FbcConstant
{

    @Override
    public void processSpecie(Element element, Node node) throws Exception
    {
        String charge = element.getAttribute(FBC_CHARGE);
        String chemicalFormula = element.getAttribute(FBC_CHEMICAL_FORMULA);

        node.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(FBC_CHARGE, String.class, charge));
        node.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(FBC_CHEMICAL_FORMULA, String.class, chemicalFormula));
    }

    @Override
    public void preprocessDiagram(Element element, Diagram diagram) throws Exception
    {
        this.diagram = diagram;
    }

    @Override
    public void postprocessDiagram(Element element, Diagram diagram) throws Exception
    {
        readFluxBounds(element);
        readListOfObjectives(element);
    }

    @Override
    public String getPackageName()
    {
        return "fbc";
    }

    private void readListOfObjectives(Element model)
    {
        Element listOfObjectives = getElement(model, LIST_OF_OBJECTIVES);
        if( listOfObjectives == null )
            return;
        DynamicProperty dp = DPSUtils.createHiddenReadOnlyTransient(FBC_ACTIVE_OBJECTIVE, String.class,
                listOfObjectives.getAttribute(FBC_ACTIVE_OBJECTIVE));
        diagram.getAttributes().add(dp);
        XmlUtil.elements(listOfObjectives, OBJECTIVE).forEach(e -> readObjective(e));
    }

    private void readObjective(Element element)
    {
        String objId = element.getAttribute(FBC_ID);
        String objName = element.getAttribute(FBC_NAME);
        String objType = element.getAttribute(FBC_TYPE);

        DynamicProperty dp = diagram.getAttributes().getProperty(FBC_LIST_OBJECTIVES);
        if( dp == null || !dp.getType().isAssignableFrom(HashMap.class) )
        {
            HashMap<String, String> listObj = new HashMap<>();
            listObj.put(objId, objType);
            diagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(FBC_LIST_OBJECTIVES, HashMap.class, listObj));
        }
        else
        {
            ((HashMap<String, String>)dp.getValue()).put(objId, objType);
        }
        
        Element listFluxObjectives = getElement(element, LIST_OF_FLUX_OBJECTIVES);
        if( listFluxObjectives == null )
            return;

        XmlUtil.elements(listFluxObjectives, FLUX_OBJECTIVE).forEach(fluxObj->readFluxObjective(fluxObj, objId, objName));
    }

    private void readFluxObjective(Element fluxObj, String idObj, String nameObj)
    {
        try
        {
            biouml.model.Node reactionNode = diagram.findNode(fluxObj.getAttribute(FBC_REACTION));
            if( reactionNode != null )
            {
                String idCoefficient = fluxObj.getAttribute(FBC_ID);
                String nameCoefficient = fluxObj.getAttribute(FBC_NAME);
                double coefficient = Double.parseDouble( fluxObj.getAttribute( FBC_COEFFICIENT ) );
                getFluxObjFunc(reactionNode).addObjectiveCoefficient(idCoefficient, nameCoefficient, idObj, nameObj, coefficient);
            }
        }
        catch( NumberFormatException ex )
        {

        }
    }

    protected static FluxObjFunc getFluxObjFunc(Node reactionNode)
    {
        FluxObjFunc fluxObjective;

        DynamicPropertySet dps = reactionNode.getAttributes();
        DynamicProperty dp = dps.getProperty(FBC_OBJECTIVES);
        if( dp == null || !dp.getType().isAssignableFrom(FluxObjFunc.class) )
        {
            fluxObjective = new FluxObjFunc();
            dps.add(DPSUtils.createHiddenReadOnlyTransient(FBC_OBJECTIVES, FluxObjFunc.class, fluxObjective));
        }
        else
        {
            fluxObjective = (FluxObjFunc)dp.getValue();
        }
        return fluxObjective;
    }

    protected void readFluxBounds(Element model)
    {
        Element fluxBoundsList = getElement(model, LIST_OF_FLUX_BOUNDS);
        if( fluxBoundsList != null )
            XmlUtil.elements(fluxBoundsList, FLUX_BOUND).forEach(e -> readBound(e));
    }
    
    private void readBound(Element element)
    {
        try
        {
            String reaction = element.getAttribute(FBC_REACTION);
            biouml.model.Node reactionNode = diagram.findNode(reaction);
            if( reactionNode != null )
            {
                FluxBounds fluxBounds = getFluxBounds(reactionNode);
                String sign = element.getAttribute(FBC_OPERATION);
                String value = element.getAttribute( FBC_VALUE );
                //                String id = element.getAttribute(FBC_ID);
                //                String name = element.getAttribute(FBC_NAME);
                fluxBounds.addBound( sign, value );
            }
        }
        catch( NumberFormatException ex )
        {

        }
    }

    protected static FluxBounds getFluxBounds(Node reactionNode)
    {
        FluxBounds fluxBounds;

        DynamicProperty dp = reactionNode.getAttributes().getProperty(FBC_BOUNDS);
        if( dp == null || !dp.getType().isAssignableFrom(FluxBounds.class) )
        {
            fluxBounds = new FluxBounds();
            reactionNode.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient(FBC_BOUNDS, FluxBounds.class, fluxBounds));
        }
        else
        {
            fluxBounds = (FluxBounds)dp.getValue();
        }
        return fluxBounds;
    }

    public static class FluxObjFunc
    {
        public ArrayList<String> idCoefficient;
        public ArrayList<String> nameCoefficient;
        public ArrayList<String> idObj;
        public ArrayList<String> nameObj;
        public ArrayList<Double> coefficient;

        public FluxObjFunc()
        {
            this.idCoefficient = new ArrayList<>();
            this.nameCoefficient = new ArrayList<>();
            this.idObj = new ArrayList<>();
            this.nameObj = new ArrayList<>();
            this.coefficient = new ArrayList<>();
        }

        public void addObjectiveCoefficient(String idCoefficient, String nameCoefficient, String idObj, String nameObj, Double coefficient)
        {
            this.idCoefficient.add( idCoefficient );
            this.nameCoefficient.add( nameCoefficient );
            this.idObj.add( idObj );
            this.nameObj.add( nameObj );
            this.coefficient.add( coefficient );
        }
    }

    public static class FluxBounds
    {
        public ArrayList<String> sign;
        public ArrayList<String> value;

        public FluxBounds()
        {
            this.sign = new ArrayList<>();
            this.value = new ArrayList<>();
        }

        public void addBound(String sign, String value)
        {
            this.sign.add( sign );
            this.value.add( value );
        }
    }
}
