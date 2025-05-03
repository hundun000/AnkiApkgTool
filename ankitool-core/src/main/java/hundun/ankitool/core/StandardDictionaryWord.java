package hundun.ankitool.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StandardDictionaryWord {
    /**
     * 来源标识，如AI生成或anki导出
     */
    private String source;
    /**
     * 来源内
     */
    private String sourceId;
    private String wordId;
    /**
     * 严格保证是汉字表示
     */
    private String vocabKanji;
    /**
     * 词汇的词性（例如：接尾）
     */
    private String vocabPoS;
    /**
     * 严格保证是假名表示
     */
    private String vocabFurigana;
    /**
     * 词汇的中文定义
     */
    private String vocabDefCN;
    private List<String> standardTags;
}
