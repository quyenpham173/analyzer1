package kr.co.weeds.analyzer1.repository.mybatis.mapper;

import java.util.List;
import kr.co.weeds.analyzer1.model.loader.SystemFamily;
import kr.co.weeds.analyzer1.model.loader.SystemInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SystemInfoMapper {

   List<SystemInfo> getSystemInfoList();

   void insertNewSystemInfo(@Param("newSystemId") String newSystemId, @Param("parentSystemId") String parentSystemId);

   List<SystemFamily> getSystemFamilyList();

}
