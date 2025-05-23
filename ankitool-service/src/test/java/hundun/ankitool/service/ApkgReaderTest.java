package hundun.ankitool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hundun.ankitool.service.ApkgReader.ReadResult;
import org.junit.Test;

import java.io.File;

public class ApkgReaderTest {

    static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }
    @Test
    public void testFun() throws Exception {
        String apkgPath = "../data/NEW-JLPT__NEW-N5.apkg";
        String outputCsv = "../data/NEW-JLPT__NEW-N5.csv";

        ReadResult readResult = ApkgReader.read(new File(apkgPath));
        ApkgReader.writeCsv(readResult, outputCsv);
        System.out.println("导出成功：" + outputCsv);
    }

}