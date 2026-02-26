package kr.co.weeds.analyzer1.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.StringReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import kr.co.weeds.dtos.NameValueObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CommonUtils {

   private static final Logger LOGGER = LogManager.getLogger(CommonUtils.class);

   private CommonUtils() {
   }

   public static String decodeURI(String data) {
      return decodeURI(data, null, null);
   }

   public static String decodeURI(String data, String paramDecodeFuncStr, String paramUriDecodeCode) {
      try {
         if (StringUtils.isEmpty(data)) {
            return null;
         }
         String result = URLDecoder.decode(data, StandardCharsets.UTF_8);
         if (paramUriDecodeCode != null) {
            try {
               result = URLDecoder.decode(result, paramUriDecodeCode);
            } catch (Exception e) {
               LOGGER.error("Error when decode param uri.", e);
            }
         }
         if (paramDecodeFuncStr != null) {
            String[] paramDecodeFunc = paramDecodeFuncStr.split(" ");
            for (String decodeFunc : paramDecodeFunc) {
               decodeFunc = decodeFunc.trim();
               if (decodeFunc.isEmpty()) {
                  continue;
               }
               String[] param = decodeFunc.split(":::::");
               if (param.length != 2) {
                  continue;
               }
               param[0] = param[0].trim();
               param[1] = param[1].trim();
               if (param[0].isEmpty() || param[1].isEmpty()) {
                  continue;
               }
               byte[] paramBytes = result.getBytes(param[0]);
               String paramString = new String(paramBytes, param[1]);
               if (paramString.isEmpty()) {
                  continue;
               }
               result = paramString;
            }
         }
         return result;
      } catch (Exception e) {
         return data;
      }
   }

   public static String decodeString(String input, String enc) {
      try {
         return URLDecoder.decode(input, enc);
      } catch (Exception e) {
         LOGGER.error("Error when decode string.", e);
      }
      return input;
   }

   public static String getFirstNodeValueFromJson(String json, String path) {
      try {
         ObjectMapper objectMapper = new ObjectMapper();
         JsonNode jsonNode = objectMapper.readTree(json);
         XmlMapper xmlMapper = new XmlMapper();
         String xml = xmlMapper.writeValueAsString(jsonNode);
         return getFirstNodeValueFromXml(xml, path);
      } catch (Exception e) {
         return null;
      }
   }

   public static String getFirstNodeValueFromXml(String xml, String path) {
      try {
         InputSource is = new InputSource(new StringReader(xml));
         Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

         XPath xpath = XPathFactory.newInstance().newXPath();

         NodeList nodeList = (NodeList) xpath.evaluate(path, document, XPathConstants.NODESET);
         if (nodeList == null || nodeList.getLength() == 0) {
            return null;
         }
         return nodeList.item(0).getTextContent();
      } catch (Exception e) {
         return null;
      }
   }

   public static List<NameValueObject> getNodeValuesFromJson(String json, String path) {
      try {
         ObjectMapper objectMapper = new ObjectMapper();
         JsonNode jsonNode = objectMapper.readTree(json);
         XmlMapper xmlMapper = new XmlMapper();
         String xml = xmlMapper.writeValueAsString(jsonNode);
         return getNodeValuesFromXml(xml, path);
      } catch (Exception e) {
         return Collections.emptyList();
      }
   }

   public static List<NameValueObject> getNodeValuesFromXml(String xml, String path) {
      try {
         InputSource is = new InputSource(new StringReader(xml));
         Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
         XPath xpath = XPathFactory.newInstance().newXPath();

         NodeList nodeList = (NodeList) xpath.evaluate(path, document, XPathConstants.NODESET);
         if (nodeList == null || nodeList.getLength() == 0) {
            return Collections.emptyList();
         }

         List<NameValueObject> result = new ArrayList<>();
         for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList cols = nodeList.item(i).getChildNodes();
            for (int c = 0; c < cols.getLength(); c++) {
               NameValueObject nvo = new NameValueObject(cols.item(c).getNodeName(), cols.item(c).getTextContent());
               result.add(nvo);
            }
         }
         return result;
      } catch (Exception e) {
         return Collections.emptyList();
      }
   }

   public static boolean isJsonAttribute(String target) {
      if (StringUtils.isEmpty(target)) {
         return true;
      }
      return (!isJsonArray(target)) && (!isJsonObject(target));
   }

   public static boolean isJsonArray(String target) {
      if (StringUtils.isEmpty(target)) {
         return false;
      }
      return target.startsWith("[");
   }

   public static boolean isJsonObject(String target) {
      if (StringUtils.isEmpty(target)) {
         return false;
      }
      return target.startsWith("{");
   }

   public static String encodeUriString(String input) {
      if (StringUtils.isBlank(input)) {
         return "";
      }
      return input.replace("%", "%25").replace("&", "%26");
   }

   public static boolean inParam(List listParams, String appParams, boolean containsCheck) {
      if (StringUtils.isBlank(appParams)) {
         return true;
      }
      String[] checkParams = appParams.trim().split("&");
      int params_size = listParams == null ? 0 : listParams.size();

      if (checkParams.length == 0) {
         return true;
      }

      boolean existParam = false;
      for (String checkParam : checkParams) {
         if (!checkParam.trim().isEmpty()) {
            existParam = true;
            break;
         }
      }

      if (!existParam) {
         return true;
      }

      for (String checkParam : checkParams) {
         if (checkParam.trim().isEmpty()) {
            continue;
         }
         int j = 0;
         for (; j < params_size; j++) {
            String paramText = listParams.get(j).toString();
            if (paramText.equals(checkParam) || isParamAsterCheck(paramText, checkParam) || (containsCheck
                && paramText.contains(checkParam))) {
               break;
            }
         }
         if (j >= params_size) {
            return false;
         }
      }
      return true;
   }

   private static boolean isParamAsterCheck(String paramText, String checkParam) {
      int allCheckIdx = checkParam.indexOf('*');
      if (allCheckIdx == -1) {
         return false;
      }
      String r = checkParam.replace("\\*", "(.*)");
      String paramRegEx = "^" + r + "$";

      return Pattern.matches(paramRegEx, paramText);
   }

   public static String urlDecode(String encodedString, String encoding) {
      try {
         if(StringUtils.isEmpty(encoding)) {
            return URLDecoder.decode(encodedString);
         } else {
            return URLDecoder.decode(encodedString, encoding);
         }
      } catch(Exception e) {
         return encodedString;
      }
   }

}
