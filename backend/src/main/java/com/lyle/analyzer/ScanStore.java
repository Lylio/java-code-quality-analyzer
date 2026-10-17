package com.lyle.analyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import static com.lyle.analyzer.Model.*;
@Repository
public class ScanStore {
 private final JdbcTemplate db;private final ObjectMapper json;
 public ScanStore(JdbcTemplate db,ObjectMapper json) {this.db=db;this.json=json;}
 public void save(Report report) {
  try {db.update("insert into scans(id,project_name,created_at,summary,report) values (?,?,?,?,?)",report.summary().id(),report.summary().projectName(),java.sql.Timestamp.from(report.summary().createdAt()),json.writeValueAsString(report.summary()),json.writeValueAsString(report));}
  catch(com.fasterxml.jackson.core.JsonProcessingException e) {throw new IllegalStateException("Unable to serialize scan",e);}
 }
 public List<Summary> list() {return db.query("select summary from scans order by created_at desc limit 100",(rs,n)->decode(rs.getString(1),Summary.class));}
 public Report get(UUID id) {
  var list=db.query("select report from scans where id=?",(rs,n)->decode(rs.getString(1),Report.class),id);
  if(list.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Scan not found");return list.getFirst();
 }
 private <T> T decode(String value,Class<T> type) {
  try{return json.readValue(value,type);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Unable to read stored scan",e);}
 }
}
