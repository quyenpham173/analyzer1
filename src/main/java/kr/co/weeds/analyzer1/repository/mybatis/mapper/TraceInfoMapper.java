package kr.co.weeds.analyzer1.repository.mybatis.mapper;

import java.util.List;
import kr.co.weeds.analyzer1.model.loader.TraceInfo;
import kr.co.weeds.analyzer1.model.loader.TraceLoginInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TraceInfoMapper {

   List<TraceInfo> getTraceInfoList();

   List<TraceLoginInfo> getTraceLoginInfoList();

   void insertNewTraceInfo(@Param("newTrace") TraceInfo newTrace, @Param("parentTrace") TraceInfo parentTrace);

}
