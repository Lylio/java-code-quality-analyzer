package com.lyle.analyzer;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
class AnalyzerTest {
 final JavaAnalyzer analyzer=new JavaAnalyzer();
 Model.Report scan(String code){return analyzer.analyze("Test",Map.of("Example.java",code.getBytes(StandardCharsets.UTF_8)));}
 @Test void java21RecordsAndCompactConstructors(){
  var r=scan("record Example(int n) { Example { if(n<0) throw new IllegalArgumentException(); } int twice(){return n*2;} }");
  assertThat(r.summary().status()).isEqualTo("COMPLETED");assertThat(r.methods()).extracting(Model.MethodMetric::complexity).containsExactlyInAnyOrder(2,1);
 }
 @Test void commentsAndStringTokensAreNotBranches(){
  var r=scan("class Example { int m(){ /* if && */ String s=\"if && ||\";return 1;} }");
  assertThat(r.methods().getFirst().complexity()).isEqualTo(1);assertThat(r.findings()).isEmpty();
 }
 @Test void nestedMethodsAndLambdasDoNotInflateOuterMethod(){
  var r=scan("class Example { void outer(){Runnable r=()->{if(true){}};class Nested {void inner(){if(true){}}}} }");
  assertThat(r.methods()).filteredOn(m->m.name().equals("outer()")).singleElement().satisfies(m->assertThat(m.complexity()).isEqualTo(1));
  assertThat(r.methods()).filteredOn(m->m.name().equals("inner()")).singleElement().satisfies(m->assertThat(m.complexity()).isEqualTo(2));
 }
 @Test void countsSpecifiedDecisionKinds(){
  var r=scan("class Example {int m(int x){if(x>0 && x<9){}for(int i=0;i<x;i++){}while(x>0){x--;}do{x++;}while(x<0);try{x++;}catch(Exception e){x--;}return x>1?1:0;} }");
  assertThat(r.methods().getFirst().complexity()).isEqualTo(8);
 }
 @Test void complexityThresholdAndLineNumbers(){
  assertThat(scan("class Example {void m(){"+"if(true){}".repeat(9)+"}}").findings()).noneMatch(f->f.ruleId().equals("COMPLEX_METHOD"));
  var over=scan("class Example {\nvoid m(){\n"+"if(true){}\n".repeat(10)+"}\n}");
  assertThat(over.findings()).filteredOn(f->f.ruleId().equals("COMPLEX_METHOD")).singleElement().satisfies(f->{assertThat(f.measured()).isEqualTo(11);assertThat(f.threshold()).isEqualTo(10);assertThat(f.line()).isEqualTo(2);});
 }
 @Test void otherFiveRules(){
  String code="class Example {\n"+"\n".repeat(301)+"void m(int a,int b,int c,int d,int e,int f){\n"+"\n".repeat(51)+"if(true){if(true){if(true){if(true){}}}}\ntry{}catch(Exception e){ /* blank */ }\n}\n}";
  assertThat(scan(code).findings()).extracting(Model.Finding::ruleId).contains("LARGE_TYPE","LONG_METHOD","MANY_PARAMETERS","DEEP_NESTING","EMPTY_CATCH");
 }
 @Test void parameterAndNestingBoundaries(){
  assertThat(scan("class Example {void m(int a,int b,int c,int d,int e){if(true){if(true){if(true){}}}}}").findings()).noneMatch(f->f.ruleId().equals("MANY_PARAMETERS")||f.ruleId().equals("DEEP_NESTING"));
 }
 @Test void switchEntryGroupsCountOnceAndDefaultAddsNothing(){
  assertThat(scan("class Example {int m(int x){return switch(x){case 1,2 -> 1;default -> 0;};}}").methods().getFirst().complexity()).isEqualTo(2);
 }
 @Test void parseFailureIsPartialAndExcluded(){
  var r=analyzer.analyze("Test",Map.of("Good.java","class Good {void m(){}}".getBytes(),"Bad.java","class Bad { void ???".getBytes()));
  assertThat(r.summary().status()).isEqualTo("PARTIAL");assertThat(r.summary().failedFiles()).isEqualTo(1);assertThat(r.summary().methodCount()).isEqualTo(1);
  assertThat(r.files()).filteredOn(f->!f.parsed()).singleElement().satisfies(f->assertThat(f.parseError()).isNotBlank());
 }
 @Test void invalidUtf8IsReported(){assertThat(analyzer.analyze("Test",Map.of("Bad.java",new byte[]{(byte)0xc3,0x28})).summary().failedFiles()).isEqualTo(1);}
 @Test void bomIsAccepted(){assertThat(scan("\uFEFFclass Example {}").summary().parsedFiles()).isEqualTo(1);}
 @Test void cappedFindingsAreExplicitlyPartial(){
  var r=scan("class Example {void m(){"+"try{}catch(Exception e){}".repeat(2010)+"}}");
  assertThat(r.findings()).hasSize(2000);assertThat(r.summary().status()).isEqualTo("PARTIAL");assertThat(r.notes()).anyMatch(n->n.contains("limited"));
 }
}
