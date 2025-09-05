package com.loganalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeepSeekService {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    // Step 1: Generate SQL query from user request
    public String generateSqlQuery(String userQuery, List<LogPattern> patterns, String apiKey) {
        String prompt = buildSqlGenerationPrompt(userQuery, patterns);
        
        try {
            String response = callDeepSeekApi(prompt, apiKey);
            return extractSqlFromResponse(response);
        } catch (Exception e) {
            logger.error("Error generating SQL query", e);
            throw new RuntimeException("Failed to generate SQL query: " + e.getMessage());
        }
    }

    // Step 2: Analyze logs and provide textual analysis
    public String analyzeLogs(String userQuery, List<LogEntry> logs, String apiKey) {
        String prompt = buildLogAnalysisPrompt(userQuery, logs);
        
        try {
            String response = callDeepSeekApi(prompt, apiKey);
            return extractAnalysisFromResponse(response);
        } catch (Exception e) {
            logger.error("Error analyzing logs", e);
            throw new RuntimeException("Failed to analyze logs: " + e.getMessage());
        }
    }

    private String buildSqlGenerationPrompt(String userQuery, List<LogPattern> patterns) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты — помощник для генерации SQL-запросов к базе данных H2, где хранятся логи в таблице log_entries.\n\n");
        prompt.append("Твоя задача: по пользовательскому запросу строить корректный SQL, который находит релевантные записи логов, основываясь исключительно на предоставленных шаблонах логов и структуре таблицы.\n\n");
        
        prompt.append("🔑 Основные правила построения SQL\n\n");
        
        prompt.append("1. Анализ пользовательского запроса\n");
        prompt.append("   • Определи ключевые сущности (например: userId, requestId, userName, email, sessionId) и действия, о которых спрашивает пользователь (например: отправка сообщения, логин).\n");
        prompt.append("   • Используй только те сущности и ключи, которые гарантированно встречаются в логах по предоставленным шаблонам.\n");
        prompt.append("   • Не добавляй условий фильтрации по ключам, отсутствующим в логах.\n");
        prompt.append("   • ❌ Нельзя: добавлять UPPER(message) LIKE '%ip=%', если в шаблонах нет ip=.\n");
        prompt.append("   • ✅ Можно: использовать UPPER(message) LIKE '%logged in from%', если это есть в шаблоне.\n\n");
        
        prompt.append("2. Зависимости между логами\n");
        prompt.append("   • Если для ответа нужно связывать логи по requestId, id, sessionId, используй CTE (WITH …) для предварительного извлечения значений.\n");
        prompt.append("   • Для связи таблиц внутри log_entries используй JOIN или IN, но только по данным, которые точно есть в шаблонах.\n");
        prompt.append("   • ❌ Запрещено выдумывать дополнительные поля (например, transactionId), если их нет в шаблонах.\n\n");
        
        prompt.append("3. Извлечение значений из message\n");
        prompt.append("   • Для извлечения данных из строки используй H2-функции с регэкспами:\n");
        prompt.append("     - REGEXP_SUBSTR(message, 'ключ=([^ ]+)', 1, 1, '', 1)\n");
        prompt.append("     - SUBSTRING(message FROM 'ключ=([^ ]+)')\n");
        prompt.append("   • Все значения трактуй как строки.\n\n");
        
        prompt.append("4. Фильтрация\n");
        prompt.append("   • Для поиска подстрок используй UPPER(message) LIKE UPPER('%pattern%') (регистронезависимый LIKE).\n");
        prompt.append("   • Для множественных возможных значений — IN.\n");
        prompt.append("   • Для плейсхолдеров ({} в коде) используй подстановку %.\n");
        prompt.append("   • Всегда опирайся только на реально встречающиеся подстроки из шаблонов.\n\n");
        
        prompt.append("5. Результат\n");
        prompt.append("   • Всегда выводи: SELECT id, timestamp, log_level, message\n");
        prompt.append("   • Обязательно добавляй: ORDER BY timestamp DESC;\n\n");
        
        prompt.append("📖 Примеры\n\n");
        prompt.append("Пример 1. Прямой поиск по id\n");
        prompt.append("Шаблоны:\n");
        prompt.append("logger.info(\"start send message for userName={} and id={}\", user.getName(), user.getId());\n");
        prompt.append("logger.info(\"message send completed for id={}\", user.getId());\n");
        prompt.append("logger.error(\"message send failed for id={}\", user.getId());\n\n");
        prompt.append("Запрос пользователя: «Отправилось ли сообщение пользователю с id 111?»\n\n");
        prompt.append("SQL:\n");
        prompt.append("SELECT id, timestamp, log_level, message\n");
        prompt.append("FROM log_entries\n");
        prompt.append("WHERE UPPER(message) LIKE UPPER('%id=111%')\n");
        prompt.append("ORDER BY timestamp DESC;\n\n");
        
        prompt.append("Пример 2. Использование зависимостей (связка через requestId)\n");
        prompt.append("Шаблоны:\n");
        prompt.append("logger.info(\"start send message. userId={}, requestId={}\", user.getId(), requestId);\n");
        prompt.append("logger.info(\"send completed success. requestId={}\", requestId);\n");
        prompt.append("logger.warn(\"send failed. requestId={}\", requestId);\n\n");
        prompt.append("Запрос пользователя: «Доставлено ли сообщение для пользователя с userId=abc-123-uuid?»\n\n");
        prompt.append("SQL:\n");
        prompt.append("WITH request_ids AS (\n");
        prompt.append("  SELECT REGEXP_SUBSTR(message, 'requestId=([^ ]+)', 1, 1, '', 1) AS requestId\n");
        prompt.append("  FROM log_entries\n");
        prompt.append("  WHERE UPPER(message) LIKE UPPER('%start send message. userid=abc-123-uuid%')\n");
        prompt.append(")\n");
        prompt.append("SELECT l.id, l.timestamp, l.log_level, l.message\n");
        prompt.append("FROM log_entries l\n");
        prompt.append("JOIN request_ids r ON UPPER(l.message) LIKE UPPER('%' || r.requestId || '%')\n");
        prompt.append("WHERE UPPER(l.message) LIKE UPPER('%send completed success.%')\n");
        prompt.append("   OR UPPER(l.message) LIKE UPPER('%send failed.%')\n");
        prompt.append("   OR UPPER(l.message) LIKE UPPER('%start send message.%')\n");
        prompt.append("ORDER BY l.timestamp DESC;\n\n");
        
        prompt.append("🎯 Итог\n");
        prompt.append("Всегда:\n");
        prompt.append("• Используй только реально существующие подстроки и ключи из шаблонов.\n");
        prompt.append("• Верни только 1 sql запрос, который точно поможет найти релевантные логи.\n");
        prompt.append("• Не придумывай новые поля.\n");
        prompt.append("• Возвращай id, timestamp, log_level, message с ORDER BY timestamp DESC.\n\n");
        
        prompt.append("Данные для генерации:\n");
        prompt.append("Структура таблицы log_entries:\n");
        prompt.append("id BIGINT,\n");
        prompt.append("timestamp TIMESTAMP,\n");
        prompt.append("log_level VARCHAR,\n");
        prompt.append("message VARCHAR\n\n");
        
        prompt.append("Запрос пользователя:\n");
        prompt.append(userQuery).append("\n\n");
        
        prompt.append("Шаблоны логов из кода:\n");
        for (LogPattern pattern : patterns) {
            prompt.append(pattern.getLogTemplate()).append("\n");
        }
        
        return prompt.toString();
    }

    private String buildLogAnalysisPrompt(String userQuery, List<LogEntry> logs) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Ты — senior DevOps инженер с опытом анализа логов.\n");
        prompt.append("Анализируй логи в контексте запроса пользователя:\n");
        prompt.append("- Временные закономерности и корреляции\n");
        prompt.append("- Последовательности событий\n");
        prompt.append("- Уровни серьезности и эскалации\n");
        prompt.append("- Определи, какие логи реально релевантны запросу\n\n");
        
        prompt.append("Запрос пользователя: \"").append(userQuery).append("\"\n\n");
        
        prompt.append("Найденные логи:\n");
        for (LogEntry log : logs) {
            prompt.append(String.format("[%s] %s: %s\n", 
                log.getTimestamp(), log.getLogLevel(), log.getMessage()));
        }
        
        prompt.append("\nВерни JSON: { \"analysis\": \"человеческое объяснение\", \"relevant_logs\": [массив релевантных логов] }");
        
        return prompt.toString();
    }

    private String callDeepSeekApi(String prompt, String apiKey) {
        WebClient webClient = webClientBuilder
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 4000);
        requestBody.put("temperature", 0.1);

        try {
            Mono<String> response = webClient.post()
                .uri(DEEPSEEK_API_URL)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);

            return response.block();
        } catch (Exception e) {
            logger.error("Error calling DeepSeek API", e);
            throw new RuntimeException("Failed to call DeepSeek API: " + e.getMessage());
        }
    }

    private String extractSqlFromResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            String content = jsonNode.path("choices").get(0).path("message").path("content").asText();
            
            // Extract SQL from ```sql blocks
            if (content.contains("```sql")) {
                int start = content.indexOf("```sql") + 6;
                int end = content.indexOf("```", start);
                if (end > start) {
                    return content.substring(start, end).trim();
                }
            }
            
            // If no SQL block found, return the content as is
            return content.trim();
        } catch (Exception e) {
            logger.error("Error extracting SQL from response", e);
            throw new RuntimeException("Failed to extract SQL from response");
        }
    }

    private String extractAnalysisFromResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            String content = jsonNode.path("choices").get(0).path("message").path("content").asText();
            
            // Try to extract JSON analysis
            if (content.contains("{") && content.contains("}")) {
                int start = content.indexOf("{");
                int end = content.lastIndexOf("}") + 1;
                String jsonPart = content.substring(start, end);
                
                JsonNode analysisJson = objectMapper.readTree(jsonPart);
                return analysisJson.path("analysis").asText();
            }
            
            // If no JSON found, return the content as is
            return content;
        } catch (Exception e) {
            logger.error("Error extracting analysis from response", e);
            return response; // Return raw response if parsing fails
        }
    }
}