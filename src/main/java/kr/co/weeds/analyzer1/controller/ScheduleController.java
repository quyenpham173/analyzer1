package kr.co.weeds.analyzer1.controller;

import java.util.List;
import kr.co.weeds.analyzer1.dto.AnalyzeJobDTO;
import kr.co.weeds.analyzer1.entity.postgres.AnalyzeJob;
import kr.co.weeds.analyzer1.service.ScheduleService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule")
@AllArgsConstructor
public class ScheduleController {

   private final ScheduleService scheduleService;

   @GetMapping()
   public ResponseEntity<List<AnalyzeJobDTO>> getSchedule(
       @RequestParam(name = "jobKey", required = false) String jobKey) {
      return ResponseEntity.ok(scheduleService.getListSchedule(jobKey));
   }

   @PostMapping()
   public ResponseEntity<AnalyzeJob> addSchedule(@RequestBody AnalyzeJobDTO analyzeJobDto) {
      return ResponseEntity.ok(scheduleService.addSchedule(analyzeJobDto));
   }

   @PutMapping()
   public ResponseEntity<AnalyzeJob> updateSchedule(@RequestBody AnalyzeJobDTO analyzeJobDto) {
      return ResponseEntity.ok(scheduleService.updateSchedule(analyzeJobDto));
   }

}
