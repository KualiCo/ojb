/*
 * ObJectRelationalBridge - Bridging Java Objects and Relational Databases
 * http://objectbridge.sourceforge.net
 * Copyright (C) 2000, 2001 Thomas Mahler, et al.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

package org.apache.ojb.broker;

public class PersonUnidirectional extends Person
{

	/**
	 * Constructor for PersonUnidirectional.
	 */
	public PersonUnidirectional()
	{
		super();
	}

	/**
	 * Constructor for PersonUnidirectional.
	 * @param pId
	 * @param pFirstname
	 * @param pLastname
	 */
	public PersonUnidirectional(int pId, String pFirstname, String pLastname)
	{
		super(pId, pFirstname, pLastname);
	}

}

