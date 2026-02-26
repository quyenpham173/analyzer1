package kr.co.weeds.analyzer1.mapstruct;

import java.util.List;
import kr.co.weeds.analyzer1.dto.AnalyzeJobDTO;
import kr.co.weeds.analyzer1.entity.postgres.AnalyzeJob;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AnalyzeJobMapper {

   AnalyzeJobMapper MAPPER = Mappers.getMapper(AnalyzeJobMapper.class);

   AnalyzeJobDTO toDto(AnalyzeJob entity);

   AnalyzeJob toEntity(AnalyzeJobDTO dto);

   List<AnalyzeJobDTO> toListDto(List<AnalyzeJob> entity);

   List<AnalyzeJob> toListEntity(List<AnalyzeJobDTO> dto);

}
