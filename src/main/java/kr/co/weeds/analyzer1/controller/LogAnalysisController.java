package kr.co.weeds.analyzer1.controller;

import java.util.List;
import kr.co.weeds.analyzer1.dto.ResponseDTO;
import kr.co.weeds.analyzer1.model.DocumentModel;
import kr.co.weeds.analyzer1.model.ana1.LogRegisterModel;
import kr.co.weeds.analyzer1.service.LogAnalysisService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyze")
@AllArgsConstructor
public class LogAnalysisController {

   private final LogAnalysisService logAnalysisService;

   @GetMapping("/test")
   public ResponseEntity<DocumentModel<LogRegisterModel>> testAnalyze(@RequestParam String documentId) {
      DocumentModel<LogRegisterModel> documentModel = logAnalysisService.testAnalyze(documentId);
      return ResponseEntity.ok(documentModel);
   }

   @GetMapping("/manual")
   public ResponseEntity<ResponseDTO> manualAnalyze(@RequestParam List<String> docIds) {
      ResponseDTO result = logAnalysisService.manualAnalyze(docIds);
      return ResponseEntity.ok(result);
   }

   @GetMapping("/re-analysis")
   public ResponseEntity<ResponseDTO> reAnalysisLog(@RequestParam(required = false, defaultValue = "") List<String> docIds,
       @RequestParam(required = false, defaultValue = "0") Integer size,
       @RequestParam(required = false, defaultValue = "0") Integer from) {
      return ResponseEntity.ok(logAnalysisService.reAnalysis(docIds, from, size));
   }

   @GetMapping("/analyze-by-date")
   public ResponseEntity<ResponseDTO> reAnalysisByDate(@RequestParam String fromDate,
       @RequestParam(required = false) String toDate) {
      return ResponseEntity.ok(logAnalysisService.reAnalysisByDate(fromDate, toDate));
   }

}
