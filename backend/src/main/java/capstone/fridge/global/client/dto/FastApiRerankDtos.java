package capstone.fridge.global.client.dto;

import java.util.List;

/**
 * FastAPI /rerank 요청·응답 (Cross-Encoder Re-ranking)
 */
public class FastApiRerankDtos {

    public record RerankReq(
            String query,
            List<RerankDoc> documents
    ) {}

    public record RerankDoc(
            long id,
            String text
    ) {}

    public record RerankRes(
            List<Long> ranked_ids
    ) {}
}
