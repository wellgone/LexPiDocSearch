package top.lvpi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.entity.LpFile;
import top.lvpi.model.dto.file.LpFileDTO;

public interface LpFileService extends IService<LpFile> {
    
    /**
     * 保存文件信息
     *
     * @param fileDTO 文件信息DTO
     * @return 文件ID
     */
    Long saveFile(LpFileDTO fileDTO);

    /**
     * 更新文件信息
     *
     * @param fileDTO 文件信息DTO
     * @return 是否更新成功
     */
    boolean updateFile(LpFileDTO fileDTO);

    /**
     * 删除文件信息
     *
     * @param fileId 文件ID
     * @return 是否删除成功
     */
    boolean deleteFile(Long fileId);

    /**
     * 根据文件ID获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件信息DTO
     */
    LpFileDTO getFileById(Long fileId);

    /**
     * 根据MD5获取文件信息
     *
     * @param md5 文件MD5值
     * @return 文件信息DTO
     */
    LpFileDTO getFileByMd5(String md5);
} 