package com.lyle.analyzer;
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import org.springframework.stereotype.Component;
import java.nio.*;
import java.nio.charset.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import static com.lyle.analyzer.Model.*;
@Component
public class JavaAnalyzer {
 public Report analyze(String project,Map<String,byte[]> sources) {
  long started=System.nanoTime();var files=new ArrayList<SourceFile>();var methods=new ArrayList<MethodMetric>();var findings=new ArrayList<Finding>();var notes=new ArrayList<String>();
  var parser=new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
  for(var entry:new TreeMap<>(sources).entrySet()) {
   String path=entry.getKey(),source;
   try {source=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(entry.getValue())).toString();}
   catch(CharacterCodingException e) {files.add(new SourceFile(path,0,false,"File is not valid UTF-8.",0,0,0,""));continue;}
   if(source.startsWith("﻿"))source=source.substring(1);
   try {
    var parsed=parser.parse(source);
    if(!parsed.isSuccessful()||parsed.getResult().isEmpty()) {
     String error=parsed.getProblems().isEmpty()?"Unable to parse Java source.":parsed.getProblems().getFirst().getVerboseMessage();
     files.add(new SourceFile(path,lineCount(source),false,clip(error,1500),0,0,0,source));continue;
    }
    var unit=parsed.getResult().orElseThrow();int methodStart=methods.size();
    for(var type:unit.findAll(TypeDeclaration.class)) {
     if(length(type)>300)add(findings,"LARGE_TYPE",path,type.getNameAsString(),type,length(type));
    }
    var callables=new ArrayList<Node>();callables.addAll(unit.findAll(MethodDeclaration.class));callables.addAll(unit.findAll(ConstructorDeclaration.class));callables.addAll(unit.findAll(CompactConstructorDeclaration.class));
    callables.sort(Comparator.comparingInt(JavaAnalyzer::line));
    for(var callable:callables) {
     if(methods.size()>=10000)throw new IllegalArgumentException("Project exceeds 10000 callable declarations.");
     int parameters;String name;boolean body;
     if(callable instanceof MethodDeclaration m) {parameters=m.getParameters().size();name=m.getSignature().asString();body=m.getBody().isPresent();}
     else if(callable instanceof ConstructorDeclaration c) {parameters=c.getParameters().size();name=c.getSignature().asString();body=true;}
     else {var c=(CompactConstructorDeclaration)callable;parameters=0;name=c.getNameAsString()+" (compact constructor)";body=true;}
     int[] decisions={body?1:0},nesting={0};
     walk(callable,callable,n->{if(decision(n))decisions[0]++;});
     nesting[0]=nesting(callable,callable,0);
     var metric=new MethodMetric(path,name,line(callable),decisions[0],length(callable),parameters,nesting[0]);methods.add(metric);
     if(metric.complexity()>10)add(findings,"COMPLEX_METHOD",path,name,callable,metric.complexity());
     if(metric.maxNesting()>3)add(findings,"DEEP_NESTING",path,name,callable,metric.maxNesting());
     if(metric.lines()>50)add(findings,"LONG_METHOD",path,name,callable,metric.lines());
     if(parameters>5)add(findings,"MANY_PARAMETERS",path,name,callable,parameters);
    }
    for(var block:unit.findAll(CatchClause.class))if(block.getBody().getStatements().isEmpty())add(findings,"EMPTY_CATCH",path,"catch ("+block.getParameter()+")",block,null);
    int maximum=methods.subList(methodStart,methods.size()).stream().mapToInt(MethodMetric::complexity).max().orElse(0);
    files.add(new SourceFile(path,lineCount(source),true,null,unit.findAll(TypeDeclaration.class).size(),methods.size()-methodStart,maximum,source));
   } catch(StackOverflowError e) {
    methods.removeIf(m->m.path().equals(path));findings.removeIf(f->f.path().equals(path));
    files.add(new SourceFile(path,lineCount(source),false,"Source nesting exceeded parser capacity.",0,0,0,source));
   }
  }
  // Each capped list is explicitly marked partial; results never imply full coverage.
  boolean capped=findings.size()>Rules.FINDING_LIMIT;
  if(capped) {findings=new ArrayList<>(findings.subList(0,Rules.FINDING_LIMIT));notes.add("Findings limited to the first 2000 in source-path order. Narrow the project to inspect the remaining findings.");}
  int failed=(int)files.stream().filter(f->!f.parsed()).count();
  if(failed>0)notes.add(failed+" file(s) could not be parsed. Their metrics/findings are excluded; see Files for diagnostics.");
  notes.add("Heuristic Java 21 syntax analysis, not a compiler, security audit or proof of correctness. No symbol/type resolution is performed.");
  var summary=new Summary(UUID.randomUUID(),project,Instant.now(),failed>0||capped?"PARTIAL":"COMPLETED",files.size(),files.size()-failed,failed,methods.size(),findings.size(),(int)methods.stream().filter(m->m.complexity()>10).count(),(System.nanoTime()-started)/1000000);
  findings.sort(Comparator.comparingInt((Finding f)->switch(f.severity()){case "HIGH"->0;case "MEDIUM"->1;default->2;}).thenComparing(Finding::path).thenComparingInt(Finding::line));
  return new Report(summary,Rules.VERSION,Rules.ALL,List.copyOf(files),List.copyOf(methods),List.copyOf(findings),List.copyOf(notes));
 }
 private static boolean excluded(Node n,Node root) {return n!=root&&(n instanceof BodyDeclaration<?>||n instanceof LambdaExpr);}
 private static void walk(Node n,Node root,Consumer<Node> action) {
  if(excluded(n,root))return;action.accept(n);for(var child:n.getChildNodes())walk(child,root,action);
 }
 private static boolean decision(Node n) {
  return n instanceof IfStmt||n instanceof ForStmt||n instanceof ForEachStmt||n instanceof WhileStmt||n instanceof DoStmt||n instanceof CatchClause||n instanceof ConditionalExpr
   ||n instanceof SwitchEntry s&&!s.getLabels().isEmpty()
   ||n instanceof BinaryExpr b&&(b.getOperator()==BinaryExpr.Operator.AND||b.getOperator()==BinaryExpr.Operator.OR);
 }
 private static int nesting(Node n,Node root,int depth) {
  if(excluded(n,root))return depth;
  boolean control=n instanceof IfStmt||n instanceof ForStmt||n instanceof ForEachStmt||n instanceof WhileStmt||n instanceof DoStmt||n instanceof SwitchStmt||n instanceof SwitchExpr||n instanceof ConditionalExpr;
  int next=depth+(control?1:0),maximum=next;
  for(var child:n.getChildNodes())if(!excluded(child,root))maximum=Math.max(maximum,nesting(child,root,next));
  return maximum;
 }
 private static int line(Node n) {return n.getBegin().map(p->p.line).orElse(1);}
 private static int length(Node n) {return n.getRange().map(r->r.end.line-r.begin.line+1).orElse(1);}
 private static int lineCount(String source) {return source.isEmpty()?0:source.split("\n",-1).length;}
 private static String clip(String value,int limit) {return value.length()<=limit?value:value.substring(0,limit);}
 private static void add(List<Finding> list,String ruleId,String path,String owner,Node node,Integer measured) {
  // Keep one extra entry as a truncation signal, without allocating an unbounded finding list.
  if(list.size()>Rules.FINDING_LIMIT)return;
  var rule=Rules.get(ruleId);String message=measured==null?rule.description():"Measured "+measured+"; threshold "+rule.threshold()+". "+rule.description();
  list.add(new Finding("F"+(list.size()+1),ruleId,rule.title(),rule.severity(),path,owner,line(node),line(node)+length(node)-1,measured,rule.threshold(),message,rule.suggestion()));
 }
}
