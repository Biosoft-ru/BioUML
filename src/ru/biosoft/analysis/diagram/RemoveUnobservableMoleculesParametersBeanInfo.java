package ru.biosoft.analysis.diagram;

import java.util.Optional;

import one.util.streamex.StreamEx;

import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub;

public class RemoveUnobservableMoleculesParametersBeanInfo extends BeanInfoEx2<RemoveUnobservableMoleculesParameters>
{

    public RemoveUnobservableMoleculesParametersBeanInfo()
    {
        super( RemoveUnobservableMoleculesParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        property( "inputPath" ).inputElement( Diagram.class ).add();
        addExpert( "deleteAllElements" );
        property( "elementNames" ).canBeNull().editor( EquationEditor.class ).simple().hideChildren().add();
        property( "outputPath" ).outputElement( Diagram.class ).auto( "$inputPath$(changed)" ).add();
    }

    public static class EquationEditor extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            return Optional.ofNullable( ( (RemoveUnobservableMoleculesParameters)getBean() ).getInputPath() )
                    .map( path -> path.optDataElement( Diagram.class ) ).map( d -> d.getRole( EModel.class ).getEquations() )
                    .map( this::getNames ).orElse( new String[] {} );
        }
        private String[] getNames(StreamEx<Equation> equations)
        {
            return equations.remove( this::shouldSkip )
                    .map( eq -> eq.getDiagramElement().getName() + " (variable:" + eq.getVariable() + ")" ).toArray( String[]::new );
        }
        private boolean shouldSkip(Equation equation)
        {
            if( Equation.TYPE_INITIAL_ASSIGNMENT.equals( equation.getType() ) )
                return true;
            DiagramElement de = equation.getDiagramElement();
            if( de instanceof Edge )
                return true;
            Base kernel = de.getKernel();
            if( kernel == null || kernel instanceof Reaction )
                return true;
            if( kernel instanceof Stub && ( "reaction".equals( kernel.getType() ) || "unknown".equals( kernel.getType() ) ) )
                return true;
            return false;
        }
    }
}
