/*
 * Eurelis OpenCms Ant Task.
 * Copyright (C) Eurelis - http://www.eurelis.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms version 3 of the GNU Lesser General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the version 3 of the GNU Lesser General Public License for more details. 
 */
package com.eurelis.opencms.ant.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.FlyweightCDATA;
import org.opencms.util.CmsUUID;

public class ManifestBuilderTask extends Task {

  private DateFormat dateformat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

  Document document = null;

  private String resourcetypes = null;
  private String explorertypes = null;

  private Boolean generateuuids = null;

  private String distfolder = null;
  private String srcfolder = null;
  private Vector<ExportPoint> exportpoints = new Vector<ExportPoint>();
  private Vector<Resource> resources = new Vector<Resource>();
  private Vector<Dependency> dependencies = new Vector<Dependency>();
  private Vector<Parameter> parameters = new Vector<Parameter>();
  private Vector<FileSet> filesets = new Vector<FileSet>();

  private String creator = "";
  private String opencmsversion = "";
  private String project = "";
  private String exportversion = "";

  private String name = "";
  private String nicename = "";
  private String group = "";
  private String moduleclass = "";
  private String moduledescription = "";
  private String version = "";
  private String authorname = "";
  private String authoremail = "";
  private String userinstalled = "";
  private String dateinstalled = "";

  private String SEPARATOR = System.getProperty("file.separator");

  @Override
  public void execute() throws BuildException {
    super.execute();
    createDocument();
    try {
      write();
    } catch (Exception e) {
      log(e, Project.MSG_ERR);
      e.printStackTrace();
    }
  }

