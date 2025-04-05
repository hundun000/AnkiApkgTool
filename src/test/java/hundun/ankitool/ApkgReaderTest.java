package hundun.ankitool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.ankitool.ankitool.ApkgReader;
import hundun.ankitool.ankitool.ApkgReader.Note;
import hundun.ankitool.ankitool.ApkgReader.ReadResult;
import org.junit.Test;


import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class ApkgReaderTest {

    static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }
    @Test
    public void testFun() throws Exception {
        String apkgPath = "data/NEW-JLPT__NEW-N5.apkg";
        String outputCsv = "data/NEW-JLPT__NEW-N5.csv";
        String outputNoteJson = "data/NEW-JLPT__NEW-N5.json";


        ReadResult readResult = ApkgReader.read(apkgPath);
        ApkgReader.writeCsv(readResult, outputCsv);
        System.out.println("导出成功：" + outputCsv);
        List<Note> notes = ApkgReader.toNodes(readResult);
        objectMapper.writeValue(new File(outputNoteJson), notes);
    }

}