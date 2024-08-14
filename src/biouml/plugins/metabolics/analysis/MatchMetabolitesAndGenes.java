package biouml.plugins.metabolics.analysis;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.plugins.metabolics.MetabolicsMatcher;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.Substance;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.AbstractTableConverterParameters;
import ru.biosoft.analysis.TableConverterSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

@ClassIcon("resources/ConvertGeneMetabolite.gif")
public class MatchMetabolitesAndGenes extends TableConverterSupport<MatchMetabolitesAndGenes.MatchMetabolitesAndGenesParameters>
{
    public MatchMetabolitesAndGenes(DataCollection<?> origin, String name)
    {
        super( origin, name, new MatchMetabolitesAndGenesParameters() );
    }
    public MatchMetabolitesAndGenes(DataCollection<?> origin, String name, MatchMetabolitesAndGenesParameters parameters)
    {
        super( origin, name, parameters );
    }

    @Override
    public void validateParameters()
    {
        super.validateParameters();
        if( MatchMetabolitesAndGenesParameters.NONE.equals( parameters.getModuleName() ) )
            throw new IllegalArgumentException( "Database name is not selected." );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        jobControl.pushProgress( 0, 5 );
        TableDataCollection source = parameters.getSourceTable().getDataElement( TableDataCollection.class );
        String[] ids = source.names().toArray( String[]::new );

        Module module = parameters.getModule();
        if( module == null )
        {
            log.log(Level.SEVERE,  "Could not find database '" + parameters.getModuleName() + "'. Check that it exists and try again." );
            return null;
        }
        jobControl.popProgress();

        MetabolicsMatcher matcher = new MetabolicsMatcher( parameters.getRoleFilter(), parameters.getInputType(),
                parameters.getOutputType(), jobControl );
        jobControl.pushProgress( 5, 95 );
        Map<String, Set<String>> revReferences = revertReferences( matcher.getReferences( ids, module ) );
        jobControl.popProgress();

        jobControl.pushProgress( 95, 100 );
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputTable() );
        fillTable( source, revReferences, result );

        result.setReferenceType(
                ReferenceTypeRegistry.getElementReferenceType( module.getCategory( parameters.getOutputType() ) ).getDisplayName() );

        if( jobControl.isStopped() )
        {
            parameters.getOutputTable().remove();
            return null;
        }

        CollectionFactoryUtils.save( result );
        jobControl.popProgress();

