package capstone.fridge.domain.recipe.agent;

import capstone.fridge.domain.recipe.agent.dto.AgentDtos;
import capstone.fridge.global.client.OpenAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentPlannerImpl implements AgentPlanner {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM = """
        You are a recipe search planner. Given the user's natural language query about food or recipes:
        1) Rewrite it into one or more search-friendly queries suitable for a recipe retrieval system (Korean).
        2) Decide whether to call retrieval (call_retrieval: true if the user is asking for recipe recommendations or search).
        3) Decide whether to use re-ranking (call_rerank: true when we need precise ordering of multiple results).

        Respond with ONLY a single JSON object, no markdown or explanation:
        {"rewritten_queries": ["query1", "query2"], "call_retrieval": true, "call_rerank": true}
        """;

    @Override
    public AgentDtos.PlanResult plan(String userQuery) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM),
                Map.of("role", "user", "content", "User query: " + userQuery)
        );

        String content = openAiClient.chat(messages);
        if (content == null || content.isBlank()) {
            return new AgentDtos.PlanResult(List.of(userQuery), true, false);
        }

        content = content.trim();
        if (content.startsWith("```")) {
            content = content.replaceFirst("^```\\w*\\n?", "").replaceFirst("\\n?```$", "").trim();
        }

        try {
            AgentDtos.PlanResult result = objectMapper.readValue(content, AgentDtos.PlanResult.class);
            if (result.getRewrittenQueries() == null || result.getRewrittenQueries().isEmpty()) {
                return new AgentDtos.PlanResult(List.of(userQuery), result.isCallRetrieval(), result.isCallRerank());
            }
            return result;
        } catch (JsonProcessingException e) {
            log.warn("Planner JSON parse failed, using fallback. content={}", content, e);
            return new AgentDtos.PlanResult(List.of(userQuery), true, false);
        }
    }
}
