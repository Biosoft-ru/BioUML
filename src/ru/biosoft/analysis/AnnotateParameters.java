package ru.biosoft.analysis;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;

import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.journal.ProjectUtils;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.PropertyInfo;
import ru.biosoft.util.TextUtil2;

@SuppressWarnings ( "serial" )
public class AnnotateParameters extends AbstractAnalysisParameters implements PropertyChangeListener
{
    private DataElementPath inputTablePath;
    private DataElementPath annotationCollectionPath;
    private PropertyInfo[] annotationColumns;
    private Species species;
    private boolean replaceDuplicates = true;
    private DataElementPath outputTablePath;

    public Species getSpecies()
    {
        return species;
    }

    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange("species", oldValue, species);
    }

    public AnnotateParameters()
    {
        addPropertyChangeListener(event -> {
            if( event.getPropertyName().equals("annotationCollection") )
            {
                setAnnotationColumns(new PropertyInfo[0]);
            }
            else if( event.getPropertyName().equals("annotationColumns") )
            {
                firePropertyChange("*", null, null);
            }
        });
        setSpecies(Species.getDefaultSpecies(null));
    }

    public void setAnnotationCollectionPath(DataElementPath collection)
    {
        Object oldValue = this.annotationCollectionPath;
        this.annotationCollectionPath = collection;
        firePropertyChange("annotationCollectionPath", oldValue, this.annotationCollectionPath);
    }

    public DataElementPath getAnnotationCollectionPath()
    {
        return annotationCollectionPath;
    }

    public DataCollection getAnnotationCollection()
    {
        return ( annotationCollectionPath == null ) ? null : annotationCollectionPath.optDataCollection();
    }

    public void setInputTablePath(DataElementPath collection)
    {
        Object oldValue = this.inputTablePath;
        this.inputTablePath = collection;
        firePropertyChange("inputTablePath", oldValue, this.inputTablePath);
        if ( collection != null )
            setSpecies(Species.getDefaultSpecies(collection.optDataCollection()));
    }
    
    public DataElementPath getDefaultAnnotationPath()
    {
        if(species == null)
            return DataElementPath.EMPTY_PATH;
        DataElementPath path = ProjectUtils.getPreferredDatabasePath( "Ensembl (" + species.getLatinName() + ")",
                ProjectUtils.getDestinationProjectPath( getOutputTablePath() ) );
        if(path == null)
        {
            path = DataElementPath.create(species.getAttributes().getValueAsString("ensemblPath"));
        }
        if(path == null)
            return DataElementPath.EMPTY_PATH;
        return path.getChildPath( "Data", "gene" );
    }

    public DataElementPath getInputTablePath()
    {
        return inputTablePath;
    }
    
    public DataElementPath getOutputTablePath()
    {
        return outputTablePath;
    }

    public void setOutputTablePath(DataElementPath path)
    {
        DataElementPath oldValue = outputTablePath;
        this.outputTablePath = path;
        firePropertyChange( "outputTablePath", oldValue, outputTablePath );
    }

    public String getOutputIcon()
    {
        return IconFactory.getIconId(getInputTablePath());
    }

    public TableDataCollection getInputTable()
    {
        return ( inputTablePath == null || ! ( inputTablePath.optDataElement() instanceof TableDataCollection ) ) ? null
                : (TableDataCollection)inputTablePath.optDataElement();
    }

    public PropertyInfo[] getAnnotationColumns()
    {
        return annotationColumns;
    }

    public void setAnnotationColumns(PropertyInfo[] group)
    {
        PropertyInfo[] oldVal = annotationColumns;
        annotationColumns = group;
        firePropertyChange("annotationColumns", oldVal, group);
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"inputTablePath"};
    }

    public String[] getAnnotationColumnKeys()
    {
        if( annotationColumns == null )
            return null;
        return StreamEx.of(annotationColumns).map( PropertyInfo::getName ).toArray( String[]::new );
    }

    public void setAnnotationColumnKeys(String[] columns)
    {
        if( columns == null )
        {            
            return;
        }
        Set<String> names = new HashSet<>(Arrays.asList(columns));
        DataCollection<?> dataCollection = getAnnotationCollection();
        if( dataCollection == null )
            return;
        DataElement dataElement = null;
        try
        {
            dataElement = dataCollection.iterator().next();
        }
        catch( Exception e )
        {
        }
        if( dataElement == null )
            return;
        PropertyInfo[] annotationColumns = StreamEx.of( BeanUtil.getRecursivePropertiesList( dataElement ) )
                .filter( info -> names.contains( info.getName() ) ).toArray( PropertyInfo[]::new );
        setAnnotationColumns( annotationColumns );
    }

    @Override
    public void read(Properties properties, String prefix)
    {
        super.read(properties, prefix);
        String annotationColumnsStr = properties.getProperty(prefix + "annotationColumns");
        if( annotationColumnsStr != null )
        {
            setAnnotationColumnKeys( TextUtil2.split( annotationColumnsStr, ';' ) );
        }
    }

    @Override
    public void write(Properties properties, String prefix)
    {
        super.write(properties, prefix);
        if( annotationColumns != null )
        {
            properties.put(prefix + "annotationColumns", String.join(";", getAnnotationColumnKeys()));
        }
    }

    /**
     * @return the replaceDuplicates
     */
    public boolean isReplaceDuplicates()
    {
        return replaceDuplicates;
    }

    /**
     * @param replaceDuplicates
     *            the replaceDuplicates to set
     */
    public void setReplaceDuplicates(boolean replaceDuplicates)
    {
        Object oldValue = this.replaceDuplicates;
        this.replaceDuplicates = replaceDuplicates;
        firePropertyChange("replaceDuplicates", oldValue, replaceDuplicates);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        this.firePropertyChange( evt );
    }
}
