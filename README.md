# The ibis-war-plugin
A customized maven-war-plugin that enables the security constraints section in the Ibis web.xml file, populates the archive manifest file and appends the uncompiled java classes to WEB-INF/classes.

# Usage  
````
<plugin>  
  <groupId>org.ibissource</groupId>  
  <artifactId>ibis-war-plugin</artifactId>  
  <version>0.2</version>  
  <extensions>true</extensions>  
</plugin>  
````

Mainly used specifically for https://github.com/ibissource/iaf
