package biouml.plugins.sbgn.title;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.exception.InternalException;

/**
 * Class which helps to parse TransPath molecule title into separate elements tree
 * @author lan
 * Rewritten algorithm to parse complexes + units of info
 * @author Alisa
 */
public class TitleElement implements Cloneable
{
    private static final Logger log = Logger.getLogger(TitleElement.class.getName());
    public static final int ARBITRARY_MULTIMER_COUNT = -1;

    private static final Map<String, String> knownErrors = new HashMap<>();

    static
    {
        try
        {
            availableSpecies = ApplicationUtils.readAsList(TitleElement.class.getResource("tp_species_tag.txt").openStream());
        }
        catch( IOException e )
        {
            log.log(Level.SEVERE, "Can not read a file \"tp_species_tag.txt\". " + e.getMessage());
        }

        // From TRANSPATH 2012.4
        knownErrors.put("p53{ub{K63}(2)", "p53{ub{K63}(2)}");
        // From TRANSPATH 2013.2
        knownErrors.put("{p}n){ub{K63}(n)}", "{p}n{ub{K63}(n)}");
        knownErrors.put("{ub{K63}:", "{ub{K63}(n)}:");
    }

    private static final Pattern pattern = Pattern.compile("\\(([A-Za-z\\.]+)\\)$");
    private List<TitleElement> subElements = new ArrayList<>();
    private String title;
    private boolean isShowTitle = false;
    private boolean isComplex = false;
    private String name;
    private List<String> modificators = new ArrayList<>();
    private List<String> units_of_info = new ArrayList<>();
    private String specie = "";
    private int multimerCount = 1;
    private String moltype = "";
    private static List<String> availableSpecies;

    private static int parseMultimerCount(String count)
    {
        return count.equals("n") ? ARBITRARY_MULTIMER_COUNT : Integer.parseInt(count);
    }

    public TitleElement(String title)
    {
        this(new SyntaxElement(fixTitle(title)));

        if( availableSpecies == null )
        {

        }
    }

    /**
     * Fixes known errors in titles
     * @param title
     * @return
     */
    private static String fixTitle(String title)
    {
        for( Map.Entry<String, String> entry : knownErrors.entrySet() )
        {
            String key = entry.getKey();
            int pos = title.indexOf(key);
            if( pos >= 0 )
            {
                String newTitle = title.substring(0, pos) + entry.getValue() + title.substring(pos + key.length());
                log.warning("Transpath title fix: " + title + " => " + newTitle);
                return newTitle;
            }
        }
        return title;
    }

    private TitleElement(SyntaxElement syntaxElement)
    {
        processSyntaxElement(syntaxElement);
    }


