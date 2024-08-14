package biouml.plugins.simulation_test;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import ru.biosoft.exception.ExceptionRegistry;
import com.developmentontheedge.application.ApplicationUtils;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

public class SemanticTestListParser
{
    public List<Category> parseFile(File file)
    {
        List<Category> categories = new ArrayList<>();
        try(BufferedReader br = ApplicationUtils.utfReader( file ))
        {
            String line = null;
            int lineNumber = 1;

            Category currentCategory = null;

            while( ( line = br.readLine() ) != null )
            {
                StringTokenizer st = new StringTokenizer(line);
                String firstCharacter = st.nextToken();
                if( firstCharacter.equals("CATEGORY") )
                {
                    StringBuilder currentCategoryName = new StringBuilder();
                    while( st.hasMoreTokens() )
                    {
                        currentCategoryName.append(st.nextToken());
                    }
                    currentCategory = new Category(currentCategoryName.toString(), null);
                    categories.add(currentCategory);
                }
                else if( firstCharacter.equals("TEST") )
                {
                    StringBuilder testName = new StringBuilder();
                    while( st.hasMoreTokens() )
                    {
                        testName.append(st.nextToken());
                    }

                    if( currentCategory.tests == null )
                    {
                        currentCategory.tests = new ArrayList<>();
                    }
                    currentCategory.tests.add(testName.toString().replaceAll("\\.test", ""));
                }
                else if( firstCharacter.charAt(0) == '#' )
                {
                    // it is a comment, ignore the string
                }
                else
                {
                    throw new Exception("Bad begining character at line " + lineNumber);
                }
                lineNumber++;
            }

        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return categories;
    }

    public static List<String> parseTestList(File file)
    {
        List<String> tests = new ArrayList<>();
        try( BufferedReader br = ApplicationUtils.utfReader(file) )
        {
            String line = null;

            while( ( line = br.readLine() ) != null )
            {
                StringTokenizer st = new StringTokenizer(line);
                String firstCharacter = st.nextToken();
                if( firstCharacter.equals("TEST") )
                {
                    StringBuilder testName = new StringBuilder();
                    while( st.hasMoreTokens() )
                        testName.append(st.nextToken());

                    tests.add(testName.toString().replaceAll("\\.test", ""));
                }
                else if( firstCharacter.equals("TESTS") )
                {
                    String[] testNames = line.substring(6, line.length()).split(",");
                    for( String testName : testNames )
                    {
                        if (testName.contains("-"))
                        {
                            int[] testRange = StreamEx.of(testName.split("-")).mapToInt(s->testNameToInt(s)).toArray();
                            List<String> allTests = IntStreamEx.range(testRange[0], testRange[1] + 1).mapToObj(i->intToTestName(i, 6)).toList();
                            tests.addAll(allTests);
                        }
                        else
                            tests.add(testName.trim());

                    }
                }
                else if( firstCharacter.trim().equals("END") )
                    return tests;
            }

        }
        catch( Exception ex )
        {
            throw ExceptionRegistry.translateException( ex );
        }
        return tests;
    }

    private static int testNameToInt(String name)
    {
        return Integer.parseInt(name.trim().replaceFirst("^0*", ""));
    }

    private static String intToTestName(int id, int length)
    {
        return String.format("%05d", id);
    }

    public HashMap<String, ArrayList<String>> parseTagFile(File file)
    {
        HashMap<String, ArrayList<String>> test2Categories = new HashMap<>();
        try(BufferedReader br = ApplicationUtils.utfReader( file ))
        {
            String line = null;
            int lineNumber = 1;
            br.readLine();

            while( ( line = br.readLine() ) != null )
            {
                StringTokenizer st = new StringTokenizer(line);
                String firstCharacter = st.nextToken();

                String testName = firstCharacter;
                ArrayList<String> testCategories = new ArrayList<>();
                testName += "/" + testName;
                while( st.hasMoreTokens() )
                {
                    String categoryName = st.nextToken();
                    if( !categoryName.matches("\\d.\\d") )
                        testCategories.add(categoryName);
                }

                test2Categories.put(testName, testCategories);
                lineNumber++;

            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return test2Categories;
    }
    public static class Category
    {
        public List<String> tests;
        public String name;
        Category(String name, List<String> tests)
        {
            this.name = name;
            this.tests = tests;
        }
    }
}
