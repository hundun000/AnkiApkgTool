package hundun.ankitool.core.util;

import java.util.ArrayList;
import java.util.List;

public class KanaOptionChecker {
    /**
     * 判断一个候选读法(candidate)是否是一个“合理的”错误选项：
     * 即按字面检查，candidate 是否按顺序包含 show 中所有的送假名片段。
     *
     * @param show      题干字符串（含汉字+任意位置的平假名），如 "お手紙を書く"
     * @param candidate 候选读法，纯平假名字符串，如 "おてがみをかく" 或 "おてがみかく"
     * @return 若 candidate 按顺序包含所有送假名片段，则返回 true；否则 false。
     */
    public static boolean isReasonable(String show, String candidate) {
        // 1. 提取所有送假名片段
        List<String> segments = extractOkuriganaSegments(show);
        // 2. 按顺序在 candidate 中查找这些片段
        int fromIndex = 0;
        for (String seg : segments) {
            int found = candidate.indexOf(seg, fromIndex);
            if (found < 0) {
                // 某段缺失或乱序 => 可直接排除
                return false;
            }
            // 下一次搜索从本段末尾继续
            fromIndex = found + seg.length();
        }
        return true;
    }

    /**
     * 从 show 中提取所有连续的尾部平假名（送假名）片段。
     *
     * @param show 题干字符串
     * @return 依出现顺序的平假名片段列表，若无送假名则返回空列表
     */
    private static List<String> extractOkuriganaSegments(String show) {
        List<String> segments = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (char c : show.toCharArray()) {
            if (JapaneseCharacterTool.isHiragana(c)) {
                buf.append(c);
            } else {
                if (buf.length() > 0) {
                    segments.add(buf.toString());
                    buf.setLength(0);
                }
            }
        }
        // 处理末尾残留
        if (buf.length() > 0) {
            segments.add(buf.toString());
        }
        return segments;
    }


    // --- 简单测试 ---
    public static void main(String[] args) {
        String show1 = "正しい";
        System.out.println(isReasonable(show1, "たたしい")); // true：包含 ["しい"]
        System.out.println(isReasonable(show1, "ただいい")); // false：缺少 "しい"

        String show2 = "お手紙を書く";
        // 正确读法
        System.out.println(isReasonable(show2, "おてがみをかく")); // true：包含 ["お"],["を"],["かく"]
        // 缺少「を」
        System.out.println(isReasonable(show2, "おてがみかく"));   // false：缺少 "を"
    }
}