  public Document createDocument() {

    document = DocumentHelper.createDocument();
    document.setXMLEncoding("UTF-8");
    Element root = document.addElement("export");

    Element info = root.addElement("info");
    if(creator!=null) info.addElement("creator").addText(creator);
    if(opencmsversion!=null) info.addElement("opencms_version").addText(opencmsversion);
    info.addElement("createdate").addText(dateformat.format(new Date()));
    if(project!=null) info.addElement("infoproject").addText(project);
    if(exportversion!=null) info.addElement("export_version").addText(exportversion);

    Element module = root.addElement("module");

    if(name!=null) module.addElement("name").addText(name);
    if(nicename!=null) module.addElement("nicename").addText(nicename);
    if(group!=null) module.addElement("group").addText(group);
    if(moduleclass!=null) module.addElement("class").addText(moduleclass);
    if(moduledescription!=null) module.addElement("description").add(new FlyweightCDATA(moduledescription));
    if(version!=null) module.addElement("version").addText(version);
    if(authorname!=null) module.addElement("authorname").add(new FlyweightCDATA(authorname));
    if(authoremail!=null) module.addElement("authoremail").add(new FlyweightCDATA(authoremail));
    module.addElement("datecreated").addText(dateformat.format(new Date()));
    module.addElement("userinstalled").addText(userinstalled);
    module.addElement("dateinstalled").addText(dateinstalled);

    Element dependenciesBlock = module.addElement("dependencies");
    for (Dependency dep : dependencies) {
      dependenciesBlock.addElement("dependency").addAttribute("name", dep.getName()).addAttribute("version", dep.getVersion());
    }

    Element exportPointsBlock = module.addElement("exportpoints");
    for (ExportPoint ep : exportpoints) {
      exportPointsBlock.addElement("exportpoint").addAttribute("uri", ep.getSrc()).addAttribute("destination", ep.getDst());
    }

    Element resourcesBlock = module.addElement("resources");
    for (Resource res : resources) {
      resourcesBlock.addElement("resource").addAttribute("uri", res.getUri());
    }

    Element parametersBlock = module.addElement("parameters");
    for (Parameter par : parameters) {
      parametersBlock.addElement("param").addAttribute("name", par.getName()).addText(par.getValue());
    }

    insertResourceTypes(module);
    insertExplorerTypes(module);

    if (!filesets.isEmpty()) {

      Element files = root.addElement("files");

      for (FileSet fileset : filesets) {
        DirectoryScanner ds = fileset.getDirectoryScanner(fileset.getProject());
        String[] dirs = ds.getIncludedDirectories();
        String[] filesColl = ds.getIncludedFiles();

        String[] excluDirsArray = ds.getExcludedDirectories();
        List<String> excluDirs = new ArrayList<String>();
        excluDirs.addAll(Arrays.asList(excluDirsArray));

        String[] excluFilesArray = ds.getExcludedFiles();
        List<String> excluFiles = new ArrayList<String>();
        excluFiles.addAll(Arrays.asList(excluFilesArray));

        CmsUUID.init("B4:B6:76:78:7F:3E");

        // FOLDERS MANAGEMENT
        for (int i = 0; i < dirs.length; i++) {
          String filepath = dirs[i];
          String filepathUnix = dirs[i].replace(SEPARATOR, "/");
          if (dirs[i] != "") {
            Element tmpFile = files.addElement("file");
            tmpFile.addElement("destination").addText(filepathUnix);

            String folderPropertiesPath = getProject().getBaseDir() + SEPARATOR + srcfolder + SEPARATOR + folderPropertiesPath(filepath);
            String tmpType = getEurelisProperty("type", folderPropertiesPath);
            if (null == tmpType) {
              tmpType = "folder";
            }
            tmpFile.addElement("type").addText(tmpType);

            if (generateuuids) {
              Element uuidNode = tmpFile.addElement("uuidstructure");
              String tmpUUID = getEurelisProperty("structureUUID", folderPropertiesPath);
              if (null != tmpUUID)
                uuidNode.addText(tmpUUID);
              else
                uuidNode.addText(new CmsUUID().toString());
              // AJOUTER SAUVEGARDE DU NOUVEL UUID
            }

            long date = new File(getProject().getBaseDir() + SEPARATOR + srcfolder + SEPARATOR + filepath).lastModified();
            if (0L == date)
              date = new Date().getTime();
            String formattedDate = dateformat.format(date);
            tmpFile.addElement("datelastmodified").addText(formattedDate);
            tmpFile.addElement("userlastmodified").addText("Admin");
            // WARNING : CONSTANT VALUE
            tmpFile.addElement("datecreated").addText(formattedDate);
            // WARNING : CONSTANT VALUE
            tmpFile.addElement("usercreated").addText("Admin");
            tmpFile.addElement("flags").addText("0");

            Element properties = tmpFile.addElement("properties");
            // props detection and implementation
            String tmpPropFile = folderPropertiesPath;
            addPropertiesToTree(properties, tmpPropFile);

            String tmpAccessFile = getProject().getBaseDir() + SEPARATOR + srcfolder + SEPARATOR + folderAccessesPath(filepath);
            addAccessesToTree(tmpFile, tmpAccessFile);
          }
        }
        // FILES MANAGEMENT
        for (int i = 0; i < filesColl.length; i++) {
          String filepath = filesColl[i];
          String filepathUnix = filesColl[i].replace(SEPARATOR, "/");
          if (filesColl[i] != "") {
            Element tmpFile = files.addElement("file");
            tmpFile.addElement("source").addText(filepathUnix);
            tmpFile.addElement("destination").addText(filepathUnix);

            String propertiesFilepath = getProject().getBaseDir() + SEPARATOR + srcfolder + SEPARATOR + filePropertiesPath(filepath);
            String tmpType = getEurelisProperty("type", propertiesFilepath);
            if (null == tmpType) {
              if (filepathUnix.endsWith(".config"))
                tmpType = "module_config";
              else if (filepathUnix.endsWith("main.jsp"))
                tmpType = "containerpage_template";
              else if (filepathUnix.endsWith(".jsp"))
                tmpType = "jsp";
              else if (filepathUnix.endsWith(".png")||filepathUnix.endsWith(".gif")||filepathUnix.endsWith(".jpg")||filepathUnix.endsWith(".jpeg"))
                tmpType = "image";
              else if (filepathUnix.endsWith(".html") && filepathUnix.contains("/models/"))
                tmpType = "containerpage";
              else
                tmpType = "plain";
            }
            tmpFile.addElement("type").addText(tmpType);

            if (generateuuids) {
              Element uuidNode = tmpFile.addElement("uuidresource");
              Element uuidNode2 = tmpFile.addElement("uuidstructure");
              String tmpUUID = getEurelisProperty("resourceUUID", propertiesFilepath);
              if (null != tmpUUID)
                uuidNode.addText(tmpUUID);
              else
                uuidNode.addText(new CmsUUID().toString());
              tmpUUID = getEurelisProperty("structureUUID", propertiesFilepath);
              if (null != tmpUUID)
                uuidNode2.addText(tmpUUID);
              else
                uuidNode2.addText(new CmsUUID().toString());
            }

            long date = new File(getProject().getBaseDir() + SEPARATOR + srcfolder + SEPARATOR + filepath).lastModified();
            if (0L == date)
              date = new Date().getTime();
            String formattedDate = dateformat.format(date);

            tmpFile.addElement("datelastmodified").addText(formattedDate);
            tmpFile.addElement("userlastmodified").addText("Admin");
            tmpFile.addElement("datecreated").addText(formattedDate);
            tmpFile.addElement("usercreated").addText("Admin");
            tmpFile.addElement("flags").addText("0");
            Element properties = tmpFile.addElement("properties");
            String tmpPropFile = propertiesFilepath;
            addPropertiesToTree(properties, tmpPropFile);

            tmpFile.addElement("accesscontrol");

          }
        }
      }
    }

    return document;
  }

