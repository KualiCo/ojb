package org.apache.ojb.odmg.shared;

import java.io.Serializable;

public class TestClassB implements Serializable
{
	private String oid;
	private String value1;
	
	private String aoid;
	private TestClassA a;
	/**
	 * Gets the value1.
	 * @return Returns a String
	 */
	public String getValue1()
	{
		return value1;
	}

	/**
	 * Sets the value1.
	 * @param value1 The value1 to set
	 */
	public void setValue1(String value1)
	{
		this.value1 = value1;
	}

	/**
	 * Gets the a.
	 * @return Returns a TestClassA
	 */
	public TestClassA getA()
	{
		return a;
	}

	/**
	 * Sets the a.
	 * @param a The a to set
	 */
	public void setA(TestClassA a)
	{
		this.a = a;
	}

	/**
	 * Gets the oid.
	 * @return Returns a String
	 */
	public String getOid()
	{
		return oid;
	}

	/**
	 * Sets the oid.
	 * @param oid The oid to set
	 */
	public void setOid(String oid)
	{
		this.oid = oid;
	}

	/**
	 * Gets the aOid.
	 * @return Returns a String
	 */
	public String getAoid()
	{
		return aoid;
	}

	/**
	 * Sets the aOid.
	 * @param aoid The aOid to set
	 */
	public void setAoid(String aoid)
	{
		this.aoid = aoid;
	}
}
