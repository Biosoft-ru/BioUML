package biouml.workbench;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Module;
import ru.biosoft.access.QuerySystemWithIndexRebuilder;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.RepositoryPane;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.ViewPart;
import ru.biosoft.gui.ViewPartRegistry;

public class SearchPane extends JPanel
{
    protected static final Logger log = Logger.getLogger(SearchPane.class.getName());

    public static final String LUCENE_PLUGIN = "biouml.plugins.lucene";
    public static final String LUCENE_FORMATTER_PREFIX = "<b>";
    public static final String LUCENE_FORMATTER_POSTFIX = "</b>";
    public static final String RESULT_VIEW_PART_NAME = "Search results";

    protected JTextField searchString;
    protected JButton searchButton;
    protected Bundle bundle;

    protected RepositoryPane repositoryPane;

    public SearchPane(RepositoryPane repositoryPane)
    {
        super(new BorderLayout());
        setPreferredSize(new Dimension(100, 25));

        this.repositoryPane = repositoryPane;

        bundle = Platform.getBundle(LUCENE_PLUGIN);
        if( bundle != null )
        {
            searchString = new JTextField();
            add(searchString, BorderLayout.CENTER);

            URL resURL = SearchPane.class.getResource("resources/search.gif");
            searchButton = new JButton(new ImageIcon(resURL));
            add(searchButton, BorderLayout.EAST);

            searchButton.addActionListener(e -> searchActionPerformed());
        }
    }

    protected void searchActionPerformed()
    {
        if( bundle == null )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(),
                    "Lucene plugin (" + LUCENE_PLUGIN + ") is necessary to search", "Warning", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String queryString = searchString.getText();
        if( queryString == null )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Search string can't be null", "Warning",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DataElement element = repositoryPane.getSelectedNode();
        if( element == null )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "No selected database to search", "Warning",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DataElementPath completePath = DataElementPath.create(element);

        Module module = Module.optModule(element);

        if( module == null )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Select database in repository tree for search", "Warning",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String relativeName = element == module?null:CollectionFactory.getRelativeName(element, module);

        Object luceneFacade = getSearchFacade(module);
        if( luceneFacade == null )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), "Search is not available for database " + module.getName(),
                    "Warning", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if( luceneFacade instanceof QuerySystemWithIndexRebuilder )
        {
            try
            {
                if( ! ( (QuerySystemWithIndexRebuilder)luceneFacade ).testHaveIndex() )
                {
                    ( (QuerySystemWithIndexRebuilder)luceneFacade ).showRebuildIndexesUI(Application.getApplicationFrame());
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        ViewPart resultPane = ViewPartRegistry.getViewPart("search.results");
        if( resultPane != null )
        {
            try
            {
                Class<?> luceneResultPaneClass = bundle.loadClass("biouml.plugins.lucene.LuceneSearchViewPart");
                if( luceneResultPaneClass.isInstance(resultPane) )
                {
                    Method setInfoMethod = luceneResultPaneClass.getMethod("setInfo", String.class, String.class);
                    setInfoMethod.invoke(resultPane, queryString, completePath.toString());
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can't print results in " + RESULT_VIEW_PART_NAME + " view part", e);
            }
            
            GUI.getManager().showViewPart( resultPane );
        }

        Thread thread = new Thread(new SearchProcess(luceneFacade, relativeName, queryString));
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    protected Object getSearchFacade(Module module)
    {
        try
        {
            Class<?> luceneQuerySystemClass = bundle.loadClass("biouml.plugins.lucene.LuceneQuerySystem");
            if( luceneQuerySystemClass.isInstance(module.getInfo().getQuerySystem()) )
            {
                Object luceneFacade = module.getInfo().getQuerySystem();
                Method testMethod = luceneQuerySystemClass.getMethod("testHaveLuceneDir");
                boolean testResult = (Boolean)testMethod.invoke(luceneFacade);
                if( testResult )
                {
                    return luceneFacade;
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not get lucene facade");
        }
        return null;
    }

    class SearchProcess implements Runnable
    {
        protected Object luceneFacade;
        protected String relativeName;
        protected String queryString;

        public SearchProcess(Object luceneFacade, String relativeName, String queryString)
        {
            this.luceneFacade = luceneFacade;
            this.relativeName = relativeName;
            this.queryString = queryString;
        }

        @Override
        public void run()
        {
            DynamicPropertySet[] dps = null;
            try
            {
                Class<?> luceneFormatterClass = bundle.loadClass("biouml.plugins.lucene.Formatter");
                Object formatter = luceneFormatterClass.getConstructor(String.class, String.class).newInstance(
                        LUCENE_FORMATTER_PREFIX, LUCENE_FORMATTER_POSTFIX);
                Method searchMethod = luceneFacade.getClass().getMethod("searchRecursive",
                        String.class, String.class, luceneFormatterClass, Integer.TYPE, Integer.TYPE);
                Integer minValue = 0;
                Integer maxValue = (Integer)luceneFacade.getClass().getField("MAX_DEFAULT_SEARCH_RESULTS_COUNT").get(null);
                dps = (DynamicPropertySet[])searchMethod.invoke(luceneFacade, relativeName, queryString, formatter, minValue,
                        maxValue);
                Collections.reverse( Arrays.asList( dps ) );
            }
            catch( Exception pe )
            {
                log.log(Level.SEVERE, "Lucene search failed", pe);
                return;
            }

            ViewPart resultPane = ViewPartRegistry.getViewPart("search.results");
            if( resultPane != null )
            {
                try
                {
                    Class<?> luceneResultPaneClass = bundle.loadClass("biouml.plugins.lucene.LuceneSearchViewPart");
                    if( luceneResultPaneClass.isInstance(resultPane) )
                    {
                        Method setResultsMethod = luceneResultPaneClass.getMethod("setResults", DynamicPropertySet[].class);
                        setResultsMethod.invoke(resultPane, new Object[] {dps});
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can't print results in " + RESULT_VIEW_PART_NAME + " view part", e);
                }
            }
        }
    }
}
