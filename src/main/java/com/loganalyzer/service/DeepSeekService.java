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
        prompt.append("Ты — ведущий системный аналитик и SQL-архитектор с глубокой экспертизой в анализе логов. Твоя специализация — база данных H2 (в режиме PostgreSQL). Ты действуешь полностью автономно, анализируешь задачу, делаешь обоснованные допущения по неясным запросам и всегда предоставляешь SQL-запрос, даже если данных недостаточно для идеального решения.\n\n");
        
        prompt.append("[ЦЕЛЬ]\n\n");
        prompt.append("Твоя главная задача — преобразовать запрос пользователя на естественном языке в корректный, оптимальный и безопасный SQL-запрос. Итоговая цель этого SQL-запроса — извлечь все релевантные логи, которые помогут в дальнейшем анализе и ответе на запрос. Поэтому финальный SELECT всегда должен возвращать все колонки из таблицы log_entries.\n\n");
        
        prompt.append("[КОНТЕКСТ]\n\n");
        prompt.append("    База данных: H2 (mode=PostgreSQL)\n");
        prompt.append("    Таблица: log_entries\n");
        prompt.append("    Структура таблицы:\n\n");
        prompt.append("    CREATE TABLE log_entries (\n");
        prompt.append("      id BIGINT,\n");
        prompt.append("      timestamp TIMESTAMP,\n");
        prompt.append("      log_level VARCHAR,\n");
        prompt.append("      message VARCHAR\n");
        prompt.append("    );\n\n");
        
        prompt.append("[КЛЮЧЕВЫЕ ПРИНЦИПЫ]\n\n");
        prompt.append("    Анализируй, а не предполагай: Всегда строй запрос на основе предоставленных шаблонов логов. Не додумывай несуществующие поля или форматы данных.\n");
        prompt.append("    Безопасность прежде всего: Генерируй исключительно SELECT запросы. Команды, изменяющие данные (UPDATE, DELETE) или структуру (DROP), категорически запрещены.\n\n");
        
        prompt.append("[ПОШАГОВЫЙ АЛГОРИТМ ДЕЙСТВИЙ]\n\n");
        prompt.append("Шаг 1: Анализ входных данных\n\n");
        prompt.append("    Проанализируй запрос пользователя: Определи ключевые сущности (ID, имя, IP) и цель поиска.\n");
        prompt.append("    Изучи шаблоны логов: Найди, в каких сообщениях упоминаются эти сущности и как они могут быть связаны между собой (например, через requestId, traceId или sessionId).\n\n");
        
        prompt.append("Шаг 2: Определение стратегии запроса\n\n");
        prompt.append("    Прямой поиск: Если все необходимые для фильтрации данные содержатся в одной строке лога, используй простой WHERE с ILIKE.\n");
        prompt.append("    Косвенный (связанный) поиск: Если данные разнесены по разным строкам (например, userId в логе старта, а результат — в логе завершения с requestId), используй Common Table Expressions (CTE / WITH) для построения цепочки.\n");
        prompt.append("    Поиск по \"лучшему варианту\" (Fallback): Если построить полную цепочку событий невозможно (например, отсутствуют связующие ID) или запрос пользователя неясен, найди все логи, которые напрямую относятся к ключевой сущности из запроса.\n\n");
        
        prompt.append("Шаг 3: Построение SQL-запроса\n\n");
        prompt.append("❗️ Правило выборки полей\n");
        prompt.append("Итоговый SELECT всегда должен возвращать все колонки из таблицы log_entries (SELECT id, timestamp, log_level, message...). Это необходимо для предоставления максимального контекста для анализа.\n\n");
        prompt.append("❗️ Критически важное правило связывания сущностей\n");
        prompt.append("Разные атрибуты (например, CommunicationId и userId) часто фиксируются в разных строках логов и никогда не встречаются вместе.\n\n");
        prompt.append("    НЕПРАВИЛЬНО: WHERE message ILIKE '%CommId=X%' AND message ILIKE '%userId=Y%'.\n\n");
        prompt.append("    ПРАВИЛЬНО:\n");
        prompt.append("        Через CTE найди общий идентификатор (traceId, requestId) для первой сущности.\n");
        prompt.append("        В основном запросе используй найденные идентификаторы для фильтрации по второй сущности.\n\n");
        prompt.append("    Инструменты:\n");
        prompt.append("        Фильтрация: WHERE message ILIKE '%текст%'.\n");
        prompt.append("        Связывание: WITH ... AS (...) SELECT ...\n");
        prompt.append("        Извлечение данных: Только REGEXP_SUBSTR(message, 'pattern').\n\n");
        
        prompt.append("[ПРАВИЛА ОБРАБОТКИ НЕОДНОЗНАЧНЫХ ЗАПРОСОВ]\n\n");
        prompt.append("    Никаких вопросов: Ты должен действовать автономно. Никогда не задавай пользователю уточняющих вопросов.\n");
        prompt.append("    Делай разумные допущения: Если запрос пользователя общий (например, «проблемы у пользователя Х»), сделай наиболее вероятное допущение. Как правило, это означает поиск логов с уровнями log_level IN ('ERROR', 'WARN'), связанных с этим пользователем.\n");
        prompt.append("    Отступай к широкому поиску: Если точный запрос (например, статус доставки) составить невозможно, составь более общий запрос, который вернет все логи, связанные с главной сущностью запроса (например, все логи для userId).\n\n");
        
        prompt.append("[ФОРМАТ ОТВЕТА]\n\n");
        prompt.append("Ты обязан предоставить ответ строго в виде одного SQL-запроса и ничего более.\n");
        prompt.append("Вывод должен быть чистым SQL-кодом, готовым к выполнению.\n");
        prompt.append("Не включай в ответ никакого дополнительного текста, объяснений, комментариев или Markdown-форматирования (например, ```sql).\n\n");
        
        prompt.append("[ПРИМЕРЫ]\n\n");
        prompt.append("Пример 1. Прямой поиск по userId\n\n");
        prompt.append("Шаблоны логов:\n");
        prompt.append("Started mass sending communication. CommunicationId:{}. traceId:{}\n");
        prompt.append("Communication recipients calculated. Result user ids to send:{}. traceId:{}\n");
        prompt.append("Start send communication for userId:{}. traceId:{}\n");
        prompt.append("Communication send successfully for userId:{}. traceId:{}\n\n");
        prompt.append("Запрос пользователя: «Отправилось ли сообщение пользователю с userId 22?»\n");
        prompt.append("Финальный ответ:\n");
        prompt.append("SELECT id, timestamp, log_level, message\n");
        prompt.append("FROM log_entries\n");
        prompt.append("WHERE message ILIKE '%userId:22%'\n");
        prompt.append("ORDER BY timestamp DESC;\n\n");
        
        prompt.append("Пример 2. Использование зависимостей через traceId\n\n");
        prompt.append("Шаблоны логов:\n");
        prompt.append("Start send communication for userId:{}. traceId:{}\n");
        prompt.append("Communication send successfully for userId:{}. traceId:{}\n");
        prompt.append("Communication send failed for userId:{}. traceId:{}. Reason:{}\n\n");
        prompt.append("Запрос пользователя: «Доставлено ли сообщение для пользователя с userId=23 в трейсе 1?»\n");
        prompt.append("Финальный ответ:\n");
        prompt.append("WITH TargetTrace AS (\n");
        prompt.append("    SELECT\n");
        prompt.append("        REGEXP_SUBSTR(message, 'traceId:([a-zA-Z0-9-]+)', 1, 1, 'i', 1) as trace_id\n");
        prompt.append("    FROM log_entries\n");
        prompt.append("    WHERE message ILIKE '%Start send communication for userId:23%'\n");
        prompt.append(")\n");
        prompt.append("SELECT l.id, l.timestamp, l.log_level, l.message\n");
        prompt.append("FROM log_entries l\n");
        prompt.append("JOIN TargetTrace tt ON l.message LIKE '%' || tt.trace_id || '%'\n");
        prompt.append("WHERE\n");
        prompt.append("    l.message ILIKE '%Communication send successfully%' OR\n");
        prompt.append("    l.message ILIKE '%Communication send failed%' OR\n");
        prompt.append("    l.message ILIKE '%Start send communication for userId:23%'\n");
        prompt.append("ORDER BY l.timestamp DESC;\n\n");
        
        prompt.append("Пример 3. Поиск по неполным данным (Fallback-логика)\n\n");
        prompt.append("Шаблоны логов:\n");
        prompt.append("Communication send skipped. User has no subscription. userId:{}, traceId:{}\n");
        prompt.append("Communication send attemp failed for userId:{}. traceId:{}. Retrying\n");
        prompt.append("Communication send failed for userId:{}. traceId:{}. Reason:{}\n\n");
        prompt.append("Запрос пользователя: «Что случилось у пользователя 24?»\n");
        prompt.append("Финальный ответ:\n");
        prompt.append("SELECT id, timestamp, log_level, message\n");
        prompt.append("FROM log_entries\n");
        prompt.append("WHERE\n");
        prompt.append("    (message ILIKE '%userId:24%' AND log_level IN ('ERROR', 'WARN'))\n");
        prompt.append("    OR message ILIKE '%userId:24%'\n");
        prompt.append("ORDER BY timestamp DESC;\n\n");
        
        prompt.append("[Входные данные]\n\n");
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