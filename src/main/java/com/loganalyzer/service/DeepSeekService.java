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
        prompt.append("Ты — умный помощник для генерации SQL-запросов к базе данных H2 (mode=PostgreSQL). В таблице log_entries хранятся журналы логирования.\n");
        prompt.append("Твоя задача\n\n");
        prompt.append("По пользовательскому запросу построить корректный, оптимизированный SQL-запрос, который найдет только релевантные записи логов. При генерации запроса ты обязан анализировать предоставленные шаблоны логов и структуру таблицы.\n\n");
        
        prompt.append("Таблица log_entries имеет структуру:\n");
        prompt.append("id BIGINT,\n");
        prompt.append("timestamp TIMESTAMP,\n");
        prompt.append("log_level VARCHAR,\n");
        prompt.append("message VARCHAR\n\n");
        
        prompt.append("🔑 План построения SQL\n\n");
        prompt.append("1. Анализ пользовательского запроса и лог‑шаблонов\n\n");
        prompt.append("    Определи ключевые сущности в запросе: userId, requestId, userName, email, sessionId, ipAddress и т.п.\n");
        prompt.append("    Определи релевантные действия (например: «отправка сообщения», «логин», «ошибка»).\n");
        prompt.append("    Сопоставь их только с реально существующими полями из шаблонов.\n");
        prompt.append("    Никогда не придумывай новых ключей или дополнительных полей.\n\n");
        
        prompt.append("2. Построение SQL‑запроса\n\n");
        prompt.append("    Если данные можно сразу достать из логов фильтрацией по message, построй прямой запрос с ILIKE.\n");
        prompt.append("    Если для ответа нужно связать разные логи (например, через requestId, id или sessionId), используй CTE (WITH …) для первичного извлечения значений.\n");
        prompt.append("    Для извлечения значений из строки message применяй функции H2:\n");
        prompt.append("        • REGEXP_SUBSTR(message, 'pattern', 1, 1, '', group) — чтобы выделить группу из лог-сообщения.\n");
        prompt.append("        • REGEXP_REPLACE для замены при необходимости.\n");
        prompt.append("    Используй только те ключи и подстроки, которые явно встречаются в шаблонах логов.\n\n");
        
        prompt.append("3. Валидация SQL‑запроса\n\n");
        prompt.append("    Проверь, что синтаксис SQL корректный именно в H2 (mode=PostgreSQL).\n");
        prompt.append("    Учти особенности H2: ILIKE работает, но substring с regex — нет, только REGEXP_SUBSTR.\n");
        prompt.append("    Проверь, что фильтрация и JOIN сделаны только по реально существующим данным.\n\n");
        
        prompt.append("4. Оптимизация SQL‑запроса\n\n");
        prompt.append("    Убедись, что нет дублирующихся условий.\n");
        prompt.append("    Убери нерелевантные проверки (ILIKE '%text%' без необходимости).\n");
        prompt.append("    Проверяй, чтобы запрос выбирал только нужные строки, а не слишком широкий диапазон.\n\n");
        
        prompt.append("🔎 Правила для финального SQL\n\n");
        prompt.append("    Результат всегда должен содержать:\n\n");
        prompt.append("SELECT id, timestamp, log_level, message\n");
        prompt.append("FROM log_entries\n");
        prompt.append("...\n");
        prompt.append("ORDER BY timestamp DESC;\n\n");
        
        prompt.append("    Не придумывай новые поля (например, transactionId).\n");
        prompt.append("    Не используй условия, отсутствующие в шаблонах логов.\n");
        prompt.append("    Допускается использование WITH … для сложных связей.\n");
        prompt.append("    Для подстрочного поиска — всегда ILIKE, для множественных значений — IN.\n");
        prompt.append("    Для извлечения значений — только REGEXP_SUBSTR.\n\n");
        
        prompt.append("📖 Примеры\n\n");
        prompt.append("Пример 1. Прямой поиск по id\n\n");
        prompt.append("Шаблоны:\n");
        prompt.append("logger.info(\"start send message for userName={} and id={}\", user.getName(), user.getId());\n");
        prompt.append("logger.info(\"created message for userName={} and id={}\", user.getName(), user.getId());\n");
        prompt.append("logger.info(\"system healthcheck. status=working\");\n");
        prompt.append("logger.info(\"message send completed for id={}\", user.getId());\n");
        prompt.append("logger.error(\"message send failed for id={}\", user.getId());\n\n");
        prompt.append("Запрос пользователя:\n");
        prompt.append("«Отправилось ли сообщение пользователю с id 111?»\n\n");
        prompt.append("SQL:\n");
        prompt.append("SELECT id, timestamp, log_level, message\n");
        prompt.append("FROM log_entries\n");
        prompt.append("WHERE message ILIKE '%id=111%'\n");
        prompt.append("ORDER BY timestamp DESC;\n\n");
        
        prompt.append("Пример 2. Использование зависимостей (связка через requestId)\n\n");
        prompt.append("Шаблоны:\n");
        prompt.append("logger.info(\"start send message. userId={}, requestId={}\", user.getId(), requestId);\n");
        prompt.append("logger.info(\"send completed success. requestId={}\", requestId);\n");
        prompt.append("logger.warn(\"send failed. requestId={}\", requestId);\n\n");
        prompt.append("Запрос пользователя:\n");
        prompt.append("«Доставлено ли сообщение для пользователя с userId=abc-123-uuid?»\n\n");
        prompt.append("SQL:\n");
        prompt.append("WITH request_ids AS (\n");
        prompt.append("  SELECT REGEXP_SUBSTR(message, 'requestId=([^ ]+)', 1, 1, '', 1) AS requestId\n");
        prompt.append("  FROM log_entries\n");
        prompt.append("  WHERE message ILIKE '%start send message. userid=abc-123-uuid%'\n");
        prompt.append(")\n");
        prompt.append("SELECT l.id, l.timestamp, l.log_level, l.message\n");
        prompt.append("FROM log_entries l\n");
        prompt.append("JOIN request_ids r ON l.message LIKE '%' || r.requestId || '%'\n");
        prompt.append("WHERE l.message ILIKE '%send completed success.%'\n");
        prompt.append("   OR l.message ILIKE '%send failed.%'\n");
        prompt.append("   OR l.message ILIKE '%start send message.%'\n");
        prompt.append("ORDER BY l.timestamp DESC;\n\n");
        
        prompt.append("Пример 3. Логирование входа через IP\n\n");
        prompt.append("Шаблоны:\n");
        prompt.append("logger.info(\"User {} logged in from {}\", user.getName(), ipAddress);\n");
        prompt.append("logger.error(\"Database connection failed: {}\", ex.getCause());\n");
        prompt.append("logger.warn(\"High memory usage detected: {}%\", memoryPercent);\n\n");
        prompt.append("Запрос пользователя:\n");
        prompt.append("«what john_doe log ip?»\n\n");
        prompt.append("SQL:\n");
        prompt.append("SELECT id, timestamp, log_level, message\n");
        prompt.append("FROM log_entries\n");
        prompt.append("WHERE message ILIKE '%User john_doe logged in from%'\n");
        prompt.append("ORDER BY timestamp DESC;\n\n");
        
        prompt.append("🎯 Результат\n\n");
        prompt.append("    Используй пошаговый план (анализ, построение, валидация, оптимизация).\n");
        prompt.append("    Верни только один окончательный SQL‑запрос, полностью готовый к выполнению и без промежуточных результатов.\n");
        prompt.append("    Запрос должен соответствовать реальным шаблонам из кода и находить только релевантные записи.\n\n");
        
        prompt.append("📥 Данные для генерации\n\n");
        prompt.append("Запрос пользователя:\n");
        prompt.append(userQuery).append("\n\n");
        
        prompt.append("Шаблоны логов:\n");
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