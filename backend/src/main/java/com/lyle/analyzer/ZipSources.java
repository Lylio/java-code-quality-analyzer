package com.lyle.analyzer;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
@Component
public class ZipSources {
 public static final int MAX_ENTRIES=2000,MAX_FILES=200,MAX_FILE_BYTES=128*1024,MAX_EXPANDED_BYTES=10*1024*1024;
 private static final Set<String> IGNORED=Set.of(".git",".idea",".gradle","target","build","out","dist","node_modules","__MACOSX");
 public Map<String,byte[]> read(InputStream input) throws IOException {
  var raw=new PushbackInputStream(new BufferedInputStream(input),4);
  byte[] header=raw.readNBytes(4);raw.unread(header);
  if(header.length!=4||header[0]!='P'||header[1]!='K'||!((header[2]==3&&header[3]==4)||(header[2]==5&&header[3]==6)))
   throw new IllegalArgumentException("Upload a valid ZIP archive of Java source files.");
  var sources=new TreeMap<String,byte[]>();var names=new HashSet<String>();int entries=0,total=0;
  try(var zip=new ZipInputStream(raw,StandardCharsets.UTF_8)) {
   ZipEntry entry;
   byte[] buffer=new byte[8192];
   while((entry=zip.getNextEntry())!=null) {
    if(++entries>MAX_ENTRIES)throw new IllegalArgumentException("ZIP exceeds the 2000-entry limit. Exclude build/dependency folders.");
    String name=entry.getName().replace('\\','/');
    if(name.startsWith("/")||name.matches("^[A-Za-z]:.*")||Arrays.asList(name.split("/",-1)).contains(".."))
     throw new IllegalArgumentException("ZIP contains an unsafe path.");
    name=java.nio.file.Path.of(name).normalize().toString().replace('\\','/');
    if(name.length()>500||!names.add(name))throw new IllegalArgumentException("ZIP contains duplicate or overlong paths.");
    boolean ignored=Arrays.stream(name.split("/")).anyMatch(IGNORED::contains);
    boolean java=!entry.isDirectory()&&!ignored&&name.endsWith(".java");
    if(java&&sources.size()>=MAX_FILES)throw new IllegalArgumentException("ZIP exceeds the 200 Java-file limit.");
    var bytes=java?new ByteArrayOutputStream():null;int size=0,read;
    while((read=zip.read(buffer))!=-1) {
     total+=read;size+=read;
     if(total>MAX_EXPANDED_BYTES)throw new IllegalArgumentException("ZIP exceeds the 10 MiB expanded-data limit, including ignored entries.");
     if(java&&size>MAX_FILE_BYTES)throw new IllegalArgumentException("Java file exceeds 128 KiB: "+name);
     if(java)bytes.write(buffer,0,read);
    }
    if(java)sources.put(name,bytes.toByteArray());
    zip.closeEntry();
   }
  }
  if(sources.isEmpty())throw new IllegalArgumentException("No Java source files found outside ignored directories.");
  return sources;
 }
}
