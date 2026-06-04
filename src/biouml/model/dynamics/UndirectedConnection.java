package biouml.model.dynamics;

import java.util.HashMap;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.standard.diagram.Util;

public class UndirectedConnection extends Connection
{
    public static enum MainVariableType
    {
        INPUT, OUTPUT, NOT_SELECTED
    }

    public static final String NOT_SELECTED_STRING = "Not selected";
    private final Map<MainVariableType, String> varTypeDescription;
    private final boolean varIsEditable;
    private MainVariableType mainVariable = MainVariableType.NOT_SELECTED;
    private String conversionFactor = "";
    private double initialValue; //TODO: delete

    public UndirectedConnection(Edge edge)
    {
        super(edge);
        mainVariable = MainVariableType.NOT_SELECTED;
        varIsEditable = edge.nodes().noneMatch( Util::isPropagatedPort );
        varTypeDescription = new HashMap<>();
        varTypeDescription.put( MainVariableType.NOT_SELECTED, NOT_SELECTED_STRING );
    }

    @Override
    public void setInputPort(Port p)
    {
        super.setInputPort( p );
        Edge edge = (Edge)getDiagramElement();
        if( !Util.isBus( edge.getInput() ) )
            varTypeDescription.put( MainVariableType.INPUT, generateVarDescription( edge.getInput(), getInputPort() ) );
    }

    @Override
    public void setOutputPort(Port p)
    {
        super.setOutputPort( p );
        Edge edge = (Edge)getDiagramElement();
        if( !Util.isBus( edge.getOutput() ) )
            varTypeDescription.put( MainVariableType.OUTPUT, generateVarDescription( edge.getOutput(), getOutputPort() ) );
    }
    
    private String generateVarDescription(Node node, Port port)
    {
        return  node.getParent() instanceof Diagram? port.getVariableName():
            ((Compartment)node.getParent()).getName()+"/"+ port.getVariableName();
    }

    public void setMainVariableType(MainVariableType mainVariable)
    {
        String stringValue = varTypeDescription.get( mainVariable );
        if( !StreamEx.of( getAvailableNames() ).toSet().contains( stringValue ) )
            return;
        MainVariableType oldValue = this.mainVariable;
        this.mainVariable = mainVariable;
        firePropertyChange( "mainVariable", oldValue, mainVariable );
    }

    public MainVariableType getMainVariableType()
    {
        return mainVariable;
    }


    public String[] getAvailableNames()
    {
        try
        {
            if( !varIsEditable )
                return new String[] {getVariableNamePath()};
            return varTypeDescription.values().toArray( new String[varTypeDescription.size()] );
        }
        catch( Exception ex )
        {
            return new String[] {NOT_SELECTED_STRING};
        }
    }

    public double getInitialValue()
    {
        return initialValue;
    }

    public void setInitialValue(double initialValue)
    {
        this.initialValue = initialValue;
    }

    @Override
    public Role clone(DiagramElement de)
    {
        UndirectedConnection connection = new UndirectedConnection((Edge)de);
        doClone(connection);
        return connection;
    }

    public void doClone(UndirectedConnection connection)
    {
        super.doClone(connection);
        connection.initialValue = initialValue;
        connection.mainVariable = mainVariable;
        connection.conversionFactor = conversionFactor;
        connection.varTypeDescription.putAll( varTypeDescription);
    }

    
    @PropertyName("Main variable")
    @PropertyDescription("Main variable.")
    public String getVariableNamePath()
    {
        return varTypeDescription.get(this.mainVariable);
    }

    public void setVariableNamePath(String variablePath)
    {
        for( MainVariableType mainVariableType : StreamEx.ofKeys( varTypeDescription, variablePath::equals ) )
        {
            MainVariableType oldValue = this.mainVariable;
            this.mainVariable = mainVariableType;
            firePropertyChange("mainVariable", oldValue, mainVariableType);
        }
    }

    
    @PropertyName("Conversion factor")
    @PropertyDescription("Conversion factor.")
    public String getConversionFactor()
    {
        return conversionFactor;
    }

    public void setConversionFactor(String conversionFactor)
    {
        this.conversionFactor = conversionFactor;
    }
}
