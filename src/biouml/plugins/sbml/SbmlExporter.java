package biouml.plugins.sbml;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

import biouml.model.Diagram;
import biouml.model.DiagramContainer;
import biouml.model.DiagramExporter;
import biouml.model.DiagramType;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnCompositeDiagramType;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.SbgnDiagramTypeConverter;
import biouml.plugins.sbml.converters.SBGNConverterNew;
import biouml.plugins.sbml.converters.SbmlConverter;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.MathDiagramType;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.state.State;

/**
 * Exporter to SBML format
 */
public class SbmlExporter extends DiagramExporter
{
    protected SbmlExportProperties properties = null;
    protected Map<String, String> newPaths;
    
    @Override
    public boolean accept(Diagram diagram)
    {
        //SbmlExporter can export SBML, SBGN or PathwaySimulation diagrams
        DiagramType diagramType = diagram.getType();
        return diagramType instanceof SbmlDiagramType || diagramType instanceof SbgnDiagramType
                || diagramType instanceof PathwaySimulationDiagramType || diagramType instanceof CompositeDiagramType;
    }
    
    @Override
    public void doExport(@Nonnull Diagram diagram, @Nonnull File file) throws Exception
    {
        DiagramType diagramType = diagram.getType();
        if( properties != null )
        {
            String stName = properties.getCurrentState();
            if( !stName.equals(SbmlExportProperties.NO_STATE) )
            {
                State st = diagram.getState( stName );
                if(st != null)
                    diagram.setStateEditingMode(st);
            }
        }
        Diagram diagramToExport = diagram;

        //convert Math to SBGN
        if (MathDiagramType.class.isAssignableFrom( diagramType.getClass()) ||CompositeDiagramType.class.isAssignableFrom( diagramType.getClass()) )
        {
            diagramToExport = new SbgnDiagramTypeConverter().convert( diagramToExport, SbgnCompositeDiagramType.class );
            diagramType = diagramToExport.getType();
        }
        
        //TODO: refactor
        if ( diagramType instanceof SbgnCompositeDiagramType)
        {
            if( properties != null )
            {
                if( properties.transformToPlain )
                {
                    diagramToExport = new CompositeModelPreprocessor().preprocess(diagramToExport, null, diagramToExport.getName());
                }
                else if( properties.inlineModels )
                {
                    diagramToExport = diagramToExport.clone(diagramToExport.getOrigin(), diagramToExport.getName());
                    SbmlUtil.inlineModelDefinitions(diagramToExport);
                }
            }

            //            diagramToExport.removeAllStates();

            //Convert SBGN to SBML
            Diagram sbmlDiagram = SBGNConverterNew.restore(diagramToExport);

            //set SBGN diagram as dynamic property
            if( properties != null && properties.isSaveBioUMLAnnotation() )
                sbmlDiagram.getAttributes()
                        .add(new DynamicProperty(SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME, Diagram.class, diagramToExport));
            else
                removeSBGN(sbmlDiagram);

            SbmlModelWriter writer = SbmlModelFactory.getWriter( sbmlDiagram );
            writer.setNewPaths( newPaths );
            writer.setWriteBioUMLAnnotation(properties.isSaveBioUMLAnnotation());
            SbmlModelFactory.writeDiagram(file, sbmlDiagram, writer);
            return;
        }

        Class<? extends SbmlDiagramType> type = properties != null?  properties.getDiagramType(): SbmlDiagramType_L3v2.class;
        
        if( properties != null && properties.isSaveBioUMLAnnotation() )
        {
            if( diagramType instanceof SbgnDiagramType )
            {
                diagramToExport = new SBGNConverterNew().restoreSBML(diagramToExport, type.newInstance());
                diagramToExport.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME, Diagram.class, diagram));
            }
            else if( diagramType instanceof SbmlDiagramType )
            {
                   SBGNConverterNew converter = new SBGNConverterNew();
                   Diagram sbgnDiagram = converter.convert(diagramToExport, null);
                   converter.appendPorts(diagram, sbgnDiagram);
                   diagramToExport.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME, Diagram.class, sbgnDiagram));
            }
            else if( ! ( diagramType.getClass().equals( type ) ) )
            {
                diagramToExport = diagram.clone(diagram.getOrigin(), diagram.getName());
                //states are not supported in SBML, remove all states
                diagramToExport.removeAllStates();
                diagramToExport = new SbmlConverter().convert(diagramToExport, type);
            }
        }
        else
        {
            if (diagram.getType() instanceof SbmlDiagramType)
            {
                diagramToExport = diagramType.getClass().equals( type ) ? diagram : new SbmlConverter().convert( diagram, type );
            }
            else if (diagram.getType() instanceof SbgnDiagramType)
            {
                diagramToExport = SBGNConverterNew.restore( diagram );
                //                if( !diagramToExport.getType().getClass().equals( type ) )
                //                    diagramToExport = new SbmlConverter().convert( diagramToExport, type );
            }
            else
            {
                diagramToExport = diagram.clone(diagram.getOrigin(), diagram.getName());
                //states are not supported in SBML, remove all states
                diagramToExport.removeAllStates();
                new SbmlConverter().convert(diagramToExport, type);
            }
        }
        SbmlModelWriter writer = SbmlModelFactory.getWriter( diagramToExport );
        writer.setNewPaths( newPaths );
        if( properties != null )
            writer.setWriteBioUMLAnnotation( properties.isSaveBioUMLAnnotation() );
        SbmlModelFactory.writeDiagram( file, diagramToExport, writer );
        diagram.restore();
    }


    protected void removeSBGN(Diagram diagram)
    {
        for( DiagramContainer node : diagram.stream( DiagramContainer.class ) )
        {
            Diagram innerDiagram = node.getDiagram();
            innerDiagram.getAttributes().remove( SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME );
            innerDiagram.removeAllStates();
            removeSBGN(innerDiagram);
        }
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }

    @Override
    public Object getProperties(DataElement de, File file)
    {
        properties = new SbmlExportProperties((Diagram)de);
        return properties;
    }

    @PropertyName("Export properties")
    @PropertyDescription("Export properties.")
    public static class SbmlExportProperties extends Option
    {
        public final static String NO_STATE = "<default>";

        private static final LinkedHashMap<String, Class<? extends SbmlDiagramType>> nameToType = new LinkedHashMap<>();
        static
        {
            nameToType.put( "level 3 version 2", SbmlDiagramType_L3v2.class );
            nameToType.put( "level 3 version 1", SbmlDiagramType_L3v1.class );
            nameToType.put( "level 2 version 4", SbmlDiagramType_L2v4.class );
            nameToType.put( "level 2 version 3", SbmlDiagramType_L2v3.class );
            nameToType.put( "level 2 version 2", SbmlDiagramType_L2v2.class );
            nameToType.put( "level 2 version 1", SbmlDiagramType_L2.class );
        }

        protected boolean transformToPlain = false;
        protected boolean saveBioUMLAnnotation = true;
        protected boolean useSbgnNotation = true;
        protected String[] possibleStates;
        protected String currentState;
        protected String level = "level 3 version 2";
        protected boolean isNotComposite;
        protected boolean inlineModels;

        public SbmlExportProperties(Diagram diagram)
        {
            List<String> states = diagram.getStateNames();
            states.add(NO_STATE);
            possibleStates = states.toArray(new String[states.size()]);
            currentState = possibleStates[0];
            isNotComposite = !DiagramUtility.isComposite( diagram );
        }

        public Object[] getPossibleLevels()
        {
            return nameToType.keySet().toArray();
        }

        public Class<? extends SbmlDiagramType> getDiagramType()
        {
            return nameToType.get( level );
        }
        public String getLevel()
        {
            return level;
        }
        public void setLevel(String level)
        {
            this.level = level;
        }

        @PropertyName("Transform to plain")
        @PropertyDescription("Transform composite model to plain before export.")
        public boolean isTransformToPlain()
        {
            return transformToPlain;
        }
        public void setTransformToPlain(boolean transformToPlain)
        {
            boolean oldValue = this.transformToPlain;
            this.transformToPlain= transformToPlain;
            if (transformToPlain)
                inlineModels = false;
            firePropertyChange("transformToPlain", oldValue, transformToPlain);
        }

        @PropertyName("BioUML annotation")
        @PropertyDescription("Save BioUML annotation.")
        public boolean isSaveBioUMLAnnotation()
        {
            return saveBioUMLAnnotation;
        }
        public void setSaveBioUMLAnnotation(boolean saveBioUMLAnnotation)
        {
            boolean oldValue = this.saveBioUMLAnnotation;
            this.saveBioUMLAnnotation = saveBioUMLAnnotation;
            firePropertyChange("saveBioUMLAnnotation", oldValue, saveBioUMLAnnotation);
        }

        @PropertyName("Inline submodels")
        @PropertyDescription("Include all external submodels as model definitions.")
        public boolean isInlineModels()
        {
            return inlineModels;
        }
        public void setInlineModels(boolean inlineModels)
        {
            boolean oldValue = this.inlineModels;
            this.inlineModels = inlineModels;
            if (inlineModels)
                transformToPlain = false;
            firePropertyChange("transformToPlain", oldValue, transformToPlain);
        }

        public String[] getPossibleStates()
        {
            return possibleStates;
        }

        @PropertyName("State")
        @PropertyDescription("State of diagram to export.")
        public String getCurrentState()
        {
            return currentState;
        }

        public void setCurrentState(String currentState)
        {
            this.currentState = currentState;
        }
        
        public boolean isNotComposite()
        {
           return isNotComposite;
        }
    }

    public static class SbmlExportPropertiesBeanInfo extends BeanInfoEx2<SbmlExportProperties>
    {
        public SbmlExportPropertiesBeanInfo()
        {
            super(SbmlExportProperties.class);
        }
        @Override
        public void initProperties() throws Exception
        {
            add("level", LevelEditor.class);
            add("currentState");
            add("saveBioUMLAnnotation");
            addHidden("transformToPlain", "isNotComposite");
            addHidden("inlineModels", "isNotComposite");
        }
    }

    public static class MethodEditor extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ( (SbmlExportProperties)getBean() ).getPossibleStates();
        }
    }

    public static class LevelEditor extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ( (SbmlExportProperties)getBean() ).getPossibleLevels();
        }
    }

    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }
}
