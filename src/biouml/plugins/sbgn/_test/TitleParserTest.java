package biouml.plugins.sbgn._test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.sbgn.title.LexemeList;
import biouml.plugins.sbgn.title.SyntaxElement;
import biouml.plugins.sbgn.title.TitleElement;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author lan
 *
 */
public class TitleParserTest extends TestCase
{

    public TitleParserTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(TitleParserTest.class);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TitleParserTest.class.getName());
        suite.addTest(new TitleParserTest("lexerTest"));
        suite.addTest(new TitleParserTest("lexerTestWeirdCases"));
        suite.addTest(new TitleParserTest("syntaxTest"));
        suite.addTest(new TitleParserTest("titleParserTest"));
        suite.addTest(new TitleParserTest("titleParserSpeciesTest"));
        suite.addTest(new TitleParserTest("titleParserMultimerTest"));
        suite.addTest(new TitleParserTest("readAllTitles"));
        return suite;
    }

    public void lexerTest()
    {
        LexemeList lexemeList = new LexemeList("(leptin)2:(LEPR-B:Jak2{pY1007}{pY1008})2");
        assertEquals("Size of lexeme list", 17, lexemeList.size());
        assertEquals("Lexemes content",
                "[LEFT_PARENTHESIS:(, STRING:leptin, RIGHT_PARENTHESIS:), INTEGER:2, COLON::, "
                        + "LEFT_PARENTHESIS:(, STRING:LEPR-B, COLON::, STRING:Jak2, " + "LEFT_BRACKET:{, STRING:pY1007, RIGHT_BRACKET:}, "
                        + "LEFT_BRACKET:{, STRING:pY1008, RIGHT_BRACKET:}, RIGHT_PARENTHESIS:), INTEGER:2]",
                lexemeList.toString());
    }

    public void lexerTestWeirdCases()
    {
        LexemeList lexemeList = new LexemeList("(R)-4'-phospho-N-pantothenoyl-L-cysteine");
        assertEquals("Size of lexeme list", 1, lexemeList.size());
        lexemeList = new LexemeList("PtdIns(5)P4K");
        assertEquals("Size of lexeme list", 1, lexemeList.size());
        lexemeList = new LexemeList("9(R)-HODE:PPARgamma (NR1C3)");
        assertEquals("Lexemes?", "[STRING:9(R)-HODE, COLON::, STRING:PPARgamma , LEFT_PARENTHESIS:(, STRING:NR1C3, RIGHT_PARENTHESIS:)]",
                lexemeList.toString());
    }

    public void syntaxTest()
    {
        String title = "((leptin)2:(LEPR-B:Jak2{pY1007}{pY1008})2)";
        SyntaxElement element = new SyntaxElement(title);
        assertEquals("Syntax tree", "ROOT:[PARENTHESES:[PARENTHESES:[STRING:leptin], " + "INTEGER:2, COLON::, "
                + "PARENTHESES:[STRING:LEPR-B, COLON::, STRING:Jak2, BRACKETS:[STRING:pY1007], BRACKETS:[STRING:pY1008]], " + "INTEGER:2]]",
                element.toString());
        assertEquals("Original string", title, element.getOriginalString());
    }

    public void titleParserTest()
    {
        String title = "((leptin)2:(LEPR-B:Jak2{pY1007}{pY1008})2)";
        TitleElement element = new TitleElement(title);
        assertEquals("Multimer?", false, element.isMultimer());
        assertEquals("Complex?", true, element.isComplex());
        List<TitleElement> subElements = element.getSubElements();
        assertEquals("2 subElements?", 2, subElements.size());
        TitleElement leptinElement = subElements.get(0);
        assertEquals("leptin?", "leptin", leptinElement.getName());
        assertEquals("leptin multimer?", true, leptinElement.isMultimer());
        assertEquals("leptin complex?", false, leptinElement.isComplex());
        TitleElement secondElement = subElements.get(1);
        assertEquals("secondElement title", "(LEPR-B:Jak2{pY1007}{pY1008})2", secondElement.getTitle());
        assertEquals("secondElement multimer?", true, secondElement.isMultimer());
        assertEquals("secondElement complex?", true, secondElement.isComplex());
        subElements = secondElement.getSubElements();
        assertEquals("2 subElements?", 2, subElements.size());
        assertEquals("LEPR-B?", "LEPR-B", subElements.get(0).getName());
        assertEquals("No modifications?", 0, subElements.get(0).getModificators().size());
        assertEquals("Jak2?", "Jak2", subElements.get(1).getName());
        assertEquals("Modificators?", "[pY1007, pY1008]", subElements.get(1).getModificators().toString());
        assertEquals("Title", element.getTitle(), element.getTitleNoSpecies());


        element = new TitleElement("ATP");
        assertEquals("Multimer?", false, element.isMultimer());
        assertEquals("Complex?", false, element.isComplex());
        assertEquals("Name?", "ATP", element.getName());
    }

    public void titleParserSpeciesTest()
    {
        TitleElement element = new TitleElement("SLAM(h){pY}");
        assertEquals("Species?", "h", element.getSpecies());
        assertEquals("Title?", "SLAM{pY}", element.getTitleNoSpecies());
        element = new TitleElement("(SH2D1A(m.s.):SLAM(m){pY})");
        assertEquals("Title?", "(SH2D1A:SLAM{pY})", element.getTitleNoSpecies());
        element = new TitleElement("(hist1h1e(m.s.){met(2)K26}:L3MBTL1(m.s.))");
        assertEquals("Mod?", "met(2)K26", element.getSubElements().get(0).getModificators().get(0));
        assertEquals("Title?", "(hist1h1e{met(2)K26}:L3MBTL1)", element.getTitleNoSpecies());
        element = new TitleElement(
                "(histone H3(h){metR18}:histone H3(h){aceK10}{aceK15}:carm1(m.s.):SRC-1(m.s.):CBP(m.s.):RelA-p65(m.s.))");
        assertEquals("6 components?", 6, element.getSubElements().size());
        assertEquals("Title?", "(histone H3{metR18}:histone H3{aceK10}{aceK15}:carm1:SRC-1:CBP:RelA-p65)", element.getTitleNoSpecies());
        element = new TitleElement("(IL-17F(h){gly})2");
        assertEquals("Title?", "(IL-17F{gly})2", element.getTitleNoSpecies());
        element = new TitleElement("poly I:C");
        assertEquals("Name?", "poly I:C", element.getName());
    }

    public void titleParserMultimerTest()
    {
        TitleElement element = new TitleElement(
                "((Glu)2:(glycine)2:(NR1{pS}2)2:NR2A:NR2B:PKAc:Pyk2{pY}:Src:Grb-2:Shc{pY}:Sos:Ras:Raf-1:MEK1)");
        assertEquals("{pS}2 multimer?", "pS*2", element.getSubElements().get(2).getModificators().get(0));
        assertEquals("Title?", "((Glu)2:(glycine)2:(NR1{pS}2)2:NR2A:NR2B:PKAc:Pyk2{pY}:Src:Grb-2:Shc{pY}:Sos:Ras:Raf-1:MEK1)",
                element.getTitleNoSpecies());
        element = new TitleElement("((NT-3)2:(trkC{pY516})2:Shc-1{pY}:Grb-2:Sos:Ras:GTP:Raf-1)");
        assertEquals("Title?", "((NT-3)2:(trkC{pY516})2:Shc-1{pY}:Grb-2:Sos:Ras:GTP:Raf-1)", element.getTitleNoSpecies());
        assertEquals("Modification?", "pY516", element.getSubElements().get(1).getModificators().get(0));
        assertEquals("Multimer?", 2, element.getSubElements().get(1).getMultimerCount());
        element = new TitleElement("(PDGFBB:PDGFRalpha{pY}n:PDGFRbeta{pY771}{pY}n:RasGAP{pY})");
        assertEquals("Title?", "(PDGFBB:PDGFRalpha{pY}n:PDGFRbeta{pY771}{pY}n:RasGAP{pY})", element.getTitleNoSpecies());
        assertEquals("pY*n", "pY*n", element.getSubElements().get(1).getModificators().get(0));
        element = new TitleElement("(ubiquitin)n");
        assertEquals("Title?", "(ubiquitin)n", element.getTitleNoSpecies());
        assertEquals("Multimer?", TitleElement.ARBITRARY_MULTIMER_COUNT, element.getMultimerCount());
        element = new TitleElement("(adiponectin(h))n");
        assertEquals("Title?", "(adiponectin)n", element.getTitleNoSpecies());
        assertEquals("Multimer?", TitleElement.ARBITRARY_MULTIMER_COUNT, element.getMultimerCount());
        assertEquals("Specie?", "h", element.getSpecies());
        element = new TitleElement("adiponectin{val:6}");
        assertEquals("Title?", "adiponectin{val}6", element.getTitleNoSpecies()); // {val:6} and {val}6 are synonymous
        assertEquals("{val:2}3", "val*6", element.getModificators().get(0));
    }

    public void readAllTitles() throws Exception
    {
        List<String> titles = ApplicationUtils.readAsList(TitleParserTest.class.getResource("titles.txt").openStream());
        Map<String, String> knownErrors = new HashMap<>();
        knownErrors.put("TNF-alpha{sol}(h)", "TNF-alpha(h){sol}");
        knownErrors.put("((TNF-alpha{sol}(h))3:TNFR1(h))", "((TNF-alpha(h){sol})3:TNFR1(h))");
        knownErrors.put("(TNF-alpha{sol}(h):TNFR2(h))", "(TNF-alpha(h){sol}:TNFR2(h))");
        knownErrors.put("ERK1{p}, ERK2{p}", "ERK1, ERK2{p}{p}");
        knownErrors.put("(ERK1{p}, ERK2{p}:RSK1{pS380}{pT573})", "(ERK1, ERK2{p}{p}:RSK1{pS380}{pT573})");
        knownErrors.put("(ERK1{p}, ERK2{p}:RSK1)", "(ERK1, ERK2{p}{p}:RSK1)");
        knownErrors.put("(RSK1{pT573}:ERK1{p}, ERK2{p})", "(RSK1{pT573}:ERK1, ERK2{p}{p})");
        knownErrors.put("AKT-1{p}, AKT-2{p}", "AKT-1, AKT-2{p}{p}");
        knownErrors.put("IRAK1 {pThr209}{pThr387} {p}n", "IRAK1  {pThr209}{pThr387}{p}n");
        knownErrors.put("(TRAF6:UbcH7:p75NTR:(NGF-p13)2:trkA{pY490}{ub{K63}(1)K485}p62)",
                "(TRAF6:UbcH7:p75NTR:(NGF-p13)2:trkAp62{pY490}{ub{K63}(1)K485})");
        knownErrors.put("TNF-alpha{sol}(h)", "TNF-alpha(h){sol}");
        knownErrors.put("Mcl-1L(h){ub}n-1", "Mcl-1L(h)n-1{ub}");
        for( String title : titles )
        {
            TitleElement titleElement = new TitleElement(title);
            assertEquals(title + ": easy", title, titleElement.toString());
            String titleConstructed = titleElement.getTitleConstructed();
            if( !title.equals(titleConstructed) && ( !knownErrors.containsKey(title) || !knownErrors.get(title).equals(titleConstructed) ) )
                fail("New error: " + title + " > " + titleConstructed);
        }
    }
}
