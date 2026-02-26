package kr.co.weeds.analyzer1.service;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import kr.co.weeds.analyzer1.constant.StringConstant;
import kr.co.weeds.analyzer1.dto.TraceConfigDTO;
import kr.co.weeds.analyzer1.entity.postgres.TraceAnalyzeConfig;
import kr.co.weeds.analyzer1.mapstruct.TraceConfigMapper;
import kr.co.weeds.analyzer1.repository.postgres.TraceConfigRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@AllArgsConstructor
public class TraceConfigInfoService {

   private static final Logger LOGGER = LogManager.getLogger(TraceConfigInfoService.class);

   private final TraceConfigRepository traceConfigRepository;

   public TraceAnalyzeConfig createTraceConfig(TraceConfigDTO traceConfig, MultipartFile configFile)
       throws IOException {
      if (configFile != null) {
         String config = new String(configFile.getBytes(), StandardCharsets.UTF_8);
         traceConfig.setConfiguration(config);
      }
      if (StringUtils.isBlank(traceConfig.getAppId())) {
         traceConfig.setAppId(StringConstant.APP_ID);
      }
      Optional<TraceAnalyzeConfig> traceConfigInfoOpt = traceConfigRepository.findByTraceIdAndAppId(
          traceConfig.getTraceId(), traceConfig.getAppId());
      if (traceConfigInfoOpt.isPresent()) {
         throw new BadRequestException(
             String.format("Trace id %s and appId %s existed in table.", traceConfig.getTraceId(),
                 traceConfig.getAppId()));
      }
      TraceAnalyzeConfig traceAnalyzeConfig = TraceConfigMapper.MAPPER.toEntity(traceConfig);
      return traceConfigRepository.save(traceAnalyzeConfig);
   }

   public TraceAnalyzeConfig updateTraceConfig(String traceId, MultipartFile configFile) throws IOException {
      Optional<TraceAnalyzeConfig> traceConfigInfoOpt = traceConfigRepository.findByTraceIdAndAppId(traceId,
          StringConstant.APP_ID);
      if (traceConfigInfoOpt.isPresent()) {
         TraceAnalyzeConfig traceConfigInfo = traceConfigInfoOpt.get();
         String config = new String(configFile.getBytes(), StandardCharsets.UTF_8);
         traceConfigInfo.setConfiguration(config);
         return traceConfigRepository.save(traceConfigInfo);
      }
      LOGGER.error("Cannot get trace config for traceID {}", traceId);
      return null;
   }

   public Properties getTraceConfig(String traceId) throws IOException {
      Optional<TraceAnalyzeConfig> configOpt = traceConfigRepository.findByTraceIdAndAppId(traceId,
          StringConstant.APP_ID);
      Properties properties = new Properties();
      if (configOpt.isPresent()) {
         String config = configOpt.get().getConfiguration();
         properties.load(new StringReader(config));
      }
      return properties;
   }

}
