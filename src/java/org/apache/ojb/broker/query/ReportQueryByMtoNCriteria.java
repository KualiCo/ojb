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
 * ReportQuery using MtoNCriteria, for internal use
 * 
 * @author <a href="mailto:jbraeuchi@hotmail.com">Jakob Braeuchi</a>
 * @version $Id: ReportQueryByMtoNCriteria.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class ReportQueryByMtoNCriteria extends ReportQueryByCriteria implements MtoNQuery
{
    private String indirectionTable;

	/**
	 * Constructor for ReportQueryByMtoNCriteria.
	 * @param targetClass
	 * @param columns
	 * @param criteria
	 * @param distinct
	 */
	public ReportQueryByMtoNCriteria(Class targetClass, String[] columns, Criteria criteria, boolean distinct)
	{
		super(targetClass, columns, criteria, distinct);
	}

	/**
	 * Constructor for ReportQueryByMtoNCriteria.
	 * @param targetClass
	 * @param columns
	 * @param criteria
	 */
	public ReportQueryByMtoNCriteria(Class targetClass, String[] columns, Criteria criteria)
	{
		super(targetClass, columns, criteria);
	}

	/**
	 * Constructor for ReportQueryByMtoNCriteria.
	 * @param targetClass
	 * @param criteria
	 */
	public ReportQueryByMtoNCriteria(Class targetClass, Criteria criteria)
	{
		super(targetClass, criteria);
	}

	/**
	 * Constructor for ReportQueryByMtoNCriteria.
	 * @param targetClass
	 * @param criteria
	 * @param distinct
	 */
	public ReportQueryByMtoNCriteria(Class targetClass, Criteria criteria, boolean distinct)
	{
		super(targetClass, criteria, distinct);
	}

	/**
	 * @see org.apache.ojb.broker.query.MtoNQuery#getIndirectionTable()
	 */
	public String getIndirectionTable()
	{
		return indirectionTable;
	}

	/**
	 * Sets the indirectionTable.
	 * @param indirectionTable The indirectionTable to set
	 */
	public void setIndirectionTable(String indirectionTable)
	{
		this.indirectionTable = indirectionTable;
	}

}
