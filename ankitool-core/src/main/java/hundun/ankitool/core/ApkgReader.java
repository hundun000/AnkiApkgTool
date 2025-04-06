package hundun.ankitool.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
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


    // CSV 字段转义
    public static String escapeCsv(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            field = field.replace("\"", "\"\"");
            return "\"" + field + "\"";
        }
        return field;
    }

}

