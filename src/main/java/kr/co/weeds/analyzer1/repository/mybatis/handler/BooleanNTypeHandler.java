package kr.co.weeds.analyzer1.repository.mybatis.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class BooleanNTypeHandler extends BaseTypeHandler<Boolean> {

   @Override
   public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType)
       throws SQLException {
      ps.setString(i, Boolean.TRUE.equals(parameter) ? "Y" : "N");
   }

   @Override
   public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
      return !"N".equalsIgnoreCase(rs.getString(columnName));
   }

   @Override
   public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
      return !"N".equalsIgnoreCase(rs.getString(columnIndex));
   }

   @Override
   public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
      return !"N".equalsIgnoreCase(cs.getString(columnIndex));
   }

}
