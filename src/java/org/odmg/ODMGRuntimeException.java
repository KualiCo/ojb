package org.odmg;/** * This is the base class for all RuntimeExceptions thrown by an ODMG implementation. * @author	David Jordan (as Java Editor of the Object Data Management Group) * @version ODMG 3.0 */public class ODMGRuntimeException extends RuntimeException{    public ODMGRuntimeException()    {    }    public ODMGRuntimeException(String msg)    {        super(msg);    }}