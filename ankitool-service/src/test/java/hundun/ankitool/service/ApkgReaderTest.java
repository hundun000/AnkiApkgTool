package hundun.ankitool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.ankitool.core.ApkgReader.ReadResult;
import hundun.ankitool.core.ApkgReader;
import hundun.ankitool.core.JlptNote;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class ApkgReaderTest {

    static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    @Test
    public void testFun() throws Exception {
        String apkgPath = "../data/NEW-JLPT__NEW-N5.apkg";
        String outputNoteJson = "../data/NEW-JLPT__NEW-N5.json";


        ReadResult readResult = ApkgReader.read(apkgPath);
        List<JlptNote> notes = JltpNoteService.toJlptNodes(readResult);
        objectMapper.writeValue(new File(outputNoteJson), notes);
        System.out.println("导出成功：" + outputNoteJson);
    }

}