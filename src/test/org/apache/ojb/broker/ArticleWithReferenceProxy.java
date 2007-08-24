package org.apache.ojb.broker;

/**
 * @ojb.class table="Artikel"
 * @ojb.modify-inherited name="productGroup"
 *                       proxy="true"
 *                       auto-update="true"
 */
public class ArticleWithReferenceProxy extends Article
{
	public ArticleWithReferenceProxy()
	{
		super();
	}
}