    private void processSyntaxElement(SyntaxElement syntaxElement)
    {
        List<SyntaxElement> elements = syntaxElement.getSubElements() == null ? Arrays.asList(syntaxElement)
                : syntaxElement.getSubElements();

        if( this.title == null )
            this.title = syntaxElement.getOriginalString();
        StringBuilder newName = new StringBuilder();
        boolean mainElementFound = false;
        boolean isNameInitialized = false; // name can be initialized in recursive call of processSyntaxElement function 
        for( int i = 0; i < elements.size(); i++ )
        {
            SyntaxElement element = elements.get(i);


            if( element.getType() == SyntaxElementType.STRING )
            {
                if( i < elements.size() - 1 && isComplexSyntaxElement(elements.get(i + 1)) )
                {
                    this.isShowTitle = true;
                    this.name = element.getOriginalString();
                    isNameInitialized = true;
                    processComplex(elements.get(i + 1).getSubElements());
                    i++;

                    if( i < elements.size() - 1 && elements.get(i + 1).getType().equals(SyntaxElementType.INTEGER) )
                    {

                        this.multimerCount = parseMultimerCount(elements.get(i + 1).getData());
                        i++;
                    }

                }
                else
                {
                    if( !mainElementFound )
                        mainElementFound = true;

                    newName.append(element.getData());
                }

            }
            else if( element.getType() == SyntaxElementType.INTEGER )
            {
                if( !mainElementFound )
                    mainElementFound = true;

                newName.append(element.getData());

            }
            else if( element.getType() == SyntaxElementType.PARENTHESES )
            {
                if( !mainElementFound )
                {
                    mainElementFound = true;

                    if( isComplexSyntaxElement(element) )
                        processComplex(element.getSubElements());
                    else
                    {
                        processSyntaxElement(element);

                    }
                    isNameInitialized = true;

                    if( i < elements.size() - 1 && elements.get(i + 1).getType().equals(SyntaxElementType.INTEGER) )
                    {
                        this.multimerCount = parseMultimerCount(elements.get(i + 1).getData());
                        i++;
                    }

                }
                else
                {
                    newName.append(element.getOriginalString());
                }

            }
            else if( element.getType() == SyntaxElementType.BRACKETS )
            {
                List<SyntaxElement> modSubElements = element.getSubElements();
                if( modSubElements == null )
                    throw new IllegalArgumentException("Invalid syntax: " + element.getOriginalString());

                String modificator = modSubElements.isEmpty() ? "" : modSubElements.get( 0 ).getData();

                // modifier_name:integer
                if( modSubElements.size() == 3 && modSubElements.get(1).getType() == SyntaxElementType.COLON
                        && modSubElements.get(2).getType() == SyntaxElementType.INTEGER )
                {
                    modificator += "*" + modSubElements.get(2).getData();


                    if( i < elements.size() - 1 && elements.get(i + 1).getType().equals(SyntaxElementType.INTEGER) )
                    {
                        throw new IllegalArgumentException("Only one syntax construction of multiple modification is valid: "
                                + element.getOriginalString() + elements.get(i + 1).getOriginalString());
                    }
                }
                // {modifier_name}integer
                else if( modSubElements.size() == 1 )
                {
                    if( i < elements.size() - 1 && elements.get(i + 1).getType().equals(SyntaxElementType.INTEGER) )
                    {
                        modificator += "*" + elements.get(i + 1).getData();
                        i += 1;
                    }

                }
                else if( modSubElements.size() == 0 )
                {
                    //do nothing, its ok
                }
                else
                    throw new IllegalArgumentException("Invalid syntax: " + element.getOriginalString());

                this.modificators.add(modificator);
            }
            else if( element.getType() == SyntaxElementType.SQUARE_BRACKETS )
            {
                if( element.getSubElements() == null || element.getSubElements().size() != 1 )
                    throw new IllegalArgumentException("Invalid syntax: " + element.getOriginalString());
                String info_unit = element.getSubElements().get(0).getData();
                this.units_of_info.add(info_unit);
            }
            else
            {
                newName.append(element.getData());
            }

        }

        if( !isNameInitialized )
        {
            this.name = newName.toString();

            if( this.name.isEmpty() )
                this.name = null;
            else
            {
                Matcher m = pattern.matcher(this.name);
                if( m.find() && !m.group(1).equals("I") && !m.group(1).equals("II") && !m.group(1).equals("fu") )
                {
                    this.specie = m.group(1);
                    this.name = m.replaceFirst("");

                    if( availableSpecies != null && !m.group(1).isEmpty() && availableSpecies.contains(m.group(1)) )
                        this.specie = m.group(1);
                }
            }
        }

    }

    private void processComplex(List<SyntaxElement> elements)
    {

        if( elements.size() == 1 && elements.get(0).getType() == SyntaxElementType.COLON ) // empty complex (:)
        {
            this.isComplex = true;
            return;
        }

        int potentialPosOfSpecie = 0;
        for( int i = 0; i < elements.size(); i++ )
        {
            SyntaxElement element = elements.get(i);
            if( element.getType() == SyntaxElementType.COLON )
            {
                if( potentialPosOfSpecie == i ) // potential position of species != colon
                    throw new IllegalArgumentException("Invalid syntax: " + elements.get(0));

                this.isComplex = true;
                subElements.add(new TitleElement(new SyntaxElement(SyntaxElementType.ROOT, elements.subList(potentialPosOfSpecie, i))));
                potentialPosOfSpecie = i + 1; // overstep the colon
            }
        }

        if( potentialPosOfSpecie == elements.size() ) // complex contains one element (A:)
            return;
        else if( potentialPosOfSpecie > 0 )
        {
            subElements.add(
                    new TitleElement(new SyntaxElement(SyntaxElementType.ROOT, elements.subList(potentialPosOfSpecie, elements.size()))));
            return;
        }

    }

