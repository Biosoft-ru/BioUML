package biouml.plugins.gtrd;

import java.util.Set;

public class Article extends Persistent {

	private long pubmedId;
	public long getPubmedId() {
		return pubmedId;
	}
	public void setPubmedId(long pubmedId) {
		this.pubmedId = pubmedId;
	}

	private String title;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	private Journal journal;
	public Journal getJournal() {
		return journal;
	}
	public void setJournal(Journal journal) {
		this.journal = journal;
	}
	
	private Set<Author> authors;
	public Set<Author> getAuthors() {
		return authors;
	}
	public void setAuthors(Set<Author> authors) {
		this.authors = authors;
	}
	
	private String pages;
	public String getPages() {
		return pages;
	}
	public void setPages(String pages) {
		this.pages = pages;
	}

	private String abstractText;
	public String getAbstractText() {
		return abstractText;
	}

	public void setAbstractText(String abstractText) {
		this.abstractText = abstractText;
	}

	@Override
	public String toString() {
		String[] authors = getAuthors().stream().map(Author::getShortName).toArray(String[]::new);
		return String.join(", ", authors) + ". "
				+ getTitle() + " " + getJournal().getShortName() + " " +getPages() + ".";
	}
	
}
