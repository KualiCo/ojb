package org.apache.ojb.broker;

/**
 * This class is used to test extent aware path expressions  
 * @author <a href="leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz</a>
 */
public abstract class BaseQualifierImpl 
    implements Qualifier
{
    private int id;
    
    private String name;

    public BaseQualifierImpl()
    {
    }
    
    /**
     * @param id
     */
    public BaseQualifierImpl(int id)
    {
        this.id = id;
    }

    
    /**
     * @see org.apache.ojb.broker.Qualifier#getId()
     */
    public int getId()
    {
        return id;
    }

    /**
     * @see org.apache.ojb.broker.Qualifier#setId(int)
     */
    public void setId(int id)
    {
        this.id = id;
    }

    /**
     * @see org.apache.ojb.broker.Qualifier#getName()
     */
    public String getName()
    {
        return name;
    }

    /**
     * @see org.apache.ojb.broker.Qualifier#setName(java.lang.String)
     */
    public void setName(String name)
    {
        this.name = name;
    }
    
    public String toString()
    {
        return getClass().getName() + "("+id+")";
    }

}
