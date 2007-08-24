package org.apache.ojb.broker;

/**
 * Insert the type's description here.
 * Creation date: (02.02.2001 15:45:59)
 * @author Thomas Mahler
 */
public class OrderPosition implements java.io.Serializable
{
    private int id;
    private int order_id;
    private int article_id;
    private InterfaceArticle article;

    /**
     * OrderPosition constructor comment.
     */
    public OrderPosition()
    {
        super();
    }

    public OrderPosition(int pId, int pOrderId, int pArticleId)
    {
        id = pId;
        order_id = pOrderId;
        article_id = pArticleId;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02.02.2001 22:25:25)
     * @return TestThreadsNLocks.org.apache.ojb.broker.InterfaceArticle
     */
    public InterfaceArticle getArticle()
    {
        return article;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02.02.2001 22:25:25)
     * @return int
     */
    public int getArticle_id()
    {
        return article_id;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02.02.2001 22:25:25)
     * @return int
     */
    public int getId()
    {
        return id;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02.02.2001 22:25:26)
     * @return int
     */
    public int getOrder_id()
    {
        return order_id;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02.02.2001 22:25:25)
     * @param newArticle TestThreadsNLocks.org.apache.ojb.broker.InterfaceArticle
     */
    public void setArticle(InterfaceArticle newArticle)
    {
        article = newArticle;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02.02.2001 22:25:25)
     * @param newArticle_id int
     */
    public void setArticle_id(int newArticle_id)
    {
        article_id = newArticle_id;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02.02.2001 22:25:26)
     * @param newId int
     */
    public void setId(int newId)
    {
        id = newId;
    }

    /**
     * Insert the method's description here.
     * Creation date: (02.02.2001 22:25:26)
     * @param newOrder_id int
     */
    public void setOrder_id(int newOrder_id)
    {
        order_id = newOrder_id;
    }

    public String toString()
    {
        return "" + id + ", " + article_id + ", " + article.getArticleName();
    }
}
