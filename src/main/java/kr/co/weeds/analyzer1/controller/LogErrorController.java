package kr.co.weeds.analyzer1.controller;

import java.io.IOException;
import java.util.List;
import kr.co.weeds.analyzer1.dto.LogErrorFileDTO;
import kr.co.weeds.analyzer1.service.LogErrorService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/log-error")
@AllArgsConstructor
public class LogErrorController {

   private final LogErrorService logErrorService;

   @GetMapping("/list")
   public ResponseEntity<List<LogErrorFileDTO>> getFailedLogs(
       @RequestParam(required = false, defaultValue = "0") Integer size,
       @RequestParam(required = false, defaultValue = "0") Integer from) throws IOException {
      return ResponseEntity.ok(logErrorService.getLogErrorsInFile(size, from));
   }

}
