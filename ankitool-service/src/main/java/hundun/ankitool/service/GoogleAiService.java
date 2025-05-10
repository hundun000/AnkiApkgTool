package hundun.ankitool.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import hundun.ankitool.core.JlptNote;
import hundun.ankitool.core.StandardDictionaryWord;
import hundun.ankitool.core.util.JapaneseCharacterTool;
import hundun.ankitool.core.util.KanaOptionChecker;
import hundun.ankitool.service.JltpNoteService.UIStrings;
import hundun.ankitool.service.remote.GoogleAiFeignClientImpl;
import hundun.ankitool.service.remote.IGoogleAiFeignClient.GenerateContentResponse;
import hundun.ankitool.service.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
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
        String show;
        String furigana;
    }

    static final int RETRY = 3;

    public @Nullable List<ConfuseResult> confuseWithRetry(List<ConfuseInput> inputs, String step2AskTemplate) {
        List<ConfuseResult> groupResult = null;
        int retry = 0;
        while (groupResult == null && retry < RETRY) {
            log.info("start aiStep2Group, retry = {}", retry);
            groupResult = this.confuse(inputs, step2AskTemplate);
            retry++;
        }
        return groupResult;
    }

    public List<ConfuseResult> confuse(List<ConfuseInput> inputs, String step2AskTemplate) {
        String ask;
        try {
            ask = step2AskTemplate + "\n" + JsonUtils.objectMapper.writeValueAsString(inputs);
        } catch (JsonProcessingException e) {
            log.error("bad inputs to ask: ", e);
            return null;
        }
        try {
            GenerateContentResponse chatResult = feignClient.singleAsk(ask);
            if (chatResult.getCandidates()[0].getFinishReason().equals("MAX_TOKENS")) {
                log.warn("MAX_TOKENS happened, chatResult = {}, ask = {}", chatResult, ask);
                return null;
            }
            String content = chatResult.getCandidates()[0].getContent().getParts()[0].getText();
            //content = content.split("</think>")[1].trim();
            content = content.replace("```json", "").replace("```", "");
            List<ConfuseResult> results;
            try {
                results = JsonUtils.objectMapper.readValue(content, JsonUtils.objectMapper.getTypeFactory().constructCollectionType(List.class, ConfuseResult.class));
            } catch (JsonProcessingException e) {
                log.error("JsonProcessingException by chatResult = {}", chatResult);
                throw e;
            }
            boolean allKana = results.stream()
                    .flatMap(it -> it.getConfusedFurigana().stream())
                    .map(it -> it.replace("〜", ""))
                    .allMatch(it -> JapaneseCharacterTool.isAllKana(it));
            if (!allKana) {
                throw new Exception("result not allKana.");
            }
            if (inputs.size() != results.size()) {
                throw new Exception("result not same size.");
            }
            List<Pair<ConfuseInput, ConfuseResult>> inputOutputPairs = new ArrayList<>();
            for (int i = 0; i < inputs.size(); i++) {
                inputOutputPairs.add(new Pair<>(inputs.get(i), results.get(i)));
            }
            boolean idEquals = inputOutputPairs.stream()
                    .allMatch(pair ->  pair.getFirst().getId().equals(pair.getSecond().getId()));
            if (!idEquals) {
                throw new Exception("id not equals.");
            }
            return results;
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
