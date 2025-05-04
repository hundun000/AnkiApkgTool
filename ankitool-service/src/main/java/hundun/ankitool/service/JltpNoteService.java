package hundun.ankitool.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.ankitool.service.ApkgReader.NoteResult;
import hundun.ankitool.service.ApkgReader.ReadResult;
import hundun.ankitool.core.JlptNote;
import hundun.ankitool.core.StandardDictionaryWord;
import hundun.ankitool.core.util.JapaneseCharacterTool;
import hundun.ankitool.service.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

public class JltpNoteService {
    public static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }
    private static String getField(String[] fields, int index) {
        return index < fields.length ? fields[index] : null;
    }

    private static String getField(List<String> fields) {
        return !fields.isEmpty() ? fields.remove(0) : null;
    }

    public static List<JlptNote> toJlptNodes(ReadResult readResult) {
        return readResult.getModelResultMap().values().stream()
                .findFirst()
                .stream()
                .flatMap(line -> line.getNotes().stream())
                .map(it -> jlptNoteFromFields(it))
                .collect(Collectors.toList());
    }

    static Map<String, String> tagMap = Map.of(
            "NEW-JLPT-SC::N5", "N5"
    );

    public static StandardDictionaryWord toStandard(JlptNote note) {
        return StandardDictionaryWord.builder()
                .source("JLPT_NOTE")
                .sourceId(note.getNoteId())
                .vocabKanji(Optional.ofNullable(note.getVocabPlus())
                        .filter(it -> it.length() > 0)
                        .or(() -> Optional.of(note.getVocabKanji())
                                .filter(it -> JapaneseCharacterTool.hasAnyKanji(it))
                        )
                        .map(it -> it.replaceAll("\\[.*?\\]", ""))
                        .orElse(null)
                )
                .vocabDefCN(note.getVocabDefCN())
                .vocabFurigana(
                        JapaneseCharacterTool.hasAnyKanji(note.getVocabKanji()) ?
                                note.getVocabFurigana() :
                                note.getVocabKanji()
                )
                .vocabPoS(note.getVocabPoS())
                .standardTags(note.getTags().stream()
                        .map(it -> tagMap.get(it))
                        .filter(it -> it != null)
                        .collect(Collectors.toList())
                )
                .build();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UIStrings {
        @JsonProperty(value = "TEXT")
        String[] text;
        @JsonProperty(value = "EXTRA_TEXT")
        String[] extraText;
        @JsonProperty(value = "TEXT_DICT")
        Map<String, String> textDict;

        public static void main(String[] args) throws JsonProcessingException {
            System.out.println(JsonUtils.objectMapper.writeValueAsString(UIStrings.builder().build()));
        }
    }



    public static Map<String, UIStrings> toSlayTheSpireModVocabularyNodes(List<JlptNote> jlptNotes, String idStart) {
        Map<String, UIStrings> result = new HashMap<>();
        UIStrings info = new UIStrings();
        String[] infoTexts = {String.valueOf(jlptNotes.size())};
        info.setText(infoTexts);
        result.put(idStart + "info", info);
        for (int i = 0; i < jlptNotes.size(); i++) {
            JlptNote note = jlptNotes.get(i);
            StandardDictionaryWord standardDictionaryWord = toStandard(note);
            String id = idStart + i;
            UIStrings word = new UIStrings();
            List<String> wordTexts = new ArrayList<>();
            wordTexts.add(note.getVocabKanji().replaceAll("\\[.*?\\]", ""));
            wordTexts.add(note.getVocabDefCN());
            wordTexts.add(standardDictionaryWord.getVocabFurigana());
            wordTexts.add(Optional.ofNullable(standardDictionaryWord.getVocabKanji()).filter(it -> it.length() > 0).orElse(null));
            word.setText(wordTexts.toArray(new String[0]));
            result.put(id, word);
        }
        return result;
    }

    public static JlptNote jlptNoteFromFields(NoteResult noteResult) {
        var fields = new ArrayList<>(noteResult.getFieldValues());
        JlptNote note = new JlptNote();
        note.setTags(noteResult.getTags());

        note.setNoteId(getField(fields));
        note.setVocabKanji(getField(fields));
        note.setVocabPitch(getField(fields));
        note.setVocabPoS(getField(fields));
        note.setVocabFurigana(getField(fields));
        note.setVocabPlus(getField(fields));
        note.setVocabDefCN(getField(fields));
        note.setVocabDefTC(getField(fields));
        note.setVocabAudio(getField(fields));
        note.setSentType1(getField(fields));
        note.setSentKanji1(getField(fields));
        note.setSentFurigana1(getField(fields));
        note.setSentDef1(getField(fields));
        note.setSentDefTC1(getField(fields));
        note.setSentAudio1(getField(fields));
        note.setSentType2(getField(fields));
        note.setSentKanji2(getField(fields));
        note.setSentFurigana2(getField(fields));
        note.setSentDef2(getField(fields));
        note.setSentDefTC2(getField(fields));
        note.setSentAudio2(getField(fields));
        note.setSentType3(getField(fields));
        note.setSentKanji3(getField(fields));
        note.setSentFurigana3(getField(fields));
        note.setSentDef3(getField(fields));
        note.setSentDefTC3(getField(fields));
        note.setSentAudio3(getField(fields));
        note.setSentType4(getField(fields));
        note.setSentKanji4(getField(fields));
        note.setSentFurigana4(getField(fields));
        note.setSentDef4(getField(fields));
        note.setSentDefTC4(getField(fields));
        note.setSentAudio4(getField(fields));
        note.setLevel(getField(fields));
        note.setAlt1(getField(fields));
        note.setAlt2(getField(fields));
        note.setAlt3(getField(fields));
        note.setAlt4(getField(fields));
        return note;
    }
}
