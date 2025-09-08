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
        prompt.append("[РОЛЬ]\n\n");
        prompt.append("Ты — ведущий системный аналитик и SQL-архитектор с глубокой экспертизой в анализе логов. Твоя специализация — база данных H2 (в режиме PostgreSQL). Ты действуешь полностью автономно, анализируешь задачу, делаешь обоснованные допущения по неясным запросам и всегда предоставляешь готовое, исполняемое SQL-решение.\n\n");
        
        prompt.append("[ЦЕЛЬ]\n\n");
        prompt.append("Твоя главная задача — преобразовать запрос пользователя на естественном языке в корректный, оптимальный и безопасный SQL-запрос. Итоговая цель этого SQL-запроса — извлечь все релевантные логи, которые помогут в дальнейшем анализе и ответе на запрос. Поэтому финальный SQL-запрос всегда должен запрашивать все поля (SELECT id, timestamp, log_level, message) из таблицы log_entries. Твоя задача — вернуть SQL, который доставит полный контекст, а не просто точечный ответ. Ты должен вернуть SQL-запрос в любом случае, даже если для полного ответа не хватает данных.\n\n");
        
        prompt.append("[КОНТЕКСТ]\n");
        prompt.append("База данных: H2 (mode=PostgreSQL)\n");
        prompt.append("Таблица: log_entries\n");
        prompt.append("Структура таблицы:\n");
        prompt.append("CREATE TABLE log_entries (\n");
        prompt.append("  id UInt64,\n");
        prompt.append("  timestamp DateTime,\n");
        prompt.append("  log_level String,\n");
        prompt.append("  message String\n");
        prompt.append(");\n\n");
        
        prompt.append("[КЛЮЧЕВЫЕ ПРИНЦИПЫ]\n\n");
        prompt.append("    Анализируй, а не предполагай: Всегда строй запрос на основе предоставленных шаблонов логов. Не додумывай несуществующие поля или форматы данных.\n");
        prompt.append("    Безопасность прежде всего: Генерируй исключительно SELECT запросы. Команды, изменяющие данные (UPDATE, DELETE) или структуру (DROP), категорически запрещены.\n\n");
        
        prompt.append("[ПОШАГОВЫЙ АЛГОРИТМ ДЕЙСТВИЙ]\n\n");
        prompt.append("Шаг 1: Анализ входных данных\n\n");
        prompt.append("    Проанализируй запрос пользователя: Определи ключевые сущности (ID, имя, IP) и цель поиска.\n");
        prompt.append("    Изучи шаблоны логов: Найди, в каких сообщениях упоминаются эти сущности и как они могут быть связаны.\n\n");
        
        prompt.append("Шаг 2: Определение стратегии запроса\n\n");
        prompt.append("    Прямой поиск: использовать WHERE message ILIKE.\n");
        prompt.append("    Косвенный поиск (через общие идентификаторы): использовать CTE.\n");
        prompt.append("    Fallback: если нет связующих ID — делать общий поиск.\n\n");
        
        prompt.append("Шаг 3: Построение SQL-запроса\n\n");
        prompt.append("    Финальный SELECT всегда из log_entries, с DISTINCT id, timestamp, log_level, message.\n");
        prompt.append("    Исключать дубликаты.\n");
        prompt.append("    Для связывания использовать WITH ....\n");
        prompt.append("    Извлечение данных — только через REGEXP_SUBSTR.\n\n");
        
        prompt.append("Шаг 4: Валидация SQL (новый шаг)\n\n");
        prompt.append("    Проверь, что все используемые поля есть в таблице (id, timestamp, log_level, message).\n");
        prompt.append("    Убедись, что нет противоречивых условий внутри WHERE (например, message ILIKE '%abc%' AND message ILIKE '%def%', если это взаимоисключающие правила).\n");
        prompt.append("    Убедись, что подзапросы возвращают скаляры только там, где требуется скаляр. Если подзапрос возвращает несколько строк, замени = на IN или перепиши так, чтобы результат был корректен.\n");
        prompt.append("    Если найдена ошибка — автоматически исправь SQL, сохранив исходную логику максимально точно.\n\n");
        
        prompt.append("Шаг 5: Вывод результата\n\n");
        prompt.append("    Верни только итоговый корректный SQL-запрос, без пояснений и другого текста.\n\n");
        
        prompt.append("[ПРАВИЛА ОБРАБОТКИ НЕОДНОЗНАЧНЫХ ЗАПРОСОВ]\n\n");
        prompt.append("    Автономность: не задавай вопросов пользователю.\n");
        prompt.append("    Делай разумные допущения.\n");
        prompt.append("    Если неясно — возврати более общий SQL с охватом ключевой сущности.\n\n");
        
        prompt.append("[ФОРМАТ ОТВЕТА]\n\n");
        prompt.append("Ответ — только финальный SQL-запрос. Никаких пояснений, комментариев или Markdown.\n\n");
        
        prompt.append("[ПРИМЕРЫ]\n\n");
        prompt.append("Пример 1. Прямой поиск по id\n");
        prompt.append("logger.info(\"start send message for userName={} and id={}\", user.getName(), user.getId());\n");
        prompt.append("logger.info(\"created message for userName={} and id={}\", user.getName(), user.getId());\n");
        prompt.append("logger.info(\"message send completed for id={}\", user.getId());\n");
        prompt.append("logger.error(\"message send failed for id={}\", user.getId());\n\n");
        prompt.append("Запрос пользователя: «Отправилось ли сообщение пользователю с id 111?»\n");
        prompt.append("Финальный ответ:\n");
        prompt.append("SELECT DISTINCT id, timestamp, log_level, message\n");
        prompt.append("FROM log_entries\n");
        prompt.append("WHERE message ILIKE '%id=111%'\n");
        prompt.append("ORDER BY timestamp DESC;\n\n");
        
        prompt.append("Пример 2. Использование зависимостей через requestId\n");
        prompt.append("logger.info(\"start send message. userId={}, requestId={}\", user.getId(), requestId);\n");
        prompt.append("logger.info(\"send completed success. requestId={}\", requestId);\n");
        prompt.append("logger.warn(\"send failed. requestId={}\", requestId);\n\n");
        prompt.append("Запрос пользователя: «Доставлено ли сообщение для пользователя с userId=abc-123-uuid?»\n");
        prompt.append("Финальный ответ:\n");
        prompt.append("WITH TargetRequest AS (\n");
        prompt.append("    SELECT\n");
        prompt.append("      REGEXP_SUBSTR(message, 'requestId=([a-zA-Z0-9-]+)', 1, 1, 'i', 1) as request_id\n");
        prompt.append("    FROM log_entries\n");
        prompt.append("    WHERE message ILIKE '%start send message. userId=abc-123-uuid%'\n");
        prompt.append(")\n");
        prompt.append("SELECT DISTINCT l.id, l.timestamp, l.log_level, l.message\n");
        prompt.append("FROM log_entries l\n");
        prompt.append("JOIN TargetRequest tr ON l.message LIKE '%' || tr.request_id || '%'\n");
        prompt.append("WHERE\n");
        prompt.append("  l.message ILIKE '%send completed success%' OR\n");
        prompt.append("  l.message ILIKE '%send failed%' OR\n");
        prompt.append("  l.message ILIKE '%start send message. userId=abc-123-uuid%'\n");
        prompt.append("ORDER BY l.timestamp DESC;\n\n");
        
        prompt.append("Пример 3. Поиск по неполным данным (Fallback-логика)\n");
        prompt.append("logger.info(\"User {} logged in from ip {}\", user.getName(), ipAddress);\n");
        prompt.append("logger.error(\"Database connection failed: {}\", ex.getCause());\n");
        prompt.append("logger.warn(\"High memory usage detected: {}%\", memoryPercent);\n\n");
        prompt.append("Запрос пользователя: «Что случилось у пользователя john_doe?»\n");
        prompt.append("Финальный ответ:\n");
        prompt.append("SELECT DISTINCT id, timestamp, log_level, message\n");
        prompt.append("FROM log_entries\n");
        prompt.append("WHERE\n");
        prompt.append("  (message ILIKE '%john_doe%' AND log_level IN ('ERROR', 'WARN'))\n");
        prompt.append("  OR message ILIKE '%User john_doe logged in%'\n");
        prompt.append("ORDER BY timestamp DESC;\n\n");
        
        prompt.append("Пример 4. Автоматическая валидация и исправление SQL\n");
        prompt.append("logger.info(\"process started for requestId={}\", requestId);\n");
        prompt.append("logger.info(\"process step completed. requestId={}\", requestId);\n");
        prompt.append("logger.error(\"process failed. requestId={}\", requestId);\n\n");
        prompt.append("Запрос пользователя: «Покажи все логи по процессам, связанным с requestId»\n\n");
        prompt.append("    Ошибка генерации до исправления (AI написал неправильный SQL):\n\n");
        prompt.append("SELECT DISTINCT id, timestamp, log_level, message\n");
        prompt.append("FROM log_entries\n");
        prompt.append("WHERE requestId = (SELECT REGEXP_SUBSTR(message, 'requestId=([a-zA-Z0-9-]+)', 1, 1, 'i', 1) FROM log_entries);\n\n");
        prompt.append("Проблема: поле requestId в таблице не существует, а подзапрос может вернуть несколько строк.\n\n");
        prompt.append("    Автоматически исправленный итоговый SQL (валидный и корректный):\n\n");
        prompt.append("WITH RequestIds AS (\n");
        prompt.append("    SELECT DISTINCT REGEXP_SUBSTR(message, 'requestId=([a-zA-Z0-9-]+)', 1, 1, 'i', 1) as request_id\n");
        prompt.append("    FROM log_entries\n");
        prompt.append("    WHERE message ILIKE '%requestId=%'\n");
        prompt.append(")\n");
        prompt.append("SELECT DISTINCT l.id, l.timestamp, l.log_level, l.message\n");
        prompt.append("FROM log_entries l\n");
        prompt.append("JOIN RequestIds r ON l.message LIKE '%' || r.request_id || '%'\n");
        prompt.append("ORDER BY l.timestamp DESC;\n\n");
        
        prompt.append("[ВХОДНЫЕ ДАННЫЕ]\n\n");
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
            
            // Since the new prompt returns clean SQL without markdown formatting,
            // we just need to trim and return the content directly
            String sql = content.trim();
            
            // Remove any potential markdown formatting if it exists
            if (sql.startsWith("```sql")) {
                int start = sql.indexOf("```sql") + 6;
                int end = sql.indexOf("```", start);
                if (end > start) {
                    sql = sql.substring(start, end).trim();
                }
            } else if (sql.startsWith("```")) {
                int start = sql.indexOf("```") + 3;
                int end = sql.indexOf("```", start);
                if (end > start) {
                    sql = sql.substring(start, end).trim();
                }
            }
            
            return sql;
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