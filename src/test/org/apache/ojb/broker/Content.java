package org.apache.ojb.broker;

import java.io.Serializable;

/**
 * This interface is used to test extent aware path expressions
 * @author <a href="leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz</a>
 */
public interface Content extends Serializable
{
    int getId();

    void setId(int id);
}
