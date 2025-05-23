package hundun.ankitool.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

@Slf4j
public class SecretConfig {
    static ObjectMapper objectMapper = new ObjectMapper();

    public static String googleKey;

    static {
        try {
            JsonNode secretFile = objectMapper.readTree(new File("data/Secret/secret.json"));
            googleKey = secretFile.get("googleKey").asText();
        } catch (IOException e) {
            log.error("bad secretFile read:", e);
        }
    }

}
