package org.apache.ojb.broker.query;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Model a Like Criteria</br> 
 * Escape Processing by Paul R. Nase 
 * <p>
 * The pattern string is a simple pattern string using % or * as a wildcard.
 * So Ander* would match Anderson and Anderton. The _ or ? character is used to match a single occurence
 * of a character. The '\' is used to escape the wildcard characters so that we can search for
 * strings containing * and ?. 
 * <p>
 * To change the escape character use setEscapeCharacter. 
 * @see LikeCriteria#setEscapeCharacter(char)
 * 
 * @author <a href="mailto:jbraeuchi@gmx.ch">Jakob Braeuchi </a>
 * @author <a href="mailto:Nase.Paul@mayo.edu">Paul Nase </a>
 * @version $Id: LikeCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class LikeCriteria extends ValueCriteria
{
    /**
     * The Dfault-Character used for Escaping Wildcards
     */
    public static final char DEFAULT_ESCPAPE_CHARACTER = '\\';

    /**
     * The Character used for Escaping Wildcards
     */
    private static char escapeCharacter = DEFAULT_ESCPAPE_CHARACTER;

	/**
	 * @param anAttribute
	 * @param aValue
	 * @param aClause
	 * @param anAlias
	 */
	public LikeCriteria(Object anAttribute, Object aValue, String aClause, String anAlias)
	{
		super(anAttribute, generateSQLSearchPattern(aValue), aClause, anAlias);
	}

	/**
	 * @param anAttribute
	 * @param aValue
	 * @param aClause
	 * @param anAlias
	 */
	public LikeCriteria(Object anAttribute, Object aValue, String aClause, UserAlias anAlias)
	{
		super(anAttribute, generateSQLSearchPattern(aValue), aClause, anAlias);
	}

    /**
     * @see org.apache.ojb.broker.query.SelectionCriteria#bind(java.lang.Object)
     */
    public void bind(Object newValue)
    {
        super.bind(generateSQLSearchPattern(newValue));
    }

    /**
     * Generate a SQL search string from the pattern string passed. 
     * The pattern string is a simple pattern string using % or * as a wildcard. 
     * So Ander* would match Anderson and Anderton. The _ or ? character is used to match a single occurence
     * of a character. The escapeCharacter is used to escape the wildcard characters so that we can search for
     * strings containing * and ?. This method converts the criteria wildcard strings to SQL wildcards.
     * 
     * @param pattern a criteria search pattern containing optional wildcards
     * @return a SQL search pattern string with all escape codes processed.
     */
	private static String generateSQLSearchPattern(Object pattern)
	{
		if (pattern == null)
		{
			return null;
		}
		else
		{
			StringBuffer sqlpattern = new StringBuffer();
			char[] chars = pattern.toString().toCharArray();

			for (int i = 0; i < chars.length; i++)
			{
				if (chars[i] == escapeCharacter)
				{
					// for the escape character add the next char as is.
					// find the next non-escape character.
					int x = i + 1;
					for (;(x < chars.length); x++)
					{
						if (chars[x] != escapeCharacter)
						{
							break;
						}
					}
					boolean oddEscapes = (((x - i) % 2) > 0) ? true : false;
					if (oddEscapes)
					{
						// only escape characters allowed are '%', '_', and '\'
						// if the escaped character is a '\', then oddEscapes
						// will be false.
						// if the character following this last escape is not a
						// '%' or an '_', eat this escape character.
						if ((x < chars.length)
							&& ((chars[x] == '%') || (chars[x] == '_')))
						{
							// leave the escape character in, along with the following char
							x++;
						}
						else
						{
							// remove the escape character, will cause problems in sql statement.
							i++; // removing the first escape character.
							if ((x < chars.length)
								&& ((chars[x] == '*') || (chars[x] == '?')))
							{
								// but if it is a '*' or a '?', we want to keep these
								// characters as is, they were 'escaped' out.
								x++; // include the first non-escape character.
							}
						}
					}
					if (i < chars.length)
					{
						sqlpattern.append(chars, i, x - i);
					}
					i = x - 1; // set index to last character copied.
				}
				else if (chars[i] == '*')
				{
					sqlpattern.append("%");
				}
				else if (chars[i] == '?')
				{
					sqlpattern.append("_");
				}
				else
				{
					sqlpattern.append(chars[i]);
				}
			}
			return sqlpattern.toString();
		}
	}

    /**
     * @return Returns the escapeCharacter.
     */
    public static char getEscapeCharacter()
    {
        return escapeCharacter;
    }
    
    /**
     * Global change of the escapeCharacter
     * @param escChar The escapeCharacter to set.
     */
    public static void setEscapeCharacter(char escChar)
    {
        escapeCharacter = escChar;
    }
    
}
