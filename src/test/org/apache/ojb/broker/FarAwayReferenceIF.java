package org.apache.ojb.broker;

import java.io.Serializable;

/**
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: FarAwayReferenceIF.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public interface FarAwayReferenceIF extends Serializable
{
    int getId();

    void setId(int id);

    String getName();

    void setName(String name);

}
