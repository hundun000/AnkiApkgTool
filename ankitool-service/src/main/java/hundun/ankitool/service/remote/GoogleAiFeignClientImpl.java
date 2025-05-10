package hundun.ankitool.service.remote;



import hundun.ankitool.service.SecretConfig;
import hundun.ankitool.service.remote.IGoogleAiFeignClient.GenerateContentRequest;
import hundun.ankitool.service.remote.IGoogleAiFeignClient.GenerateContentRequest.Content;
import hundun.ankitool.service.remote.IGoogleAiFeignClient.GenerateContentRequest.Content.Part;
import hundun.ankitool.service.remote.IGoogleAiFeignClient.GenerateContentRequest.GenerationConfig;
import hundun.ankitool.service.remote.IGoogleAiFeignClient.GenerateContentResponse;

import java.util.List;

public class GoogleAiFeignClientImpl {

    IGoogleAiFeignClient googleAiFeignClient;

    String key;
    //String model = "gemini-2.0-flash";
    String model = "gemini-2.5-flash-preview-04-17";
    /**
     * 好的，如果输出的 List<ConfuseResult> 里包含生成的错误答案，并且这部分需要有一定的创作性来生成 * plausible* 但 incorrect 的选项，那么适合使用一个 中等偏高 的 temperature。
     * 例如，0.5 到 0.8 之间的 temperature 比较合适。
     */
    float temperature = 0.6f;
    public GoogleAiFeignClientImpl() {
        googleAiFeignClient = IGoogleAiFeignClient.instance();
        this.key = SecretConfig.googleKey;
    }


    public GenerateContentResponse singleAsk(String ask) throws Exception {
        GenerateContentRequest requestModel = GenerateContentRequest.builder()
                .contents(List.of(
                        Content.builder()
                                .parts(List.of(
                                        Part.builder()
                                                .text(ask)
                                                .build()
                                ))
                                .build()
                ))
                .generationConfig(GenerationConfig.builder()
                        .temperature(temperature)
                        .build())
                .build();

        // start conversation with model
        return googleAiFeignClient.generateContent(model, key, requestModel);

    }
}
