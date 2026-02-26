package kr.co.weeds.analyzer1.mapstruct;

import kr.co.weeds.analyzer1.dto.TraceConfigDTO;
import kr.co.weeds.analyzer1.entity.postgres.TraceAnalyzeConfig;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TraceConfigMapper {

   TraceConfigMapper MAPPER = Mappers.getMapper(TraceConfigMapper.class);

   TraceConfigDTO toDto(TraceAnalyzeConfig entity);

   TraceAnalyzeConfig toEntity(TraceConfigDTO dto);

}
