package org.apache.ojb.broker.util;

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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

/**
 * Helper class for all SQL related stuff.
 * 
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel </a>
 * @version $Id: SqlHelper.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class SqlHelper
{
    /** define the name of the pseudo column holding the class to be instantiated. */
    public static final String OJB_CLASS_COLUMN = "OJB_CLAZZ"; 

    /**
	 * Helper Class for a split column <br>
	 * ie: sum (distinct amount) as theAmount
	 * 
	 * <pre>
	 *  prefix = 'sum (distinct '
	 *  column = 'amount'
	 *  suffix = ') as theAmount'
	 * </pre>
	 */
	public static final class PathInfo
	{
		public String column;
		public String prefix;
		public String suffix;
        public final String path;  //original Path

		PathInfo(String aPath, String aPrefix, String aColumn, String aSuffix)
        {
            path = aPath;
			column = aColumn;
			prefix = aPrefix;
			suffix = aSuffix;
		}
	}

	/**
	 * remove functions and () from path <br>
	 * ie: avg(amount) -> amount <br>
	 * ie: sum (accounts.amount) -> accounts.amount <br>
	 * ie: count(distinct id) as theCount-> id <br>
	 * 
	 * @param aPath
	 *            the path to the attribute
	 */
	public static String cleanPath(String aPath)
	{
		return splitPath(aPath).column;
	}

    /**
     * Split a path into column , prefix and suffix, the prefix contains all
     * info up to the column <br>
     * ie: avg(amount) -> amount , avg( , )<br>
     * ie: sum (accounts.amount) as theSum -> accounts.amount , sum( , ) as
     * theSum <br>
     * ie: count( distinct id ) as bla -> id , count(distinct , ) as bla <br>
     * Supports simple expressions ie: price * 1.05
     * 
     * TODO: cannot resolve multiple attributes in expression 
     * ie: price - bonus
     * 
     * @param aPath
     * @return PathInfo
     */
    public static PathInfo splitPath(String aPath)
    {
        String prefix = null;
        String suffix = null;
        String colName = aPath;
 
        if (aPath == null)
        {
            return new PathInfo(null, null, null, null);
        }

        // ignore leading ( and trailing ) ie: sum(avg(col1))
        int braceBegin = aPath.lastIndexOf("(");
        int braceEnd = aPath.indexOf(")");
        int opPos = StringUtils.indexOfAny(aPath, "+-/*");

        if (braceBegin >= 0 && braceEnd >= 0 && braceEnd > braceBegin)
        {
            int colBegin;
            int colEnd;
            String betweenBraces;

            betweenBraces = aPath.substring(braceBegin + 1, braceEnd).trim();
            // look for ie 'distinct name'
            colBegin = betweenBraces.indexOf(" ");
            // look for multiarg function like to_char(col,'format_mask')
            colEnd = betweenBraces.indexOf(",");
            colEnd = colEnd > 0 ? colEnd : betweenBraces.length();
            prefix = aPath.substring(0, braceBegin + 1) + betweenBraces.substring(0, colBegin + 1);
            colName = betweenBraces.substring(colBegin + 1, colEnd);
            suffix = betweenBraces.substring(colEnd) + aPath.substring(braceEnd);
        }
        else if (opPos >= 0)
        {
            colName = aPath.substring(0, opPos).trim();
            suffix = aPath.substring(opPos);
        }
        
        return new PathInfo(aPath, prefix, colName.trim(), suffix);
    }
    
    /**
     * Returns the name of the class to be instantiated.
     * @param rs the Resultset
     * @return null if the column is not available
     */
    public static String getOjbClassName(ResultSet rs)
    {
        try
        {
            return rs.getString(OJB_CLASS_COLUMN);
        }
        catch (SQLException e)
        {
            return null;
        }
    }

}
