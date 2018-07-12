/*
   Copyright 2018 Integration Partners

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.ibissource.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.war.WarMojo;
import org.apache.maven.project.MavenProject;

/**
 * This plugin enables the security constraints section in the Ibis web.xml file, 
 * populates the archive manifest file and appends the uncompiled java classes 
 * to WEB-INF/classes.
 * 
 * @author Niels Meijer
 */
@Mojo(name = "war", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME )
public class IbisWarPluginMojo extends WarMojo {

	@Parameter( defaultValue = "${project.build.sourceDirectory}", required = true )
	private String sourceDirectory;

	@Parameter( property = "war.enableSecurityConstraints", defaultValue = "true" )
	private boolean enableSecurityConstraints;

	@Parameter( property = "war.packageJavaClasses", defaultValue = "true" )
	private boolean packageJavaClasses;

	@Override
	public void buildExplodedWebapp(File webapplicationDirectory) throws MojoExecutionException, MojoFailureException {
		//It's Preferable to use the default web.xml file :)
		setFailOnMissingWebXml(false);

		//For debugging purposes we add the raw uncompiled java code to the war file.
		if(packageJavaClasses) {
			addJavaClassesToWar();
		}

		//Exclude gitignore files by default, but allow people to overwrite this
		if(getPackagingExcludes().length == 0)
			setPackagingExcludes(".gitignore");

		//Trigger the webapp build
		super.buildExplodedWebapp(webapplicationDirectory);

		//Enable the security constraints in the IBIS web.xml
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
			getLog().warn("Skipping security constraints in web.xml");
		}
	}

	public void addJavaClassesToWar() {
		Resource[] webResources = getWebResources();
		List<Resource> resources = new LinkedList<Resource>();

		if(webResources != null) {
			resources.addAll(Arrays.asList(webResources));
			for(Resource resource : resources) {
				getLog().info("Copying custom webResource ["+resource.getDirectory()+"] to ["+resource.getTargetPath()+"]");
			}
		}

		Resource resource = new Resource();
		resource.setDirectory(sourceDirectory);
		resource.setTargetPath("WEB-INF/classes");
		resources.add(resource);
		getLog().info("Copying java webResources ["+sourceDirectory+"] to [WEB-INF\\classes]");

		webResources = resources.toArray(new Resource[resources.size()]);
		setWebResources(webResources);
	}

	/**
	 * We like to populate the manifest with more information about the project
	 */
	@Override
	public MavenArchiveConfiguration getArchive()
	{
		MavenProject project = getProject();

		MavenArchiveConfiguration archive = super.getArchive();
		if(archive == null)
			archive = new MavenArchiveConfiguration();

		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
		archive.addManifestEntry("Project", project.getName());
		if(project.getDescription() != null)
			archive.addManifestEntry("Description", project.getDescription());

		archive.addManifestEntry("Created-Time", dt.format(new Date()));
		archive.addManifestEntry("Build-Version", project.getVersion());
		archive.addManifestEntry("Build-Artifact", project.getArtifactId());
		archive.setAddMavenDescriptor(false);
		return archive;
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