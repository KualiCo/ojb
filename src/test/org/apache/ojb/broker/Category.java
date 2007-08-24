package org.apache.ojb.broker;

/**
 * 
 * @author <a href="leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz</a>
 */
public class Category extends BaseQualifierImpl
{
    private String description;

    public Category()
    {
        super();
    }
    
    public Category(int id)
    {
        super(id);
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String value)
    {
        description = value;
    }
}
