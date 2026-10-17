package com.lyle.analyzer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ScanApiTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate db;
 @BeforeEach void clean(){db.update("delete from scans");}
 @Test void uploadPersistsAndExports()throws Exception{
  var file=new MockMultipartFile("file","source.zip","application/zip",ZipSourcesTest.zip(Map.of("Example.java","class Example {void m(){try{}catch(Exception e){}}}".getBytes())));
  String response=mvc.perform(multipart("/api/scans").file(file).param("projectName","Example")).andExpect(status().isCreated()).andExpect(jsonPath("$.summary.findingCount").value(1)).andReturn().getResponse().getContentAsString();
  String id=json.readTree(response).path("summary").path("id").asText();
  mvc.perform(get("/api/scans")).andExpect(status().isOk()).andExpect(jsonPath("$[0].projectName").value("Example"));
  mvc.perform(get("/api/scans/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.files[0].content").value(org.hamcrest.Matchers.containsString("class Example")));
  mvc.perform(get("/api/scans/"+id+"/report.json")).andExpect(status().isOk()).andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString("attachment")));
  String csv=mvc.perform(get("/api/scans/"+id+"/findings.csv")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertThat(csv).contains("EMPTY_CATCH").contains("\r\n");
  assertThat(db.queryForObject("select count(*) from scans",Integer.class)).isEqualTo(1);
 }
 @Test void invalidUploadsAreNotPersisted()throws Exception{
  var bad=new MockMultipartFile("file","bad.zip","application/zip","not ZIP".getBytes());
  mvc.perform(multipart("/api/scans").file(bad).param("projectName","X")).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/scans").file(bad).param("projectName","   ")).andExpect(status().isBadRequest());
  assertThat(db.queryForObject("select count(*) from scans",Integer.class)).isZero();
 }
 @Test void rulesAndMissingScan()throws Exception{mvc.perform(get("/api/rules")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));mvc.perform(get("/api/scans/"+UUID.randomUUID())).andExpect(status().isNotFound());}
 @Test void csvEscaping(){assertThat(ScanController.cell("=A1+2")).isEqualTo("\"'=A1+2\"");assertThat(ScanController.cell("a\"b")).isEqualTo("\"a\"\"b\"");}
}
