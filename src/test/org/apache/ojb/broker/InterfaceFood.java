package org.apache.ojb.broker;

import java.io.Serializable;

/**
 * interface used for testing polymorphic m:n collections
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public interface InterfaceFood extends Serializable
{
    String getName();
    int getCalories();

}
