package capstone.fridge.domain.recipe.agent;

import capstone.fridge.domain.recipe.dto.RecipeResponseDTO;
import capstone.fridge.global.client.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AgentGeneratorImpl implements AgentGenerator {

    private final OpenAiClient openAiClient;

    private static final String SYSTEM = """
        You are a helpful recipe assistant. Given the user's question and a list of retrieved recipes,
        write a short, friendly answer in Korean that recommends or explains recipes based on the list.
        Mention 2-3 recipe names naturally. If the list is empty, say you couldn't find matching recipes and suggest trying different keywords.
        Do not make up recipe names; only use those from the list.
        """;

    @Override
    public String generate(String userQuery, List<RecipeResponseDTO.RecipeDTO> recipes) {
        String recipeSummary = formatRecipesForContext(recipes);

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM),
                Map.of("role", "user", "content",
                        "User question: " + userQuery + "\n\nRetrieved recipes:\n" + recipeSummary)
        );

        String content = openAiClient.chat(messages);
        return content != null ? content.trim() : "검색 결과를 바탕으로 추천해 드리지 못했습니다. 다른 검색어로 시도해 보세요.";
    }

    private static String formatRecipesForContext(List<RecipeResponseDTO.RecipeDTO> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return "(no recipes)";
        }
        return recipes.stream()
                .limit(10)
                .map(r -> "- " + r.getTitle() + (r.getDescription() != null && !r.getDescription().isBlank() ? ": " + r.getDescription().substring(0, Math.min(100, r.getDescription().length())) + "..." : ""))
                .collect(Collectors.joining("\n"));
    }
}
