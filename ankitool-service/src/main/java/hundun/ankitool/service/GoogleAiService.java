package hundun.ankitool.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import hundun.ankitool.core.JlptNote;
import hundun.ankitool.core.StandardDictionaryWord;
import hundun.ankitool.core.util.JapaneseCharacterTool;
import hundun.ankitool.service.JltpNoteService.UIStrings;
import hundun.ankitool.service.remote.GoogleAiFeignClientImpl;
import hundun.ankitool.service.remote.IGoogleAiFeignClient.GenerateContentResponse;
import hundun.ankitool.service.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GoogleAiService {

    GoogleAiFeignClientImpl feignClient;


    public GoogleAiService() {
        feignClient = new GoogleAiFeignClientImpl();
    }

    @Data
    public static class ConfuseResult {
        String id;
        List<String> confusedFurigana;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ConfuseInput {
        String id;
        String furigana;
    }




    public List<ConfuseResult> confuse(List<ConfuseInput> inputs, String step2AskTemplate) {
        String ask;
        try {
            ask = step2AskTemplate + "\n" + JsonUtils.objectMapper.writeValueAsString(inputs);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        try {
            GenerateContentResponse chatResult = feignClient.singleAsk(ask);
            String content = chatResult.getCandidates()[0].getContent().getParts()[0].getText();
            //content = content.split("</think>")[1].trim();
            content = content.replace("```json", "").replace("```", "");
            List<ConfuseResult> nodes = JsonUtils.objectMapper.readValue(content, JsonUtils.objectMapper.getTypeFactory().constructCollectionType(List.class, ConfuseResult.class));
            boolean allKana = nodes.stream()
                    .flatMap(it -> it.getConfusedFurigana().stream())
                    .map(it -> it.replace("〜", ""))
                    .allMatch(it -> JapaneseCharacterTool.isAllKana(it));
            if (!allKana) {
                throw new Exception("result not allKana.");
            }
            List<String> askIds = inputs.stream()
                    .map(it -> it.getId())
                    .collect(Collectors.toList());
            List<String> resultIds = nodes.stream()
                    .map(it -> it.getId())
                    .collect(Collectors.toList());
            if (!askIds.equals(resultIds)) {
                logNotEquals(askIds, resultIds);
                throw new Exception("id not equals.");
            }
            return nodes;
        } catch (Exception e) {
            log.error("bad confuse: ", e);
        }
        return null;
    }

    protected void logNotEquals(List<String> list1, List<String> list2) {
        List<String> same = new ArrayList<>(list1);
        same.retainAll(list2);
        List<String> list1Diff = new ArrayList<>(list1);
        list1Diff.removeAll(same);
        List<String> list2Diff = new ArrayList<>(list2);
        list2Diff.removeAll(same);
        log.warn("NotEquals list1Diff = {}, list2Diff = {}", list1Diff, list2Diff);
    }
}
