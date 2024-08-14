package biouml.plugins.physicell;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.physicell.core.CellFunctions;
import ru.biosoft.physicell.core.CellFunctions.Function;

public class FunctionsProperties extends Option
{
    private CellFunctions functions = new CellFunctions();

    private String customRule;
    private String phenotypeUpdate;
    private String velocityUpdate;
    private String membraneInteraction;
    private String membraneDistance;
    private String volumeUpdate;
    private String migrationUpdate;
    private String contact;
    private String orientation;
    private String instantiate = PhysicellConstants.NOT_SELECTED;

    private DataElementPath customRuleCustom;
    private DataElementPath phenotypeUpdateCustom;
    private DataElementPath velocityUpdateCustom;
    private DataElementPath membraneInteractionCustom;
    private DataElementPath membraneDistanceCustom;
    private DataElementPath volumeUpdateCustom;
    private DataElementPath migrationUpdateCustom;
    private DataElementPath contactCustom;
    private DataElementPath orientationCustom;
    private DataElementPath instantiateCustom;

    public FunctionsProperties()
    {

    }

    public FunctionsProperties(CellFunctions functions)
    {
        this.functions = functions.clone();
        this.customRule = getName( functions.customCellRule );
        this.phenotypeUpdate = getName( functions.updatePhenotype );
        this.velocityUpdate = getName( functions.updateVelocity );
        this.membraneInteraction = getName( functions.membraneInteraction );
        this.membraneDistance = getName( functions.membraneDistanceCalculator );
        this.volumeUpdate = getName( functions.updateVolume );
        this.migrationUpdate = getName( functions.updateMigration );
        this.contact = getName( functions.contact );
        this.orientation = getName( functions.set_orientation );
        this.instantiate = getName( functions.customCellRule );
    }

    private String getName(Function f)
    {
        if( f == null )
            return PhysicellConstants.NOT_SELECTED;
        else
            return f.getName();
    }

