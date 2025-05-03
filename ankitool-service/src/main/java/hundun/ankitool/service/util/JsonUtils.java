package hundun.ankitool.service.util;

import com.fasterxml.jackson.databind.*;

public class JsonUtils {
    public static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
}
