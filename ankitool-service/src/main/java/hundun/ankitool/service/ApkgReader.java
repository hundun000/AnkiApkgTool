package hundun.ankitool.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApkgReader {

    // 解压 .apkg 文件到临时目录
    public static File unzipApkg(File zipFile) throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "apkg_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
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

        Map<Long, ModelResult> modelResultMap;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ModelResult {
        Long midId;
        List<String> fieldNames;
        List<NoteResult> notes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class NoteResult {
        String id;
        List<String> fieldValues;
        List<String> tags;
    }

    // 导出 notes 表中的卡片字段
    public static ReadResult read(File apkgFile) throws Exception {
        File tempDir = ApkgReader.unzipApkg(apkgFile);
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
            Map<Long, ModelResult> modelResultMap = new HashMap<>();

            Iterator<Entry<String, JsonNode>> iterator = modelsNode.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                long modelId = Long.parseLong(entry.getKey());
                JsonNode fldsNode = entry.getValue().get("flds");
                List<String> fieldNames = new ArrayList<>();
                for (JsonNode fieldNode : fldsNode) {
                    fieldNames.add(fieldNode.get("name").asText());
                }
                modelResultMap.put(modelId,
                        ModelResult.builder()
                                .midId(modelId)
                                .fieldNames(fieldNames)
                                .notes(new ArrayList<>())
                                .build()
                );
            }

            // 3. 查询 notes 表，导出字段内容
            String sql = "SELECT mid, flds, tags FROM notes";
            try (
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);
            ) {
                boolean headerWritten = false;

                while (rs.next()) {
                    long mid = rs.getLong("mid");
                    String flds = rs.getString("flds");
                    String tags = rs.getString("tags"); // Tags 是空格分隔字符串

                    var midResult = modelResultMap.get(mid);
                    if (midResult == null) {
                        System.err.println("警告：未找到模型 ID " + mid + "，跳过该记录");
                        continue;
                    }

                    modelResultMap.get(mid).getNotes().add(
                            NoteResult.builder()
                                    .fieldValues(Arrays.asList(flds.split("\u001F")))
                                    .tags(Optional.ofNullable(tags)
                                            .filter(it -> it.length() > 0)
                                            .map(it -> Arrays.asList(it.split(" ")))
                                            .orElseGet(() -> new ArrayList<>())
                                    )
                                    .build()
                    );
                }
            }

            return new ReadResult(modelResultMap);
        }

    }

    public static void writeCsv(ReadResult readResult, String outputPath) throws IOException {
        try (
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))
        ) {

            for (var entry : readResult.getModelResultMap().entrySet()) {
                Long mid = entry.getKey();
                ModelResult lines = entry.getValue();
                List<String> fieldNames = lines.getFieldNames();
                if (fieldNames == null) {
                    System.err.println("警告：未找到模型 ID " + mid + "，跳过该记录");
                    continue;
                }
                writer.write(String.join(",", fieldNames));
                writer.write("\n");
                for (var lineData : lines.getNotes()) {
                    var values = lineData.getFieldValues();
                    for (int i = 0; i < fieldNames.size(); i++) {
                        String value = i < values.size() ? escapeCsv(values.get(i)) : "";
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


    // CSV 字段转义
    public static String escapeCsv(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            field = field.replace("\"", "\"\"");
            return "\"" + field + "\"";
        }
        return field;
    }

}

