package com.lyle.analyzer;
import java.util.*;
import static com.lyle.analyzer.Model.*;
public final class Rules {
 private Rules() {}
 public static final String VERSION="1.0";
 public static final int FINDING_LIMIT=2000;
 public static final List<Rule> ALL=List.of(
  new Rule("COMPLEX_METHOD","Excessive method complexity","HIGH",10,"Simplified cyclomatic complexity exceeds 10. Counts if, loops, catch, ternary, non-default switch entries, && and ||; starts at 1 for a body. Nested callable/type/lambda bodies are excluded.","Extract independent decisions into named methods and review unnecessary branching."),
  new Rule("DEEP_NESTING","Deep nesting","HIGH",3,"More than 3 syntactic levels of if, loops, switch or conditional expressions within a callable. Else-if contributes another syntactic level.","Use guard clauses or extract nested work into smaller methods."),
  new Rule("LONG_METHOD","Long method","MEDIUM",50,"A method or explicit constructor spans more than 50 physical lines, including comments and blank lines.","Separate responsibilities into focused methods; review the method before splitting mechanically."),
  new Rule("LARGE_TYPE","Large class or type","MEDIUM",300,"A class, interface, enum or record spans more than 300 physical lines, including nested declarations.","Review whether the type combines responsibilities that deserve separate types."),
  new Rule("MANY_PARAMETERS","Too many parameters","MEDIUM",5,"More than 5 explicitly declared parameters on a method or constructor.","Consider a cohesive parameter object when those values form one concept."),
  new Rule("EMPTY_CATCH","Empty catch block","LOW",null,"A catch body contains no statements. A comment alone does not count as handling the exception.","Handle, propagate, or explicitly document why ignoring this exception is appropriate.")
 );
 public static Rule get(String id) {return ALL.stream().filter(r->r.id().equals(id)).findFirst().orElseThrow();}
}
