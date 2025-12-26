package capstone.fridge.global.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

    @Value("${qdrant.url}")
    private String qdrantUrl;

    @Value("${qdrant.api-key}")
    private String apiKey;

    @Bean
    public QdrantClient qdrantClient() {

        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(qdrantUrl, 6334, true); // true = SSL(HTTPS) 사용

        if (apiKey != null && !apiKey.isBlank()) {
            grpcClientBuilder.withApiKey(apiKey);
        }

        return new QdrantClient(grpcClientBuilder.build());
    }
}