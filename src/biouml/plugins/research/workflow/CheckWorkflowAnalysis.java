package biouml.plugins.research.workflow;

import java.beans.PropertyEditor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.developmentontheedge.beans.DPSProperties;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.PropertyEditorEx;
import com.developmentontheedge.beans.editors.TagEditorSupport;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.plugins.research.workflow.RunWorkflowAnalysis.RunWorkflowParameters;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.standard.type.Type;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CheckWorkflowAnalysis extends AnalysisMethodSupport<CheckWorkflowAnalysis.CheckWorkflowParameters>
{
    protected StringBuffer errors = null;
    private boolean hadErrors = false;
    public CheckWorkflowAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new CheckWorkflowParameters() );
        errors = new StringBuffer();
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        DataElementPath path = getParameters().getInputPath();
        DataElement de = path.getDataElement();
        if( ! ( de instanceof FolderCollection ) && ! ( de instanceof Diagram ) )
            throw new ParameterNotAcceptableException( "Workflow Path", de.getName() );
        if( de instanceof Diagram && ! ( ( (Diagram)de ).getType() instanceof WorkflowDiagramType ) )
            throw new IllegalArgumentException( "Input diagram is not workflow." );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath path = getParameters().getInputPath();
        DataElement de = path.getDataElement();
        checkRecursive( de );
        return new Object[] {};
    }

    private void checkRecursive(DataElement de)
    {
        if( de == null )
            return;
        if( de instanceof Diagram )
        {
            checkWorkflow( (Diagram)de );
        }
        else if( de instanceof FolderCollection )
        {
            jobControl.forCollection( ( (FolderCollection)de ).getNameList(), element -> {
                try
                {
                    checkRecursive( ( (FolderCollection)de ).get( element ) );

                }
                catch( Throwable t )
                {
                    throw ExceptionRegistry.translateException( t );
                }
                return true;
            } );
        }
    }

    private void checkWorkflow(Diagram workflow)
    {
        log.info( "Checking workflow " + workflow.getCompletePath() + "..." );
        jobControl.setPreparedness( 5 );
        jobControl.pushProgress( 5, 95 );
        hadErrors = false;
        checkElements( workflow );
        if( !hadErrors )
            log.info( "Workflow is correct" );
        jobControl.setPreparedness( 95 );
        log.info( "Done" );
    }

    protected void addError(String error)
    {
        errors.append( "* " ).append( error ).append( '\n' );
    }

    protected void checkElements(Compartment compartment)
    {
        jobControl.forCollection( compartment.getNameList(), element -> {
            try
            {
                DiagramElement de = compartment.get( element );
                if( de.getKernel().getType().equals( Type.ANALYSIS_METHOD ) )
                {
                    boolean analysisError = checkAnalysis( (Node)de );
                    if( analysisError )
                    {
                        log.info( errors.toString() );
                        errors.delete( 0, errors.length() );
                        hadErrors = true;
                    }
                }
                else if( de.getKernel().getType().equals( Type.ANALYSIS_CYCLE ) )
                {
                    checkElements( (Compartment)de );
                }
            }
            catch( Throwable t )
            {
                throw ExceptionRegistry.translateException( t );
            }
            return true;
        } );
    }

    private boolean checkAnalysis(Node de)
    {
        String analysisName = de.getAttributes().getValueAsString( AnalysisDPSUtils.PARAMETER_ANALYSIS_FULLNAME );
        if( analysisName == null )
        {
            addError( "Can not find analysis name in node '" + de.getCompleteNameInDiagram() + "'" );
            return true;
        }
        AnalysisMethod analysisMethod = AnalysisDPSUtils.getAnalysisMethodByNode( de.getAttributes() );
        if( analysisMethod == null )
        {
            addError( "Can not find analysis '" + analysisName + "' referred by node '" + de.getCompleteNameInDiagram() + "'" );
            return true;
        }

        AnalysisParameters oldParameters = WorkflowEngine.getAnalysisParametersByNode( de );
        if( oldParameters == null )
        {
            addError( "Workflow node '" + de.getCompleteNameInDiagram() + "' refers to unknown analysis '" + analysisName + "'" );

        }
        ComponentModel model = ComponentFactory.getModel( oldParameters );
        Set<String> exclude = new HashSet<>();
        exclude.addAll( Arrays.asList( oldParameters.getInputNames() ) );
        exclude.addAll( Arrays.asList( oldParameters.getOutputNames() ) );

        StringBuffer errors = new StringBuffer();
        Properties oldProperties = new DPSProperties( de.getAttributes() );
        String prefix = DPSUtils.PARAMETER_ANALYSIS_PARAMETER + ".";

        for( int i = 0; i < model.getPropertyCount(); i++ )
        {
            Property property = model.getPropertyAt( i );
            if( !property.isVisible( Property.SHOW_EXPERT ) )
                continue;
            String name = property.getName();
            if( exclude.contains( name ) )
                continue;

            if( !oldProperties.containsKey( prefix + name ) )
            {
                errors.append( "\nNew property or property that was undefined: '" + name + "'" );
                continue;
            }

            String valueStr = oldProperties.getProperty( prefix + name );

            Object value = property.getValue();
            if( value == null && valueStr != null )
            {
                errors.append( "\nNull value from string '" + valueStr + "' in property '" + name + "'" );
                continue;
            }

            String[] tags = null;
            Object editorObj = null;
            try
            {
                editorObj = property.getPropertyEditorClass().newInstance();

                if( editorObj != null )
                {

                    if( editorObj instanceof PropertyEditorEx )
                    {
                        PropertyEditorEx editor = (PropertyEditorEx)editorObj;
                        Object owner = property.getOwner();
                        if( owner instanceof Property.PropWrapper )
                            owner = ( (Property.PropWrapper)owner ).getOwner();
                        editor.setValue( property.getValue() );
                        editor.setBean( owner );
                        editor.setDescriptor( property.getDescriptor() );
                        tags = editor.getTags();
                    }
                    else
                    {
                        tags = ( (PropertyEditor)editorObj ).getTags();
                    }
                }
            }
            catch( Exception e )
            {
            }
            if( tags != null && tagsNotEmpty( tags ) )
            {
                if( editorObj instanceof TagEditorSupport && value instanceof Integer )
                {
                    Integer numValue = (Integer)value;
                    if( numValue < 0 || numValue >= tags.length )
                    {
                        errors.append( "\nValue '" + valueStr + "' is not in available selector range for property '" + name + "'" );
                        continue;
                    }
                }
                else
                {
                    boolean found = false;
                    Set<String> tagSet = new HashSet<>( Arrays.asList( tags ) );
                    if( value.getClass().isArray() )
                        found = StreamEx.of( (Object[])value ).allMatch( o -> tagSet.contains( o.toString() ) );
                    else
                        found = tagSet.contains( value.toString() );
                    if( !found )
                    {
                        errors.append( "\nValue '" + valueStr + "' is not in available values for property '" + name + "'" );
                        continue;
                    }
                }
            }
        }

        for( Object nameObj : oldProperties.keySet() )
        {
            String nameStr = nameObj.toString();
            if( !nameStr.startsWith( prefix ) )
                continue;
            String name = nameStr.substring( prefix.length() );
            if( exclude.contains( name ) )
                continue;
            Property curProperty = model.findProperty( name );
            //Some analyses manually write subproperties with additional prefix
            //TODO: move comparison to AnalysisMethodSupport and override for complex parameters in corresponding analyses
            if( curProperty == null && !name.contains( "." ) )
            {
                errors.append( "\nCan not find property '" + name + "'" );
                continue;
            }
        }

        if( analysisMethod instanceof RunWorkflowAnalysis )
        {
            DataElementPath workflowPath = ( (RunWorkflowParameters)oldParameters ).getWorkflowPath();
            if( workflowPath == null )
                errors.append( "\nWorkflow path is null" );
            else
            {
                Diagram subWorkflow = workflowPath.optDataElement( Diagram.class );
                if( subWorkflow == null )
                    errors.append( "\nWorkflow diagram not found for path '" + workflowPath.getName() + "'" );
                else
                    checkWorkflow( subWorkflow );
            }
        }

        boolean hadErrors = false;
        if( errors.length() > 0 )
        {
            addError( "Changed properties of analysis '" + analysisName + "' referred by node '" + de.getCompleteNameInDiagram() + "':"
                    + errors.toString() );
            hadErrors = true;
        }

        return hadErrors;
    }

    private boolean tagsNotEmpty(String[] tags)
    {
        if( tags.length == 0 )
            return false;
        if( tags.length == 1 && tags[0].equals( ColumnNameSelector.NONE_COLUMN ) )
            return false;
        return true;
    }

    @SuppressWarnings ( "serial" )
    public static class CheckWorkflowParameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputPath;

        @PropertyName ( "Workflow Path" )
        @PropertyDescription ( "Path to workflow or folder containing workflows" )
        public DataElementPath getInputPath()
        {
            return inputPath;
        }

        public void setInputPath(DataElementPath inputPath)
        {
            Object oldValue = this.inputPath;
            this.inputPath = inputPath;
            firePropertyChange( "this.inputPath", oldValue, inputPath );
        }
    }
    public static class CheckWorkflowParametersBeanInfo extends BeanInfoEx2<CheckWorkflowParameters>
    {
        public CheckWorkflowParametersBeanInfo()
        {
            super( CheckWorkflowParameters.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            property( "inputPath" ).inputElement( ru.biosoft.access.core.DataElement.class ).add();
        }
    }

}
