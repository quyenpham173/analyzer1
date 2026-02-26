package kr.co.weeds.analyzer1.controller;

import java.io.IOException;
import java.util.Properties;
import kr.co.weeds.analyzer1.dto.TraceConfigDTO;
import kr.co.weeds.analyzer1.entity.postgres.TraceAnalyzeConfig;
import kr.co.weeds.analyzer1.service.TraceConfigInfoService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/trace-config")
@AllArgsConstructor
public class TraceConfigInfoController {

   private final TraceConfigInfoService traceConfigInfoService;

   @PostMapping(consumes = {"multipart/form-data"})
   public ResponseEntity<TraceAnalyzeConfig> createTraceConfig(@RequestPart("traceConfig") TraceConfigDTO traceConfig,
       @RequestPart(value = "configFile", required = false) MultipartFile configFile) throws IOException {
      return ResponseEntity.ok(traceConfigInfoService.createTraceConfig(traceConfig, configFile));
   }

   @PutMapping("/{traceId}")
   public ResponseEntity<TraceAnalyzeConfig> updateTraceConfig(@PathVariable String traceId,
       @RequestBody MultipartFile configFile) throws IOException {
      return ResponseEntity.ok(traceConfigInfoService.updateTraceConfig(traceId, configFile));
   }

   @GetMapping()
   public ResponseEntity<Properties> getTraceConfig(@RequestParam("traceId") String traceId) throws IOException {
      return ResponseEntity.ok(traceConfigInfoService.getTraceConfig(traceId));
   }

}
