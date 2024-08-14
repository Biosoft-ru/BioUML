package biouml.plugins.gtrd;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public abstract class ChIPExperiment extends Experiment
{
    protected String antibody;
    protected String controlId;
    protected ExperimentType expType;
    protected DataElementPath alignment;
    protected DataElementPath peak;
    
    public ChIPExperiment(DataCollection<?> parent, String id )
    {
        super( parent, id );
    }
    
    public String getAntibody()
    {
        return antibody;
    }
    
    public void setAntibody(String antibody)
    {
        this.antibody = antibody;
    }
    
    public String getControlId()
    {
        return controlId;
    }
    
    public void setControlId(String controlId)
    {
        this.controlId = controlId;
    }
    
    
    public DataElementPath getControl()
    {
        if(controlId == null)
            return null;
        return DataElementPath.create(this).getSiblingPath(controlId);
    }
    
    public ExperimentType getExpType()
    {
        return expType;
    }

    public void setExpType(ExperimentType expType)
    {
        this.expType = expType;
    }

    public boolean isControlExperiment()
    {
        return expType == ExperimentType.CHIP_CONTROL;
    }

    /*
    private Article[] articles = new Article[0];
    public Article[] getArticles()
    {
        return articles;
    }
    public void setArticles(Article[] articles)
    {
        this.articles = articles;
    }
    */
    
    protected List<Article> articles = new ArrayList<>();
    public List<Article> getArticles()
    {
        return articles;
    }
    public void setArticles(List<Article> articles)
    {
        this.articles = articles;
    }

    //FIXME: these methods were needed to make lucene index on articles, who commented them?
    /*
    private String authors = "";
    public String getAuthors()
    {
        return authors;
    }
    public void setAuthors(String authors)
    {
        this.authors = authors;
    }
    
    private String articleYear = "";
    public String getArtcleYear()
    {
        return articleYear;
    }
    public void setArticleYear(String val)
    {
        this.articleYear = val;
    }
    
    private String articleTitle;
    public String getArticleTitle()
    {
        return articleTitle;
    }
    public void setArticleTitle(String val)
    {
        this.articleTitle = val;
    }
    */
    
    public DataElementPath getAlignment()
    {
        return alignment;
    }

    public void setAlignment(DataElementPath alignment)
    {
        this.alignment = alignment;
    }

    public DataElementPath getPeak()
    {
        return peak;
    }

    public void setPeak(DataElementPath peak)
    {
        this.peak = peak;
    }

}
