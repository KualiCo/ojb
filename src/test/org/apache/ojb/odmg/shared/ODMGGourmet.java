package org.apache.ojb.odmg.shared;

import org.apache.ojb.broker.Gourmet;

/**
 * class used to test polymorphic m:n collections (ODMG-variant)
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public class ODMGGourmet extends Gourmet
{
    public ODMGGourmet()
    {
    }

    public ODMGGourmet(String name)
    {
        super(name);
    }
}
