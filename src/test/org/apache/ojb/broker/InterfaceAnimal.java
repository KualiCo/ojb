package org.apache.ojb.broker;

import java.io.Serializable;

/**
 * This interface is used to test polymorph collections
 * @author <a href="mailto:schneider@mendel.imp.univie.ac.at">Georg Schneider</a>
 *
 */
public interface InterfaceAnimal extends Serializable
{
    int getAge();
    String getName();
}