    private boolean isComplexSyntaxElement(SyntaxElement element)
    {
        if( !element.getType().equals(SyntaxElementType.PARENTHESES) )
            return false;

        List<SyntaxElement> elements = element.getSubElements() == null ? Arrays.asList(element) : element.getSubElements();

        for( SyntaxElement elem : elements )
            if( elem.getType() == SyntaxElementType.COLON )
                return true;

        return false;
    }

    public boolean isComplex()
    {
        return this.isComplex;
    }

    public boolean isShowTitle()
    {
        return this.isShowTitle;
    }

    public boolean isMultimer()
    {
        return multimerCount != 1;
    }

    /**
     * @return the subElements
     */
    public List<TitleElement> getSubElements()
    {
        return Collections.unmodifiableList(subElements);
    }

    /**
     * @return the title
     */
    public String getTitle()
    {
        return title;
    }

    public String getTitleNoSpecies()
    {
        StringBuilder result = new StringBuilder();
        if( name != null )
            result.append(name);

        if( isComplex )
        {
            result.append("(");
            for( TitleElement subElement : subElements )
            {
                if( result.length() > 1 )
                    result.append(':');
                result.append(subElement.getTitleNoSpecies());
            }
            result.append(")");
        }
        for( String modificator : modificators )
        {
            if( modificator.contains("*") )
            {
                String[] fields = modificator.split("\\*");
                result.append("{").append(fields[0]).append("}").append(fields[1]);
            }
            else
                result.append("{").append(modificator).append("}");
        }

        if( multimerCount == 1 )
            return result.toString();
        else
        {
            if( isComplex )
                return result.toString() + ( multimerCount == ARBITRARY_MULTIMER_COUNT ? "n" : multimerCount );
            else
                return "(" + result + ")" + ( multimerCount == ARBITRARY_MULTIMER_COUNT ? "n" : multimerCount );
        }
    }

    /**
     * For testing only. Must return the same as getTitle()
     */
    public String getTitleConstructed()
    {
        StringBuilder result = new StringBuilder();
        boolean isMultimer = multimerCount != 1;

        if( isComplex || isMultimer )
            result.append("(");

        if( name != null )
            result.append(name);
        for( TitleElement subElement : subElements )
        {
            if( result.length() > 1 )
                result.append(':');
            result.append(subElement.getTitleConstructed());
        }
        if( specie != null && !specie.isEmpty() )
            result.append("(").append(specie).append(")");
        for( String modificator : modificators )
        {
            if( modificator.contains("*") )
            {
                String[] fields = modificator.split("\\*");
                result.append("{").append(fields[0]).append("}").append(fields[1]);
            }
            else
                result.append("{").append(modificator).append("}");
        }

        if( isComplex || isMultimer )
            result.append(")");

        if( isMultimer )
            return result.append(multimerCount == ARBITRARY_MULTIMER_COUNT ? "n" : multimerCount).toString();
        else
            return result.toString();


    }

    /**
     * @return the modificators
     */
    public List<String> getModificators()
    {
        return Collections.unmodifiableList(modificators);
    }

    /**
     * @return the specie
     */
    public String getSpecies()
    {
        return specie;
    }

    /**
     * @return the multimerCount
     */
    public int getMultimerCount()
    {
        return multimerCount;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return molecule type
     */
    public String getType()
    {
        return moltype;
    }

    /**
     * @return the unit of information
     */
    public List<String> getUnitOfInfo()
    {
        return Collections.unmodifiableList(units_of_info);
    }

    @Override
    public String toString()
    {
        return title;
    }

    @Override
    public TitleElement clone()
    {
        try
        {
            TitleElement result = (TitleElement)super.clone();
            result.modificators = new ArrayList<>(modificators);
            result.subElements = new ArrayList<>(subElements);
            return result;
        }
        catch( CloneNotSupportedException e )
        {
            throw new InternalException(e);
        }
    }

    public TitleElement()
    {
        title = "";
        name = "";
        modificators = new ArrayList<>();
        subElements = new ArrayList<>();
    }

    public void setModifiers(List<String> modificators)
    {
        this.modificators = modificators;
        this.title = getTitleConstructed();
    }

    public void setSubElements(List<TitleElement> subElements)
    {
        this.subElements = subElements;
        this.title = getTitleConstructed();
    }

    public void setMultimerCount(int multimerCount)
    {
        this.multimerCount = multimerCount;
        this.title = getTitleConstructed();
    }
}