    @PropertyName ( "Volume update" )
    public String getVolumeUpdate()
    {
        return volumeUpdate;
    }
    public void setVolumeUpdate(String volumeUpdate)
    {
        Object oldValue = this.volumeUpdate;
        this.volumeUpdate = volumeUpdate;
        firePropertyChange( "volumeUpdate", oldValue, volumeUpdate );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Migration update" )
    public String getMigrationUpdate()
    {
        return migrationUpdate;
    }
    public void setMigrationUpdate(String migrationUpdate)
    {
        Object oldValue = this.migrationUpdate;
        this.migrationUpdate = migrationUpdate;
        firePropertyChange( "migrationUpdate", oldValue, migrationUpdate );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Custom rule" )
    public String getCustomRule()
    {
        return customRule;
    }
    public void setCustomRule(String customRule)
    {
        Object oldValue = this.customRule;
        this.customRule = customRule;
        firePropertyChange( "customRule", oldValue, customRule );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Phenotype update" )
    public String getPhenotypeUpdate()
    {
        return phenotypeUpdate;
    }
    public void setPhenotypeUpdate(String phenotypeUpdate)
    {
        Object oldValue = this.phenotypeUpdate;
        this.phenotypeUpdate = phenotypeUpdate;
        firePropertyChange( "phenotypeUpdate", oldValue, phenotypeUpdate );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Velocity update" )
    public String getVelocityUpdate()
    {
        return velocityUpdate;
    }
    public void setVelocityUpdate(String velocityUpdate)
    {
        Object oldValue = this.velocityUpdate;
        this.velocityUpdate = velocityUpdate;
        firePropertyChange( "velocityUpdate", oldValue, velocityUpdate );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Membrane interaction" )
    public String getMembraneInteraction()
    {
        return membraneInteraction;
    }
    public void setMembraneInteraction(String membraneInteraction)
    {
        Object oldValue = this.membraneInteraction;
        this.membraneInteraction = membraneInteraction;
        firePropertyChange( "membraneInteraction", oldValue, membraneInteraction );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Contact" )
    public String getContact()
    {
        return contact;
    }
    public void setContact(String contact)
    {
        Object oldValue = this.contact;
        this.contact = contact;
        firePropertyChange( "contact", oldValue, contact );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Membrane distance calculator" )
    public String getMembraneDistance()
    {
        return membraneDistance;
    }
    public void setMembraneDistance(String membraneDistance)
    {
        Object oldValue = this.membraneDistance;
        this.membraneDistance = membraneDistance;
        firePropertyChange( "membraneDistance", oldValue, membraneDistance );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Orientation" )
    public String getOrientation()
    {
        return orientation;
    }
    public void setOrientation(String orientation)
    {
        Object oldValue = this.orientation;
        this.orientation = orientation;
        firePropertyChange( "orientation", oldValue, orientation );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Cell creation" )
    public String getInstantiate()
    {
        return instantiate;
    }
    public void setInstantiate(String instantiate)
    {
        Object oldValue = this.instantiate;
        this.instantiate = instantiate;
        firePropertyChange( "instantiate", oldValue, instantiate );
        firePropertyChange( "*", null, null );
    }

    public CellFunctions getFunctions()
    {
        return functions;
    }
    public void setFunctions(CellFunctions functions)
    {
        Object oldValue = this.functions;
        this.functions = functions;
        firePropertyChange( "functions", oldValue, functions );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Custom Custom rule" )
    public DataElementPath getCustomRuleCustom()
    {
        return customRuleCustom;
    }
    public void setCustomRuleCustom(DataElementPath customRuleCustom)
    {
        Object oldValue = this.customRuleCustom;
        this.customRuleCustom = customRuleCustom;
        firePropertyChange( "customRuleCustom", oldValue, customRuleCustom );
    }

    @PropertyName ( "Custom Phenotype update" )
    public DataElementPath getPhenotypeUpdateCustom()
    {
        return phenotypeUpdateCustom;
    }
    public void setPhenotypeUpdateCustom(DataElementPath phenotypeUpdateCustom)
    {
        Object oldValue = this.phenotypeUpdateCustom;
        this.phenotypeUpdateCustom = phenotypeUpdateCustom;
        firePropertyChange( "phenotypeUpdateCustom", oldValue, phenotypeUpdateCustom );
    }

    @PropertyName ( "Custom Velocity update" )
    public DataElementPath getVelocityUpdateCustom()
    {
        return velocityUpdateCustom;
    }
    public void setVelocityUpdateCustom(DataElementPath velocityUpdateCustom)
    {
        Object oldValue = this.velocityUpdateCustom;
        this.velocityUpdateCustom = velocityUpdateCustom;
        firePropertyChange( "velocityUpdateCustom", oldValue, velocityUpdateCustom );
    }

    @PropertyName ( "Custom Membrane interaction" )
    public DataElementPath getMembraneInteractionCustom()
    {
        return membraneInteractionCustom;
    }
    public void setMembraneInteractionCustom(DataElementPath membraneInteractionCustom)
    {
        Object oldValue = this.membraneInteractionCustom;
        this.membraneInteractionCustom = membraneInteractionCustom;
        firePropertyChange( "membraneInteractionCustom", oldValue, membraneInteractionCustom );
    }

    @PropertyName ( "Custom Membrane distance" )
    public DataElementPath getMembraneDistanceCustom()
    {
        return membraneDistanceCustom;
    }
    public void setMembraneDistanceCustom(DataElementPath membraneDistanceCustom)
    {
        Object oldValue = this.membraneDistanceCustom;
        this.membraneDistanceCustom = membraneDistanceCustom;
        firePropertyChange( "membraneDistanceCustom", oldValue, membraneDistanceCustom );
    }

    @PropertyName ( "Custom Volume update" )
    public DataElementPath getVolumeUpdateCustom()
    {
        return volumeUpdateCustom;
    }
    public void setVolumeUpdateCustom(DataElementPath volumeUpdateCustom)
    {
        Object oldValue = this.volumeUpdateCustom;
        this.volumeUpdateCustom = volumeUpdateCustom;
        firePropertyChange( "volumeUpdateCustom", oldValue, volumeUpdateCustom );
    }

    @PropertyName ( "Custom Migration update" )
    public DataElementPath getMigrationUpdateCustom()
    {
        return migrationUpdateCustom;
    }
    public void setMigrationUpdateCustom(DataElementPath migrationUpdateCustom)
    {
        Object oldValue = this.migrationUpdateCustom;
        this.migrationUpdateCustom = migrationUpdateCustom;
        firePropertyChange( "migrationUpdateCustom", oldValue, migrationUpdateCustom );
    }

    @PropertyName ( "Custom Contact" )
    public DataElementPath getContactCustom()
    {
        return contactCustom;
    }
    public void setContactCustom(DataElementPath contactCustom)
    {
        Object oldValue = this.contactCustom;
        this.contactCustom = contactCustom;
        firePropertyChange( "contactCustom", oldValue, contactCustom );
    }

    @PropertyName ( "Custom Orientation" )
    public DataElementPath getOrientationCustom()
    {
        return orientationCustom;
    }
    public void setOrientationCustom(DataElementPath orientationCustom)
    {
        Object oldValue = this.orientationCustom;
        this.orientationCustom = orientationCustom;
        firePropertyChange( "orientationCustom", oldValue, orientationCustom );
    }

    @PropertyName ( "Custom Instantiator" )
    public DataElementPath getInstantiateCustom()
    {
        return instantiateCustom;
    }
    public void setInstantiateCustom(DataElementPath instantiateCustom)
    {
        this.instantiateCustom = instantiateCustom;
    }

    public boolean isDefaultPhenotype()
    {
        return !PhysicellConstants.CUSTOM.equals( getPhenotypeUpdate() );
    }

    public boolean isDefaultVolume()
    {
        return !PhysicellConstants.CUSTOM.equals( getVolumeUpdate() );
    }

    public boolean isDefaultVelocity()
    {
        return !PhysicellConstants.CUSTOM.equals( getVelocityUpdate() );
    }

    public boolean isDefaultRule()
    {
        return !PhysicellConstants.CUSTOM.equals( getCustomRule() );
    }

    public boolean isDefaultMBInteraction()
    {
        return !PhysicellConstants.CUSTOM.equals( getMembraneInteraction() );
    }

    public boolean isDefaultMBDistance()
    {
        return !PhysicellConstants.CUSTOM.equals( getMembraneDistance() );
    }

    public boolean isDefaultContact()
    {
        return !PhysicellConstants.CUSTOM.equals( getContact() );
    }

    public boolean isDefaultOrientation()
    {
        return !PhysicellConstants.CUSTOM.equals( getOrientation() );
    }

    public boolean isDefaultMigration()
    {
        return !PhysicellConstants.CUSTOM.equals( getMigrationUpdate() );
    }

    public boolean isDefaultInstantiate()
    {
        return !PhysicellConstants.CUSTOM.equals( this.getInstantiate() );
    }

    public void setCustom(String name, DataElementPath dep)
    {
        switch( name )
        {
            case "custom_cell_rule":
            {
                setCustomRule( PhysicellConstants.CUSTOM );
                setCustomRuleCustom( dep );
                break;
            }
            case "update_migration_bias":
            {
                setMigrationUpdate( PhysicellConstants.CUSTOM );
                setMigrationUpdateCustom( dep );
                break;
            }
            case "update_phenotype":
            {
                setPhenotypeUpdate( PhysicellConstants.CUSTOM );
                setPhenotypeUpdateCustom( dep );
                break;
            }
            case "instantiate_cell":
            {
                setInstantiate( PhysicellConstants.CUSTOM );
                setInstantiateCustom( dep );
                break;
            }
            case "volume_update_function":
            {
                setVolumeUpdate( PhysicellConstants.CUSTOM );
                setVolumeUpdateCustom( dep );
                break;
            }
            case "update_velocity":
            {
                setVelocityUpdate( PhysicellConstants.CUSTOM );
                setVelocityUpdateCustom( dep );
                break;
            }
            case "contact_function":
            {
                setContact( PhysicellConstants.CUSTOM );
                setContactCustom( dep );
                break;
            }
            case "add_cell_basement_membrane_interactions":
            {
                setMembraneInteraction( PhysicellConstants.CUSTOM );
                setMembraneInteractionCustom( dep );
                break;
            }
            case "calculate_distance_to_membrane":
            {
                setMembraneDistance( PhysicellConstants.CUSTOM );
                setMembraneDistanceCustom( dep );
                break;
            }
        }
    }

    public void setNotSelected(String name)
    {
        switch( name )
        {
            case "custom_cell_rule":
            {
                setCustomRule( PhysicellConstants.NOT_SELECTED );
                break;
            }
            case "update_migration_bias":
            {
                setMigrationUpdate( PhysicellConstants.NOT_SELECTED );
                break;
            }
            case "update_phenotype":
            {
                setPhenotypeUpdate( PhysicellConstants.NOT_SELECTED );
                break;
            }
            case "instantiate_cell":
            {
                setInstantiate( PhysicellConstants.NOT_SELECTED );
                break;
            }
            case "volume_update_function":
            {
                setVolumeUpdate( PhysicellConstants.NOT_SELECTED );
                break;
            }
            case "update_velocity":
            {
                setVelocityUpdate( PhysicellConstants.NOT_SELECTED );
                break;
            }
            case "contact_function":
            {
                setContact( PhysicellConstants.NOT_SELECTED );
                break;
            }
            case "add_cell_basement_membrane_interactions":
            {
                setMembraneInteraction( PhysicellConstants.NOT_SELECTED );
                break;
            }
            case "calculate_distance_to_membrane":
            {
                setMembraneDistance( PhysicellConstants.NOT_SELECTED );
                break;
            }
        }
    }
}