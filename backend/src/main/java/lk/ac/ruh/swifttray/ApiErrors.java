package lk.ac.ruh.swifttray;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String,String>> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Request could not be completed.":e.getReason())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException e) {
        var f=e.getBindingResult().getFieldErrors().get(0);
        return ResponseEntity.badRequest().body(Map.of("message",f.getField()+": "+f.getDefaultMessage()));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String,String>> malformed() { return ResponseEntity.badRequest().body(Map.of("message","Please check the submitted values.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String,String>> conflict() { return ResponseEntity.status(409).body(Map.of("message","This record already exists or changed. Refresh and try again.")); }
}
