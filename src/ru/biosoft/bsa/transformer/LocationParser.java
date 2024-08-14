package ru.biosoft.bsa.transformer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

public class LocationParser
{
    private final static int   TYPE_LEFT_BRACKET   = 0 ;
    private final static int   TYPE_RIGHT_BRACKET  = 1 ;
    private final static int   TYPE_KRYGIK         = 2 ;
    private final static int   TYPE_LESS           = 3 ;
    private final static int   TYPE_DOT            = 4 ;
    private final static int   TYPE_COMMA          = 5 ;
    private final static int   TYPE_COLON          = 6 ;
    private final static int   TYPE_JOIN           = 7 ;
    private final static int   TYPE_COMPLEMENT     = 8 ;
    private final static int   TYPE_ORDER          = 9 ;
    private final static int   TYPE_NUMBER         = 10 ;
    private final static int   TYPE_REFERENCE      = 11 ;
    private final static int   TYPE_EOLN           = 12 ;
    private final static int   TYPE_GREATER        = 13 ;

    private final static Token TOKEN_LEFT_BRACKET  = new Token(TYPE_LEFT_BRACKET );
    private final static Token TOKEN_RIGHT_BRACKET = new Token(TYPE_RIGHT_BRACKET);
    private final static Token TOKEN_KRYGIK        = new Token(TYPE_KRYGIK       );
    private final static Token TOKEN_LESS          = new Token(TYPE_LESS         );
    private final static Token TOKEN_DOT           = new Token(TYPE_DOT          );
    private final static Token TOKEN_COMMA         = new Token(TYPE_COMMA        );
    private final static Token TOKEN_COLON         = new Token(TYPE_COLON        );
    private final static Token TOKEN_JOIN          = new Token(TYPE_JOIN         );
    private final static Token TOKEN_COMPLEMENT    = new Token(TYPE_COMPLEMENT   );
    private final static Token TOKEN_ORDER         = new Token(TYPE_ORDER        );
    private final static Token TOKEN_EOLN          = new Token(TYPE_EOLN         );
    private final static Token TOKEN_GREATER       = new Token(TYPE_GREATER      );

    public LocationParser(String curName,String location)
    {
        this.curName = curName;
        tokenizer = new Tokenizer( location );
    }

    void storeCurrSites()
    {
        sites.addAll( currSites );
        currSites.clear();
    }

    public List<Site> parse() throws Exception
    {
        parseLocation();
        return sites;
    }


    void parseLocation() throws Exception
    {
        Token  token  = tokenizer.next();

        switch (token.type)
        {
            case  TYPE_LESS          :
            case  TYPE_NUMBER        :
            case  TYPE_LEFT_BRACKET  :
                tokenizer.back();
                parseStartSite();
                parseMiddleSite();
                parseEndSite();
                storeCurrSites();
                fBetween = false;
                return ;
            case  TYPE_ORDER         :
            case  TYPE_JOIN          :
                parseJoin();
                return;
            case  TYPE_COMPLEMENT    :
                parseComplement();
                return;
            case  TYPE_REFERENCE     :
                // Skip 'replace' from old EMBL format
                if( token.value.equals("replace") )
                    return;
                parseReference();
                return  ;

            default: error(token);
        }
    }

    void error(Token token) throws Exception
    {
        error("Unexpected token: ",token);
    }

    void error(String message,Token token) throws Exception
    {
        throw new Exception(message+token.type+","+token.value);
    }

    /**
     * @todo Rename variables to common code style.
     */
    public static class Site implements Cloneable
    {
        public String         type  ;
        public int            start ;
        public int            length = 1;
        public String         name;
        public int            strand;
        public DynamicPropertySet siteProperties = new DynamicPropertySetAsMap();
        public boolean        startBefore;
        public boolean        fBetween;
        public boolean        fEndsAfter;

        public Site()
        {}

        public Site( String name,String type,int start,int length,int strand,
                     boolean startBefore,boolean between,boolean endsAfter )
        {
            this.name        = name;
            this.type        = type;
            this.start       = start;
            this.length      = length;
            this.strand      = strand;
            this.startBefore = startBefore;
            this.fBetween    = between;
            this.fEndsAfter  = endsAfter;
        }

        @Override
        public String toString()
        {
            return "Site "+name+" start="+start+" length="+length+" strand="+strand+" startBefore="+startBefore+", between="+fBetween+",fEndsAfter="+fEndsAfter;//+" map="+map;
        }

