package org.apache.ojb.broker;

import java.util.List;

/**
 * 
 * @author <a href="leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz</a>
 */
public abstract class BaseContentImpl 
    implements Content
{
    /**
     * 
     */
    private int id;
    
    private List qualifiers;

    public BaseContentImpl()
    {
    }
    

    /**
     * @param id
     */
    public BaseContentImpl(int id)
    {
        this.id = id;
    }

    /**
     * @see org.apache.ojb.broker.Content#getId()
     */
    public int getId()
    {
        return id;
    }

    /**
     * @see org.apache.ojb.broker.Content#setId(int)
     */
    public void setId(int id)
    {
        this.id = id;
    }

     public List getQualifiers()
     {
       return qualifiers;
     }

     public void setQualifiers(List qualifiers)
     {
       this.qualifiers = qualifiers;
     }
}
