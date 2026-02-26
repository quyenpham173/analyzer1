package kr.co.weeds.analyzer1.controller;

import java.util.Map;
import kr.co.weeds.analyzer1.service.LogAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

   private final LogAnalysisService logAnalysisService;

   @GetMapping("/throughput")
   public Map<String, Object> getThroughput() {
      return Map.of(
          "todayProcessed", logAnalysisService.getThroughput()
      );
   }

   @PutMapping("/throughput")
   public Map<String, Object> resetThroughput() {
      logAnalysisService.resetThroughput();
      return Map.of(
          "todayProcessed", logAnalysisService.getThroughput()
      );
   }
}
