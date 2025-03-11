package top.lvpi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.dto.doc.DocAddRequest;
import top.lvpi.model.dto.doc.DocQueryRequest;
import top.lvpi.model.dto.doc.DocUpdateRequest;
import top.lvpi.model.entity.Doc;
import top.lvpi.model.vo.DocVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocService extends IService<Doc> {
    /**
     * 创建图书
     */
    int addDoc(DocAddRequest docAddRequest);

    /**
     * 删除图书
     */
    boolean deleteDoc(Long id);

    /**
     * 更新图书
     */
    boolean updateDoc(DocUpdateRequest docUpdateRequest);

    /**
     * 根据 id 获取图书
     */
    Doc getDocById(Long id);

    /**
     * 获取图书列表（分页）
     */
    IPage<Doc> listDocs(DocQueryRequest docQueryRequest);

    /**
     * 获取图书总数
     */
    long countDocs(DocQueryRequest docQueryRequest);

    /**
     * 搜索图书
     */
    IPage<Doc> searchDocs(String keyword, int pageNum, int pageSize);

    /**
     * 高级搜索
     */
    IPage<Doc> advancedSearch(String keyword, String category, String author, 
                                 Integer yearFrom, Integer yearTo, int page, int size);

    /**
     * 模板搜索
     */
    IPage<Doc> templateSearch(String field, String value, int page, int size);

    /**
     * 嵌套搜索
     */
    IPage<Doc> nestedSearch(String keyword, Integer maxYear, int page, int size);

    /**
     * 检查图书是否已有章节
     */
    boolean hasDocSections(Long docId);

    /**
     * 从Excel文件导入图书信息
     * @param file Excel文件
     * @return 导入结果信息
     */
    String importDocsFromExcel(MultipartFile file);

    /**
     * 从Excel文件字节数组导入图书信息
     * @param fileContent Excel文件内容
     * @param fileName 文件名
     * @return 导入结果信息
     */
    String importDocsFromExcel(byte[] fileContent, String fileName);

    /**
     * 获取并保存图书OPAC信息
     * @param id 图书ID
     * @return 处理结果信息
     */
    String getAndSaveOpacInfo(Long id);

    /**
     * 批量获取并保存图书OPAC信息
     * @return 处理结果信息
     */
    String batchGetAndSaveOpacInfo();

    /**
     * 获取所有没有封面的图书列表
     * @return 没有封面的图书列表
     */
    List<Doc> getDocsWithoutCover();

    /**
     * 获取所有未获取OPAC信息的图书
     * @return 未获取OPAC信息的图书列表
     */
    List<Doc> getDocsWithoutOpac();

    /**
     * 获取所有未提取章节的图书列表
     * @return 未提取章节的图书列表
     */
    List<Doc> getDocsWithoutSections();

    /**
     * 获取图书列表（分页）- 包含标签信息
     */
    IPage<DocVO> listDocsWithTags(DocQueryRequest docQueryRequest);

    /**
     * 获取所有非空的图书类目
     */
    List<String> getAllNonEmptyCategories();

    
    /**
     * 将图书数据转换为DocVO分页列表
     * @param docPage 图书分页数据
     * @return DocVO分页列表
     */
    IPage<DocVO> convertToDocVOPage(IPage<Doc> docPage);
    
    /**
     * 获取所有需要解析OPAC主题标签的图书
     * @return 需要解析主题的图书列表
     */
    List<Doc> listDocsForTopicParsing();
    
    /**
     * 解析图书的OPAC主题标签
     * @param docId 图书ID
     * @return 是否解析成功
     */
    boolean parseDocOPACTopics(Long docId);
    
    /**
     * 标记图书的主题解析状态
     * @param docId 图书ID
     * @param success 是否解析成功
     * @return 是否操作成功
     */
    boolean markTopicParseStatus(Long docId, boolean success);
} 