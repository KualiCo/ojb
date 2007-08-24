package org.apache.ojb.broker.metadata;

/* Copyright 2002-2005 The Apache Software Foundation
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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * This class is responsible for reading and writing DescriptorRepository objects
 * from and to persistent media.
 * Currently only XML file based persistence is supported.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: RepositoryPersistor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class RepositoryPersistor implements Configurable
{
    // TODO: Refactoring of the metadata reading/handling?

    private static Logger log = LoggerFactory.getLogger(RepositoryPersistor.class);

    private static final String SER_FILE_SUFFIX = "serialized";
    private static final String SERIALIZED_REPOSITORY_PATH = "serializedRepositoryPath";

    private boolean useSerializedRepository = false;

    public RepositoryPersistor()
    {
        OjbConfigurator.getInstance().configure(this);
    }

    public void configure(Configuration pConfig) throws ConfigurationException
    {
        useSerializedRepository = ((MetadataConfiguration) pConfig).useSerializedRepository();
    }

    /**
     * Write the {@link DescriptorRepository} to the given output object.
     */
    public void writeToFile(DescriptorRepository repository, ConnectionRepository conRepository, OutputStream out)
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        try
        {
            if (log.isDebugEnabled())
                log.debug("## Write repository file ##" +
                        repository.toXML() +
                        "## End of repository file ##");


            String eol = SystemUtils.LINE_SEPARATOR;
            StringBuffer buf = new StringBuffer();
            // 1. write XML header
            buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + eol);

            buf.append("<!DOCTYPE descriptor-repository SYSTEM \"repository.dtd\" >" + eol + eol);
