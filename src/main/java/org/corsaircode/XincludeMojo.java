package org.corsaircode;


import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "touch", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class XincludeMojo extends AbstractMojo {
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/*.xml"};

    /**
     * The directory which contains the resources you want scanned for xml files.
     */
    @Parameter(defaultValue = "${basedir}/src/main/resources")
    private File resourcesDirectory;

    /**
     * The directory where to place the processed xml files
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
    private File resourcesOutput;

    /**
     * A list of files to include. Can contain ant-style wildcards and double wildcards.
     * The default includes are
     * <code>**&#47;*.xml</code>
     */
    @Parameter
    private String[] includes;

    /**
     * A list of files to exclude. Can contain ant-style wildcards and double wildcards.
     */
    @Parameter
    private String[] excludes;

    public void execute() throws MojoExecutionException {

        FileUtils.mkdir(resourcesOutput.getPath());

        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(resourcesDirectory);
        if (includes != null && includes.length != 0) {
            scanner.setIncludes(includes);
        } else {
            scanner.setIncludes(DEFAULT_INCLUDES);
        }

        if (excludes != null && excludes.length != 0) {
            scanner.setExcludes(excludes);
        }

        scanner.addDefaultExcludes();
        scanner.scan();

        List<String> includedFiles = Arrays.asList(scanner.getIncludedFiles());

        for (String resource : includedFiles) {

            try {
                File xmlFile = new File(resourcesDirectory + "\\" + resource);
                final InputStream xmlStream = new FileInputStream(xmlFile);

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setXIncludeAware(true);
                factory.setNamespaceAware(true);
                DocumentBuilder docBuilder = factory.newDocumentBuilder();

                if (!docBuilder.isXIncludeAware()) {
                    throw new IllegalStateException();
                }
                docBuilder.setEntityResolver(new EntityResolver() {
                    @Override
                    public InputSource resolveEntity(String publicId, String systemId) throws IOException {
                        File file = new File(systemId);
                        if (file.exists()) {
                            return new InputSource(new FileInputStream(file));
                        } else {
                            return null;
                        }
                    }
                });
                Document doc = docBuilder.parse(xmlStream);
                Source source = new DOMSource(doc);
                Result result = new StreamResult(new FileOutputStream(resourcesOutput + "\\" + resource));
                TransformerFactory transformerFactory = TransformerFactory
                        .newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.transform(source, result);
            } catch (FileNotFoundException e) {
                throw new MojoExecutionException("FileNotFoundException", e);
            } catch (ParserConfigurationException e) {
                throw new MojoExecutionException("ParserConfigurationException", e);
            } catch (TransformerConfigurationException e) {
                throw new MojoExecutionException("TransformerConfigurationException", e);
            } catch (TransformerException e) {
                throw new MojoExecutionException("TransformerException", e);
            } catch (IOException e) {
                throw new MojoExecutionException("IOException", e);
            } catch (SAXException e) {
                throw new MojoExecutionException("SAXException", e);
            }
        }


    }

}