  private void insertExplorerTypes(Element module) {
    if (null != explorertypes) {
      File xml = new File(explorertypes);
      SAXReader reader = new SAXReader();
      Document doc;
      try {
        doc = reader.read(xml);
        Element root = doc.getRootElement();
        module.add(root);
      } catch (DocumentException e) {
        module.addElement("explorertypes");
      }

    }
  }

  private void insertResourceTypes(Element module) {
    if (null != resourcetypes) {
      File xml = new File(resourcetypes);
      SAXReader reader = new SAXReader();
      Document doc;
      try {
        doc = reader.read(xml);
        Element root = doc.getRootElement();
        module.add(root);
      } catch (DocumentException e) {
        module.addElement("resourcetypes");
      }
    }

  }

  public void write() throws Exception {
    File tmpFile = new File(distfolder + "/manifest.xml");
    FileOutputStream fos = new FileOutputStream(tmpFile);
    XMLWriter writer = new XMLWriter(fos, OutputFormat.createPrettyPrint());
    writer.write(document);
    writer.close();
  }

  public void setGenerateuuids(String generateuuids) {
    this.generateuuids = ("true".equals(generateuuids)) ? true : false;
  }

  public void setResourcetypes(String resourceTypes) {
    this.resourcetypes = resourceTypes;
  }

  public void setExplorertypes(String explorerTypes) {
    this.explorertypes = explorerTypes;
  }

  public void setDistfolder(String distFolder) {
    this.distfolder = distFolder;
  }

