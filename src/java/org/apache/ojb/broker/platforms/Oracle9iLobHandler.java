package org.apache.ojb.broker.platforms;

/* Copyright 2003-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.lang.reflect.InvocationTargetException;

/**
 * Handles the Oracle LOB problems for 9i.
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 * @author <a href="mailto:erik@cj.com">Erik Forkalsrud</a>
 * @author <a href="mailto:martin.kalen@curalia.se">Martin Kal&eacute;n</a>
 * @version CVS $Id: Oracle9iLobHandler.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class Oracle9iLobHandler
{

    protected static Logger logger = LoggerFactory.getLogger(Oracle9iLobHandler.class);

	private static ClobWrapper createTempCLOB(Connection connection, ClobWrapper clob)
	{
        if (clob == null) {
            return null;
        }

		ClobWrapper tempClob = null;
        try
        {
            // Create a temporary CLOB with duration session
            tempClob = ClobWrapper.createTemporary(connection, true, ClobWrapper.getDurationSessionValue());

            // Open the CLOB in readonly mode
            clob.open(ClobWrapper.getModeReadOnlyValue());

            // Open the temporary CLOB in readwrite mode to enable writing
            tempClob.open(ClobWrapper.getModeReadWriteValue());

            // No of bytes read each trip to database
            int bytesread;

            // Get the input stream for reading from the CLOB
            Reader clobReader = clob.getCharacterStream();

            // Get the output stream for writing into the CLOB
            Writer tempClobWriter = tempClob.getCharacterOutputStream();

            // Create a buffer to read data
            // getBufferSize() returns the optimal buffer size
            char[] charbuffer = new char[clob.getBufferSize()];

            // Read from the CLOB and write into the temporary CLOB
            while ((bytesread = clobReader.read(charbuffer)) != -1)
            {
                tempClobWriter.write(charbuffer, 0, bytesread);
            }

            // Flush and close the streams
            tempClobWriter.flush();
            tempClobWriter.close();
            clobReader.close();

            // Close the CLOBs
            clob.close();
            tempClob.close();
        }
        catch (Exception e)
        {
            logger.error("Error during temporary CLOB write", e);
            freeTempLOB(tempClob, null);
        }
		return tempClob;
	}

	public static String convertCLOBtoString(Connection connection, Object nativeclob)
	{
		ClobWrapper temp = new ClobWrapper();
		temp.setClob(nativeclob);
		/**
		 * first, convert the clob to another clob. Thanks Oracle, you rule.
		 */
		ClobWrapper clob = createTempCLOB(connection, temp);

		if (clob == null) {
            return null;
        }

        String retval = null;
        // Buffer to hold the CLOB data
        StringBuffer clobdata = new StringBuffer();
        // No of bytes read each trip to database
        int bytesread = 0;
        try
        {
            // Open the CLOB in readonly mode
            clob.open(ClobWrapper.getModeReadOnlyValue());
            // Open the stream to read data
            Reader clobReader = clob.getCharacterStream();
            //  Buffer size is fixed using the getBufferSize() method which returns
            //  the optimal buffer size to read data from the LOB
            char[] charbuffer = new char[clob.getBufferSize()];
            // Keep reading from the CLOB and append it to the stringbuffer till
            // there is no more to read
            while ((bytesread = clobReader.read(charbuffer)) != -1)
            {
                clobdata.append(charbuffer, 0, bytesread);
            }
            // Close the input stream
            clobReader.close();
            // Close the CLOB
            clob.close();
            retval = clobdata.toString();
            clobdata = null;
        }
        catch (Exception e)
        {
            logger.error("Error during CLOB read", e);
            freeTempLOB(clob, null);
        }
		return retval;
	}

	public static Object createCLOBFromString(Connection conn, String clobData)
	{
        if (clobData == null) {
            return null;
        }
        ClobWrapper clob = null;
        try
        {
            clob = ClobWrapper.createTemporary(conn, true, ClobWrapper.getDurationSessionValue());
            if (clob != null) {
                // Open the temporary CLOB in readwrite mode to enable writing
                clob.open(ClobWrapper.getModeReadWriteValue());

                // Clear the previous contents of the CLOB
                clob.trim(0);

                // Get the output stream to write
                Writer tempClobWriter = clob.getCharacterOutputStream();

                if (tempClobWriter != null) {
                    // Write the data into the temporary CLOB
                    tempClobWriter.write(clobData);

                    // Flush and close the stream
                    tempClobWriter.flush();
                    tempClobWriter.close();
                }

                // Close the temporary CLOB
                clob.close();
            }
        }
        catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            freeTempLOB(clob, null);
            if (t instanceof java.lang.UnsatisfiedLinkError) {
                logger.error("Oracle JDBC-driver version does not match installed OCI-driver");
            } else {
                logger.error("Error during temporary CLOB write", t);
            }
        }
        catch (Exception e)
        {
            logger.error("Error during temporary CLOB write", e);
            freeTempLOB(clob, null);
        }
		return clob == null ? null : clob.getClob();
	}

    private static BlobWrapper createTempBLOB(Connection connection, BlobWrapper blob)
    {
        if (blob == null) {
            return null;
        }

        BlobWrapper tempBlob = null;
        try
        {
            // Create a temporary BLOB with duration session
            tempBlob = BlobWrapper.createTemporary(connection, true, BlobWrapper.getDurationSessionValue());

            // Open the CLOB in readonly mode
            blob.open(BlobWrapper.getModeReadOnlyValue());

            // Open the temporary CLOB in readwrite mode to enable writing
            tempBlob.open(BlobWrapper.getModeReadWriteValue());

            // No of bytes read each trip to database
            int bytesread;

            // Get the input stream for reading from the BLOB
            InputStream blobInputStream = blob.getBinaryStream();

            // Get the output stream for writing into the BLOB
            OutputStream tempBlobOutputStream = tempBlob.getBinaryOutputStream();

            // Create a buffer to read data
            // getBufferSize() returns the optimal buffer size
            byte[] bytebuffer = new byte[blob.getBufferSize()];

            // Read from the BLOB and write into the temporary BLOB
            while ((bytesread = blobInputStream.read(bytebuffer)) != -1)
            {
                tempBlobOutputStream.write(bytebuffer, 0, bytesread);
            }

            // Flush and close the streams
            tempBlobOutputStream.flush();
            tempBlobOutputStream.close();
            blobInputStream.close();

            // Close the BLOBs
            blob.close();
            tempBlob.close();
        }
        catch (Exception e)
        {
            logger.error("Error during temporary BLOB write", e);
            freeTempLOB(null, tempBlob);
        }
        return tempBlob;
    }

    public static byte[] convertBLOBtoByteArray(Connection connection, Object nativeblob)
    {
        BlobWrapper temp = new BlobWrapper();
        temp.setBlob(nativeblob);
        /**
         * first, convert the blob to another blob. Thanks Oracle, you rule.
         */
        BlobWrapper blob = createTempBLOB(connection, temp);
        if (blob == null) {
            return null;
        }

        byte[] retval = null;
        // Buffer to hold the BLOB data
        ByteArrayOutputStream blobdata = new ByteArrayOutputStream();
        // No of bytes read each trip to database
        int bytesread = 0;
        try
        {
            // Open the BLOB in readonly mode
            blob.open(BlobWrapper.getModeReadOnlyValue());
            // Open the stream to read data
            InputStream blobInputStream = blob.getBinaryStream();
            //  Buffer size is fixed using the getBufferSize() method which returns
            //  the optimal buffer size to read data from the LOB
            byte[] bytebuffer = new byte[blob.getBufferSize()];
            // Keep reading from the BLOB and append it to the bytebuffer till
            // there is no more to read
            while ((bytesread = blobInputStream.read(bytebuffer)) != -1)
            {
                blobdata.write(bytebuffer, 0, bytesread);
            }
            // Close the input and output streams stream
            blobInputStream.close();
            blobdata.flush();
            blobdata.close();

            // Close the BLOB
            blob.close();
            retval = blobdata.toByteArray();
            blobdata = null;
        }
        catch (Exception e)
        {
            logger.error("Error during BLOB read", e);
            freeTempLOB(null, blob);
        }
        return retval;
    }

    public static Object createBLOBFromByteArray(Connection conn, byte[] blobData)
    {
        if (blobData == null) {
            return null;
        }

        BlobWrapper blob = null;
        try
        {
            blob = BlobWrapper.createTemporary(conn, true, BlobWrapper.getDurationSessionValue());

            // Open the temporary BLOB in readwrite mode to enable writing
            blob.open(BlobWrapper.getModeReadWriteValue());

            // Clear the previous contents of the BLOB
            blob.trim(0);

            // Get the output stream to write
            OutputStream tempBlobOutputStream = blob.getBinaryOutputStream();

            // Write the data into the temporary BLOB
            tempBlobOutputStream.write(blobData);

            // Flush and close the stream
            tempBlobOutputStream.flush();
            tempBlobOutputStream.close();

            // Close the temporary BLOB
            blob.close();
        }
        catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            freeTempLOB(null, blob);
            if (t instanceof java.lang.UnsatisfiedLinkError) {
                logger.error("Oracle JDBC-driver version does not match installed OCI-driver");
            } else {
                logger.error("Error during temporary BLOB write", t);
            }
        }
        catch (Exception e)
        {
            logger.error("Error during temporary BLOB write", e);
            freeTempLOB(null, blob);
        }
        return blob == null ? null : blob.getBlob();
    }

	/**
	 * Frees the temporary LOBs when an exception is raised in the application
	 * or when the LOBs are no longer needed. If the LOBs are not freed, the
	 * space used by these LOBs are not reclaimed.
     * @param clob CLOB-wrapper to free or null
     * @param blob BLOB-wrapper to free or null
	 */
	private static void freeTempLOB(ClobWrapper clob, BlobWrapper blob)
	{
		try
		{
			if (clob != null)
			{
				// If the CLOB is open, close it
				if (clob.isOpen())
				{
					clob.close();
				}

				// Free the memory used by this CLOB
				clob.freeTemporary();
			}

			if (blob != null)
			{
				// If the BLOB is open, close it
				if (blob.isOpen())
				{
					blob.close();
				}

				// Free the memory used by this BLOB
				blob.freeTemporary();
			}
		}
		catch (Exception e)
		{
            logger.error("Error during temporary LOB release", e);
		}
	}

}
