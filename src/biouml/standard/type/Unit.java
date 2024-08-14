package biouml.standard.type;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

import biouml.model.dynamics.EModel;
import biouml.model.dynamics.util.EModelHelper;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElementSupport;

/**
 * Unit to specify kinetic constant, parameter or variable units.
 * @pending refine the definition according CellML/SBML approach
 */
@PropertyName("Unit")
@PropertyDescription("The unit expressed in base units. Constant, paremeter or varable unit.")
public class Unit extends MutableDataElementSupport implements PropertyChangeListener
{
    public static final String UNDEFINED = "";
    private String title;
    private String comment;
    private BaseUnit[] baseUnits = new BaseUnit[0];
    protected DynamicPropertySet attributes;

    //auto generated formula
    private String formula = null;
    
    public Unit()
    {
        this(null, "new_unit");
    }
    
    public Unit(DataCollection parent, String name)
    {
        super(parent, name);
        this.title = name;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        if (pce.getSource() instanceof BaseUnit)
            resetFormula();
    }

    public void setName(String name)
    {
        if( !name.equals( this.name ) && getParent() != null && getParent() instanceof EModel )
        {
            EModelHelper helper = new EModelHelper((EModel)getParent());
            helper.renameUnit(getName(), name);
        }
    }
    
    @PropertyName("Title")
    @PropertyDescription("The object title (generally it is object brief name).")
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        String oldValue = this.title;
        this.title = title;
        firePropertyChange("title", oldValue, title);
    }

    @PropertyName("Comment")
    @PropertyDescription("Arbitrary text comments.")
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        String oldValue = this.comment;
        this.comment = comment;
        firePropertyChange("comment", oldValue, comment);
    }

    @PropertyName("Base units")
    @PropertyDescription("Base units (type | multiplier | scale | exponent).")
    public BaseUnit[] getBaseUnits()
    {
        return this.baseUnits;
    }
    public void setBaseUnits(BaseUnit[] baseUnits)
    {
    	BaseUnit[] oldBaseUnits = getBaseUnits();
        for( BaseUnit baseUnit : oldBaseUnits )
        {            
                baseUnit.removePropertyChangeListener( this );
                baseUnit.setParent(null);         
        }
        
        if( baseUnits != null )
        {
            for( BaseUnit baseUnit : baseUnits )
            {
                if( baseUnit.getParent() == null )
                {
                    baseUnit.addPropertyChangeListener( this );
                    baseUnit.setParent(this);
                }
            }
        }
        this.baseUnits = baseUnits;
        this.resetFormula();
        firePropertyChange("base_units", oldBaseUnits, baseUnits);
    }
    
    public DynamicPropertySet getAttributes()
    {
        if( attributes == null )
        {
            attributes = new DynamicPropertySetAsMap();
            attributes.addPropertyChangeListener(e -> {
                firePropertyChange(new PropertyChangeEvent(this, "attributes/" + e.getPropertyName(), e.getOldValue(), e.getNewValue()));
            });
        }
        return attributes;
    }

    public Unit clone(DataCollection parent, String name)
    {
        Unit newUnit = new Unit(parent, name);
        newUnit.setTitle(title);
        newUnit.setComment(comment);

        if( baseUnits != null )
        {
            BaseUnit[] newBaseUnits = StreamEx.of(baseUnits).map( baseUnit -> baseUnit.clone( newUnit ) ).toArray( BaseUnit[]::new );
            newUnit.setBaseUnits(newBaseUnits);
        }
        return newUnit;
    }
    
    public String calcBaseUnitName(Integer index, Object unit)
    {
        return generateFormula( (BaseUnit)unit );
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public static class BaseUnitTypeEditor extends StringTagEditorSupport
    {
        public BaseUnitTypeEditor()
        {
            super( getBaseUnitsList().toArray( new String[0] ) );
        }
    }

    private static List<String> baseUnitsList = StreamEx
            .of( "ampere", "gram", "katal", "metre", "second", "watt", "becquerel", "gray", "kelvin", "mole", "siemens", "weber",
                    "candela", "henry", "kilogram", "newton", "sievert", "coulomb", "hertz", "litre", "ohm", "steradian",
                    "dimensionless", "item", "lumen", "pascal", "tesla", "farad", "joule", "lux", "radian", "volt" ).sorted().toList();

    public static List<String> getBaseUnitsList()
    {
        return baseUnitsList;
    }
    
    public static List<String> getBaseSubstanceUnits()
    {
        List<String> substanceUnits = new ArrayList<>();
        substanceUnits.add("mole");
        substanceUnits.add("item");
        substanceUnits.add("gram");
        substanceUnits.add("dimensionless");
        return substanceUnits;
    }
    
    public static List<String> getBaseVolumeUnits()
    {
        List<String> substanceUnits = new ArrayList<>();
        substanceUnits.add("litre");
        substanceUnits.add("cubic metre");
        substanceUnits.add("dimensionless");
        return substanceUnits;
    }
    
    public static List<String> getBaseTimeUnits()
    {
        List<String> substanceUnits = new ArrayList<>();
        substanceUnits.add("second");
        substanceUnits.add("dimensionless");
        return substanceUnits;
    }
    
    @PropertyName("Formula")
    public String getFormula()
    {
        if (formula == null)
            formula = generateFormula();
        return formula;
    }
    
    public void setFormula(String formula)
    {
        //do nothing
    }
    
    private void resetFormula()
    {
        formula = null;
    }

    public String generateFormula(BaseUnit unit)
    {
        String result = unit.getType();            
        double scale = unit.getScale();
        double multiplier = unit.getMultiplier();
        int exponent = Math.abs( unit.getExponent());
        boolean brackets = false;
        
        if( scale != 0 )
        {
            if( scale == 1 )
                result = result + "*10";
            else
                result = result + "*10^(" + scale + ")";
            brackets = true;
        }

        if( multiplier != 1 )
        {
            result = multiplier + "*" + result;
            brackets = true;
        }

        if( exponent != 1 )
        {
            if( brackets )
                result = "(" + result + ")^" + exponent;
            else
                result = result + "^" + exponent;
        }

        return  result;
    }
    
    private boolean isNumerator(BaseUnit unit)
    {
        return unit.getExponent() >= 0;
    }
    
    private boolean isSimple(BaseUnit unit)
    {
        return unit.scale == 0 && unit.multiplier == 1;
    }
    
    public String generateFormula()
    {
        StringBuilder result = new StringBuilder();
        BaseUnit[] units = getBaseUnits();
        int unitLength = units.length;
        if( unitLength == 0 )
            return result.toString();

        String part = generateFormula( units[0] );
        if( !isSimple( units[0] ) )
            part = "(" + part + ")";

        if( unitLength == 1 )
        {
            if( isNumerator( units[0] ) )
                return generateFormula( units[0] );
            else
                return "1 / " + part;
        }

        if( isNumerator( units[0] ) )
            result.append( part );
        else
            result.append( "1 / " + part );

        for( int i = 1; i < unitLength; i++ )
        {
            part = generateFormula( units[i] );
            if( !isSimple( units[i] ) )
                part = "(" + part + ")";
            if( isNumerator( units[i] ) )
                result.append( " * " + part );
            else
                result.append( " / " + part );
        }

        return result.toString();
    }   
    
    /**
     * @return true if both units are equivalent
     */
    public static boolean equals(Unit u1, Unit u2)
    {
        if( u1.getBaseUnits().length != u2.getBaseUnits().length )
            return false;
        
        Set<BaseUnit> alreadyFound = new HashSet<>();
        boolean found;
        for (int i=0; i< u1.getBaseUnits().length; i++)
        {          
            found = false;
            BaseUnit b1 = u1.getBaseUnits()[i];            
            for (int j=0; j< u2.getBaseUnits().length; j++)
            {
                BaseUnit b2 = u2.getBaseUnits()[i];
                if (!alreadyFound.contains( b2) && equals(b1,b2))
                {
                    found = true;
                    alreadyFound.add( b2 );
                }
            }
            if (!found)
                return false;
        }
        return true;
    }
    
    /**
     * @return true if both base units are equivalent
     */
    public static boolean equals(BaseUnit u1, BaseUnit u2)
    {
        if( !u1.getType().equals( u2.getType() ) )
            return false;
        if (u1.getExponent() != u2.getExponent())
            return false;        
        if (u1.getMultiplier() != u2.getMultiplier())
            return false;       
        if (u1.getScale() != u2.getScale())
            return false;
        return true;
    }

	public String getUnitFormula() 
	{
        StringBuilder result = new StringBuilder();
        BaseUnit[] units = getBaseUnits();
        int unitLength = units.length;
        
        if( unitLength == 0 )
            return result.toString();

        String part = generateBaseUnitFormula( units[0], true, true );  
        result.append(part);       

        for( int i = 1; i < unitLength; i++ )
        {
        	if( units[i - 1].getExponent()== 1 || 
        			(units[i - 1].getMultiplier()== 1 && units[i - 1].getScale()== 0))
            	part = generateBaseUnitFormula( units[i], false, false );
            else
            	part = generateBaseUnitFormula( units[i], true, false );
            if( isNumerator( units[i] ) )
                result.append( " * " + part );
            else
                result.append( " / " + part );
        }

        return result.toString();
    } 
	
    public String generateBaseUnitFormula(BaseUnit unit, boolean multNeeded, boolean isFirst)
    {
    	StringBuilder result = new StringBuilder();            
        double scale = unit.getScale();
        double multiplier = unit.getMultiplier();
        int exponent = isFirst ? unit.getExponent() : Math.abs( unit.getExponent()); 
        boolean brackets = false;
        
        if( multiplier != 1 || multNeeded)
        {
            result.append(multiplier);
            if (multiplier != 1)
            	brackets = true;
        }
        
        if( scale != 0 )
        {
        	if (result.length() != 0)
        		result.append("*");
            if( scale == 1 )
                result.append("10");
            else
                result.append("10^(" + scale + ")");
            brackets = true;
        }
        
        if (scale != 0 || multiplier != 1 || multNeeded)
        	result.append(" ");
        
        result.append(unit.getType());

        if( exponent != 1 )
        {
            if( brackets )
                result.insert(0, "(").append(")^" + exponent);
            else
                result.append("^" + exponent);
        }

        return  result.toString();
    }
}