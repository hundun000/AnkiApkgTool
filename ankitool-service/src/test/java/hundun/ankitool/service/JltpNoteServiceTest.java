package hundun.ankitool.service;

import hundun.ankitool.service.ApkgReader.ReadResult;
import hundun.ankitool.core.JlptNote;
import hundun.ankitool.core.StandardDictionaryWord;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class JltpNoteServiceTest {



    @Test
    public void testJlptNote() throws Exception {
        String apkgPath = "../data/NEW-JLPT__NEW-N5.apkg";
        String outputNoteJson = "../data/NEW-JLPT__NEW-N5.json";

        ReadResult readResult = ApkgReader.read(new File(apkgPath));
        List<JlptNote> notes = JltpNoteService.toJlptNodes(readResult);
        JltpNoteService.objectMapper.writeValue(new File(outputNoteJson), notes);
        System.out.println("导出成功：" + outputNoteJson);
    }


    @Test
    public void testStandard() throws Exception {
        String outputNoteJson = "../data/NEW-JLPT__NEW-N5.json";
        String outputWordsJson = "../data/NEW-JLPT__NEW-N5.words.json";

        List<JlptNote> notes = JltpNoteService.objectMapper.readValue(
                new File(outputNoteJson),
                JltpNoteService.objectMapper.getTypeFactory().constructCollectionType(List.class, JlptNote.class)
        );
        List<StandardDictionaryWord> words = notes.stream()
                .map(it -> JltpNoteService.toStandard(it))
                .collect(Collectors.toList());
        JltpNoteService.objectMapper.writeValue(new File(outputWordsJson), words);
        System.out.println("导出成功：" + outputWordsJson);
    }
}