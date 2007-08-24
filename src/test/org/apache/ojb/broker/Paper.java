package org.apache.ojb.broker;

/**
 * This interface is used to test extent aware path expressions 
 * @author <a href="leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz</a>
 */
public class Paper 
    extends BaseContentImpl
{
    /**
     * 
     */
    private String date;
    
    /**
     * 
     */
    private String author;
    
    
    /**
     * @return
     */
    public String getAuthor()
    {
        return author;
    }

    /**
     * @return
     */
    public String getDate()
    {
        return date;
    }

    /**
     * @param string
     */
    public void setAuthor(String author)
    {
        this.author = author;
    }

    /**
     * @param string
     */
    public void setDate(String date)
    {
        this.date = date;
    }

}
