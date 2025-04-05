package hundun.ankitool.ankitool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApkgReader {

    // 解压 .apkg 文件到临时目录
    public static File unzipApkg(String zipFilePath) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "apkg_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(tempDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }

        return tempDir;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReadResult {
        Map<Long, List<String>> midToFieldNamesMap;
        Map<Long, List<String>> midToLinesMap;
    }

    // 导出 notes 表中的卡片字段
    public static ReadResult read(String apkgPath) throws Exception {
        File tempDir = ApkgReader.unzipApkg(apkgPath);
        File dbFile = null;
        for (String name : new String[]{"collection.anki21b", "collection.anki21", "collection.anki2"}) {
            File f = new File(tempDir, name);
            if (f.exists()) {
                dbFile = f;
                break;
            }
        }
        if (dbFile == null || !dbFile.exists()) {
            throw new Exception("未找到 collection 数据库文件（anki2/anki21）！");
        }

        // 1. 获取模型 JSON
        String modelJson = null;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT models FROM col")) {
                if (rs.next()) {
                    modelJson = rs.getString("models");
                }
            }

            if (modelJson == null) {
                throw new Exception("未能读取模型 JSON！");
            }

            // 2. 使用 Jackson 解析模型 JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode modelsNode = mapper.readTree(modelJson);
            Map<Long, List<String>> modelFields = new HashMap<>();
            Map<Long, List<String>> midToLineDataMap = new HashMap<>();

            Iterator<Entry<String, JsonNode>> it = modelsNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                long modelId = Long.parseLong(entry.getKey());
                JsonNode fldsNode = entry.getValue().get("flds");
                List<String> fieldNames = new ArrayList<>();
                for (JsonNode fieldNode : fldsNode) {
                    fieldNames.add(fieldNode.get("name").asText());
                }
                modelFields.put(modelId, fieldNames);
            }

            // 3. 查询 notes 表，导出字段内容
            String sql = "SELECT mid, flds FROM notes";
            try (
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);
            ) {
                boolean headerWritten = false;

                while (rs.next()) {
                    long mid = rs.getLong("mid");
                    String flds = rs.getString("flds");

                    List<String> fieldNames = modelFields.get(mid);
                    if (fieldNames == null) {
                        System.err.println("警告：未找到模型 ID " + mid + "，跳过该记录");
                        continue;
                    }

                    if (!midToLineDataMap.containsKey(mid)) {
                        midToLineDataMap.put(mid, new ArrayList<>());
                    }

                    midToLineDataMap.get(mid).add(flds);
                }
            }

            return new ReadResult(modelFields, midToLineDataMap);
        }

    }

    public static void writeCsv(ReadResult readResult, String outputPath) throws IOException {
        try (
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))
        ) {

            for (Entry<Long, List<String>> entry : readResult.getMidToLinesMap().entrySet()) {
                Long mid = entry.getKey();
                List<String> lines = entry.getValue();
                List<String> fieldNames = readResult.getMidToFieldNamesMap().get(mid);
                if (fieldNames == null) {
                    System.err.println("警告：未找到模型 ID " + mid + "，跳过该记录");
                    continue;
                }
                writer.write(String.join(",", fieldNames));
                writer.write("\n");
                for (String lineData : lines) {
                    String[] values = lineData.split("\u001F");
                    for (int i = 0; i < fieldNames.size(); i++) {
                        String value = i < values.length ? escapeCsv(values[i]) : "";
                        writer.write(value);
                        if (i < fieldNames.size() - 1) {
                            writer.write(",");
                        }
                    }
                }
                writer.write("\n");
            }
        }
    }


    public static List<Note> toNodes(ReadResult readResult) {
        return readResult.getMidToLinesMap().values().stream()
                .findFirst()
                .stream()
                .flatMap(line -> line.stream())
                .map(it -> Note.fromFields(it.split("\u001F")))
                .collect(Collectors.toList());
    }

    // CSV 字段转义
    public static String escapeCsv(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            field = field.replace("\"", "\"\"");
            return "\"" + field + "\"";
        }
        return field;
    }

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

    /**
     * 对应卡包来源：<a href="https://github.com/5mdld/anki-jlpt-decks">...</a>
     */
    @Data
    public static class Note {
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

        private static String getField(String[] fields, int index) {
            return index < fields.length ? fields[index] : null;
        }

        // Static factory method to create Note object from CSV fields
        public static Note fromFields(String[] fields) {
            Note note = new Note();
            note.noteId = getField(fields, 0);
            note.vocabKanji = getField(fields, 1);
            note.vocabPitch = getField(fields, 2);
            note.vocabPoS = getField(fields, 3);
            note.vocabFurigana = getField(fields, 4);
            note.vocabPlus = getField(fields, 5);
            note.vocabDefCN = getField(fields, 6);
            note.vocabDefTC = getField(fields, 7);
            note.vocabAudio = getField(fields, 8);
            note.sentType1 = getField(fields, 9);
            note.sentKanji1 = getField(fields, 10);
            note.sentFurigana1 = getField(fields, 11);
            note.sentDef1 = getField(fields, 12);
            note.sentDefTC1 = getField(fields, 13);
            note.sentAudio1 = getField(fields, 14);
            note.sentType2 = getField(fields, 15);
            note.sentKanji2 = getField(fields, 16);
            note.sentFurigana2 = getField(fields, 17);
            note.sentDef2 = getField(fields, 18);
            note.sentDefTC2 = getField(fields, 19);
            note.sentAudio2 = getField(fields, 20);
            note.sentType3 = getField(fields, 21);
            note.sentKanji3 = getField(fields, 22);
            note.sentFurigana3 = getField(fields, 23);
            note.sentDef3 = getField(fields, 24);
            note.sentDefTC3 = getField(fields, 25);
            note.sentAudio3 = getField(fields, 26);
            note.sentType4 = getField(fields, 27);
            note.sentKanji4 = getField(fields, 28);
            note.sentFurigana4 = getField(fields, 29);
            note.sentDef4 = getField(fields, 30);
            note.sentDefTC4 = getField(fields, 31);
            note.sentAudio4 = getField(fields, 32);
            note.level = getField(fields, 33);
            note.alt1 = getField(fields, 34);
            note.alt2 = getField(fields, 35);
            note.alt3 = getField(fields, 36);
            note.alt4 = getField(fields, 37);
            return note;
        }
    }
}

