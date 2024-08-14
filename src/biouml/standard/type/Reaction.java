package biouml.standard.type;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.dynamics.FormulaDelegate;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.util.TextUtil;

@ClassIcon ( "resources/reaction.gif" )
@PropertyName ( "Reaction" )
public class Reaction extends Referrer implements DataCollection<SpecieReference>, FormulaDelegate
{
    /** Format to generate unique name for new reaction instance. */
    public static final DecimalFormat NAME_FORMAT = new DecimalFormat("R000000");

    /** Primary collection. */
    protected VectorDataCollection<SpecieReference> collection;

    protected KineticLaw kineticLaw;
    protected boolean reversible;

    protected boolean fast;

    public Reaction(DataCollection<?> origin, String name)
    {
        super(origin, name, TYPE_REACTION);
        init(name);
    }

    protected void init(String name)
    {
        String dcName = TextUtil.nullToEmpty(name);
        collection = new VectorDataCollection<>(dcName);
        kineticLaw = new KineticLaw(this);
    }

    public String getSpecieTitle(Integer index, Object obj)
    {
        SpecieReference specie = (SpecieReference)obj;
        return specie.getTitle();
    }

    @PropertyName ( "Species references" )
    @PropertyDescription ( "References to species involved into the reaction, its role and stoichiometry." )
    public @Nonnull SpecieReference[] getSpecieReferences()
    {
        return collection.toArray(new SpecieReference[collection.getSize()]);
    }
    /** Should be used only to load initial set of specie references. */
    public void setSpecieReferences(SpecieReference[] species)
    {
        SpecieReference[] oldValue = getSpecieReferences();
        Set<String> oldNames = collection.names().collect(Collectors.toSet());
        for( SpecieReference sr : species )
        {
            collection.put(sr);
            oldNames.remove(sr.getName());
        }
        for( String oldName : oldNames )
        {
            try
            {
                collection.remove(oldName);
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
        }

        firePropertyChange("specieReferences", oldValue, getSpecieReferences());
    }

    @PropertyName ( "Kinetic law" )
    @PropertyDescription ( "Kinetic law describes the reaction kinetics." )
    public KineticLaw getKineticLaw()
    {
        return kineticLaw;
    }
    public void setKineticLaw(KineticLaw kineticLaw)
    {
        KineticLaw oldValue = this.kineticLaw;
        this.kineticLaw = kineticLaw;
        kineticLaw.setParent(this);
        firePropertyChange("kineticLaw", oldValue, kineticLaw);
    }

    @PropertyName ( "Reversible" )
    @PropertyDescription ( "Indicates whether the reaction is reversible." )
    public boolean isReversible()
    {
        return reversible;
    }
    public void setReversible(boolean reversible)
    {
        boolean oldValue = this.reversible;
        this.reversible = reversible;
        firePropertyChange("reversible", oldValue, reversible);
    }

    @PropertyName ( "Fast" )
    @PropertyDescription ( "Indicates whether the reaction is fast." )
    public boolean isFast()
    {
        return fast;
    }
    public void setFast(boolean fast)
    {
        boolean oldValue = this.fast;
        this.fast = fast;
        firePropertyChange("fast", oldValue, fast);
    }

    @Override
    public String getFormula()
    {
        return kineticLaw.getFormula();
    }

    @Override
    public void setFormula(String formula)
    {
        kineticLaw.setFormula(formula);
    }

    // ///////////////////////////////////////////////////////////////
    // DataCollection interface implementation through delegation
    //
    @Override
    public DataCollectionInfo getInfo()
    {
        return collection.getInfo();
    }

    @Override
    public int getSize()
    {
        return collection.getSize();
    }

    @Override
    public boolean isEmpty()
    {
        return getSize() == 0;
    }

    @Override
    public @Nonnull Class<Base> getDataElementType()
    {
        return Base.class;
    }

    @Override
    public boolean isMutable()
    {
        return collection.isMutable();
    }

    @Override
    public boolean contains(String name)
    {
        return collection.contains(name);
    }

    @Override
    public boolean contains(DataElement de)
    {
        return contains(de.getName());
    }

    @Override
    public @Nonnull Iterator<SpecieReference> iterator()
    {
        return collection.iterator();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        return collection.getNameList();
    }

    @Override
    public SpecieReference get(String name) throws Exception
    {
        return collection.get(name);
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        return collection.getDescriptor(name);
    }

    @Override
    public SpecieReference put(SpecieReference obj) throws DataElementPutException
    {
        SpecieReference oldRef = collection.get(obj.getName());
        setSpecieReferences(Stream.concat(Stream.of(obj), collection.stream().filter(sr -> !sr.getName().equals(obj.getName())))
                .toArray(SpecieReference[]::new));
        return oldRef;
    }

    @Override
    public void remove(String name) throws Exception
    {
        setSpecieReferences(collection.stream().filter(sr -> !sr.getName().equals(name)).toArray(SpecieReference[]::new));
    }

    @Override
    public void addDataCollectionListener(DataCollectionListener l)
    {
        collection.addDataCollectionListener(l);
    }

    @Override
    public void removeDataCollectionListener(DataCollectionListener l)
    {
        collection.removeDataCollectionListener(l);
    }

    private DataElementPath completeName = null;
    @Override
    public @Nonnull DataElementPath getCompletePath()
    {
        if( completeName == null )
        {
            DataCollection<?> origin = getOrigin();
            completeName = ( origin == null ? DataElementPath.EMPTY_PATH : origin.getCompletePath() ).getChildPath(getName());
        }
        return completeName;
    }

    @Override
    public void close() throws Exception
    {
    }

    public void init()
    {
    }

    @Override
    public void release(String dataElementName)
    {
    }

    @Override
    public DataElement getFromCache(String dataElementName)
    {
        return null;
    }

    @Override
    public boolean isPropagationEnabled()
    {
        return collection.isPropagationEnabled();
    }

    @Override
    public void setPropagationEnabled(boolean propagationEnabled)
    {
        collection.setPropagationEnabled(propagationEnabled);
    }

    @Override
    public void propagateElementWillChange(DataCollection<?> source, DataCollectionEvent primaryEvent)
    {
        collection.propagateElementWillChange(source, primaryEvent);
    }

    @Override
    public void propagateElementChanged(DataCollection<?> source, DataCollectionEvent primaryEvent)
    {
        collection.propagateElementChanged(source, primaryEvent);
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        return SpecieReference.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public void reinitialize() throws LoggedException
    {
    }

    @Override
    public Reaction clone(DataCollection<?> newOrigin, String newName)
    {
        Reaction clone = (Reaction)super.clone(newOrigin, newName);
        clone.listenerList = null;
        clone.collection = new VectorDataCollection<>(collection.getName());
        collection.stream().map(sr -> new SpecieReference(clone.collection, sr.getName())).forEach(clone.collection::put);
        clone.kineticLaw = new KineticLaw(clone, kineticLaw);
        return clone;
    }

    public StreamEx<SpecieReference> stream()
    {
        if( this.isEmpty() )
            return StreamEx.empty();

        return StreamEx.of(names()).map(name -> {
            try
            {
                return get(name);
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
        });
    }

}
