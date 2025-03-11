package top.lvpi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.mapper.NoteMapper;
import top.lvpi.mapper.NoteReportRelateMapper;
import top.lvpi.model.entity.Note;
import top.lvpi.model.entity.NoteReportRelate;
import top.lvpi.model.request.BatchNoteOrderRequest.NoteOrder;
import top.lvpi.model.request.BatchNoteHierarchyRequest.NoteHierarchy;
import top.lvpi.model.vo.NoteVO;
import top.lvpi.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    @Autowired
    private NoteReportRelateMapper noteReportRelateMapper;

    private static final Long DEFAULT_REPORT_ID = 1L;  // 设置默认报告ID

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createNote(Note note, Long reportId) {
        // 保存笔记
        boolean success = save(note);
        if (!success) {
            return false;
        }

        // 使用默认报告ID或提供的报告ID
        Long finalReportId = reportId != null ? reportId : DEFAULT_REPORT_ID;
        
        // 关联笔记到报告
        return relateToReport(note.getId(), finalReportId);
    }

    @Override
    public boolean updateNote(Note note) {    
        if (note == null || note.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Note existNote = getById(note.getId());
        if (existNote == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        note.setModifiedTime(LocalDateTime.now());
        return updateById(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNote(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 1. 检查笔记是否存在
        Note note = getById(id);
        if (note == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "笔记不存在");
        }

        // 2. 收集所有需要删除的笔记ID（包括子笔记）
        Set<Long> allNoteIds = new HashSet<>();
        collectAllChildNoteIds(id, allNoteIds);
        allNoteIds.add(id);

        // 3. 删除所有笔记的关联关系
        LambdaQueryWrapper<NoteReportRelate> relateWrapper = new LambdaQueryWrapper<>();
        relateWrapper.in(NoteReportRelate::getNoteId, allNoteIds);
        noteReportRelateMapper.delete(relateWrapper);

        // 4. 删除所有笔记
        return this.removeByIds(allNoteIds);
    }

    @Override
    public Note getNoteById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return getById(id);
    }

    @Override
    public IPage<Note> listNotes(int current, int size, String keyword) {
        Page<Note> page = new Page<>(current, size);
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.like(Note::getContent, keyword)
                    .or()
                    .like(Note::getSourceName, keyword)
                    .or()
                    .like(Note::getTags, keyword);
        }
        queryWrapper.orderByDesc(Note::getCreateTime);
        return page(page, queryWrapper);
    }

    @Override
    public IPage<Note> searchBySource(String sourceName, int current, int size) {
        if (StringUtils.isBlank(sourceName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<Note> page = new Page<>(current, size);
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Note::getSourceName, sourceName)
                .orderByDesc(Note::getCreateTime);
        return page(page, queryWrapper);
    }

    @Override
    public IPage<Note> searchByTags(String tags, int current, int size) {
        if (StringUtils.isBlank(tags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<Note> page = new Page<>(current, size);
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Note::getTags, tags)
                .orderByDesc(Note::getCreateTime);
        return page(page, queryWrapper);
    }

    @Override
    public IPage<Note> searchByTimeRange(String startTime, String endTime, int current, int size) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            if (StringUtils.isNotBlank(startTime)) {
                start = LocalDateTime.parse(startTime, formatter);
            }
            if (StringUtils.isNotBlank(endTime)) {
                end = LocalDateTime.parse(endTime, formatter);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间格式错误");
        }

        Page<Note> page = new Page<>(current, size);
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        if (start != null) {
            queryWrapper.ge(Note::getCreateTime, start);
        }
        if (end != null) {
            queryWrapper.le(Note::getCreateTime, end);
        }
        queryWrapper.orderByDesc(Note::getCreateTime);
        return page(page, queryWrapper);
    }

    @Override
    public List<Note> getUserNotes(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Note::getUserId, userId)
                .orderByDesc(Note::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean relateToReport(Long noteId, Long reportId) {
        if (noteId == null || reportId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 检查是否已经关联
        LambdaQueryWrapper<NoteReportRelate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NoteReportRelate::getNoteId, noteId)
                .eq(NoteReportRelate::getReportId, reportId);
        if (noteReportRelateMapper.selectCount(queryWrapper) > 0) {
            return true;
        }
        // 创建关联
        NoteReportRelate relate = new NoteReportRelate();
        relate.setNoteId(noteId);
        relate.setReportId(reportId);
        relate.setCreateTime(LocalDateTime.now());
        return noteReportRelateMapper.insert(relate) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeFromReport(Long noteId, Long reportId) {
        if (noteId == null || reportId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<NoteReportRelate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NoteReportRelate::getNoteId, noteId)
                .eq(NoteReportRelate::getReportId, reportId);
        return noteReportRelateMapper.delete(queryWrapper) > 0;
    }

    @Override
    public List<NoteVO> getReportNoteTree(Long reportId) {
        // 1. 获取报告关联的所有笔记
        List<Note> allNotes = this.baseMapper.selectList(
            new QueryWrapper<Note>()
                .inSql("id", 
                    "SELECT note_id FROM lp_note_report_relate WHERE report_id = " + reportId)
                .orderByAsc("parent_id")
                .orderByAsc("order_num")
                .orderByAsc("create_time")
        );

        // 2. 构建树形结构
        Map<Long, List<NoteVO>> parentChildMap = new HashMap<>();
        List<NoteVO> rootNotes = new ArrayList<>();

        // 转换所有笔记为VO
        List<NoteVO> allNoteVOs = allNotes.stream()
            .map(note -> BeanUtil.copyProperties(note, NoteVO.class))
            .collect(Collectors.toList());

        // 按父ID分组
        for (NoteVO noteVO : allNoteVOs) {
            if (noteVO.getParentId() == 0) {
                rootNotes.add(noteVO);
            } else {
                parentChildMap.computeIfAbsent(noteVO.getParentId(), k -> new ArrayList<>()).add(noteVO);
            }
        }

        // 递归设置子节点
        for (NoteVO noteVO : allNoteVOs) {
            List<NoteVO> children = parentChildMap.get(noteVO.getId());
            if (children != null) {
                noteVO.setChildren(children);
            }
        }

        return rootNotes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateNoteHierarchy(Long noteId, Long parentId, Integer orderNum) {
        // 1. 验证笔记是否存在
        Note currentNote = getById(noteId);
        if (currentNote == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "笔记不存在");
        }

        // 2. 验证父节点是否存在（如果指定了父节点）
        if (parentId != 0) {
            Note parentNote = getById(parentId);
            if (parentNote == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "父节点不存在");
            }
            // 验证父节点是否属于同一个用户
            if (!parentNote.getUserId().equals(currentNote.getUserId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能将笔记移动到其他用户的笔记下");
            }
        }

        // 3. 验证是否形成循环引用
        if (parentId != 0) {
            Long currentParentId = parentId;
            while (currentParentId != 0) {
                if (currentParentId.equals(noteId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能将节点移动到其子节点下");
                }
                Note parentNote = getById(currentParentId);
                if (parentNote == null) break;
                currentParentId = parentNote.getParentId();
            }
        }

        // 4. 更新节点
        Note note = new Note();
        note.setId(noteId);
        note.setParentId(parentId);
        note.setOrderNum(orderNum);
        note.setModifiedTime(LocalDateTime.now());

        return updateById(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateNoteOrder(List<NoteOrder> notes) {
        if (CollUtil.isEmpty(notes)) {
            return true;
        }

        // 批量更新
        return this.updateBatchById(
            notes.stream()
                .map(note -> {
                    Note lpNote = new Note();
                    lpNote.setId(note.getId());
                    lpNote.setOrderNum(note.getOrderNum());
                    lpNote.setModifiedTime(LocalDateTime.now());
                    return lpNote;
                })
                .collect(Collectors.toList())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateNoteHierarchy(List<NoteHierarchy> notes) {
        if (CollUtil.isEmpty(notes)) {
            return true;
        }

        // 1. 获取所有需要更新的笔记
        List<Long> noteIds = notes.stream()
                .map(NoteHierarchy::getId)
                .collect(Collectors.toList());
        Map<Long, Note> existingNotes = this.listByIds(noteIds)
                .stream()
                .collect(Collectors.toMap(Note::getId, note -> note));

        // 2. 验证所有笔记是否存在
        for (NoteHierarchy note : notes) {
            if (!existingNotes.containsKey(note.getId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "笔记不存在: " + note.getId());
            }
        }

        // 3. 验证父节点是否存在（如果指定了父节点）
        Set<Long> parentIds = notes.stream()
                .map(NoteHierarchy::getParentId)
                .filter(id -> id != 0)
                .collect(Collectors.toSet());
        if (!parentIds.isEmpty()) {
            Map<Long, Note> parentNotes = this.listByIds(parentIds)
                    .stream()
                    .collect(Collectors.toMap(Note::getId, note -> note));
            
            for (NoteHierarchy note : notes) {
                if (note.getParentId() != 0 && !parentNotes.containsKey(note.getParentId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "父节点不存在: " + note.getParentId());
                }
            }
        }

        // 4. 验证是否形成循环引用
        Map<Long, NoteHierarchy> noteMap = notes.stream()
                .collect(Collectors.toMap(NoteHierarchy::getId, note -> note));

        for (NoteHierarchy note : notes) {
            if (note.getParentId() != 0) {
                Set<Long> ancestors = new HashSet<>();
                Long currentId = note.getId();
                Long parentId = note.getParentId();

                while (parentId != 0) {
                    if (parentId.equals(currentId)) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                            "检测到循环引用: 笔记 " + note.getId() + " 不能成为其自身的子节点");
                    }
                    if (ancestors.contains(parentId)) {
                        break; // 已经检查过的祖先节点，无需重复检查
                    }
                    ancestors.add(parentId);
                    
                    // 检查新的层级关系中是否有这个父节点
                    NoteHierarchy parentNote = noteMap.get(parentId);
                    if (parentNote != null) {
                        parentId = parentNote.getParentId();
                    } else {
                        Note existingParent = this.getById(parentId);
                        if (existingParent == null) break;
                        parentId = existingParent.getParentId();
                    }
                }
            }
        }

        // 5. 批量更新
        LocalDateTime now = LocalDateTime.now();
        return this.updateBatchById(
            notes.stream()
                .map(note -> {
                    Note lpNote = new Note();
                    lpNote.setId(note.getId());
                    lpNote.setParentId(note.getParentId());
                    lpNote.setOrderNum(note.getOrderNum());
                    lpNote.setModifiedTime(now);
                    return lpNote;
                })
                .collect(Collectors.toList())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteNotes(List<Long> noteIds) {
        if (CollUtil.isEmpty(noteIds)) {
            return true;
        }

        // 1. 获取实际存在的笔记
        List<Note> existingNotes = this.listByIds(noteIds);
        if (existingNotes.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "所有笔记都不存在");
        }
        
        // 记录实际要删除的笔记ID
        List<Long> existingNoteIds = existingNotes.stream()
                .map(Note::getId)
                .collect(Collectors.toList());
        
        // 如果有部分笔记不存在，记录日志
        if (existingNoteIds.size() < noteIds.size()) {
            List<Long> nonExistingNoteIds = noteIds.stream()
                    .filter(id -> !existingNoteIds.contains(id))
                    .collect(Collectors.toList());
            log.warn("以下笔记不存在，将跳过删除: {}", nonExistingNoteIds);
        }

        // 2. 收集所有需要删除的笔记ID（包括子笔记）
        Set<Long> allNoteIds = new HashSet<>();
        for (Long noteId : existingNoteIds) {
            collectAllChildNoteIds(noteId, allNoteIds);
        }
        allNoteIds.addAll(existingNoteIds);

        // 3. 删除所有笔记的关联关系
        if (!allNoteIds.isEmpty()) {
            LambdaQueryWrapper<NoteReportRelate> relateWrapper = new LambdaQueryWrapper<>();
            relateWrapper.in(NoteReportRelate::getNoteId, allNoteIds);
            noteReportRelateMapper.delete(relateWrapper);
        }

        // 4. 删除所有笔记
        return this.removeByIds(allNoteIds);
    }

    /**
     * 递归收集所有子笔记ID
     */
    private void collectAllChildNoteIds(Long noteId, Set<Long> allNoteIds) {
        LambdaQueryWrapper<Note> childrenQuery = new LambdaQueryWrapper<>();
        childrenQuery.eq(Note::getParentId, noteId);
        List<Note> children = this.list(childrenQuery);
        
        for (Note child : children) {
            allNoteIds.add(child.getId());
            // 递归收集子笔记的子笔记
            collectAllChildNoteIds(child.getId(), allNoteIds);
        }
    }
} 