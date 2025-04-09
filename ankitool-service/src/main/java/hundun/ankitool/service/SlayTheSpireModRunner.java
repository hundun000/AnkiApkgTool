package hundun.ankitool.service;

import com.fasterxml.jackson.databind.JsonNode;
import hundun.ankitool.core.ApkgReader;
import hundun.ankitool.core.ApkgReader.ReadResult;
import hundun.ankitool.core.JlptNote;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class SlayTheSpireModRunner {
    public static void main(String[] args) throws Exception {
        String apkgPathFolder = "../data/SlayTheSpireMod/";
        String outputFolder = "../data/SlayTheSpireMod/";

        File inputFolder = new File(apkgPathFolder);
        for (File inputFile : Objects.requireNonNull(inputFolder.listFiles())) {
            if (!inputFile.getName().endsWith(".apkg")) {
                continue;
            }
            String mainName = inputFile.getName().replace(".apkg", "");
            String outputFileName = mainName + ".json";
            ReadResult readResult = ApkgReader.read(inputFile);
            List<JlptNote> notes = JltpNoteService.toJlptNodes(readResult);
            JsonNode result = JltpNoteService.toSlayTheSpireModVocabularyNodes(notes, "CET46:" + mainName + "_");
            JltpNoteService.objectMapper.writeValue(new File(outputFolder + outputFileName), result);
            System.out.println("导出成功：" + outputFileName);
        }
    }
}