//            strReturn += "<!DOCTYPE descriptor-repository SYSTEM \"repository.dtd\" [" + eol;
//            strReturn += "<!ENTITY database-metadata SYSTEM \""+ConnectionRepository.DATABASE_METADATA_FILENAME+"\">" + eol;
//            strReturn += "<!ENTITY user SYSTEM \"repository_user.xml\">" + eol;
//            strReturn += "<!ENTITY junit SYSTEM \"repository_junit.xml\">" + eol;
//            strReturn += "<!ENTITY internal SYSTEM \"repository_internal.xml\"> ]>" + eol + eol;

            buf.append("<!-- OJB RepositoryPersistor generated this file on " + new Date().toString() + " -->" + eol);

            buf.append(tags.getOpeningTagNonClosingById(RepositoryElements.MAPPING_REPOSITORY) + eol);
            buf.append("  " + tags.getAttribute(RepositoryElements.REPOSITORY_VERSION, DescriptorRepository.getVersion()) + eol);
            buf.append("  " + tags.getAttribute(RepositoryElements.ISOLATION_LEVEL, repository.getIsolationLevelAsString()) + eol);
            buf.append(">" + eol);

            if(conRepository != null) buf.append(eol + eol + conRepository.toXML() + eol + eol);
            if(repository != null) buf.append(repository.toXML());

            buf.append(tags.getClosingTagById(RepositoryElements.MAPPING_REPOSITORY));

            PrintWriter pw = new PrintWriter(out);
            pw.print(buf.toString());
            pw.flush();
            pw.close();
        }
        catch (Exception e)
        {
            log.error("Could not write to output stream" + out, e);
        }
    }

    /**
     * Read the repository configuration file.
     * <br>
     * If configuration property <code>useSerializedRepository</code> is <code>true</code>
     * all subsequent calls read a serialized version of the repository.
     * The directory where the serialized repository is stored can be specified
     * with the <code>serializedRepositoryPath</code> entry in OJB.properties.
     * Once a serialized repository is found changes to repository.xml will be
     * ignored. To force consideration of these changes the serialized repository
     * must be deleted manually.
     */
    public DescriptorRepository readDescriptorRepository(String filename)
            throws MalformedURLException, ParserConfigurationException, SAXException, IOException
    {
        DescriptorRepository result;
        if (useSerializedRepository)
        // use serialized repository
        {
            // build File object pointing to serialized repository location
            Configuration config = OjbConfigurator.getInstance().getConfigurationFor(null);
            String pathPrefix = config.getString(SERIALIZED_REPOSITORY_PATH, ".");
            File serFile = new File(pathPrefix + File.separator + filename + "." + SER_FILE_SUFFIX);

            if (serFile.exists() && serFile.length() > 0)
            // if file exists load serialized version of repository
            {
                try
                {
                    long duration = System.currentTimeMillis();
                    result = deserialize(serFile);
                    log.info("Read serialized repository in " + (System.currentTimeMillis() - duration) + " ms");
                }
                catch (Exception e)
                {
                    log.error("error in loading serialized repository. Will try to use XML version.", e);
                    result = (DescriptorRepository) buildRepository(filename, DescriptorRepository.class);
                }
            }
            else
            // if no serialized version exists, read it from xml and write serialized file
            {
                long duration = System.currentTimeMillis();
                result = (DescriptorRepository) buildRepository(filename, DescriptorRepository.class);
                log.info("Read repository from file took " + (System.currentTimeMillis() - duration) + " ms");
                serialize(result, serFile);
            }
        }
        // don't use serialized repository
        else
        {
            long duration = System.currentTimeMillis();
            result = (DescriptorRepository) buildRepository(filename, DescriptorRepository.class);
            log.info("Read class descriptors took " + (System.currentTimeMillis() - duration) + " ms");
        }
        return result;
    }

    public DescriptorRepository readDescriptorRepository(InputStream inst)
            throws MalformedURLException, ParserConfigurationException, SAXException, IOException
    {
        long duration = System.currentTimeMillis();
        InputSource inSource = new InputSource(inst);
        DescriptorRepository result = (DescriptorRepository) readMetadataFromXML(inSource, DescriptorRepository.class);
        log.info("Read class descriptors took " + (System.currentTimeMillis() - duration) + " ms");
        return result;
    }

    /**
     * Read the repository configuration file and extract connection handling information.
     */
    public ConnectionRepository readConnectionRepository(String filename)
            throws MalformedURLException, ParserConfigurationException, SAXException, IOException
    {
        long duration = System.currentTimeMillis();
        ConnectionRepository result = (ConnectionRepository) buildRepository(filename, ConnectionRepository.class);
        log.info("Read connection repository took " + (System.currentTimeMillis() - duration) + " ms");
        return result;
    }

    /**
     * Read the repository configuration file and extract connection handling information.
     */
    public ConnectionRepository readConnectionRepository(InputStream inst)
            throws MalformedURLException, ParserConfigurationException, SAXException, IOException
    {
        long duration = System.currentTimeMillis();
        InputSource inSource = new InputSource(inst);
        ConnectionRepository result = (ConnectionRepository) readMetadataFromXML(inSource, ConnectionRepository.class);
        log.info("Read connection repository took " + (System.currentTimeMillis() - duration) + " ms");
        return result;
    }

    protected DescriptorRepository deserialize(File serFile)
    {
        DescriptorRepository result = null;
        try
        {
            FileInputStream fis = new FileInputStream(serFile);
            // deserialize repository
            result = (DescriptorRepository) SerializationUtils.deserialize(fis);
        }
        catch (Exception e)
        {
            log.error("Deserialisation failed, using input path: " + serFile.getAbsolutePath(), e);
        }
        return result;
    }

    protected void serialize(DescriptorRepository repository, File file)
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(file);
            // serialize repository
            SerializationUtils.serialize(repository, fos);
        }
        catch (Exception e)
        {
            log.error("Serialization failed, using output path: " + file.getAbsolutePath(), e);
        }
    }

    /**
     *
     * TODO: We should re-design the configuration file reading
     */
    private Object buildRepository(String repositoryFileName, Class targetRepository)
            throws MalformedURLException, ParserConfigurationException, SAXException, IOException
    {
        URL url = buildURL(repositoryFileName);
        /*
        arminw:
        strange, when using 'url.openStream()' argument repository
        could not be parsed
        ipriha:
        parser needs a base url to find referenced entities.
        */
        // InputSource source = new InputSource(url.openStream());
        
        String pathName = url.toExternalForm();

        log.info("Building repository from :" + pathName);
        InputSource source = new InputSource(pathName);
        URLConnection conn = url.openConnection();
        conn.setUseCaches(false);
        conn.connect();
        InputStream in = conn.getInputStream();
        source.setByteStream(in);
        try
		{
        	return readMetadataFromXML(source, targetRepository);
		}
        finally
		{
        	try
			{
        		in.close();
			}
        	catch (IOException x)
			{
                log.warn("unable to close repository input stream [" + x.getMessage() + "]", x);
			}
		}
    }

    /**
     * Read metadata by populating an instance of the target class
     * using SAXParser.
     */
    private Object readMetadataFromXML(InputSource source, Class target)
            throws MalformedURLException, ParserConfigurationException, SAXException, IOException
    {
        // TODO: make this configurable
        boolean validate = false;
        
        // get a xml reader instance:
        SAXParserFactory factory = SAXParserFactory.newInstance();
        log.info("RepositoryPersistor using SAXParserFactory : " + factory.getClass().getName());
        if (validate)
        {
            factory.setValidating(true);
        }
        SAXParser p = factory.newSAXParser();
        XMLReader reader = p.getXMLReader();
        if (validate)
        {
            reader.setErrorHandler(new OJBErrorHandler());
        }

        Object result;
        if (DescriptorRepository.class.equals(target))
        {
            // create an empty repository:
            DescriptorRepository repository = new DescriptorRepository();
            // create handler for building the repository structure
            ContentHandler handler = new RepositoryXmlHandler(repository);
            // tell parser to use our handler:
            reader.setContentHandler(handler);
            reader.parse(source);
            result = repository;
        }
        else if (ConnectionRepository.class.equals(target))
        {
            // create an empty repository:
            ConnectionRepository repository = new ConnectionRepository();
            // create handler for building the repository structure
            ContentHandler handler = new ConnectionDescriptorXmlHandler(repository);
            // tell parser to use our handler:
            reader.setContentHandler(handler);
            reader.parse(source);
            //LoggerFactory.getBootLogger().info("loading XML took " + (stop - start) + " msecs");
            result = repository;
        }
        else
            throw new MetadataException("Could not build a repository instance for '" + target +
                    "', using source " + source);
        return result;
    }

    private URL buildURL(String repositoryFileName) throws MalformedURLException
    {
        //j2ee compliant lookup of resources
        URL url = ClassHelper.getResource(repositoryFileName);

        // don't be too strict: if resource is not on the classpath, try ordinary file lookup
        if (url == null)
        {
            try
            {
                url = new File(repositoryFileName).toURL();
            }
            catch (MalformedURLException ignore)
            {
            }
        }

        if (url != null)
        {
            log.info("OJB Descriptor Repository: " + url);
        }
        else
        {
            throw new MalformedURLException("did not find resource " + repositoryFileName);
        }
        return url;
    }

    // inner class
    class OJBErrorHandler implements ErrorHandler
    {
        public void warning(SAXParseException exception)
                throws SAXException
        {
            logMessage(exception, false);
        }

        public void error(SAXParseException exception)
                throws SAXException
        {
            logMessage(exception, false);
        }

        public void fatalError(SAXParseException exception)
                throws SAXException
        {
            logMessage(exception, true);
        }

        void logMessage(SAXParseException e, boolean isFatal)
        {
            String msg = e.getMessage();
            if (isFatal)
            {
                log.error("## " + e.getSystemId() + " - line " + e.getLineNumber() +  ": " + msg + " ##");
            }
            else
            {
                log.warn(e.getSystemId() + " - line " + e.getLineNumber() + ": " + msg);
            }
        }
    }
}