        return result;
    }

    @Override
    protected String getSourceColumnName()
    {
        return parameters.getSourceColumnPrefix() + " ID";
    }

    @SuppressWarnings ( "serial" )
    @PropertyName ( "Parameters" )
    @PropertyDescription ( "MatchMetabolitesAndGenes parameters" )
    public static class MatchMetabolitesAndGenesParameters extends AbstractTableConverterParameters
    {
        private static final String NONE = "(none)";
        public static final String METABOLITE_TO_GENE = "metabolites to genes";
        public static final String GENE_TO_METABOLITE = "genes to metabolites";

        private DataElementPath sourceTable;
        private String moduleName = NONE;
        private String roleFilter = MetabolicsMatcher.DEFAULT_FILTER;
        private String strategy = GENE_TO_METABOLITE;
        private Class<? extends DataElement> inputType = Protein.class;
        private Class<? extends DataElement> outputType = Substance.class;

        public String getSourceColumnPrefix()
        {
            if( NONE.equals( moduleName ) )
                return "";
            return ReferenceTypeRegistry.getElementReferenceType( getModule().getCategory( inputType ) ).getSource();
        }
        @PropertyName ( "Input table" )
        @PropertyDescription ( "Data set to be converted" )
        public DataElementPath getSourceTable()
        {
            return sourceTable;
        }
        public void setSourceTable(DataElementPath sourceTable)
        {
            Object oldValue = this.sourceTable;
            this.sourceTable = sourceTable;
            firePropertyChange( "sourceTable", oldValue, sourceTable );

            if( sourceTable != null )
            {
                ReferenceType sourceType = ReferenceTypeRegistry.getElementReferenceType( sourceTable.getDataCollection() );
                if( "Proteins".equals( sourceType.getObjectType() ) )
                    setStrategy( GENE_TO_METABOLITE );
                else if( "Substances".equals( sourceType.getObjectType() ) )
                    setStrategy( METABOLITE_TO_GENE );
                String name = ModulesHolder.getInstance()
                        .modules()
                        .findAny( m -> sourceType == ReferenceTypeRegistry.getElementReferenceType( m.getCategory( inputType ) ) )
                        .map( m -> m.getName() )
                        .orElse( getModuleName() );
                setModuleName( name );
            }
        }

        @PropertyName ( "Database name" )
        @PropertyDescription ( "Database from which metabolites should be taken" )
        public String getModuleName()
        {
            return moduleName;
        }
        public void setModuleName(String moduleName)
        {
            Object oldValue = this.moduleName;
            this.moduleName = moduleName;
            firePropertyChange( "moduleName", oldValue, moduleName );
        }

        @PropertyName ( "Include into result" )
        @PropertyDescription ( "Elements of the selected type will be included into result" )
        public String getRoleFilter()
        {
            return roleFilter;
        }
        public void setRoleFilter(String roleFilter)
        {
            Object oldValue = this.roleFilter;
            this.roleFilter = roleFilter;
            firePropertyChange( "roleFilter", oldValue, roleFilter );
        }
        static final String[] availableRoleFilters = new String[] {MetabolicsMatcher.DEFAULT_FILTER,
                MetabolicsMatcher.REACTANT_FILTER, MetabolicsMatcher.PRODUCT_FILTER};

        @PropertyName ( "Matching strategy" )
        @PropertyDescription ( "Matching strategy" )
        public String getStrategy()
        {
            return strategy;
        }
        public void setStrategy(String strategy)
        {
            Object oldValue = this.strategy;
            this.strategy = strategy;
            switch( strategy )
            {
                case METABOLITE_TO_GENE:
                    inputType = Substance.class;
                    outputType = Protein.class;
                    break;
                case GENE_TO_METABOLITE:
                    inputType = Protein.class;
                    outputType = Substance.class;
                    break;
                default:
                    break;
            }
            firePropertyChange( "strategy", oldValue, strategy );
        }
        static final String[] availableStrategies = new String[] {GENE_TO_METABOLITE, METABOLITE_TO_GENE};

        public Module getModule()
        {
            if( NONE.equals( moduleName ) )
                return null;
            return ModulesHolder.getInstance().modules().findAny( m -> moduleName.equals( m.getName() ) ).orElse( null );
        }

        public Class<? extends DataElement> getInputType()
        {
            return inputType;
        }
        public Class<? extends DataElement> getOutputType()
        {
            return outputType;
        }
        public String getSuffix()
        {
            return ( outputType == Protein.class ? "genes (" : "metabolites (" ) + roleFilter + ")";
        }
    }

    public static class MatchMetabolitesAndGenesParametersBeanInfo extends BeanInfoEx2<MatchMetabolitesAndGenesParameters>
    {
        public MatchMetabolitesAndGenesParametersBeanInfo()
        {
            super( MatchMetabolitesAndGenesParameters.class );
        }
        protected MatchMetabolitesAndGenesParametersBeanInfo(Class<? extends MatchMetabolitesAndGenesParameters> clazz,
                String messageBundle)
        {
            super( clazz, messageBundle );
        }

        @Override
        public void initProperties() throws Exception
        {
            property( "sourceTable" ).inputElement( TableDataCollection.class ).add();
            property( "moduleName" ).tags( bean -> ModulesHolder.getInstance().modulesNames() ).add();
            property( "roleFilter" ).tags( MatchMetabolitesAndGenesParameters.availableRoleFilters ).add();
            property( "strategy" ).tags( MatchMetabolitesAndGenesParameters.availableStrategies ).add();
            property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$sourceTable$ $suffix" ).add();
            property( new PropertyDescriptorEx( "suffix", beanClass, "getSuffix", null ) ).hidden().readOnly().add();
        }
    }

    public static class ModulesHolder
    {
        private static ModulesHolder instance;
        public static synchronized ModulesHolder getInstance()
        {
            if( instance == null )
                instance = new ModulesHolder();
            return instance;
        }

        private List<Module> availableModules;
        private ModulesHolder()
        {
            availableModules = CollectionFactoryUtils.getDatabases()
                    .stream()
                    .filter( Module.class::isInstance ).map( m -> (Module)m )
                    .filter( m -> m.getType().isCategorySupported() )
                    .filter( m -> ! ( ModulesHolder.shouldSkip( m ) ) ).collect( Collectors.toList() );
        }
        private static boolean shouldSkip(Module module)
        {
            try
            {
                String path = module.getCompletePath().toString();
                ModuleType type = module.getType();
                String rCat = type.getCategory( Reaction.class );
                DataElementPath rPath = DataElementPath.create( path + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + rCat );
                if( !rPath.exists() )
                    return true;

                String pCat = type.getCategory( Protein.class );
                DataElementPath pPath = DataElementPath.create( path + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + pCat );
                if( !pPath.exists() )
                    return true;

                String sCat = type.getCategory( Substance.class );
                DataElementPath sPath = DataElementPath.create( path + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + sCat );

                return !sPath.exists();
            }
            catch( Throwable t )
            {
                return true;
            }
        }

        public StreamEx<Module> modules()
        {
            return StreamEx.of( availableModules );
        }

        public StreamEx<String> modulesNames()
        {
            return modules()
                    .map( Module::getName )
                    .prepend( MatchMetabolitesAndGenesParameters.NONE );
        }
    }
}
