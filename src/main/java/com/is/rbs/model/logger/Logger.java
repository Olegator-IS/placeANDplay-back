package com.is.rbs.model.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class Logger {
    public void logRequestDetails(HttpStatus statusCode, long currentTime, String method, String url, String requestId, String clientIp,
                                  long executionTime,
                                  Object requestBody, Object result) {

        ObjectMapper objectMapper = new ObjectMapper();
        String callerClassName = new Throwable().getStackTrace()[1].getClassName();
        org.slf4j.Logger dynamicLogger = LoggerFactory.getLogger(callerClassName);
        try {
            Api api = new Api(method, url, requestId);
            Client client = new Client("clientName", clientIp, "clientHost");

            if(requestBody.toString().length()>10000){
                requestBody = "Запрос больше чем 10кб";
            }
            if(result.toString().length()>10000){
                result = "Ответ больше чем 10кб";
            }

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);
            String resultBodyJson = objectMapper.writeValueAsString(result);

            String apiJson = objectMapper.writeValueAsString(api);
            String clientJson = objectMapper.writeValueAsString(client);


            // Логируем с уровнем INFO, передавая JSON-строки напрямую
            if (statusCode == HttpStatus.OK||statusCode == HttpStatus.CREATED) {
                dynamicLogger.info("{\"time\": \"{}\"," +
                        "  \"level\": \"{}\"," +
                        "  \"msg\": \"{}\"," +
                        "  \"latency\": {}," +
                        "  \"api\": {}," +
                        "  \"client\": {}" +
                        "}",currentTime,"INFO","INFO",executionTime,apiJson,clientJson);
            } else if (statusCode == HttpStatus.BAD_REQUEST||statusCode == HttpStatus.UNAUTHORIZED
            || statusCode == HttpStatus.CONFLICT){
                dynamicLogger.warn("{\"time\": \"{}\"," +
                                "  \"level\": \"{}\"," +
                                "  \"msg\": \"{}\"," +
                                "  \"requestBody\": {}," +
                                "  \"latency\": {}," +
                                "  \"api\": {}," +
                                "  \"client\": {}," +
                                "  \"resultBody\": {}" +
                                "}",currentTime,"WARN","WARN",
                        sanitizeLog(requestBodyJson),executionTime,apiJson,clientJson,resultBodyJson);
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
                dynamicLogger.error("{\"time\": \"{}\"," +
                        "  \"level\": \"{}\"," +
                        "  \"msg\": \"{}\"," +
                        "  \"requestBody\": {}," +
                        "  \"latency\": {}," +
                        "  \"api\": {}," +
                        "  \"client\": {}," +
                        "  \"resultBody\": {}" +
                        "}",currentTime,"ERROR","ERROR",sanitizeLog(requestBodyJson),executionTime,apiJson,clientJson,resultBodyJson);
            }
        } catch (Exception e) {
            dynamicLogger.error("Error while logging request details", e);
        }
    }

    public static String sanitizeLog(String log) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> logMap = objectMapper.readValue(log, Map.class);
            // Находим поле password
            if (logMap != null && logMap.containsKey("password")) {
               logMap.put("password", "[hidden]");
            }
            return objectMapper.writeValueAsString(logMap);
        } catch (Exception e) {
            return log; // Если произошла ошибка, возвращаем оригинальный лог
        }
    }
}
