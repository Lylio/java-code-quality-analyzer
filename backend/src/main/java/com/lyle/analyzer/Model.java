package com.lyle.analyzer;
import java.time.Instant;
import java.util.*;
public final class Model {
 private Model() {}
 public record Rule(String id,String title,String severity,Integer threshold,String description,String suggestion) {}
 public record Finding(String id,String ruleId,String title,String severity,String path,String owner,int line,int endLine,Integer measured,Integer threshold,String message,String suggestion) {}
 public record MethodMetric(String path,String name,int line,int complexity,int lines,int parameters,int maxNesting) {}
 public record SourceFile(String path,int lines,boolean parsed,String parseError,int typeCount,int methodCount,int maxComplexity,String content) {}
 public record Summary(UUID id,String projectName,Instant createdAt,String status,int fileCount,int parsedFiles,int failedFiles,int methodCount,int findingCount,int highComplexityMethods,long durationMs) {}
 public record Report(Summary summary,String ruleVersion,List<Rule> rules,List<SourceFile> files,List<MethodMetric> methods,List<Finding> findings,List<String> notes) {}
}
