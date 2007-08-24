package org.apache.ojb.odmg.shared;

import java.io.Serializable;
import java.util.Locale;

public class TestClassA implements Serializable
{
	private String oid;
	private String value1;
	private String value2;
	private int 	value3;
	private Locale locale = Locale.GERMANY;

	private String 	boid;
	private TestClassB b;
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
	 * Gets the value2.
	 * @return Returns a String
	 */
	public String getValue2()
	{
		return value2;
	}

	/**
	 * Sets the value2.
	 * @param value2 The value2 to set
	 */
	public void setValue2(String value2)
	{
		this.value2 = value2;
	}

	/**
	 * Gets the value3.
	 * @return Returns a int
	 */
	public int getValue3()
	{
		return value3;
	}

	/**
	 * Sets the value3.
	 * @param value3 The value3 to set
	 */
	public void setValue3(int value3)
	{
		this.value3 = value3;
	}

	/**
	 * Gets the b.
	 * @return Returns a TestClassB
	 */
	public TestClassB getB()
	{
		return b;
	}

	/**
	 * Sets the b.
	 * @param b The b to set
	 */
	public void setB(TestClassB b)
	{
		this.b = b;
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
	 * Gets the bOid.
	 * @return Returns a String
	 */
	public String getBoid()
	{
		return boid;
	}

	/**
	 * Sets the bOid.
	 * @param bOid The bOid to set
	 */
	public void setBoid(String boid)
	{
		this.boid = boid;
	}

	public Locale getLocale()
	{
		return locale;
	}

	public void setLocale(Locale locale)
	{
		this.locale = locale;
	}
}
