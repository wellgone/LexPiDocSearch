-- 创建数据库
CREATE DATABASE IF NOT EXISTS LPMS DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE LPMS;

-- 用户表
CREATE TABLE IF NOT EXISTS lp_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    user_account VARCHAR(256) NOT NULL UNIQUE COMMENT '账号',
    user_password VARCHAR(512) NOT NULL COMMENT '密码',
    user_name VARCHAR(256) NOT NULL COMMENT '用户名',
    user_email VARCHAR(256) COMMENT '邮箱',
    user_login_num INT DEFAULT 0 COMMENT '登录次数',
    user_avatar_id VARCHAR(256) COMMENT '头像ID',
    user_role VARCHAR(256) DEFAULT 'user' COMMENT '用户角色：user/admin',
    user_state INT DEFAULT 0 COMMENT '用户状态：0-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_user_account(user_account)
) ENGINE=InnoDB AUTO_INCREMENT=1 COMMENT '用户表' COLLATE = utf8mb4_unicode_ci;

-- 图书表
CREATE TABLE IF NOT EXISTS lp_book (
  id bigint UNSIGNED NOT NULL COMMENT '书籍的唯一标识符',
  title varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '书名',
  sub_title varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '副标题',
  author varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '作者',
  publisher varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '出版社',
  publication_year varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '出版年份',
  publication_date varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '出版日期',
  isbn varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '国际标准书号',
  category varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '书籍分类',
  key_word varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '主题词/关键词',
  summary text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '书籍摘要',
  note text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `source` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '来源',
  file_name text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'minio文件储存名称，唯一',
  pic_url text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '封面链接',
  is_ocr int NULL DEFAULT NULL COMMENT '是否ocr',
  status int NULL DEFAULT NULL COMMENT '状态',
  page_size int NULL DEFAULT NULL COMMENT '页数',
  create_time datetime NULL DEFAULT NULL COMMENT '创建时间',
  modified_time datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  `type` int NULL DEFAULT NULL COMMENT '文件类型，1表示书籍，2表示用户自定义PDF文件',
  md5 varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'md5',
  cn varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '中图分类号',
  series varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '众编',
  score int NULL DEFAULT NULL COMMENT '评分；带不可消除的水印的0，低质量的1，高质量的5',
  is_extracted int NULL DEFAULT NULL COMMENT '是否已经抽取文本',
  has_parse_opac_topic int NULL DEFAULT NULL COMMENT '是否已解析opac主题词',
  is_indexed int NULL DEFAULT 0 COMMENT '是否进行elasticsearch索引',
  is_deleted int NULL DEFAULT 0 COMMENT '是否已删除',
  topic text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'opac主题',
  opac_series text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'opac丛编',
  is_opaced int NULL DEFAULT NULL COMMENT '是否已获取opac',
  isbn_format varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'isbn_format',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_title`(`title`) USING BTREE,
  INDEX `idx_isbn`(`isbn`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图书管理表' ROW_FORMAT = Dynamic;

-- 图书封面img表
CREATE TABLE IF NOT EXISTS lp_img (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `img_data` longtext NOT NULL COMMENT '图片base64数据',
    `book_id` bigint DEFAULT NULL COMMENT '图书ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modified_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图书封面图片表';

-- 图书章节表
CREATE TABLE IF NOT EXISTS `lp_book_section` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  `book_id` bigint NULL DEFAULT NULL COMMENT '书籍id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文档名称',
  `section_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '分段的文本内容',
  `page_num` int NULL DEFAULT NULL COMMENT '分段所在的页码',
  `coordinates` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分段坐标',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` int NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_book_id`(`book_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图书章节表' ROW_FORMAT = Dynamic;

-- 主题表
CREATE TABLE IF NOT EXISTS `lp_topic` (
  `id` bigint NOT NULL COMMENT '主键',
  `name` varchar(255) NOT NULL COMMENT '标签名称',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父级标签ID',
  `level` int NULL DEFAULT NULL COMMENT '层级',
  `type` varchar(50) DEFAULT NULL COMMENT '标签类型',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id`) USING BTREE,
  INDEX `idx_level`(`level`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '标签表' ROW_FORMAT = Dynamic;

-- 书籍与主题关联表
CREATE TABLE IF NOT EXISTS lp_book_topic (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '书籍与主题关联表ID',
    book_id BIGINT NOT NULL COMMENT '书籍ID',
    topic_id BIGINT NOT NULL COMMENT '主题ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modified_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    INDEX idx_book_id(book_id),
    INDEX idx_topic_id(topic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍与主题关联表';

-- 笔记表 (lp_note)
CREATE TABLE IF NOT EXISTS `lp_note` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '唯一标识，自增主键',
  `content` TEXT NOT NULL COMMENT '笔记内容',
  `source_name` VARCHAR(255) COMMENT '来源书籍名称',
  `source_press` VARCHAR(255) COMMENT '出版社',
  `source_author` VARCHAR(255) COMMENT '作者',
  `source_page_size` VARCHAR(255) COMMENT '页码',
  `source_publication_date` DATE COMMENT '出版日期',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认值为当前时间戳',
  `modified_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间，默认值为当前时间戳，并在更新时自动更新',
  `user_id` BIGINT COMMENT '创建笔记的用户ID（可选，如果需要关联用户）',
  `source_url` VARCHAR(1024) COMMENT '来源 URL（可选，如果笔记来自网络资源）',
  `tags` VARCHAR(255) COMMENT '标签（可选，用于分类和搜索）',
  `parent_id` BIGINT COMMENT '父级笔记ID',
  `order_num` INT COMMENT '排序字段',
  PRIMARY KEY (`id`),
  INDEX `idx_source_name` (`source_name`), -- 为来源书籍名称创建索引，加快按来源搜索的速度
  INDEX `idx_create_time` (`create_time`), -- 为创建时间创建索引，加快按时间排序的速度
  INDEX `idx_parent_id` (`parent_id`) -- 为父ID创建索引，加快树形结构查询速度
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';

-- 检索报告表 (lp_search_report)
CREATE TABLE IF NOT EXISTS `lp_search_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '唯一标识，自增主键',
  `title` VARCHAR(255) NOT NULL COMMENT '报告标题',
  `type` VARCHAR(50) COMMENT '报告类型',
  `search_subject` INT COMMENT '是否设为检索菜单子选项',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_id` BIGINT COMMENT '创建报告的用户ID（可选）',
  PRIMARY KEY (`id`),
  INDEX `idx_title` (`title`), -- 为标题创建索引
  INDEX `idx_create_time` (`create_time`) -- 为创建时间创建索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检索报告表';

-- 笔记与检索报告关系表 (lp_note_report_relate)
CREATE TABLE IF NOT EXISTS `lp_note_report_relate` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '唯一标识，自增主键',
  `note_id` BIGINT NOT NULL COMMENT '笔记ID，外键关联到 lp_note 表',
  `report_id` BIGINT NOT NULL COMMENT '检索报告ID，外键关联到 search_report 表',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `user_id` BIGINT COMMENT '创建报告的用户ID（可选）',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`note_id`) REFERENCES `lp_note` (`id`),
  FOREIGN KEY (`report_id`) REFERENCES `lp_search_report` (`id`),
  UNIQUE INDEX `idx_note_report` (`note_id`, `report_id`) -- 创建唯一索引，防止重复关联
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记与检索报告关系表';

CREATE TABLE IF NOT EXISTS `lp_file` (
  `file_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件存储名称',
  `file_original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件原名称',
  `file_suffix` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件扩展名',
  `file_size` bigint NOT NULL COMMENT '文件大小',
  `file_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件地址',
  `file_note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '文件备注',
  `file_md5` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '文件MD5值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0表示未删除，1表示已删除）',
  PRIMARY KEY (`file_id`) USING BTREE,
  INDEX `idx_file_name`(`file_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='文件信息表';

CREATE TABLE IF NOT EXISTS `lp_book_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `book_id` bigint DEFAULT NULL COMMENT '书籍ID',
  `file_id` bigint NOT NULL COMMENT '文件ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  INDEX `idx_book_id`(`book_id`) USING BTREE,
  INDEX `idx_file_id`(`file_id`) USING BTREE,
  UNIQUE INDEX `idx_book_file`(`book_id`, `file_id`, `is_deleted`) USING BTREE COMMENT '避免重复关联'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='书籍文件关联表';