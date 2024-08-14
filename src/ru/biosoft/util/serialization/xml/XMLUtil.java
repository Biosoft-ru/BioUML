/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the reusable ccl java library
 * (http://www.kclee.com/clemens/java/ccl/).
 *
 * The Initial Developer of the Original Code is
 * Chr. Clemens Lee.
 * Portions created by Chr. Clemens Lee are Copyright (C) 2002
 * Chr. Clemens Lee. All Rights Reserved.
 *
 * Contributor(s): Chr. Clemens Lee <clemens@kclee.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package ru.biosoft.util.serialization.xml;

/**
 * This class provides generic helper methods to deal
 * with xml or xslt processing.<p>
 * 
 * Credit: code to escape a string so it can be used as xml
 *         text content or an attribute value is taken from
 *         the Apache Xerces project.
 *
 * @author    Chr. Clemens Lee (mailto:clemens@kclee.com)
 */
public class XMLUtil
{
    /**
     * Identifies the last printable character in the Unicode range
     * that is supported by the encoding used with this serializer.
     * For 8-bit encodings this will be either 0x7E or 0xFF.
     * For 16-bit encodings this will be 0xFFFF. Characters that are
     * not printable will be escaped using character references.
     */
    static private int _lastPrintable = 0x7E;

    /**
     * Encode special XML characters into the equivalent character references.
     * These five are defined by default for all XML documents.
     * Converts '<', '>', '"'. and '\'' to "lt", "gt", "quot", or "apos".
     */
    static private String getEntityRef( char ch )
    {
        switch ( ch )
        {
            case '<':
                return "lt";
            case '>':
                return "gt";
            case '"':
                return "quot";
            case '\'':
                return "apos";
            case '&':
                return "amp";
            default:
                return null;
        }
    }

    /**
     * This is an utility class which should never be
     * instantiated.
     */
    private XMLUtil()
    {
        super();
    }

    /**
     * If there is a suitable entity reference for this
     * character, return it. The list of available entity
     * references is almost but not identical between
     * XML and HTML.
     */
    static public String escape( char ch )
    {
        String charRef;

        charRef = getEntityRef( ch );
        if ( charRef != null )
        {
            return "&" + charRef + ";";
        }
        else if ( ( ch >= ' ' && ch <= _lastPrintable && ch != 0xF7 ) ||
                  ch == '\n' || ch == '\r' || ch == '\t' )
        {
            // If the character is not printable, print as character reference.
            // Non printables are below ASCII space but not tab or line
            // terminator, ASCII delete, or above a certain Unicode threshold.
            return "" + ch;
        }
        else
        {
            return "&#"
                   + Integer.toString( ch )
                   + ";";
        }
    }

    /**
     * Escapes a string so it may be returned as text content or attribute
     * value. Non printable characters are escaped using character references.
     * Where the format specifies a deault entity reference, that reference
     * is used (e.g. <tt>&amp;lt;</tt>).
     *
     * @param   source   the string to escape or "" for null.
     */
    static public String escape( String source )
    {
        if ( source == null )
        {
            return "";
        }

        StringBuffer pBuffer = new StringBuffer();

        for ( int i = 0 ; i < source.length() ; ++i )
        {
            pBuffer.append( escape( source.charAt( i ) ) );
        }

        return pBuffer.toString();
    }
}
