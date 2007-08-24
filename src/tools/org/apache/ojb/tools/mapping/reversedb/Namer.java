package org.apache.ojb.tools.mapping.reversedb;

/* Copyright 2003-2005 The Apache Software Foundation
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
 * @author <a href="mailto:wk5657@henleykidd.ns01.us">G. Wayne Kidd</a>
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class Namer
{
	/* (non-Javadoc)
	 * @see org.apache.ojb.tools.mapping.reversedb.Namer#nameClass(java.lang.String, java.lang.String)
	 */
	public static String nameClass(String tableName) 
    {
		StringBuffer sb = new StringBuffer();
		char[] chars = new char[tableName.length()];
		chars = tableName.toCharArray();
		char c;
		boolean nextup = false;
		for (int i = 0; i < chars.length; i++) {
			if (i==0) c = Character.toUpperCase(chars[i]);
			else if (chars[i]=='_') {
				 nextup = true;
				 continue;
			}
			else if (nextup) {
				nextup = false;
				c = Character.toUpperCase(chars[i]);
			} 
			else c = Character.toLowerCase(chars[i]);
			sb.append(c);
		}
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see org.apache.ojb.tools.mapping.reversedb.Namer#nameField(java.lang.String, java.lang.String)
	 */
	public static String nameField(String columnName)
    {
		StringBuffer sb = new StringBuffer();
		char[] chars = new char[columnName.length()];
		chars = columnName.toCharArray();
		char c;
		boolean nextup = false;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i]=='_') {
				 nextup = true;
				 continue;
			}
			else if (nextup) {
				nextup = false;
				c = Character.toUpperCase(chars[i]);
			} 
			else c = Character.toLowerCase(chars[i]);
			sb.append(c);
		}
		return sb.toString();
	}
}
