package biouml.plugins.sbml._test;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.developmentontheedge.application.ApplicationUtils;

public class TestListParser
{
    public List<Category> parseFile(File file)
    {
        List<Category> categories = new ArrayList<>();
        try(BufferedReader br = ApplicationUtils.utfReader( file ))
        {
            String line = null;
            int lineNumber = 1;

            Category currentCategory = null;

            while ( (line = br.readLine()) != null)
            {
                StringTokenizer st = new StringTokenizer(line);
                String firstCharacter = st.nextToken();
                if (firstCharacter.equals("CATEGORY"))
                {
                    StringBuilder currentCategoryName = new StringBuilder();
                    while (st.hasMoreTokens())
                    {
                        currentCategoryName.append(st.nextToken());
                    }
                    currentCategory = new Category(currentCategoryName.toString(), null);
                    categories.add(currentCategory);
                }
                else if (firstCharacter.equals("TEST"))
                {
                    StringBuilder testName = new StringBuilder();
                    while (st.hasMoreTokens())
                    {
                        testName.append(st.nextToken());
                    }

                    if (currentCategory.tests == null)
                    {
                        currentCategory.tests = new ArrayList<>();
                    }
                    currentCategory.tests.add(testName.toString().replaceAll("\\.test", ""));
                }
                else if (firstCharacter.charAt(0) == '#')
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
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return categories;
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