        @Override
        public synchronized Site clone()
        {
            try
            {
                return (Site)super.clone();
            } catch (CloneNotSupportedException e)
            {
                throw new InternalError();
            }
        }
    }

    class Tokenizer implements Iterator<Token>
    {
        int curPos = 0;
        StringTokenizer  strTok ;

        Tokenizer(String str)
        {
            strTok = new StringTokenizer(str,"<>.()^,: \t\n",true);
        }

        public void back()
        {
            fBack = true;
        }

        @Override
        public boolean hasNext()
        {
            if (fBack)
                return true;

            return strTok.hasMoreTokens();
        }

        private Token prev;
        private boolean fBack = false;
        @Override
        public Token next()
        {
            if (fBack)
            {
                fBack = false;
                return prev;
            }

            if (!strTok.hasMoreTokens())
                return TOKEN_EOLN;

            String token = strTok.nextToken();

            switch (token.charAt(0))
            {
                case ' '  :
                case '\t' :
                case '\n' :  prev=next()       ; break;
                case '('   : prev=TOKEN_LEFT_BRACKET  ; break;
                case ')'   : prev=TOKEN_RIGHT_BRACKET ; break;
                case '^'   : prev=TOKEN_KRYGIK        ; break;
                case '<'   : prev=TOKEN_LESS          ; break;
                case '>'   : prev=TOKEN_GREATER       ; break;
                case '.'   : prev=TOKEN_DOT           ; break;
                case ','   : prev=TOKEN_COMMA         ; break;
                case ':'   : prev=TOKEN_COLON         ; break;
                default:
                    if      (token.equalsIgnoreCase("join"))        prev=TOKEN_JOIN;
                    else if (token.equalsIgnoreCase("complement"))  prev=TOKEN_COMPLEMENT;
                    else if (token.equalsIgnoreCase("order"))       prev=TOKEN_ORDER;
                    else if ((prev=isNumber(token))==null )         prev=new Token(TYPE_REFERENCE,token);
            }
            return prev;
        }
    }

    Token isNumber(String s)
    {
        try
        {
            return new Token(TYPE_NUMBER,Integer.parseInt(s,10));
        }
        catch( Exception exc )
        {
        }
        return null;
    }

    void parseJoin() throws Exception
    {
        Token  token  = tokenizer.next();

        if (token != TOKEN_LEFT_BRACKET)
            error("No bracket was:",token);

        do
        {
            parseLocation();
            token = tokenizer.next();
        }
        while (token==TOKEN_COMMA);

        if (token != TOKEN_RIGHT_BRACKET)
            error("No closed bracket was",token);

        return ;
    }

    void parseComplement()  throws Exception
    {
        Token  token  = tokenizer.next();
        if (token != TOKEN_LEFT_BRACKET)
            error("No bracket,was: ",token);

        fComplement = true;
        parseLocation();
        fComplement = false;
        if (tokenizer.next() != TOKEN_RIGHT_BRACKET)
            error("No closed bracket,was: ",token);
        return ;
    }

    void parseReference()  throws Exception
    {
        Token  token  = tokenizer.next();
        if (token != TOKEN_COLON)
            error("No colon was: ",token);

        token  = tokenizer.next();

        boolean bracketOpened = false;
        if (token == TOKEN_LEFT_BRACKET)
        {
            bracketOpened = true;
            // In some sequences bracket absents after reference
            // error("No open bracket: ",token); // Old code
        }
        else
            tokenizer.back();

        parseLocation();
        if( bracketOpened )
        {
            token  = tokenizer.next();
            if (token != TOKEN_RIGHT_BRACKET)
                error("No close bracket: ",token);
        }
    }

    void parseMiddleSite() throws Exception
    {
        Token token  = tokenizer.next();
        switch (token.type)
        {
            case TYPE_DOT:
                token  = tokenizer.next();
                if (token != TOKEN_DOT)
                    error("Dot expected,was: ",token);
                return;

            case  TYPE_KRYGIK :
                fBetween = true;
                return;

            default: tokenizer.back();

        }
    }


    Tokenizer tokenizer ;

