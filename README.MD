# 律π -- 知识管理系统
一个基于Spring Boot + React的前后端分离 知识管理平台，具备（针对已OCR的）PDF文档/图书管理与全文检索功能，并接入AI大模型。

![项目架构图](./demoPic/architecture_diagram.svg)

![核心业务流程](./demoPic/pdf_search_highlight_flow.svg)

## 目录
- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [环境要求](#环境要求)
- [部署指南](#部署指南)
  - [Docker部署](#docker部署推荐)
  - [手动部署](#手动部署)
- [开发指南](#开发指南)
- [项目结构](#项目结构)
- [标签设计](#关于标签设计)
- [使用声明](#软件使用声明)
- [许可证](#开源许可与商用授权声明)

## 功能特性

- [x] 支持多用户
- [x] 自定义标签体系
- [x] PDF文档/图书管理
- [x] PDF文档全文检索
- [x] PDF文档关键词高亮显示
- [x] 书籍基本信息一键搜刮
- [x] 完备的后端API文档
- [x] 接入AI大模型对话-DeepSeek
- [x] 检索报告、笔记管理
- [ ] 检索报告导出、分享
- [ ] Prompt提示词管理，与笔记联动
- [ ] 结合AI大模型，实现检索报告的自动生成

## demo截图
1.  登录界面
![登录界面](./demoPic/login.png)

2. 欢迎界面
![欢迎界面](./demoPic/welcome.png)

3. 用户管理界面
![用户管理](./demoPic/user_manage.png)
> 注意：用户管理界面需要管理员权限

4. 文档管理
![文档管理](./demoPic/book_manage.png)
> 支持图书及其他PDF文档导入
> 支持图书标签分类

![文档管理-批量导入](./demoPic/import_document.gif)
> 支持批量导入

![文档管理-图书基本信息搜刮](./demoPic/get_book_info.gif)
> 支持图书基本信息一键搜刮

![文档管理-文档标签设置](./demoPic/set_tags.gif)
> 支持文档标签设置

5. 文档检索
![文档检索](./demoPic/book_search.png)
> 文档基本检索

![文档检索-高级检索](./demoPic/book_list_search.gif)

![文档预览](./demoPic/book_preview.png)
> 文档全文预览

6. 主题列表--带层级关系标签
![主题列表](./demoPic/topic_list.png)
> 支持层级主题/标签分类

7. 自定义标签--不带层级关系标签
![自定义标签](./demoPic/custom_topic.png)
> 支持自定义标签分类

8. 全文检索
![全文检索](./demoPic/full_text_search.png)
> 检索结果关键词高亮显示、按条件过滤、命中书籍页面关键词高亮显示

![全文检索](./demoPic/full_text_search.gif)

![全文检索-高级检索](./demoPic/full_text_search_advanced.png)
> 支持高级检索：支持多字段多条件检索，支持同段、同句检索

9. 添加笔记
![笔记](./demoPic/note.gif)
> 支持在PDF页面中选中文本添加为笔记，可添加到指定检索报告中（通过在检索报告页面设置关联菜单）

10. 检索报告&笔记管理
![检索报告](./demoPic/search_report.png)
> 支持添加/管理检索报告、搜索检索报告；  
> 支持笔记删除、编辑管理，支持笔记复制；
> 支持笔记拖动排序，自带层级序号

11. AI助手
![AI助手](./demoPic/ai_assistant.png)
> 接入AI大模型，暂支持DeepSeek大模型

## 技术栈

### 后端技术栈
- Spring Boot 3.2.1
- MySQL 8.0
- Elasticsearch 8.12.2
- Redis 7.2
- MinIO
- Sa-Token 认证
- MyBatis-Plus
- PDFBox
- Swagger
- Knife4j

### 前端技术栈
- React 18
- Ant Design
- UmiJS
- TypeScript

## 环境要求

### 硬件要求

#### 最小硬件配置建议
CPU：
- Elasticsearch: 至少 2 核心
- MySQL: 至少 1 核心
- 其他服务 (Redis, MinIO, Backend, Frontend): 共需 2 核心
- 总计最小需求：4-6 核心 CPU

内存 (RAM)：
- Elasticsearch: 至少 2GB（通过 ES_JAVA_OPTS 配置）
- MySQL: 至少 1GB
- Redis: 512MB
- MinIO: 512MB
- Backend (Java 应用): 1GB
- Frontend + Nginx: 512MB
- 总计最小需求：6GB RAM

存储空间：
- Elasticsearch 数据: 至少 10GB
- MySQL 数据: 至少 5GB
- Redis 数据: 1GB
- MinIO 对象存储: 10GB（取决于文档存储需求）
- 应用日志和其他: 2GB
- 总计最小需求：30GB 存储空间

网络：
- 建议至少 100Mbps 带宽
- 所有服务都在同一个 bridge 网络中

#### 推荐配置
为了获得更好的性能和稳定性，建议：
- CPU: 8 核心
- 内存: 16GB RAM
- 存储: 100GB SSD
- 网络: 1Gbps

#### 注意事项
- Elasticsearch 是最消耗资源的服务，特别是内存使用
- 如果有大量并发用户，应该相应增加内存和 CPU
- 如果需要存储大量文档，需要相应增加 MinIO 的存储空间
- 建议使用 SSD 存储以提高性能，特别是对 Elasticsearch 和 MySQL
- 需要根据实际使用场景（并发用户数、数据量、文档存储需求等）来调整这些配置。

### 软件要求
- Docker 20.10.x 及以上（推荐部署方式）
- Docker Compose 2.x 及以上
- Git

### 开发环境要求
- JDK 17+
- Node.js 18+
- Maven 3.8+
- MySQL 8.0
- Redis 7.2
- Elasticsearch 8.12.2
- MinIO

## 部署指南

前置条件：
- 安装 Git： [Download Git](https://git-scm.com/downloads)
- 安装 Docker： [Download Docker](https://www.docker.com/)
- 安装 Docker Compose： [Download Docker Compose](https://docs.docker.com/compose/install/)

### Docker部署（推荐）

#### 1. 克隆项目
```bash
git clone https://github.com/wellgone/LexiBookSearch.git
cd LexiBookSearch
```

#### 2. 环境配置
1. 复制环境变量模板：
```bash
cp .env.example .env
```

2. 修改`.env`文件，配置必要的环境变量：
```properties
# MySQL配置
MYSQL_ROOT_PASSWORD=your_password
MYSQL_DATABASE=LPMS
MYSQL_PORT=3306

# Redis配置
REDIS_PASSWORD=your_redis_password
REDIS_PORT=6379

# Elasticsearch配置
ELASTICSEARCH_PORT=9200
ELASTIC_USERNAME=elastic
ELASTIC_PASSWORD=your_es_password
ES_JAVA_OPTS="-Xms1g -Xmx1g"

# MinIO配置
MINIO_ROOT_USER=your_minio_user
MINIO_ROOT_PASSWORD=your_minio_password
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001

# 后端配置
BACKEND_PORT=9090
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xms1024m -Xmx1024m

# Nginx配置
NGINX_PORT=80
NGINX_SSL_PORT=443

# AI配置（可选）
DEEPSEEK_API_KEY=your_api_key
```

> 注意：请根据自身数据量调整ES、Java的内存配置。

#### 3. 启动服务
```bash
# 启动所有服务
docker-compose up -d
# 注意1：如果Docker Compose 是standalone版，应当使用`docker compose up -d`，下同；
# 注意2：如果需要只是重建部分服务，应当在重建完成后重启Nginx服务。

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f
```

#### 4. 访问服务
- 主控台: http://localhost:80
- 后端API: http://localhost:9090/doc.html
- MinIO控制台: http://localhost:9001
- Elasticsearch: http://localhost:9200

### 手动部署

#### 1. 后端部署

1. 配置Java环境
```bash
# 确保安装JDK 17+
java -version
```

2. 配置application.yml

3. 编译打包
```bash
cd bookMS
mvn clean package -DskipTests
```

4. 运行后端服务
```bash
java -jar target/bookms.jar
```

#### 2. 前端部署

1. 安装依赖
```bash
cd bookApp
npm install 
或 yarn install
```

2. 配置环境变量
```bash
cp .env.example .env.production
```

编辑`.env.production`：
```properties
VITE_API_URL=http://localhost:9090
VITE_ES_URL=http://localhost:9200
VITE_ES_USERNAME=elastic
VITE_ES_PASSWORD=your_es_password
```

3. 构建前端
```bash
npm run build
```

4. 配置Nginx
> 因本项目为前后端分离项目，注意配置相关代理，否则无法访问后端API；可参考默认配置；

## 开发指南

### 后端开发
1. 在IDE中导入bookMS项目
2. 配置application.yml中的数据库等连接信息
3. 运行LvPiApplication主类

### 前端开发
1. 进入bookApp目录
2. 安装依赖:
```bash
yarn install
```
3. 启动开发服务器:
```bash
yarn dev
```
4. 访问 http://localhost:8000

## 项目结构

```
.
├── bookMS/                  # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/bookms/
│   │   │   │   ├── controller/       # REST API 控制器
│   │   │   │   ├── service/          # 业务逻辑层
│   │   │   │   ├── repository/       # 数据访问层
│   │   │   │   ├── model/            # 数据实体类
│   │   │   │   ├── config/           # 配置类
│   │   │   │   ├── utils/            # 工具类
│   │   │   │   └── Application.java  # 启动类
│   │   │   └── resources/
│   │   │       ├── application.yml   # 主配置文件
│   │   │       ├── bootstrap.yml     # 环境配置
│   │   │       └── mapper/           # MyBatis XML映射文件
│   │   │
│   │   └── Dockerfile            # 后端Dockerfile
│   └── pom.xml               # Maven依赖配置
│
├── bookApp/                 # 前端项目
│   ├── src/
│   │   ├── pages/           # 页面组件
│   │   ├── components/      # 通用UI组件
│   │   ├── services/        # API请求服务
│   │   ├── models/          # 数据模型
│   │   ├── layouts/         # 全局布局
│   │   ├── config/          # UmiJS配置
│   │   ├── public/          # 静态资源
│   │   └── global.less      # 全局样式
│   ├── Dockerfile           # 前端Dockerfile
│   ├── package.json         # 依赖配置
│
├── docker/                  # Docker相关配置
│   ├── config/              # 配置文件
│   │   ├── backend/         # 后端配置文件
│   │   ├── frontend/        # 前端配置文件
│   │   └── elasticsearch/   # ES配置文件
│   ├── nginx/               # Nginx配置
│   │   └── nginx.conf
│   └── filedata/            # 文档数据
└── docker-compose.yml       

```

## 使用说明  

**注意！注意！注意！全文检索系基于OCR处理后的文本层，如要实现PDF全文检索，PDF文档导入系统前需经过OCR处理。**   
**OCR的精确度很大程度决定全文检索的准确性。**
> 章节管理以及索引管理页面可以看到实际的OCR结果。

**请务必按顺序操作：导入/添加图书信息 --> 获取OPAC数据（如不希望获取可以跳过） --> 添加标签分类（如不希望添加可以跳过） --> 抽取内容并建立索引 --> 全文检索；**


> 注意：
> - 添加标签需在建立索引之前，否则全文检索时无法按标签过滤数据；
> - 抽取内容并建立索引，需要等待一段时间，具体取决于图书数量和系统配置，请耐心等待。
> - 批量导入图书信息时，请按`book_import_template.xlsx`模板格式导入，否则会导致导入失败;如类型为图书，其中书名与isbn 为必填项，其他项为选填项；图书路径应当是（挂载）**filedata目录下的相对路径**，如：`book.pdf`、`/文件夹2/book2.pdf`、`/文件夹3/book3.pdf`；

### 具体操作
1. 单文导入

1.1  **导入：在“图书/文档管理”页面，点击“新增”按钮，填写基本信息后上传文档(注意上传文档前请先保存基本信息)；如希望在全文检索中可以通过标签及主题分类过滤，应当在此时设置主题标签及自定义标签；**

1.2 **获取OPAC（可选）：点击“获取OPAC”按钮，即可通过书籍类文档的ISBN号从“http://opac.nlc.cn/”网站书籍基本信息。**

1.3 **抽取：在“图书/文档管理”页面，点击“抽取”按钮，即可将PDF文档抽取为文本层。**

1.4 **索引：在“图书/文档管理”页面，点击“索引”按钮，即可将文本层索引为全文检索服务。**

2. 批量导入

2.1 **按**`book_import_template.xlsx`模板编辑待导入文档信息；

2.2 **在“图书/文档管理”页面，点击“批量导入”按钮，选择导入Excel文档，点击“确认”按钮，即可实现批量自动导入；**

2.3 **导入完成后，如需要获取OPAC信息，可点击“获取OPAC”按钮；**

2.4 **点击“提取**&索引”按钮，即可实现批量自动抽取及索引。

按上述步骤导入文档后即可在全文检索页面进行全文检索。



## 标签设计
> 通过给文档打标签可以给文档添加添加更多的分类，方便在检索后进行过滤。  
> 该标签体系设计，不管是带层级关系还是不带层级关系标签都可无限维度拓展，但考虑到通用性，本项目在全文检索页面中只设了“通用标签”以及“主题分类”两个维度；如有需要，请按业务需求自行拓展。  

`lp_topic`表是基于图书的分类，每个标签代表一个图书的分类，标签可以有多个，每个标签可以有多个文档。

1. 【带层级关系的主题分类】    
  这类标签具有父子关系，使用 parent_id 存储父标签。  
    • 约定：level 数值采用自然树中所处的深度（如0、1、2、3……）  
    • 要求：由于一般层级不会太深，约定由 0 到 99 表示树状结构的层级关系。  
2. 【不带层级的通用自定义标签】    
  这类标签没有父子关系，且应用单一业务场景。  
    • 约定：将 level 固定为 100  
    • parent_id 固定为 0，表示没有层级结构。  
3. 【不带层级的书籍类型】  
  比如民法、刑法等，这里认为这类标签无需层级结构。  
    • 约定：将 level 固定为 101  
    • parent_id 固定为 0。  
4. 【不带层级的实务专题】  
  例如劳动专题、执行专题等，同样不具有层级关系。  
    • 约定：将 level 固定为 102  
    • parent_id 固定为 NULL。

5. 【不带层级的其他分类】   
   根据业务需求，可以自定义标签，但是不具有层级关系。  
   • 约定：将 level 固定为 103...  
   • parent_id 固定为 NULL。  

## 软件使用声明

1. 本系统仅供个人学习、研究及合法知识管理用途
2. 禁止用于以下非法用途：
   - 传播盗版电子书、未经授权/许可的他人知识产权作品
   - 存储或传播违法、涉密信息
   - 任何违反当地法律法规的行为
3. 使用者需对自身行为负责，开发者不承担任何因非法使用导致的任何责任

## 开源许可与商用授权声明

本项目采用 **AGPLv3 协议** 进行开源，并附加以下商业条款：

- ✅ 允许个人用户自由使用、修改和分发
- ✅ 必须保留原始版权声明和许可证文件
- ⚠️ 商业使用（包括但不限于集成到SaaS平台、商业解决方案、盈利性服务等）必须获得书面授权
- ⚠️ 禁止去除/修改原始版权声明
- ⚠️ 禁止用于违法用途

完整授权条款请参见：
- [AGPLv3 许可证](LICENSE)  
- [附加商业条款](NOTICE)

如需商业授权，请联系：law_pi@163.com
