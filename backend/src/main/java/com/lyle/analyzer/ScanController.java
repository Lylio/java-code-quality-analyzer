package com.lyle.analyzer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Semaphore;
import static com.lyle.analyzer.Model.*;
@RestController @RequestMapping("/api")
public class ScanController {
 private final ZipSources zip;private final JavaAnalyzer analyzer;private final ScanStore store;
 private final Semaphore slot=new Semaphore(1);
 public ScanController(ZipSources zip,JavaAnalyzer analyzer,ScanStore store){this.zip=zip;this.analyzer=analyzer;this.store=store;}
 @GetMapping("/rules") public List<Rule> rules(){return Rules.ALL;}
 @GetMapping("/scans") public List<Summary> list(){return store.list();}
 @GetMapping("/scans/{id}") public Report get(@PathVariable UUID id){return store.get(id);}
 @PostMapping(value="/scans",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
 public Report scan(@RequestParam String projectName,@RequestParam MultipartFile file) throws IOException {
  if(projectName.isBlank()||projectName.strip().length()>100)throw new IllegalArgumentException("Project name must contain 1–100 characters.");
  if(file.isEmpty())throw new IllegalArgumentException("Choose a non-empty source ZIP.");
  if(!slot.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Another scan is running. Try again shortly.");
  try(var input=file.getInputStream()) {var report=analyzer.analyze(projectName.strip(),zip.read(input));store.save(report);return report;}
  finally{slot.release();}
 }
 @GetMapping(value="/scans/{id}/report.json",produces=MediaType.APPLICATION_JSON_VALUE)
 public ResponseEntity<Report> json(@PathVariable UUID id){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=scan-"+id+".json").body(store.get(id));}
 @GetMapping(value="/scans/{id}/findings.csv",produces="text/csv")
 public ResponseEntity<byte[]> csv(@PathVariable UUID id){
  var report=store.get(id);var out=new StringBuilder("Severity,Rule,File,Line,Owner,Measured,Threshold,Message,Recommendation\r\n");
  for(var f:report.findings())out.append(String.join(",",List.of(cell(f.severity()),cell(f.ruleId()),cell(f.path()),cell(f.line()),cell(f.owner()),cell(f.measured()),cell(f.threshold()),cell(f.message()),cell(f.suggestion())))).append("\r\n");
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=findings-"+id+".csv").contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(out.toString().getBytes(StandardCharsets.UTF_8));
 }
 static String cell(Object value){String text=value==null?"":value.toString();if(!text.isEmpty()&&"=+-@\t\r\n".indexOf(text.charAt(0))>=0)text="'"+text;return "\""+text.replace("\"","\"\"")+"\"";}
}
