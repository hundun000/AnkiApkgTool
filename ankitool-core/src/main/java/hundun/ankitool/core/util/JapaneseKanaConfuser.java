package hundun.ankitool.core.util;

import hundun.ankitool.core.util.KanjiKanaAligner.CharKanaPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JapaneseKanaConfuser {
    // 清音-浊音-半浊音对应表（支持三向替换）
    private static final String[][][] VOICED_TRIPLES = {
            // か行
            {{"か", "が"}}, {{"き", "ぎ"}}, {{"く", "ぐ"}}, {{"け", "げ"}}, {{"こ", "ご"}},
            // さ行
            {{"さ", "ざ"}}, {{"し", "じ"}}, {{"す", "ず"}}, {{"せ", "ぜ"}}, {{"そ", "ぞ"}},
            // た行
            {{"た", "だ"}}, {{"ち", "ぢ"}}, {{"つ", "づ"}}, {{"て", "で"}}, {{"と", "ど"}},
            // は行（特殊：三向替换）
            {{"は", "ば", "ぱ"}}, {{"ひ", "び", "ぴ"}}, {{"ふ", "ぶ", "ぷ"}}, {{"へ", "べ", "ぺ"}}, {{"ほ", "ぼ", "ぽ"}},
            // 片假名か行
            {{"カ", "ガ"}}, {{"キ", "ギ"}}, {{"ク", "グ"}}, {{"ケ", "ゲ"}}, {{"コ", "ゴ"}},
            // 片假名さ行
            {{"サ", "ザ"}}, {{"シ", "ジ"}}, {{"ス", "ズ"}}, {{"セ", "ゼ"}}, {{"ソ", "ゾ"}},
            // 片假名た行
            {{"タ", "ダ"}}, {{"チ", "ヂ"}}, {{"ツ", "ヅ"}}, {{"テ", "デ"}}, {{"ト", "ド"}},
            // 片假名は行（三向替换）
            {{"ハ", "バ", "パ"}}, {{"ヒ", "ビ", "ピ"}}, {{"フ", "ブ", "プ"}}, {{"ヘ", "ベ", "ペ"}}, {{"ホ", "ボ", "ポ"}}
    };

    // 拗音对应表
    private static final String[][] SMALL_KANA_PAIRS = {
            {"や", "ゃ"}, {"ゆ", "ゅ"}, {"よ", "ょ"},
            {"ヤ", "ャ"}, {"ユ", "ュ"}, {"ヨ", "ョ"},
            //{"あ", "ぁ"}, {"い", "ぃ"}, {"う", "ぅ"}, {"え", "ぇ"}, {"お", "ぉ"},
            //{"ア", "ァ"}, {"イ", "ィ"}, {"ウ", "ゥ"}, {"Э", "ェ"}, {"オ", "ォ"},
            {"つ", "っ"}, {"ツ", "ッ"}
    };

    // 类似假名对应表
    private static final String[][] SIMILAR_KANA = {
            {"ね", "れ"}, {"わ", "れ"}, {"る", "ろ"}, {"め", "ぬ"},
            {"ノ", "ヌ"}, {"ソ", "ン"}, {"シ", "ツ"}, {"コ", "ユ"},
            {"あ", "お"}, {"い", "り"}, {"う", "つ"}, {"え", "へ"}, {"お", "を"},
            {"ク", "ケ"}, {"タ", "ナ"}, {"ヒ", "ビ"}, {"フ", "ワ"}
    };
    static Random random = new Random();
    public static List<String> generateConfusingKana(JapaneseText japaneseText, int n, List<String> resultList) {
        if (japaneseText.getKana() == null || japaneseText.getKana().isEmpty()) {
            return null;
        }
        Set<String> existList = new HashSet<>();
        existList.add(japaneseText.getKana());

        List<String> confusedTexts = confuseKana(japaneseText, existList);

        Collections.shuffle(confusedTexts);
        resultList.addAll(
                confusedTexts.stream()
                        .filter(it -> !it.equals(japaneseText.getKana()))
                        .limit(n)
                        .collect(Collectors.toList())
        );

        return resultList;
    }

    static final int STRATEGY_SIZE = 2;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ConfusedWord {
        @Nullable
        Character kanji;
        String originKana;
        List<String> confusedKanaList;
    }

    /**
     * 所有可能情况的笛卡尔积
     **/
    private static List<String> confusedWordsToTexts(List<ConfusedWord> confusedWords) {
        List<String> currentBuilders = new ArrayList<>();
        currentBuilders.add("");
        for (ConfusedWord confusedWord : confusedWords) {
            List<String> candidates = new ArrayList<>(confusedWord.getConfusedKanaList());
            candidates.add(confusedWord.getOriginKana());
            List<String> nextBuilders = new ArrayList<>();
            // 每一个currentBuilder加上一个candidate得到新的若干nextBuilder
            currentBuilders.forEach(currentBuilder -> {
                candidates.forEach(candidate -> {
                    nextBuilders.add(currentBuilder + candidate);
                });
            });
            currentBuilders = nextBuilders;
        }
        return currentBuilders;
    }

    /**
     * 改变japaneseText中的读音的任意位数（不包括原文）。输出所有可能的情况。
     * 若有汉字，则只改变汉字部分对应的假名；
     * 若无汉字，则改变所有假名部分；
     */
    private static List<String> confuseKana(JapaneseText japaneseText, Collection<String> existList) {
        if (japaneseText.getKana().isEmpty()) {
            return new ArrayList<>();
        }
        // 输入没有汉字
        if (japaneseText.getKanji() == null) {
            // 假设japaneseText对应一个placeholder汉字
            japaneseText = JapaneseText.builder()
                    .kana(japaneseText.getKana())
                    .kanji("占")
                    .build();
        }

        List<List<CharKanaPair>> alignResult = KanjiKanaAligner.align(japaneseText.getKanji(), japaneseText.getKana());
        if (alignResult.isEmpty()) {
            throw new RuntimeException("can not align, may be bad input: " + japaneseText);
        }
        List<CharKanaPair> kanjiMap = alignResult.get(0);
        List<ConfusedWord> confusedWords = kanjiMap.stream()
                .map(it -> ConfusedWord.builder()
                        .kanji(it.getKanji())
                        .originKana(it.getKana())
                        .confusedKanaList(new ArrayList<>())
                        .build())
                .collect(Collectors.toList());

        for (ConfusedWord confusedWord : confusedWords) {
            if (confusedWord.getKanji() == null) {
                continue;
            }
            List<String> subResult = confuseCharKanaPair(confusedWord.getOriginKana(), existList);
            confusedWord.getConfusedKanaList().addAll(subResult);
        }
        List<String> confusedTexts = confusedWordsToTexts(confusedWords);
        return confusedTexts;
    }

    /**
     * 改变kana任意位数（不包括原文）。输出所有可能的情况。
     */
    private static List<String> confuseCharKanaPair(String kana, Collection<String> existResultList) {
        List<Integer> tryStrategyOrderedList = IntStream.range(0, STRATEGY_SIZE)
                .mapToObj(it -> it)
                .collect(Collectors.toList());
        // 以顺序尝试所有策略
        List<String> result = new ArrayList<>();
        // 临时加入原始kana
        result.add(kana);
        for (Integer strategy : tryStrategyOrderedList) {
            // value是和原词的距离
            Map<String, Integer> inputs = new HashMap<>();
            inputs.put(kana, 0);

            while (!inputs.isEmpty()) {
                // 对比原始kana，inputs均变化了k位
                // 收集下一个循环要用的变化了（k+1）位的情况
                Map<String, Integer> nextInputs = new HashMap<>();
                for (var input : inputs.entrySet()) {
                    // 对比原始kana，subResults均变化了[k-1, k-1]位
                    Map<String, Integer> subResults = confuseKanaBit(input.getKey(), input.getValue(), 2, strategy, existResultList, result);
                    // 变化了[k-1, k]位的情况已经记录在result里了，移除后，subResults均变化了（k+1）位
                    subResults.entrySet().removeIf(it -> result.contains(it.getKey()));
                    if (!subResults.isEmpty()) {
                        result.addAll(subResults.keySet());
                        nextInputs.putAll(subResults);
                    }
                }
                inputs = nextInputs;
            }

        }
        // 移除临时加入原始kana
        result.remove(kana);
        return result;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class JapaneseText {
        String kanji;
        String kana;
    }


    /**
     * 改变kana的n位
     */
    private static Map<String, Integer> confuseKanaBit(String kana, int currentDistance, int maxDistance, int strategy, Collection<String> existResultList, List<String> existInputs) {
        Map<String, Integer> result = new HashMap<>();
        Set<String> ignoreList = new HashSet<>();
        ignoreList.addAll(existResultList);
        ignoreList.addAll(existInputs);
        List<Integer> tryPosOrderedList = IntStream.range(0, kana.length())
                .mapToObj(it -> it)
                .collect(Collectors.toList());
        // 以顺序尝试所有位置
        for (int pos : tryPosOrderedList) {
            String target = kana.substring(pos, pos + 1);

            switch (strategy) {
                case 0: // 清浊音变换
                    for (String[][] group : VOICED_TRIPLES) {
                        for (String[] pair : group) {
                            // 遍历所有可能的替换
                            for (int i = 0; i < pair.length; i++) {
                                if (pair[i].equals(target)) {
                                    // 尝试所有其他变体
                                    for (int j = 0; j < pair.length; j++) {
                                        if (j != i) {
                                            String candidate = replaceChar(kana, pos, pair[j]);
                                            if (!candidate.equals(kana)) {
                                                if (!ignoreList.contains(candidate)) {
                                                    result.put(candidate, currentDistance + 1);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;

                case 1: // 拗音变换
                    if (pos == 0) {
                        break;
                    }
                    for (String[] pair : SMALL_KANA_PAIRS) {
                        if (pair[0].equals(target)) {
                            String candidate = replaceChar(kana, pos, pair[1]);
                            if (!ignoreList.contains(candidate)) {
                                result.put(candidate, currentDistance + 1);
                            }
                        } else if (pair[1].equals(target)) {
                            String candidate = replaceChar(kana, pos, pair[0]);
                            if (!ignoreList.contains(candidate)) {
                                result.put(candidate, currentDistance + 1);
                            }
                        }
                    }
                    break;
                default:
            }
        }

        return result;
    }

    private static String replaceChar(String str, int pos, String replacement) {
        return str.substring(0, pos) + replacement + str.substring(pos + 1);
    }

    public static void main(String[] args) {
        // 测试用例
        JapaneseText[] testCases = {
                new JapaneseText(null, "さざなみ"),     // 无汉字
                new JapaneseText("見直す", "みなおす"),     // 无浊音
                new JapaneseText("漣", "さざなみ"),     // ざ：浊音
                new JapaneseText("正しい", "ただしい"), // だ：浊音
        };

        for (JapaneseText testCase : testCases) {
            System.out.println("\n测试输入: " + testCase);
            List<String> confusingAnswers = generateConfusingKana(testCase, 5, new ArrayList<>());

            if (confusingAnswers == null) {
                System.out.println("无法生成混淆答案");
            } else {
                System.out.println("生成混淆答案 (" + confusingAnswers.size() + "个):");
                for (String answer : confusingAnswers) {
                    int diff = countCharDifference(answer, testCase.getKana());
                    System.out.println(answer + ", diff = " + diff);
                }
            }
        }
    }

    public static int countCharDifference(String s1, String s2) {
        int length = Math.min(s1.length(), s2.length());
        int diff = 0;
        for (int i = 0; i < length; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                diff++;
            }
        }
        diff += Math.abs(s1.length() - s2.length());
        return diff;
    }
}