    void parseEndSite() throws Exception
    {
        Token  token  = tokenizer.next();

        switch (token.type)
        {
            case  TYPE_GREATER :
                fEndsAfter = true;
                parseEndSite();
                return;

            case  TYPE_LEFT_BRACKET :
                List<Site> newSites = new ArrayList<>( currSites );
                currSites.clear();
                do
                {
                    token  = tokenizer.next();

                    if (token.type != TYPE_NUMBER)
                        error(token);

                    int end = token.number;//Integer.parseInt(token.value);

                    for (Site site : StreamEx.of( newSites ).map( Site::clone ))
                    {
                        site.length = end-site.start+1;
                        site.fBetween = fBetween;
                        currSites.add(site);
                    }

                    token  = tokenizer.next();

                }while (token==TOKEN_DOT);

                if (token.type!=TYPE_RIGHT_BRACKET)
                    error("No close bracket,was: ",token);

                return;

            case  TYPE_NUMBER :
                int end = token.number;//Integer.parseInt(token.value);
                for (int i=0; i<currSites.size(); i++)
                {
                    Site site   = currSites.get(i);
                    site.length = end-site.start+1;
                    site.fBetween   = fBetween;
                    site.fEndsAfter = fEndsAfter;
                }
                fEndsAfter = false;
                return;

            default: tokenizer.back();
        }
    }

    void parseStartSite() throws Exception
    {
        Token token  = tokenizer.next();
        switch (token.type)
        {
            case  TYPE_LESS          :
                fStartBefore = true;
                parseStartSite();
                return ;

            case  TYPE_LEFT_BRACKET  :
                do
                {
                    token  = tokenizer.next();
                    if (token.type != TYPE_NUMBER)
                        error("Number expected,was:",token);

                    createSite(curName,token.number);//Integer.parseInt(token.value));

                    token  = tokenizer.next();
                }while (token == TOKEN_DOT);

                if (token != TOKEN_RIGHT_BRACKET)
                    error("Close bracket expected was:",token);

                return ;


            case TYPE_NUMBER :
                createSite(curName,token.number);//Integer.parseInt(token.value));
                return ;

            default: error(token);
        }
    }

    public static class Token
    {
        int    type ;
        String value;
        int    number;
        Token(int type)
        {
            this(type,null);
        }
        Token(int type,String value )
        {
            this.type   = type  ;
            this.value  = value ;
        }
        Token(int type,int number )
        {
            this.type   = type  ;
            this.number = number ;
        }
        @Override
        public String toString()
        {
            String str = null;

            switch(type)
            {
                case  TYPE_LEFT_BRACKET   :  str="TYPE_LEFT_BRACKET " ;      break;
                case  TYPE_RIGHT_BRACKET  :  str="TYPE_RIGHT_BRACKET" ;      break;
                case  TYPE_KRYGIK         :  str="TYPE_KRYGIK"        ;      break;
                case  TYPE_LESS           :  str="TYPE_LESS"          ;      break;
                case  TYPE_DOT            :  str="TYPE_DOT"           ;      break;
                case  TYPE_COMMA          :  str="TYPE_COMMA"         ;      break;
                case  TYPE_COLON          :  str="TYPE_COLON"         ;      break;
                case  TYPE_JOIN           :  str="TYPE_JOIN"          ;      break;
                case  TYPE_COMPLEMENT     :  str="TYPE_COMPLEMENT"    ;      break;
                case  TYPE_ORDER          :  str="TYPE_ORDER"         ;      break;
                case  TYPE_NUMBER         :  str="TYPE_NUMBER"        ;      break;
                case  TYPE_REFERENCE      :  str="TYPE_REFERENCE"     ;      break;
                case  TYPE_EOLN           :  str="TYPE_EOLN"          ;      break;
                case  TYPE_GREATER        :  str="TYPE_GREATER"       ;      break;
            }
            return str+",("+value+")";
        }
    }

    void createSite(String name,int start)
    {
        Site site        = new Site();
        site.type        = name ;
        site.name        = name+'_'+generator++;
        site.start       = start;
        site.strand      = fComplement ? ru.biosoft.bsa.Site.STRAND_MINUS : ru.biosoft.bsa.Site.STRAND_PLUS;
        site.fBetween    = fBetween;
        site.startBefore = fStartBefore ;
        fStartBefore     = false;
        currSites.add( site );
    }

    List<Site> sites = new ArrayList<>();
    List<Site> currSites = new ArrayList<>();

    private boolean fComplement  = false;
    private boolean fBetween     = false;
    private boolean fEndsAfter   = false;
    private boolean fStartBefore = false;
    private final String  curName ;
    private int     generator    = 1;
}
