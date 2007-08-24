package org.apache.ojb.broker;


/**
 * @ojb.class table="Artikel"
 *            proxy="dynamic"
 *            include-inherited="true"
 */
public class Article extends AbstractArticle implements InterfaceArticle, java.io.Serializable
{
	protected Article()
	{
	}
	
	public static Article createInstance()
	{
		return new Article();
	}
}
