package kr.co.weeds.analyzer1.repository.mybatis.mapper;

import java.util.List;
import kr.co.weeds.analyzer1.model.loader.AppInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppInfoMapper {

   List<AppInfo> getAppInfoList();

}
