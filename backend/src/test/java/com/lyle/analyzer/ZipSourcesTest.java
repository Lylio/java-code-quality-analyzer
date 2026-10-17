package com.lyle.analyzer;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;
class ZipSourcesTest {
 static byte[] zip(Map<String,byte[]> files) throws IOException {var out=new ByteArrayOutputStream();try(var z=new ZipOutputStream(out)){for(var f:files.entrySet()){z.putNextEntry(new ZipEntry(f.getKey()));z.write(f.getValue());z.closeEntry();}}return out.toByteArray();}
 Map<String,byte[]> read(byte[] bytes)throws IOException{return new ZipSources().read(new ByteArrayInputStream(bytes));}
 @Test void ignoreBuildFoldersAndSort()throws Exception {assertThat(read(zip(Map.of("src/Z.java","class Z{}".getBytes(),"src/A.java","class A{}".getBytes(),"target/G.java","bad".getBytes()))).keySet()).containsExactly("src/A.java","src/Z.java");}
 @Test void traversalPathsAreRejected()throws Exception{for(String name:List.of("../X.java","/X.java","C:/X.java","src/../../x.txt","..\\X.java")){var bytes=zip(Map.of(name,new byte[0]));assertThatThrownBy(()->read(bytes)).hasMessageContaining("unsafe");}}
 @Test void normalizedDuplicatesAreRejected()throws Exception{var bytes=zip(Map.of("src/./A.java",new byte[0],"src/A.java",new byte[0]));assertThatThrownBy(()->read(bytes)).hasMessageContaining("duplicate");}
 @Test void sourceAndExpandedLimitsAreEnforced()throws Exception{
  var source=zip(Map.of("X.java",new byte[ZipSources.MAX_FILE_BYTES+1]));assertThatThrownBy(()->read(source)).hasMessageContaining("128 KiB");
  var bomb=zip(Map.of("ignored.bin",new byte[ZipSources.MAX_EXPANDED_BYTES+1]));assertThatThrownBy(()->read(bomb)).hasMessageContaining("expanded");
 }
 @Test void tooManyJavaFilesAreRejected()throws Exception{var files=new HashMap<String,byte[]>();for(int i=0;i<201;i++)files.put(i+".java",new byte[0]);var bytes=zip(files);assertThatThrownBy(()->read(bytes)).hasMessageContaining("200 Java");}
 @Test void nonZipAndNoJavaAreRejected()throws Exception{assertThatThrownBy(()->read("not ZIP".getBytes())).hasMessageContaining("valid ZIP");var bytes=zip(Map.of("README.md",new byte[0]));assertThatThrownBy(()->read(bytes)).hasMessageContaining("No Java");}
}
