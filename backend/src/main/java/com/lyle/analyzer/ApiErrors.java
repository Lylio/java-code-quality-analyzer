package com.lyle.analyzer;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.io.IOException;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(IllegalArgumentException.class)
 public ProblemDetail invalid(IllegalArgumentException e){return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,e.getMessage());}
 @ExceptionHandler(IOException.class)
 public ProblemDetail zip(IOException e){return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"ZIP could not be read. Check archive integrity and UTF-8 filenames.");}
 @ExceptionHandler(MaxUploadSizeExceededException.class)
 public ProblemDetail size(MaxUploadSizeExceededException e){return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,"Upload exceeds 20 MiB. Create a ZIP containing source files only.");}
 @ExceptionHandler(ResponseStatusException.class)
 public ProblemDetail status(ResponseStatusException e){return ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason()==null?"Request failed":e.getReason());}
}
