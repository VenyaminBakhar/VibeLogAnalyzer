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
        prompt.append("Тебе дана структура таблицы log_entries и шаблоны логов из исходного кода.\n");
        prompt.append("На основе запроса пользователя нужно построить SQL-запрос для извлечения релевантных логов, которые помогут ответить на запрос.\n");
        prompt.append("Запрос должен учитывать ВСЕ релевантные шаблоны логов: успешные, неуспешные и промежуточные операции, чтобы дать полную картину.\n\n");
        
        prompt.append("### 🔑 Правила построения SQL:\n\n");
        prompt.append("1. **Пошаговый анализ запроса**\n");
        prompt.append("   - Определи ключевые сущности (например: userId, requestId, userName, email, sessionId и т.д.) и операции (например: отправка сообщения).\n");
        prompt.append("   - Учти, что идентификаторы могут быть строками, UUID, email-адресами, числами и т.д.\n\n");
        
        prompt.append("2. **Зависимости между логами**\n");
        prompt.append("   - Если для ответа по целевым логам нужны промежуточные данные (например, нужно сначала найти requestId по userId), используй CTE (`WITH … AS`).\n");
        prompt.append("   - Для связывания логов используй `JOIN` или `IN`.\n\n");
        
        prompt.append("3. **Извлечение значений из messages**\n");
        prompt.append("   - Для извлечения используем H2-функции с регулярками:\n");
        prompt.append("     - `REGEXP_SUBSTR(message, 'ключ=([^ ]+)', 1, 1, '', 1)`\n");
        prompt.append("     - или `SUBSTRING(message FROM 'ключ=([^ ]+)')`\n");
        prompt.append("   - Всегда рассматривай результат как строку.\n\n");
        
        prompt.append("4. **Фильтрация**\n");
        prompt.append("   - Для поиска подстрок: `UPPER(message) LIKE UPPER('%pattern%')` (регистронезависимый поиск).\n");
        prompt.append("   - Для множественных вариантов: `IN`.\n");
        prompt.append("   - В плейсхолдерах из кода использовать `%` или регулярные выражения.\n\n");
        
        prompt.append("5. **Результат**\n");
        prompt.append("   - Всегда возвращай `id, timestamp, log_level, message`.\n");
        prompt.append("   - Обязательно добавляй:\n");
        prompt.append("     - `ORDER BY timestamp DESC`\n");
        prompt.append("     - `LIMIT 1000`\n");
        prompt.append("   - Используй только таблицу `log_entries`.\n\n");
        
        prompt.append("### 📖 Примеры\n\n");
        prompt.append("#### Пример 1 (простой)\n");
        prompt.append("Запрос: «Отправилось ли сообщение пользователю с id 111?»\n");
        prompt.append("```sql\n");
        prompt.append("SELECT id, timestamp, log_level, message\n");
        prompt.append("FROM log_entries\n");
        prompt.append("WHERE UPPER(message) LIKE UPPER('%id=111%')\n");
        prompt.append("ORDER BY timestamp DESC\n");
        prompt.append("LIMIT 1000;\n");
        prompt.append("```\n\n");
        
        prompt.append("#### Пример 2 (с зависимостью)\n");
        prompt.append("Запрос: «Доставлено ли сообщение для пользователя с userId=abc-123-uuid?»\n");
        prompt.append("```sql\n");
        prompt.append("WITH request_ids AS (\n");
        prompt.append("    SELECT REGEXP_SUBSTR(message, 'requestId=([^ ]+)', 1, 1, '', 1) AS requestId\n");
        prompt.append("    FROM log_entries\n");
        prompt.append("    WHERE UPPER(message) LIKE UPPER('%start send message. userid=abc-123-uuid%')\n");
        prompt.append(")\n");
        prompt.append("SELECT l.id, l.timestamp, l.log_level, l.message\n");
        prompt.append("FROM log_entries l\n");
        prompt.append("JOIN request_ids r ON UPPER(l.message) LIKE UPPER('%' || r.requestId || '%')\n");
        prompt.append("WHERE UPPER(l.message) LIKE UPPER('%send completed success.%')\n");
        prompt.append("   OR UPPER(l.message) LIKE UPPER('%send failed.%')\n");
        prompt.append("   OR UPPER(l.message) LIKE UPPER('%start send message.%')\n");
        prompt.append("ORDER BY l.timestamp DESC\n");
        prompt.append("LIMIT 1000;\n");
        prompt.append("```\n\n");
        
        prompt.append("Структура таблицы log_entries:\n");
        prompt.append("id BIGINT,\n");
        prompt.append("timestamp TIMESTAMP,\n");
        prompt.append("log_level VARCHAR,\n");
        prompt.append("message VARCHAR\n\n");
        
        prompt.append("Шаблоны логов из кода:\n");
        for (LogPattern pattern : patterns) {
            prompt.append(pattern.getLogTemplate()).append("\n");
        }
        
        prompt.append("\nЗапрос пользователя:\n");
        prompt.append("\"").append(userQuery).append("\"");
        prompt.append("\n\nОсобые инструкции:\n");
        prompt.append("Верни только sql запрос без дополнительной информации или вопросов");
        
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