package kr.co.weeds.analyzer1.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kr.co.weeds.analyzer1.dto.AnalyzeJobDTO;
import kr.co.weeds.analyzer1.entity.postgres.AnalyzeJob;
import kr.co.weeds.analyzer1.exceptions.TechnicalException;
import kr.co.weeds.analyzer1.exceptions.constants.TechnicalAlertCode;
import kr.co.weeds.analyzer1.exceptions.pojo.AlertMessage;
import kr.co.weeds.analyzer1.mapstruct.AnalyzeJobMapper;
import kr.co.weeds.analyzer1.repository.postgres.AnalyzeJobRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleService {

   private final AnalyzeJobRepository analyzeJobRepository;

   public List<AnalyzeJobDTO> getListSchedule(String jobKey) {
      List<AnalyzeJobDTO> results = new ArrayList<>();
      if (StringUtils.isBlank(jobKey)) {
         List<AnalyzeJob> analyzeJobs = analyzeJobRepository.findAll();
         results = AnalyzeJobMapper.MAPPER.toListDto(analyzeJobs);
      } else {
         Optional<AnalyzeJob> jobOptional = analyzeJobRepository.findById(jobKey);
         if (jobOptional.isPresent()) {
            results.add(AnalyzeJobMapper.MAPPER.toDto(jobOptional.get()));
         }
      }
      return results;
   }

   public AnalyzeJob addSchedule(AnalyzeJobDTO analyzeJobDto) {
      Optional<AnalyzeJob> analyzeJobOptional = analyzeJobRepository.findById(analyzeJobDto.getJobKey());
      if (analyzeJobOptional.isPresent()) {
         throw new TechnicalException(AlertMessage.alert(TechnicalAlertCode.RESOURCE_EXIST),
             HttpStatus.BAD_REQUEST);
      }
      AnalyzeJob analyzeJob = AnalyzeJobMapper.MAPPER.toEntity(analyzeJobDto);
      return analyzeJobRepository.save(analyzeJob);
   }

   public AnalyzeJob updateSchedule(AnalyzeJobDTO analyzeJobDto) {
      Optional<AnalyzeJob> analyzeJobOptional = analyzeJobRepository.findById(analyzeJobDto.getJobKey());
      if (analyzeJobOptional.isPresent()) {
         AnalyzeJob analyzeJob = analyzeJobOptional.get();
         analyzeJob.setJobValue(getNewValue(analyzeJob.getJobValue(), analyzeJob.getJobValue()).toString());
         analyzeJob.setDescription(getNewValue(analyzeJob.getDescription(), analyzeJob.getDescription()).toString());
         analyzeJob.setEnabled((Boolean) getNewValue(analyzeJob.getEnabled(), analyzeJob.getEnabled()));
         analyzeJob.setJobType(getNewValue(analyzeJob.getJobType(), analyzeJob.getJobType()).toString());
         analyzeJob.setJobMethod(getNewValue(analyzeJob.getJobMethod(), analyzeJob.getJobMethod()).toString());
         return analyzeJobRepository.save(analyzeJob);
      }
      throw new TechnicalException(AlertMessage.alert(TechnicalAlertCode.RESOURCE_NOT_FOUND),
          HttpStatus.BAD_REQUEST);
   }

   private Object getNewValue(Object newValue, Object oldValue) {
      if (newValue != null) {
         return newValue;
      } else {
         return oldValue;
      }
   }

}
