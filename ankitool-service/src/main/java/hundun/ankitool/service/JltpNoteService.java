package hundun.ankitool.service;

import hundun.ankitool.core.ApkgReader.ReadResult;
import hundun.ankitool.core.JlptNote;

import java.util.List;
import java.util.stream.Collectors;

public class JltpNoteService {

    private static String getField(String[] fields, int index) {
        return index < fields.length ? fields[index] : null;
    }

    public static List<JlptNote> toJlptNodes(ReadResult readResult) {
        return readResult.getMidToLinesMap().values().stream()
                .findFirst()
                .stream()
                .flatMap(line -> line.stream())
                .map(it -> jlptNoteFromFields(it.split("\u001F")))
                .collect(Collectors.toList());
    }

    public static JlptNote jlptNoteFromFields(String[] fields) {
        JlptNote note = new JlptNote();
        note.setNoteId(getField(fields, 0));
        note.setVocabKanji(getField(fields, 1));
        note.setVocabPitch(getField(fields, 2));
        note.setVocabPoS(getField(fields, 3));
        note.setVocabFurigana(getField(fields, 4));
        note.setVocabPlus(getField(fields, 5));
        note.setVocabDefCN(getField(fields, 6));
        note.setVocabDefTC(getField(fields, 7));
        note.setVocabAudio(getField(fields, 8));
        note.setSentType1(getField(fields, 9));
        note.setSentKanji1(getField(fields, 10));
        note.setSentFurigana1(getField(fields, 11));
        note.setSentDef1(getField(fields, 12));
        note.setSentDefTC1(getField(fields, 13));
        note.setSentAudio1(getField(fields, 14));
        note.setSentType2(getField(fields, 15));
        note.setSentKanji2(getField(fields, 16));
        note.setSentFurigana2(getField(fields, 17));
        note.setSentDef2(getField(fields, 18));
        note.setSentDefTC2(getField(fields, 19));
        note.setSentAudio2(getField(fields, 20));
        note.setSentType3(getField(fields, 21));
        note.setSentKanji3(getField(fields, 22));
        note.setSentFurigana3(getField(fields, 23));
        note.setSentDef3(getField(fields, 24));
        note.setSentDefTC3(getField(fields, 25));
        note.setSentAudio3(getField(fields, 26));
        note.setSentType4(getField(fields, 27));
        note.setSentKanji4(getField(fields, 28));
        note.setSentFurigana4(getField(fields, 29));
        note.setSentDef4(getField(fields, 30));
        note.setSentDefTC4(getField(fields, 31));
        note.setSentAudio4(getField(fields, 32));
        note.setLevel(getField(fields, 33));
        note.setAlt1(getField(fields, 34));
        note.setAlt2(getField(fields, 35));
        note.setAlt3(getField(fields, 36));
        note.setAlt4(getField(fields, 37));
        return note;
    }
}
