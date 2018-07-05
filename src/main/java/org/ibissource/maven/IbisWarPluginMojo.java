package org.ibissource.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.war.WarMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

/**
 * This plugin enables the security constraints section in the ibis web.xml file
 *
 */
@Mojo(name = "war", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME )
public class IbisWarPluginMojo extends WarMojo {

	@Parameter( defaultValue = "${project}", readonly = true, required = true )
	private MavenProject project;

	@Parameter( defaultValue = "${session}", readonly = true, required = true )
	private MavenSession session;

	@Parameter( defaultValue = "${settings}", readonly = true, required = true )
	private Settings settings;

	@Parameter( property = "war.enableSecurityConstraints", defaultValue = "true" )
	private boolean enableSecurityConstraints;

	@Override
	public void buildExplodedWebapp(File webapplicationDirectory) throws MojoExecutionException, MojoFailureException {
		super.buildExplodedWebapp(webapplicationDirectory);

		if(enableSecurityConstraints) {
			getLog().info("Enabling security constraints");
			File webXmlFile = new File( webapplicationDirectory, "WEB-INF/web.xml" );
			if(webXmlFile.exists()) {
				enableSecurityConstraints( webXmlFile );
			}
			else {
				if(isFailOnMissingWebXml()) {
					throw new MojoExecutionException("webXml does not exist!?");
				}
				else {
					getLog().warn("no web.xml found, skipping...");
				}
			}
		}
		else {
			getLog().warn("Skipping security constraints");
		}
	}

	private void enableSecurityConstraints(File webXml) throws MojoExecutionException {
		try {
			String xml = getFileContent(webXml);
			xml = xml.replace("<!-- security-constraint>", "<security-constraint>");
			xml = xml.replace("</security-role -->", "</security-role>");

			FileOutputStream fop = new FileOutputStream(webXml);
			byte[] contentInBytes = xml.getBytes();

			fop.write(contentInBytes);
			fop.flush();
			fop.close();
		} catch (IOException e) {
			throw new MojoExecutionException("failed enabling web.xml security constraints", e);
		}
	}

	public String getFileContent( File file ) throws IOException {
		return getFileContent(new FileInputStream(file));
	}

	public String getFileContent( InputStream fis ) throws IOException {
		Reader r = null;
		try {
			StringBuilder sb = new StringBuilder();
			r = new InputStreamReader(fis, "UTF-8");  //or whatever encoding
			char[] buf = new char[1024];
			int amt = r.read(buf);
			while(amt > 0) {
				sb.append(buf, 0, amt);
				amt = r.read(buf);
			}
			return sb.toString();
		}
		finally {
			if(r != null)
				r.close();
		}
	}
}