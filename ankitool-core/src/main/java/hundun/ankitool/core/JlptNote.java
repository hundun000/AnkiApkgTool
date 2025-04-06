package hundun.ankitool.core;

import lombok.Data;

/**
 * 对应卡包来源：<a href="https://github.com/5mdld/anki-jlpt-decks">...</a>
 */
@Data
public class JlptNote {
    /*
    {
      "noteId": "0b463316-2ed7-11ef-a183-99ad05e145f0",
      "vocabKanji": "〜やすい",
      "vocabPitch": "",
      "vocabPoS": "接尾",
      "vocabFurigana": "〜やすい",
      "vocabPlus": "〜易い",
      "vocabDefCN": "简单的，容易……的",
      "vocabDefTC": "",
      "vocabAudio": "[sound:やすい_ヤス＼イ_2_NHK-2016.mp3]",
      "sentType1": "",
      "sentKanji1": "わかりやすい説明",
      "sentFurigana1": "わかり<b>やすい</b>説明[せつめい]",
      "sentDef1": "易懂的说明",
      "sentDefTC1": "",
      "sentAudio1": "[sound:hypertts-51f531cff8f727ddf6be79753868cc517c283f58cacf14a2a76d7164.mp3]",
      "sentType2": "対",
      "sentKanji2": "〜にくい",
      "sentFurigana2": "〜にくい",
      "sentDef2": "难……",
      "sentDefTC2": "",
      "sentAudio2": "[sound:hypertts-0da28562abe222c87326c159d5089255b6956e962ac6c2fde20c4add.mp3]",
      "sentType3": "",
      "sentKanji3": "",
      "sentFurigana3": "",
      "sentDef3": "",
      "sentDefTC3": "",
      "sentAudio3": "",
      "sentType4": "",
      "sentKanji4": "",
      "sentFurigana4": "",
      "sentDef4": "",
      "sentDefTC4": "",
      "sentAudio4": "",
      "level": "412",
      "alt1": "",
      "alt2": "",
      "alt3": "",
      "alt4": ""
    }
     */

    // 笔记ID
    private String noteId;

    // 词汇的最常见表示，有可能假名形式比汉字形式更常见则为假名（例如：〜やすい）
    private String vocabKanji;

    // 词汇的发音（音调）
    private String vocabPitch;

    // 词汇的词性（例如：接尾）
    private String vocabPoS;

    // 词汇的假名表示
    private String vocabFurigana;

    // 词汇的其他信息
    private String vocabPlus;

    // 词汇的中文定义
    private String vocabDefCN;

    // 词汇的繁体中文定义
    private String vocabDefTC;

    // 词汇的音频文件路径
    private String vocabAudio;

    // 句子1的类型（例如：描述词汇的类型）
    private String sentType1;

    // 句子1的汉字表示
    private String sentKanji1;

    // 句子1的假名表示
    private String sentFurigana1;

    // 句子1的中文定义
    private String sentDef1;

    // 句子1的繁体中文定义
    private String sentDefTC1;

    // 句子1的音频文件路径
    private String sentAudio1;

    // 句子2的类型
    private String sentType2;

    // 句子2的汉字表示
    private String sentKanji2;

    // 句子2的假名表示
    private String sentFurigana2;

    // 句子2的中文定义
    private String sentDef2;

    // 句子2的繁体中文定义
    private String sentDefTC2;

    // 句子2的音频文件路径
    private String sentAudio2;

    // 句子3的类型
    private String sentType3;

    // 句子3的汉字表示
    private String sentKanji3;

    // 句子3的假名表示
    private String sentFurigana3;

    // 句子3的中文定义
    private String sentDef3;

    // 句子3的繁体中文定义
    private String sentDefTC3;

    // 句子3的音频文件路径
    private String sentAudio3;

    // 句子4的类型
    private String sentType4;

    // 句子4的汉字表示
    private String sentKanji4;

    // 句子4的假名表示
    private String sentFurigana4;

    // 句子4的中文定义
    private String sentDef4;

    // 句子4的繁体中文定义
    private String sentDefTC4;

    // 句子4的音频文件路径
    private String sentAudio4;

    // 词汇的等级（例如：412）
    private String level;

    // 替代选项1
    private String alt1;

    // 替代选项2
    private String alt2;

    // 替代选项3
    private String alt3;

    // 替代选项4
    private String alt4;


}
