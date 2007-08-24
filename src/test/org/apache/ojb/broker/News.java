package org.apache.ojb.broker;

/**
 * This interface is used to test extent aware path expressions 
 * @author <a href="leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz</a>
 */
public class News 
    extends BaseContentImpl
{
    
    public News()
    {
        super();
    }
    
    /**
     * @param i
     */
    public News(int id)
    {
        super(id);
    }

    /**
     * 
     */
    private String headline;    


    /**
     * @return
     */
    public String getHeadline()
    {
        return headline;
    }

    /**
     * @param string
     */
    public void setHeadline(String headline)
    {
        this.headline = headline;
    }

}