  public void setSrcfolder(String srcFolder) {
    this.srcfolder = srcFolder;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public void setOpencmsversion(String opencmsversion) {
    this.opencmsversion = opencmsversion;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public void setExportversion(String exportversion) {
    this.exportversion = exportversion;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setNicename(String nicename) {
    this.nicename = nicename;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public void setModuleclass(String moduleclass) {
    this.moduleclass = moduleclass;
  }

  public void setModuledescription(String moduledescription) {
    this.moduledescription = moduledescription;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void setAuthorname(String authorname) {
    this.authorname = authorname;
  }

  public void setAuthoremail(String authoremail) {
    this.authoremail = authoremail;
  }

  public void setUserinstalled(String userinstalled) {
    this.userinstalled = userinstalled;
  }

  public void setDateinstalled(String dateinstalled) {
    this.dateinstalled = dateinstalled;
  }

  public ExportPoint createExportPoint() {
    ExportPoint ep = new ExportPoint();
    exportpoints.add(ep);
    return ep;
  }

  public class ExportPoint {
    public ExportPoint() {
    }

    String src;
    String dst;

    public void setSrc(String src) {
      this.src = src;
    }

    public void setDst(String dst) {
      this.dst = dst;
    }

    public String getSrc() {
      return src;
    }

    public String getDst() {
      return dst;
    }
  }

  public Resource createResource() {
    Resource res = new Resource();
    resources.add(res);
    return res;
  }

  public class Resource {
    public Resource() {
    }

    String uri;

    public void setUri(String uri) {
      this.uri = uri;
    }

    public String getUri() {
      return uri;
    }
  }

  public Parameter createParameter() {
    Parameter par = new Parameter();
    parameters.add(par);
    return par;
  }

  public class Parameter {
    public Parameter() {
    }

    String name;
    String value;

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public Dependency createDependency() {
    Dependency dep = new Dependency();
    dependencies.add(dep);
    return dep;
  }

  public class Dependency {
    public Dependency() {
    }

    String name;
    String version;

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getVersion() {
      return version;
    }
  }

  public void addFileset(FileSet fileset) {
    filesets.add(fileset);
  }

  private String folderPropertiesPath(String string) {
    if (string.contains(SEPARATOR))
      return string.subSequence(0, string.lastIndexOf(SEPARATOR) + 1).toString() + "__properties" + SEPARATOR + "__" + string.subSequence(string.lastIndexOf(SEPARATOR) + 1, string.length()).toString() + ".properties";
    else
      return "__properties" + SEPARATOR + "__" + string + ".properties";
  }

  private String folderAccessesPath(String string) {
    if (string.contains(SEPARATOR))
      return string.subSequence(0, string.lastIndexOf(SEPARATOR) + 1).toString() + "__acl" + SEPARATOR + "__" + string.subSequence(string.lastIndexOf(SEPARATOR) + 1, string.length()).toString() + ".xml";
    else
      return "__acl" + SEPARATOR + "__" + string + ".xml";
  }

  private String filePropertiesPath(String string) {
    if (string.contains(SEPARATOR))
      return string.subSequence(0, string.lastIndexOf(SEPARATOR) + 1).toString() + "__properties" + SEPARATOR + string.subSequence(string.lastIndexOf(SEPARATOR) + 1, string.length()).toString() + ".properties";
    else
      return "__properties" + SEPARATOR + string + ".properties";
  }

  private String getEurelisProperty(String key, String propFilePath) {
    String ret = null;
    try {
      Properties props = new Properties();
      if (new File(propFilePath).exists())
        props.load(new FileInputStream(propFilePath));
      else {
        log(propFilePath + " is not an existing file", Project.MSG_WARN);
        if(propFilePath.contains("jsp"))
          System.err.println(propFilePath + " is not an existing file");
      }
      if (!props.isEmpty()) {
        try {
          ret = (String) props.get("EurelisProperty." + key);
        } catch (Exception e) {
          log(e, Project.MSG_ERR);
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      log(e, Project.MSG_ERR);
      e.printStackTrace();
    }

    return ret;
  }

  private void addPropertiesToTree(Element root, String propFilePath) {
    try {
      Properties props = new Properties();
      if (new File(propFilePath).exists())
        props.load(new FileInputStream(propFilePath));
      if (!props.isEmpty()) {
        for (Object keyObject : props.keySet()) {
          try {
            String key = (String) keyObject;
            if(key == null)
              continue;
            String value = props.getProperty(key);
            if(value == null)
              continue;
            if (value.length() > 0) {
              if (key.contains("EurelisProperty")) {
                continue;
              }
              Element property = root.addElement("property");
              property.addElement("name").addText(key.matches("^.*\\.[is]$") ? key.substring(0, key.length() - 2) : key);
              property.addElement("value").add(new FlyweightCDATA(value));
              if (key.endsWith(".s"))
                property.addAttribute("type", "shared");                
            }
          } catch (Exception e) {
            log(e, Project.MSG_ERR);
            e.printStackTrace();
          }
        }
      }

    } catch (Exception e) {
      log(e, Project.MSG_ERR);
      e.printStackTrace();
    }
  }

  private void addAccessesToTree(Element root, String propFilePath) {
    if (null != propFilePath) {
      File xml = new File(propFilePath);
      SAXReader reader = new SAXReader();
      Document doc;
      try {
        doc = reader.read(xml);
        Element elem = doc.getRootElement();
        if (null != elem)
          root.add(elem);
        else
          root.addElement("accesscontrol");
      } catch (DocumentException e) {
        root.addElement("accesscontrol");
      }
    }
  }

}