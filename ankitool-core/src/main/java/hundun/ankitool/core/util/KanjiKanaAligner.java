package hundun.ankitool.core.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KanjiKanaAligner {
    /**
     * 该算法的时间复杂度是指数级的; 应对词素分析结果的短文本使用。<br>
     * <br>
     * 示例：词汇「見直す（みなおす）」<br>
     * <br>
     * - 汉字：見直<br>
     * - 假名读音：みなおす<br>
     * <br>
     * 可能的对齐方式包括：<br>
     * 1. 見 → み，直 → なおす<br>
     * 2. 見 → みな，直 → おす<br>
     * <br>
     * 这两种分割方式都能组成原始的假名读音「みなおす」，因此在对齐时存在多种可能。<br>
     */
    public static List<List<CharKanaPair>> align(String inputStr, String kanaStr) {
        List<List<CharKanaPair>> results = new ArrayList<>();
        backtrack(inputStr, kanaStr, 0, 0, new ArrayList<>(), results);
        return results;
    }

    private static void backtrack(String inputStr, String kanaStr, int strIndex, int kanaIndex,
                                  List<CharKanaPair> currentList, List<List<CharKanaPair>> results) {
        if (strIndex == inputStr.length() && kanaIndex == kanaStr.length()) {
            results.add(new ArrayList<>(currentList));
            return;
        }
        if (strIndex >= inputStr.length() || kanaIndex > kanaStr.length()) {
            return;
        }

        char c = inputStr.charAt(strIndex);

        if (JapaneseCharacterTool.isKanji(c)) {
            for (int len = 1; kanaIndex + len <= kanaStr.length(); len++) {
                String kanaSegment = kanaStr.substring(kanaIndex, kanaIndex + len);
                currentList.add(new CharKanaPair(c, kanaSegment));
                backtrack(inputStr, kanaStr, strIndex + 1, kanaIndex + len, currentList, results);
                currentList.remove(currentList.size() - 1);
            }
        } else {
            // 假名字符本身，必须与读音对齐
            if (kanaIndex < kanaStr.length() && kanaStr.charAt(kanaIndex) == c) {
                currentList.add(new CharKanaPair(null, String.valueOf(c)));
                backtrack(inputStr, kanaStr, strIndex + 1, kanaIndex + 1, currentList, results);
                currentList.remove(currentList.size() - 1);
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CharKanaPair {
        @Nullable
        public Character kanji;
        public String kana;
    }

    public static void main(String[] args) {
        String kanji = "見直す見直す";
        String kana = "みなおすみなおす";

        List<List<CharKanaPair>> alignments = align(kanji, kana);

        for (List<CharKanaPair> alignment : alignments) {
            System.out.println(alignment);
        }
    }
}
