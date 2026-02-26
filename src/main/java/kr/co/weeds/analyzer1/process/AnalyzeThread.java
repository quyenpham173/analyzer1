package kr.co.weeds.analyzer1.process;

import java.util.concurrent.CountDownLatch;
import kr.co.weeds.analyzer1.model.DocumentModel;
import kr.co.weeds.analyzer1.model.LoaderInfo;
import kr.co.weeds.analyzer1.model.config.LogAnalysisConfig;
import kr.co.weeds.analyzer1.model.lh.LogHouseModel;
import kr.co.weeds.analyzer1.service.LogAnalysisService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class AnalyzeThread implements Runnable {

   private DocumentModel<LogHouseModel> documentModel;

   private LogAnalysisConfig logAnalysisConfig;

   private LoaderInfo loaderInfo;

   private LogAnalysisService analysisService;

   private CountDownLatch countDownLatch;

   @Override
   public void run() {
      analysisService.startAnalysisThread(documentModel, logAnalysisConfig, loaderInfo);
      countDownLatch.countDown();
   }

}
