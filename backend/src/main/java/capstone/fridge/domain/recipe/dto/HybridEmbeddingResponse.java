package capstone.fridge.domain.recipe.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HybridEmbeddingResponse {
    private List<Float> dense;
    private SparseData sparse;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SparseData {
        private List<Integer> indices;
        private List<Float> values;
    }
}
