package hundun.ankitool.service;

import hundun.ankitool.service.ApkgReader.ReadResult;
import hundun.ankitool.core.JlptNote;
import hundun.ankitool.service.GoogleAiService.ConfuseInput;
import hundun.ankitool.service.GoogleAiService.ConfuseResult;
import hundun.ankitool.service.JltpNoteService.UIStrings;
import hundun.ankitool.service.util.FileUtils;
import hundun.ankitool.service.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SlayTheSpireModRunner {
    static String apkgPathFolder = "./data/SlayTheSpireMod/";
    static String jsonFolder = "./data/SlayTheSpireMod/";

    boolean withConfuse;

    GoogleAiService googleAiService = new GoogleAiService();

    public static class ApkgParserTask {
        public static void main(String[] args) throws Exception {
            File inputFolder = new File(apkgPathFolder);
            for (File inputFile : Objects.requireNonNull(inputFolder.listFiles())) {
                if (!inputFile.getName().endsWith(".apkg")) {
                    continue;
                }
                String mainName = inputFile.getName().replace(".apkg", "");
                String outputFileName = mainName + ".json";
                ReadResult readResult = ApkgReader.read(inputFile);
                List<JlptNote> notes = JltpNoteService.toJlptNodes(readResult);
                Map<String, UIStrings> result = JltpNoteService.toSlayTheSpireModVocabularyNodes(notes, "CET46:" + mainName + "_");
                JltpNoteService.objectMapper.writeValue(new File(jsonFolder + outputFileName), result);
                System.out.println("导出成功：" + outputFileName);
            }
        }
    }

    public static class ConfuseTask {
        public static void main(String[] args) throws Exception {
            File resultFile = new File(jsonFolder + "N5.json");
            SlayTheSpireModRunner runner = new SlayTheSpireModRunner();
            runner.confuse(resultFile, 5);
        }
    }

    static final int GROUP_SIZE = 10;
    public static final int NORMAL_UISTRINGS_INDEX = 0;
    public static final int MEANING_UISTRINGS_INDEX = 1;
    public static final int KANA_UISTRINGS_INDEX = 2;
    public static final int KANJI_UISTRINGS_INDEX = 3;

    public void confuse(File resultFile, Integer limit) throws Exception {

        File step2AskTemplateFile = new File(jsonFolder + "Step2AskTemplate.txt");
        String step2AskTemplate = FileUtils.readAllLines(step2AskTemplateFile).stream().collect(Collectors.joining("\n"));



        Map<String, UIStrings> resultMap = JsonUtils.objectMapper.readValue(
                resultFile,
                JltpNoteService.objectMapper.getTypeFactory().constructMapType(Map.class, String.class, UIStrings.class)
        );
        List<ConfuseInput> standardDictionaryWords = resultMap.entrySet().stream()
                .filter(it -> it.getValue().getTextDict() == null)
                .filter(it -> !it.getKey().contains("_info"))
                .map(it -> {
                    String normal = it.getValue().text[NORMAL_UISTRINGS_INDEX];
                    String kana = it.getValue().text[KANA_UISTRINGS_INDEX];
                    String kanji = it.getValue().text[KANJI_UISTRINGS_INDEX];
                    // 有汉字时，需要task生成混淆后的furigana
                    String confuseInputKana = kanji != null ? kana : null;
                    if (confuseInputKana != null) {
                        return ConfuseInput.builder()
                                .id(it.getKey())
                                .furigana(confuseInputKana)
                                .build();
                    } else {
                        return null;
                    }
                })
                .filter(it -> it != null)
                .limit(limit != null ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
        List<List<ConfuseInput>> taskGroups = splitAiTaskGroups(standardDictionaryWords);

        for (int i = 0; i < taskGroups.size(); i++) {
            // taskGroup的所有歌词合起来问一次
            var taskGroup = taskGroups.get(i);
            log.info("start confuse Group[{}] size = {}", i, taskGroup.size());
            List<ConfuseResult> groupResult = googleAiService.confuse(taskGroup, step2AskTemplate);
            if (groupResult != null) {
                // 将结果分配回taskGroup
                for (ConfuseResult confuseResult : groupResult) {
                    UIStrings target = resultMap.get(confuseResult.getId());
                    if (target.getTextDict() == null) {
                        target.setTextDict(new HashMap<>());
                    }
                    target.getTextDict().put("ConfusedFurigana", confuseResult.getConfusedFurigana().stream().collect(Collectors.joining("|")));
                }
            } else {
                log.error("cannot handle taskGroup = {}", taskGroup);
            }
        }
        JsonUtils.objectMapper.writeValue(resultFile, resultMap);
    }

    private <T> List<List<T>> splitAiTaskGroups(List<T> inputs) {
        inputs = new ArrayList<>(inputs);
        List<List<T>> result = new ArrayList<>();
        while (inputs.size() > GROUP_SIZE) {
            List<T> sub = inputs.subList(0, GROUP_SIZE);
            inputs = inputs.subList(GROUP_SIZE, inputs.size());
            result.add(sub);
        }
        result.add(inputs);
        return result;
    }
}
