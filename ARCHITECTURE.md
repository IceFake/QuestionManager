# QuestionManager — 项目架构设计文档

> **版本**: 1.0  
> **日期**: 2026-03-10  
> **项目类型**: Android App (Kotlin)  
> **架构模式**: MVVM + Repository Pattern  
> **最低 SDK**: 24 (Android 7.0)  
> **目标 SDK**: 36

---

## 目录

1. [项目概述](#1-项目概述)
2. [核心功能清单](#2-核心功能清单)
3. [技术栈选型](#3-技术栈选型)
4. [项目目录结构](#4-项目目录结构)
5. [架构分层设计](#5-架构分层设计)
6. [数据库设计 (Room)](#6-数据库设计-room)
7. [网络层设计 (DeepSeek API)](#7-网络层设计-deepseek-api)
8. [UI 页面与导航设计](#8-ui-页面与导航设计)
9. [ViewModel 设计](#9-viewmodel-设计)
10. [Repository 设计](#10-repository-设计)
11. [关键业务流程](#11-关键业务流程)
12. [依赖配置参考](#12-依赖配置参考)
13. [后续开发路线](#13-后续开发路线)

---

## 1. 项目概述

**QuestionManager** 是一个 Android 提问管理器应用。用户可以通过以下两种方式获取问题：

- **URL 解析**：输入一个网址，应用抓取网页内容并解析出问题列表
- **手动输入**：用户直接输入问题

对于每个问题，应用调用 **DeepSeek API** 自动生成答案，并以**条目 (QuestionItem)** 的形式持久化存储在本地。用户可以：

- 对不满意的答案**重新生成**
- 自行**设置和编辑 AI 提示词**
- **全局搜索**问题条目
- 对任意条目进行**深挖 (Drill-down)**，生成引申问题并建立条目间的链接关系

---

## 2. 核心功能清单

| 编号 | 功能模块 | 描述 |
|------|---------|------|
| F1 | URL 内容解析 | 输入网址 → 抓取网页 → 解析出问题列表 |
| F2 | 手动输入问题 | 用户直接输入一个或多个问题 |
| F3 | AI 答案生成 | 调用 DeepSeek API 为每个问题生成答案 |
| F4 | 答案重新生成 | 对不满意的答案点击重新生成 |
| F5 | 条目持久化存储 | 使用 Room 数据库本地存储所有问题条目 |
| F6 | 提示词管理 | 用户可自定义/编辑 AI 生成答案时使用的 system prompt |
| F7 | 条目链接关系 | 问题之间支持引申关系（父→子链接） |
| F8 | 全局搜索 | 支持对所有条目的问题文本进行全文搜索 |
| F9 | 深挖功能 | 对某个问题调用 AI 生成引申问题列表，用户选择后生成新条目并链接 |

---

## 3. 技术栈选型

| 类别 | 技术 | 说明 |
|------|------|------|
| 语言 | Kotlin | 主力开发语言 |
| UI 框架 | Jetpack Compose | 现代声明式 UI |
| 架构 | MVVM | ViewModel + LiveData/StateFlow |
| 本地数据库 | Room | SQLite ORM，支持 Flow 响应式 |
| 网络请求 | Retrofit + OkHttp | REST API 调用 (DeepSeek) |
| JSON 解析 | Kotlinx Serialization / Gson | API 响应解析 |
| 网页解析 | Jsoup | HTML 内容抓取与解析 |
| 异步处理 | Kotlin Coroutines + Flow | 协程 & 响应式流 |
| 依赖注入 | Hilt (Dagger) | 依赖管理 |
| 导航 | Navigation Compose | 页面导航 |
| 图片/图标 | Material Icons Extended | UI 图标 |
| 数据存储 | DataStore | 偏好设置（如提示词、生成参数） |
| 安全存储 | EncryptedSharedPreferences (security-crypto) | API Key 等敏感信息加密存储 |

---

## 4. 项目目录结构

```
app/src/main/java/com/example/questionmanager/
├── QuestionManagerApp.kt              # Application 类 (Hilt 入口)
├── MainActivity.kt                    # 唯一 Activity (Single Activity)
│
├── data/                              # ──── 数据层 ────
│   ├── local/                         # 本地数据源
│   │   ├── db/
│   │   │   ├── AppDatabase.kt         # Room 数据库定义
│   │   │   ├── dao/
│   │   │   │   ├── QuestionDao.kt     # 问题条目 DAO
│   │   │   │   ├── QuestionLinkDao.kt # 条目链接 DAO
│   │   │   │   └── PromptDao.kt       # 提示词 DAO
│   │   │   └── entity/
│   │   │       ├── QuestionEntity.kt  # 问题条目实体
│   │   │       ├── QuestionLinkEntity.kt # 条目链接实体
│   │   │       └── PromptEntity.kt    # 提示词实体
│   │   └── datastore/
│   │       ├── SettingsDataStore.kt   # 用户设置 (普通偏好)
│   │       └── SecureDataStore.kt     # 加密存储 (API Key 等敏感信息)
│   │
│   ├── remote/                        # 远程数据源
│   │   ├── api/
│   │   │   ├── DeepSeekApiService.kt  # DeepSeek Retrofit 接口
│   │   │   └── WebParserService.kt    # 网页抓取服务
│   │   └── model/
│   │       ├── DeepSeekRequest.kt     # API 请求模型
│   │       └── DeepSeekResponse.kt    # API 响应模型
│   │
│   └── repository/                    # 仓库层
│       ├── QuestionRepository.kt      # 问题条目仓库
│       ├── AiRepository.kt            # AI 相关仓库
│       └── SettingsRepository.kt      # 设置仓库
│
├── domain/                            # ──── 领域层 (可选) ────
│   ├── model/
│   │   ├── Question.kt               # 问题领域模型
│   │   ├── QuestionLink.kt           # 链接领域模型
│   │   └── Prompt.kt                 # 提示词领域模型
│   └── usecase/
│       ├── GenerateAnswerUseCase.kt   # 生成答案用例
│       ├── ParseUrlUseCase.kt         # 解析网址用例
│       ├── SearchQuestionsUseCase.kt  # 搜索问题用例
│       └── DrillDownUseCase.kt        # 深挖问题用例
│
├── ui/                                # ──── 表示层 ────
│   ├── navigation/
│   │   └── NavGraph.kt               # 导航图定义
│   │
│   ├── theme/
│   │   ├── Color.kt                  # 颜色定义
│   │   ├── Theme.kt                  # 主题定义
│   │   └── Type.kt                   # 字体定义
│   │
│   ├── screen/
│   │   ├── home/                     # 首页 (问题列表)
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   │
│   │   ├── detail/                   # 条目详情页
│   │   │   ├── DetailScreen.kt
│   │   │   └── DetailViewModel.kt
│   │   │
│   │   ├── input/                    # 问题输入页 (URL / 手动)
│   │   │   ├── InputScreen.kt
│   │   │   └── InputViewModel.kt
│   │   │
│   │   ├── search/                   # 搜索页
│   │   │   ├── SearchScreen.kt
│   │   │   └── SearchViewModel.kt
│   │   │
│   │   ├── drilldown/                # 深挖选择页
│   │   │   ├── DrillDownScreen.kt
│   │   │   └── DrillDownViewModel.kt
│   │   │
│   │   └── settings/                 # 设置页 (API Key, 提示词)
│   │       ├── SettingsScreen.kt
│   │       └── SettingsViewModel.kt
│   │
│   └── component/                    # 通用 UI 组件
│       ├── QuestionCard.kt           # 问题条目卡片
│       ├── AnswerSection.kt          # 答案展示区域
│       ├── LoadingIndicator.kt       # 加载指示器
│       ├── SearchBar.kt              # 搜索栏
│       └── LinkedQuestionChip.kt     # 链接条目标签
│
├── di/                                # ──── 依赖注入 ────
│   ├── AppModule.kt                  # 全局模块 (Database, DataStore)
│   ├── NetworkModule.kt              # 网络模块 (Retrofit, OkHttp)
│   └── RepositoryModule.kt           # 仓库模块
│
└── util/                              # ──── 工具类 ────
    ├── Constants.kt                  # 常量定义
    ├── Resource.kt                   # 通用状态封装 (Success/Error/Loading)
    └── Extensions.kt                 # Kotlin 扩展函数
```

---

## 5. 架构分层设计

```
┌──────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌────────────┐  ┌────────────┐  ┌────────────────────────┐  │
│  │  Screens   │  │ Components │  │  Navigation (NavGraph) │  │
│  │ (Compose)  │  │ (Compose)  │  │                        │  │
│  └─────┬──────┘  └────────────┘  └────────────────────────┘  │
│        │ observes StateFlow                                  │
│  ┌─────▼──────┐                                              │
│  │ ViewModels │  ← Hilt 注入 Repository                      │
│  └─────┬──────┘                                              │
├────────┼─────────────────────────────────────────────────────┤
│        │ calls                                               │
│  ┌─────▼──────────────┐    Domain Layer (可选)               │
│  │     Use Cases       │   ← 封装复杂业务逻辑                  │
│  └─────┬──────────────┘                                      │
├────────┼─────────────────────────────────────────────────────┤
│        │ calls                                               │
│  ┌─────▼──────────────┐    Data Layer                        │
│  │   Repositories      │   ← 数据调度中心                      │
│  └──┬──────────┬──────┘                                      │
│     │          │                                             │
│  ┌──▼───┐  ┌──▼────────┐                                    │
│  │ Room │  │  Retrofit  │                                    │
│  │DAO   │  │ DeepSeek   │                                    │
│  │      │  │ API / Jsoup│                                    │
│  └──────┘  └───────────┘                                     │
└──────────────────────────────────────────────────────────────┘
```

**数据流方向**: UI → ViewModel → UseCase(可选) → Repository → DataSource (Local/Remote)

**状态管理**: 使用 `StateFlow` 从 ViewModel 向 UI 暴露状态，UI 通过 `collectAsState()` 订阅。

---

## 6. 数据库设计 (Room)

### 6.1 ER 图

```
┌─────────────────────────┐       ┌────────────────────────────┐
│    question_items        │       │     question_links         │
├─────────────────────────┤       ├────────────────────────────┤
│ id: Long (PK, 自增)      │──┐    │ id: Long (PK, 自增)         │
│ question: String         │  │    │ parentId: Long (FK → id)   │
│ answer: String?          │  ├───>│ childId: Long (FK → id)    │
│ status: String           │  │    │ relationType: String       │
│ promptId: Long? (FK)     │  │    │ createdAt: Long            │
│ sourceUrl: String?       │  │    └────────────────────────────┘
│ createdAt: Long          │  │
│ updatedAt: Long          │  │    ┌────────────────────────────┐
└─────────────────────────┘  │    │       prompts              │
                              │    ├────────────────────────────┤
                              └───>│ id: Long (PK, 自增)         │
                                   │ name: String               │
                                   │ systemPrompt: String       │
                                   │ isDefault: Boolean         │
                                   │ createdAt: Long            │
                                   │ updatedAt: Long            │
                                   └────────────────────────────┘
```

### 6.2 实体定义

#### QuestionEntity

```kotlin
@Entity(tableName = "question_items")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "question")
    val question: String,           // 问题文本
    
    @ColumnInfo(name = "answer")
    val answer: String? = null,     // AI 生成的答案 (Markdown 格式)
    
    @ColumnInfo(name = "status")
    val status: String = "PENDING", // PENDING | GENERATING | COMPLETED | ERROR
    
    @ColumnInfo(name = "prompt_id")
    val promptId: Long? = null,     // 关联的提示词 ID
    
    @ColumnInfo(name = "source_url")
    val sourceUrl: String? = null,  // 来源网址 (如有)
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

**status 枚举说明**:
| 状态 | 说明 |
|------|------|
| `PENDING` | 等待生成答案 |
| `GENERATING` | 正在调用 AI 生成中 |
| `COMPLETED` | 答案已生成 |
| `ERROR` | 生成失败 |

#### QuestionLinkEntity

```kotlin
@Entity(
    tableName = "question_links",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["child_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parent_id"),
        Index("child_id")
    ]
)
data class QuestionLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "parent_id")
    val parentId: Long,             // 父问题 ID (引申来源)
    
    @ColumnInfo(name = "child_id")
    val childId: Long,              // 子问题 ID (引申目标)
    
    @ColumnInfo(name = "relation_type")
    val relationType: String = "DRILL_DOWN", // DRILL_DOWN | RELATED
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

#### PromptEntity

```kotlin
@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,               // 提示词名称 (如"默认"、"学术风格")
    
    @ColumnInfo(name = "system_prompt")
    val systemPrompt: String,       // System Prompt 内容
    
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,  // 是否为默认提示词
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 6.3 DAO 接口

#### QuestionDao

```kotlin
@Dao
interface QuestionDao {
    @Query("SELECT * FROM question_items ORDER BY created_at DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>
    
    @Query("SELECT * FROM question_items WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?
    
    @Query("SELECT * FROM question_items WHERE question LIKE '%' || :query || '%'")
    fun searchQuestions(query: String): Flow<List<QuestionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>): List<Long>
    
    @Update
    suspend fun updateQuestion(question: QuestionEntity)
    
    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)
    
    @Query("UPDATE question_items SET answer = :answer, status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateAnswer(id: Long, answer: String?, status: String, updatedAt: Long = System.currentTimeMillis())
}
```

#### QuestionLinkDao

```kotlin
@Dao
interface QuestionLinkDao {
    @Query("SELECT * FROM question_links WHERE parent_id = :parentId")
    fun getChildLinks(parentId: Long): Flow<List<QuestionLinkEntity>>
    
    @Query("SELECT * FROM question_links WHERE child_id = :childId")
    fun getParentLinks(childId: Long): Flow<List<QuestionLinkEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: QuestionLinkEntity): Long
    
    @Delete
    suspend fun deleteLink(link: QuestionLinkEntity)
    
    // 获取某条目的所有子条目详情
    @Query("""
        SELECT qi.* FROM question_items qi 
        INNER JOIN question_links ql ON qi.id = ql.child_id 
        WHERE ql.parent_id = :parentId
    """)
    fun getLinkedChildQuestions(parentId: Long): Flow<List<QuestionEntity>>
    
    // 获取某条目的所有父条目详情
    @Query("""
        SELECT qi.* FROM question_items qi 
        INNER JOIN question_links ql ON qi.id = ql.parent_id 
        WHERE ql.child_id = :childId
    """)
    fun getLinkedParentQuestions(childId: Long): Flow<List<QuestionEntity>>
}
```

#### PromptDao

```kotlin
@Dao
interface PromptDao {
    @Query("SELECT * FROM prompts ORDER BY created_at DESC")
    fun getAllPrompts(): Flow<List<PromptEntity>>
    
    @Query("SELECT * FROM prompts WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultPrompt(): PromptEntity?
    
    @Query("SELECT * FROM prompts WHERE id = :id")
    suspend fun getPromptById(id: Long): PromptEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: PromptEntity): Long
    
    @Update
    suspend fun updatePrompt(prompt: PromptEntity)
    
    @Delete
    suspend fun deletePrompt(prompt: PromptEntity)
    
    // 将所有提示词的 isDefault 设为 false
    @Query("UPDATE prompts SET is_default = 0")
    suspend fun clearDefaultPrompt()
}
```

### 6.4 Database

```kotlin
@Database(
    entities = [
        QuestionEntity::class,
        QuestionLinkEntity::class,
        PromptEntity::class
    ],
    version = 1,
    exportSchema = true  // schema JSON 文件将导出至 app/schemas/ 目录，须纳入版本控制
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun questionLinkDao(): QuestionLinkDao
    abstract fun promptDao(): PromptDao
}
```

### 6.5 数据库迁移策略

> **背景**: 当后续版本需要修改表结构（新增字段、修改类型等）时，若无迁移方案，应用在升级时会抛出 `IllegalStateException` 导致崩溃。必须在开发初期就建立迁移规范。

#### 6.5.1 Schema 版本控制

`exportSchema = true` 会将每个版本的数据库 schema 导出为 JSON 文件。需要在 `app/build.gradle.kts` 中配置导出目录：

```kotlin
android {
    // ...existing config...
    
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}
```

**并将 `app/schemas/` 目录纳入 Git 版本控制**（不要加入 `.gitignore`），便于后续使用 AutoMigration。

#### 6.5.2 迁移方案选择

| 场景 | 推荐方案 | Room 版本要求 |
|------|---------|-------------|
| 简单变更 (新增列、新增表) | `AutoMigration` | Room 2.4.0+ ✅ |
| 复杂变更 (重命名列、删表、数据转换) | 手写 `Migration` 对象 | 任何版本 |
| 开发阶段频繁变更 | `fallbackToDestructiveMigration()` | 任何版本 |

#### 6.5.3 AutoMigration 示例 (推荐)

当未来需要在 `question_items` 中新增 `tags` 字段时：

```kotlin
// 1. 修改 Entity，新增字段并提供默认值
@Entity(tableName = "question_items")
data class QuestionEntity(
    // ...existing fields...
    @ColumnInfo(name = "tags", defaultValue = "")
    val tags: String = ""
)

// 2. Database 版本号 +1，声明 AutoMigration
@Database(
    entities = [QuestionEntity::class, QuestionLinkEntity::class, PromptEntity::class],
    version = 2,                   // ← 递增
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)   // ← Room 自动生成迁移 SQL
    ]
)
abstract class AppDatabase : RoomDatabase() {
    // ...existing abstract functions...
}
```

#### 6.5.4 手写 Migration 示例 (复杂场景)

```kotlin
// 当 AutoMigration 无法处理的场景 (如重命名列)
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 示例: 将 source_url 重命名为 origin_url
        db.execSQL("ALTER TABLE question_items RENAME COLUMN source_url TO origin_url")
    }
}

// 在 DI 模块中注册
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
    return Room.databaseBuilder(context, AppDatabase::class.java, "question_manager_db")
        .addMigrations(MIGRATION_2_3)
        .build()
}
```

#### 6.5.5 开发阶段降级策略

在 Phase 1-2 开发阶段，schema 可能频繁变化，可临时使用 `fallbackToDestructiveMigration()` 避免手动写迁移，**但在正式发版前必须移除**：

```kotlin
// ⚠️ 仅限开发阶段使用，正式版本必须移除！
Room.databaseBuilder(context, AppDatabase::class.java, "question_manager_db")
    .fallbackToDestructiveMigration()   // 版本不匹配时清空数据重建
    .build()
```

---

## 7. 网络层设计 (DeepSeek API)

### 7.1 DeepSeek API 模型

#### 请求模型

```kotlin
@Serializable
data class DeepSeekRequest(
    val model: String = "deepseek-chat",     // 模型名
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2048,
    val stream: Boolean = false               // 是否流式返回
)

@Serializable
data class Message(
    val role: String,   // "system" | "user" | "assistant"
    val content: String
)
```

#### 响应模型

```kotlin
@Serializable
data class DeepSeekResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage?
)

@Serializable
data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String?
)

@Serializable
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
```

### 7.2 Retrofit 接口

```kotlin
interface DeepSeekApiService {
    
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authHeader: String,   // "Bearer sk-xxx"
        @Body request: DeepSeekRequest
    ): DeepSeekResponse
}
```

**Base URL**: `https://api.deepseek.com/`

### 7.3 网页解析服务

> **⚠️ 适用范围**: Jsoup 仅能解析**服务端渲染的静态 HTML** 页面。对于现代 SPA 框架（React/Vue/Angular）渲染的动态页面，Jsoup 无法执行 JavaScript，获取到的内容可能为空白或仅包含 `<div id="app"></div>` 等占位符。

```kotlin
class WebParserService {
    /**
     * 抓取网页内容并提取文本
     * 使用 Jsoup 进行 HTML 解析
     * 
     * 【局限性】仅适用于静态 HTML 页面 (SSR/传统服务端渲染)
     * 对于 SPA 动态页面需要后续升级为 WebView 方案
     */
    suspend fun fetchAndParse(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(15_000)
                .get()
            
            val text = doc.body().text()
            
            // 降级检查: 若提取内容过短，可能是 SPA 页面
            if (text.length < 50) {
                return@withContext Result.failure(
                    IllegalStateException("网页内容过少，可能是动态渲染页面 (SPA)，暂不支持解析。请尝试手动输入问题。")
                )
            }
            
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**后续升级方案 (Phase 5+)**:

| 方案 | 说明 | 适用场景 |
|------|------|---------|
| **Android WebView + JavaScript Bridge** | 使用隐藏 WebView 加载页面，等待 JS 渲染完成后提取 DOM | 大部分 SPA 页面 |
| **Headless Chrome (远端)** | 通过自建后端服务运行 Puppeteer/Playwright 渲染页面 | 需要服务端支持 |
| **Readability 算法增强** | 集成 Mozilla Readability 的 Kotlin 移植版，提升正文提取准确率 | 静态页面内容提取优化 |

### 7.4 AI 生成答案的 Prompt 结构

生成答案时的消息构造：

```
messages = [
    { role: "system", content: <用户自定义的 systemPrompt> },
    { role: "user",   content: "请回答以下问题：\n\n{question}" }
]
```

深挖（生成引申问题）时的消息构造：

```
messages = [
    { role: "system", content: "你是一个问题分析助手。请基于给定的问题，生成 5 个有深度的引申问题。
      严格要求：
      1. 仅返回一个合法的 JSON 数组，不要包含任何其他文字、解释或 Markdown 格式
      2. 数组中每个元素是一个纯字符串
      3. 示例格式：[\"问题1\", \"问题2\", \"问题3\", \"问题4\", \"问题5\"]" },
    { role: "user",   content: "原始问题：{question}\n\n已有答案：{answer}\n\n请生成引申问题。" }
]
```

#### 深挖 JSON 响应容错解析策略

> **问题**: LLM 经常在 JSON 外包裹 Markdown 代码块（`` ```json ... ``` ``）或附加额外说明文字，导致直接 `Json.decodeFromString` 失败。

**解析优先级** (逐级降级):

```kotlin
/**
 * 从 AI 响应文本中安全提取问题列表
 * 支持多种格式容错
 */
object AiResponseParser {
    
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    fun parseDrillDownQuestions(rawResponse: String): List<String> {
        // 策略 1: 直接尝试解析为 JSON 数组
        tryParseJsonArray(rawResponse)?.let { return it }
        
        // 策略 2: 提取 Markdown 代码块中的 JSON
        extractJsonFromCodeBlock(rawResponse)?.let { jsonStr ->
            tryParseJsonArray(jsonStr)?.let { return it }
        }
        
        // 策略 3: 正则提取第一个 [...] JSON 数组片段
        extractFirstJsonArray(rawResponse)?.let { jsonStr ->
            tryParseJsonArray(jsonStr)?.let { return it }
        }
        
        // 策略 4: 降级为按行分割 (兜底)
        return parseByLines(rawResponse)
    }
    
    private fun tryParseJsonArray(text: String): List<String>? = try {
        json.decodeFromString<List<String>>(text.trim())
    } catch (e: Exception) { null }
    
    private fun extractJsonFromCodeBlock(text: String): String? {
        val regex = Regex("""```(?:json)?\s*\n?([\s\S]*?)\n?\s*```""")
        return regex.find(text)?.groupValues?.get(1)?.trim()
    }
    
    private fun extractFirstJsonArray(text: String): String? {
        val regex = Regex("""\[[\s\S]*?]""")
        return regex.find(text)?.value
    }
    
    private fun parseByLines(text: String): List<String> {
        return text.lines()
            .map { it.trim() }
            .map { it.removePrefix("-").removePrefix("•").trim() }   // 去除列表符号
            .map { it.replace(Regex("""^\d+[.、)]\s*"""), "") }      // 去除序号
            .filter { it.isNotBlank() && it.length > 5 }            // 过滤空行和过短行
    }
}
```

---

## 8. UI 页面与导航设计

### 8.1 页面一览

```
┌─────────────────────────────────────────────────┐
│                  NavGraph                       │
│                                                 │
│  ┌──────────┐    ┌──────────┐   ┌───────────┐  │
│  │  Home    │───>│  Detail  │──>│ DrillDown │  │
│  │  Screen  │    │  Screen  │   │  Screen   │  │
│  └────┬─────┘    └──────────┘   └───────────┘  │
│       │                                         │
│  ┌────▼─────┐    ┌──────────┐                   │
│  │  Input   │    │ Settings │                   │
│  │  Screen  │    │  Screen  │                   │
│  └──────────┘    └──────────┘                   │
│                                                 │
│  ┌──────────┐                                   │
│  │  Search  │                                   │
│  │  Screen  │                                   │
│  └──────────┘                                   │
└─────────────────────────────────────────────────┘
```

### 8.2 各页面详细设计

#### HomeScreen — 首页 (问题列表)

```
┌─────────────────────────────────┐
│  QuestionManager          [⚙️]  │  ← TopAppBar + 设置按钮
├─────────────────────────────────┤
│  [🔍 搜索问题...]               │  ← 搜索栏 (点击跳转 SearchScreen)
├─────────────────────────────────┤
│  ┌─ QuestionCard ─────────────┐ │
│  │ Q: 什么是MVVM架构？         │ │
│  │ 状态: ✅ 已生成             │ │
│  │ 来源: https://...          │ │
│  │ 引申: 2 个子问题            │ │
│  └────────────────────────────┘ │
│  ┌─ QuestionCard ─────────────┐ │
│  │ Q: Kotlin协程的原理？       │ │
│  │ 状态: ⏳ 生成中             │ │
│  └────────────────────────────┘ │
│  ...                            │
├─────────────────────────────────┤
│           [＋ 新增问题]          │  ← FAB 跳转 InputScreen
└─────────────────────────────────┘
```

**功能**:
- 展示所有问题条目列表（按创建时间倒序）
- 点击条目 → 跳转 DetailScreen
- 点击搜索栏 → 跳转 SearchScreen
- FAB → 跳转 InputScreen
- TopAppBar 设置图标 → 跳转 SettingsScreen

---

#### InputScreen — 问题输入页

```
┌─────────────────────────────────┐
│  ← 新增问题                      │
├─────────────────────────────────┤
│                                 │
│  输入方式:                       │
│  [URL解析]  [手动输入]  ← Tab切换 │
│                                 │
│ ─── URL 解析模式 ───             │
│  [请输入网址URL...            ]  │
│  [解析]                          │
│                                 │
│  解析结果:                       │
│  ☑ 问题1: 什么是...             │
│  ☑ 问题2: 如何...               │
│  ☐ 问题3: 为什么...             │
│                                 │
│ ─── 手动输入模式 ───             │
│  [请输入问题...               ]  │
│  [+ 添加更多问题]                │
│                                 │
│  使用提示词: [默认提示词 ▼]       │
│                                 │
│  [确认并生成答案]                 │
└─────────────────────────────────┘
```

**功能**:
- 两种模式切换：URL 解析 / 手动输入
- URL 模式：输入网址 → 解析 → 展示问题列表（可勾选）
- 手动模式：逐条输入问题
- 选择使用的提示词
- 确认后批量创建条目并开始 AI 生成

---

#### DetailScreen — 条目详情页

```
┌─────────────────────────────────┐
│  ← 问题详情            [⋮]     │  ← 更多菜单 (删除/编辑)
├─────────────────────────────────┤
│                                 │
│  Q: 什么是MVVM架构？             │
│  来源: https://example.com      │
│  创建时间: 2026-03-10           │
│                                 │
│  ── 答案 ──────────────────     │
│  MVVM (Model-View-ViewModel)   │
│  是一种软件架构模式...            │
│  (Markdown 渲染)                │
│                                 │
│  [🔄 重新生成答案]               │
│                                 │
│  ── 关联问题 ──────────────     │
│  来源:                          │
│  ← "设计模式有哪些？"            │
│                                 │
│  引申:                          │
│  → "MVVM与MVP的区别？"          │
│  → "ViewModel的生命周期？"       │
│                                 │
│  [🔍 深挖此问题]                 │  ← 跳转 DrillDownScreen
└─────────────────────────────────┘
```

**功能**:
- 展示问题文本、答案（Markdown 渲染）、状态
- 重新生成答案按钮
- 展示父/子链接条目，点击可跳转
- 深挖按钮 → 跳转 DrillDownScreen
- 更多菜单：编辑问题、删除条目

---

#### DrillDownScreen — 深挖选择页

```
┌─────────────────────────────────┐
│  ← 深挖: 什么是MVVM架构？        │
├─────────────────────────────────┤
│                                 │
│  AI 为您生成了以下引申问题:       │
│                                 │
│  ☑ MVVM与MVC、MVP有何区别？      │
│  ☑ ViewModel如何管理生命周期？    │
│  ☐ DataBinding在MVVM中的作用？   │
│  ☐ MVVM架构的最佳实践有哪些？     │
│  ☐ 如何测试MVVM架构的应用？       │
│                                 │
│  [🔄 重新生成引申问题]           │
│                                 │
│  使用提示词: [默认提示词 ▼]       │
│                                 │
│  [确认并生成选中条目]             │
└─────────────────────────────────┘
```

**功能**:
- 调用 AI 生成当前问题的引申问题列表
- 用户勾选要生成的引申问题
- 确认后：创建新条目 + 建立链接关系 + 开始生成答案

---

#### SearchScreen — 搜索页

```
┌─────────────────────────────────┐
│  ← [🔍 MVVM...              ]  │  ← 搜索输入框 (自动聚焦)
├─────────────────────────────────┤
│                                 │
│  搜索结果 (3):                   │
│                                 │
│  ┌─ QuestionCard ─────────────┐ │
│  │ Q: 什么是MVVM架构？         │ │  ← 高亮匹配文字
│  │ 状态: ✅                    │ │
│  └────────────────────────────┘ │
│  ┌─ QuestionCard ─────────────┐ │
│  │ Q: MVVM与MVP的区别？        │ │
│  │ 状态: ✅                    │ │
│  └────────────────────────────┘ │
│  ...                            │
└─────────────────────────────────┘
```

**功能**:
- 实时搜索（debounce 300ms）
- 搜索问题文本
- 点击结果 → 跳转 DetailScreen

---

#### SettingsScreen — 设置页

```
┌─────────────────────────────────┐
│  ← 设置                         │
├─────────────────────────────────┤
│                                 │
│  ── API 配置 ──────────────     │
│  DeepSeek API Key:              │
│  [sk-xxxxx...                ]  │
│  API Base URL:                  │
│  [https://api.deepseek.com/  ]  │
│  模型:                          │
│  [deepseek-chat ▼]              │
│                                 │
│  ── 提示词管理 ──────────────    │
│  ┌─ PromptCard ──────────────┐  │
│  │ 📝 默认提示词 (默认)       │  │
│  │ [编辑] [删除]              │  │
│  └───────────────────────────┘  │
│  ┌─ PromptCard ──────────────┐  │
│  │ 📝 学术风格                │  │
│  │ [编辑] [设为默认] [删除]   │  │
│  └───────────────────────────┘  │
│  [+ 新建提示词]                  │
│                                 │
│  ── 其他设置 ──────────────     │
│  生成参数:                      │
│  Temperature: [0.7]             │
│  Max Tokens:  [2048]            │
└─────────────────────────────────┘
```

**功能**:
- API Key 输入与保存（加密存储于 DataStore）
- 提示词 CRUD 管理
- 设置默认提示词
- AI 生成参数配置

---

### 8.3 导航路由定义

```kotlin
sealed class Screen(val route: String) {
    object Home     : Screen("home")
    object Input    : Screen("input")
    object Detail   : Screen("detail/{questionId}") {
        fun createRoute(questionId: Long) = "detail/$questionId"
    }
    object DrillDown : Screen("drilldown/{questionId}") {
        fun createRoute(questionId: Long) = "drilldown/$questionId"
    }
    object Search   : Screen("search")
    object Settings : Screen("settings")
}
```

---

## 9. ViewModel 设计

### 9.1 HomeViewModel

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questionRepository: QuestionRepository
) : ViewModel() {
    
    // 状态
    data class HomeUiState(
        val questions: List<Question> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadQuestions()
    }
    
    private fun loadQuestions() { /* 从 Repository 获取 Flow<List<Question>> */ }
    fun deleteQuestion(id: Long) { /* 删除条目 */ }
}
```

### 9.2 InputViewModel

```kotlin
@HiltViewModel
class InputViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val aiRepository: AiRepository,
    private val parseUrlUseCase: ParseUrlUseCase
) : ViewModel() {
    
    data class InputUiState(
        val inputMode: InputMode = InputMode.MANUAL,    // URL / MANUAL
        val url: String = "",
        val manualQuestions: List<String> = listOf(""),
        val parsedQuestions: List<ParsedQuestion> = emptyList(), // 带勾选状态
        val isParsing: Boolean = false,
        val selectedPromptId: Long? = null,
        val availablePrompts: List<Prompt> = emptyList(),
        val error: String? = null
    )
    
    fun parseUrl(url: String) { /* 调用 ParseUrlUseCase */ }
    fun addManualQuestion(text: String) { /* 添加手动输入问题 */ }
    fun confirmAndGenerate() { /* 创建条目 + 启动 AI 生成 */ }
}

enum class InputMode { URL, MANUAL }
data class ParsedQuestion(val text: String, val isSelected: Boolean = true)
```

### 9.3 DetailViewModel

```kotlin
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val questionRepository: QuestionRepository,
    private val aiRepository: AiRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val questionId: Long = savedStateHandle["questionId"]!!
    
    data class DetailUiState(
        val question: Question? = null,
        val parentQuestions: List<Question> = emptyList(),
        val childQuestions: List<Question> = emptyList(),
        val isRegenerating: Boolean = false,
        val error: String? = null
    )
    
    fun regenerateAnswer() { /* 重新调用 AI 生成答案 */ }
    fun deleteQuestion() { /* 删除当前条目 */ }
}
```

### 9.4 DrillDownViewModel

```kotlin
@HiltViewModel
class DrillDownViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val questionRepository: QuestionRepository,
    private val drillDownUseCase: DrillDownUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val questionId: Long = savedStateHandle["questionId"]!!
    
    data class DrillDownUiState(
        val originalQuestion: Question? = null,
        val suggestedQuestions: List<ParsedQuestion> = emptyList(),
        val isGenerating: Boolean = false,
        val selectedPromptId: Long? = null,
        val error: String? = null
    )
    
    fun generateDrillDownQuestions() { /* 调用 AI 生成引申问题 */ }
    fun confirmSelected() { /* 创建新条目 + 建立链接 + 生成答案 */ }
}
```

### 9.5 SearchViewModel

```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchQuestionsUseCase: SearchQuestionsUseCase
) : ViewModel() {
    
    data class SearchUiState(
        val query: String = "",
        val results: List<Question> = emptyList(),
        val isSearching: Boolean = false
    )
    
    fun onQueryChanged(query: String) {
        // debounce 300ms → 调用搜索
    }
}
```

### 9.6 SettingsViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val questionRepository: QuestionRepository // for prompts
) : ViewModel() {
    
    data class SettingsUiState(
        val apiKey: String = "",
        val baseUrl: String = "https://api.deepseek.com/",
        val model: String = "deepseek-chat",
        val temperature: Double = 0.7,
        val maxTokens: Int = 2048,
        val prompts: List<Prompt> = emptyList(),
        val isSaving: Boolean = false
    )
    
    fun saveApiKey(key: String) { /* 保存到 DataStore */ }
    fun createPrompt(name: String, content: String) { /* 新建提示词 */ }
    fun updatePrompt(prompt: Prompt) { /* 编辑提示词 */ }
    fun deletePrompt(id: Long) { /* 删除提示词 */ }
    fun setDefaultPrompt(id: Long) { /* 设为默认 */ }
}
```

---

## 10. Repository 设计

### 10.1 QuestionRepository

```kotlin
class QuestionRepository @Inject constructor(
    private val questionDao: QuestionDao,
    private val questionLinkDao: QuestionLinkDao,
    private val promptDao: PromptDao
) {
    // ── 问题条目 CRUD ──
    fun getAllQuestions(): Flow<List<Question>>
    suspend fun getQuestionById(id: Long): Question?
    suspend fun insertQuestion(question: Question): Long
    suspend fun insertQuestions(questions: List<Question>): List<Long>
    suspend fun updateQuestion(question: Question)
    suspend fun deleteQuestion(id: Long)
    suspend fun updateAnswer(id: Long, answer: String?, status: QuestionStatus)
    
    // ── 链接关系 ──
    fun getChildQuestions(parentId: Long): Flow<List<Question>>
    fun getParentQuestions(childId: Long): Flow<List<Question>>
    suspend fun createLink(parentId: Long, childId: Long, type: RelationType = RelationType.DRILL_DOWN)
    suspend fun removeLink(parentId: Long, childId: Long)
    
    // ── 搜索 ──
    fun searchQuestions(query: String): Flow<List<Question>>
    
    // ── 提示词 ──
    fun getAllPrompts(): Flow<List<Prompt>>
    suspend fun getDefaultPrompt(): Prompt?
    suspend fun insertPrompt(prompt: Prompt): Long
    suspend fun updatePrompt(prompt: Prompt)
    suspend fun deletePrompt(id: Long)
    suspend fun setDefaultPrompt(id: Long)
}
```

### 10.2 AiRepository

```kotlin
class AiRepository @Inject constructor(
    private val deepSeekApiService: DeepSeekApiService,
    private val webParserService: WebParserService,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 并发限流信号量
     * 限制同时进行的 AI API 请求数量，避免触发 Rate Limit (429)
     * permits = 3 表示最多同时 3 个请求
     */
    private val apiSemaphore = Semaphore(permits = 3)
    
    /**
     * 为问题生成答案 (受限流保护)
     */
    suspend fun generateAnswer(question: String, systemPrompt: String): Result<String> {
        return apiSemaphore.withPermit {
            try {
                val apiKey = settingsRepository.apiKeyFlow.first()
                val request = DeepSeekRequest(
                    messages = listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = "请回答以下问题：\n\n$question")
                    )
                )
                val response = deepSeekApiService.chatCompletion(
                    authHeader = "Bearer $apiKey",
                    request = request
                )
                val answer = response.choices.firstOrNull()?.message?.content
                    ?: return@withPermit Result.failure(Exception("AI 返回内容为空"))
                Result.success(answer)
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    // Rate Limit: 等待后重试一次
                    delay(5000)
                    return@withPermit generateAnswer(question, systemPrompt) // 重试
                }
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 批量为问题生成答案 (内部使用 Semaphore 自动限流)
     * 外部无需手动控制并发
     */
    suspend fun generateAnswersBatch(
        questions: List<Pair<Long, String>>,   // (questionId, questionText)
        systemPrompt: String,
        onEachResult: suspend (id: Long, Result<String>) -> Unit
    ) = coroutineScope {
        questions.map { (id, question) ->
            async {
                val result = generateAnswer(question, systemPrompt)
                onEachResult(id, result)
            }
        }.awaitAll()
    }
    
    /**
     * 基于问题和答案生成引申问题列表 (受限流保护)
     * 响应解析使用 AiResponseParser 进行多策略容错
     */
    suspend fun generateDrillDownQuestions(question: String, answer: String): Result<List<String>> {
        return apiSemaphore.withPermit {
            // ... 调用 API 后使用 AiResponseParser.parseDrillDownQuestions() 解析
        }
    }
    
    /**
     * 从网页内容中解析出问题列表
     * 先用 Jsoup 抓取内容，再调用 AI 提取问题
     */
    suspend fun parseQuestionsFromUrl(url: String): Result<List<String>>
}
```

> **限流策略说明**:
> - 使用 `kotlinx.coroutines.sync.Semaphore(permits = 3)` 控制并发上限
> - 所有 AI 请求通过 `apiSemaphore.withPermit { }` 获取许可后才执行
> - 遇到 HTTP 429 (Rate Limit) 时自动等待 5 秒后重试
> - `generateAnswersBatch()` 封装了批量生成逻辑，供 ViewModel 层直接调用

### 10.3 SettingsRepository

```kotlin
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val secureDataStore: SecureDataStore
) {
    // API Key (通过 EncryptedSharedPreferences 加密存储)
    val apiKeyFlow: Flow<String>
    suspend fun saveApiKey(key: String)
    
    // Base URL
    val baseUrlFlow: Flow<String>
    suspend fun saveBaseUrl(url: String)
    
    // 模型名
    val modelFlow: Flow<String>
    suspend fun saveModel(model: String)
    
    // Temperature
    val temperatureFlow: Flow<Double>
    suspend fun saveTemperature(temp: Double)
    
    // Max Tokens
    val maxTokensFlow: Flow<Int>
    suspend fun saveMaxTokens(tokens: Int)
}
```

### 10.4 SecureDataStore — API Key 加密存储方案

> **问题**：DataStore 默认以明文存储数据，API Key 等敏感信息在设备被 root 或数据备份提取时可能泄露。
> 
> **方案**：使用 `androidx.security:security-crypto` 提供的 `EncryptedSharedPreferences`，基于 Android Keystore 的 `MasterKey` 进行 AES256 加密。

```kotlin
/**
 * 使用 EncryptedSharedPreferences 安全存储敏感信息 (API Key 等)
 * 加密基于 Android Keystore 的 MasterKey (AES256-GCM)
 */
class SecureDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /** 读取 API Key (解密) */
    fun getApiKey(): Flow<String> = callbackFlow {
        trySend(encryptedPrefs.getString("api_key", "") ?: "")
        awaitClose()
    }
    
    /** 保存 API Key (加密) */
    suspend fun saveApiKey(key: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString("api_key", key).apply()
    }
}
```

**DI 配置** (`AppModule.kt`):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSecureDataStore(@ApplicationContext context: Context): SecureDataStore {
        return SecureDataStore(context)
    }
}
```

> **注意**: `EncryptedSharedPreferences` 要求 minSdk ≥ 23 (Android 6.0)，本项目 minSdk = 24 满足要求。

---

## 11. 关键业务流程

### 11.1 URL 解析 → 生成答案 流程

```
用户输入 URL
    │
    ▼
InputViewModel.parseUrl()
    │
    ▼
AiRepository.parseQuestionsFromUrl()
    ├── WebParserService.fetchAndParse(url) → 网页文本
    └── DeepSeekApiService.chatCompletion()  → 提取问题列表
    │
    ▼
展示问题列表 (用户勾选)
    │
    ▼
InputViewModel.confirmAndGenerate()
    │
    ▼
QuestionRepository.insertQuestions() → 批量插入 (status=PENDING)
    │
    ▼
AiRepository.generateAnswersBatch() ← 内部 Semaphore(3) 限流
    │  (最多 3 个请求并发，其余排队等待)
    │
    ├── 对每个条目:
    │   ├── QuestionRepository.updateAnswer(id, status=GENERATING)
    │   ├── apiSemaphore.withPermit { DeepSeek API 调用 }
    │   │   └── HTTP 429 → 自动等待 5s 重试
    │   └── QuestionRepository.updateAnswer(id, answer, status=COMPLETED/ERROR)
    │
    ▼
UI 通过 Flow 自动更新列表状态
```

### 11.2 深挖问题 流程

```
用户在 DetailScreen 点击 [深挖此问题]
    │
    ▼
DrillDownViewModel.generateDrillDownQuestions()
    │
    ▼
AiRepository.generateDrillDownQuestions(question, answer)
    └── 调用 DeepSeek API → 返回引申问题 JSON 列表
    │
    ▼
展示引申问题列表 (用户勾选)
    │
    ▼
DrillDownViewModel.confirmSelected()
    │
    ▼
对每个选中的引申问题:
    ├── QuestionRepository.insertQuestion() → 新条目 (childId)
    ├── QuestionRepository.createLink(parentId, childId, DRILL_DOWN)
    └── 异步 AiRepository.generateAnswer() → 生成答案
    │
    ▼
导航回 DetailScreen → 链接列表自动更新
```

### 11.3 重新生成答案 流程

```
用户在 DetailScreen 点击 [重新生成答案]
    │
    ▼
DetailViewModel.regenerateAnswer()
    │
    ▼
QuestionRepository.updateAnswer(id, answer=null, status=GENERATING)
    │
    ▼
AiRepository.generateAnswer(question, systemPrompt)
    │
    ├── 成功 → QuestionRepository.updateAnswer(id, newAnswer, COMPLETED)
    └── 失败 → QuestionRepository.updateAnswer(id, null, ERROR)
    │
    ▼
UI 通过 Flow 自动更新
```

### 11.4 全局搜索 流程

```
用户在 SearchScreen 输入关键字
    │
    ▼ (debounce 300ms)
SearchViewModel.onQueryChanged(query)
    │
    ▼
SearchQuestionsUseCase(query)
    │
    ▼
QuestionRepository.searchQuestions(query)
    └── QuestionDao.searchQuestions("%query%") → Flow<List>
    │
    ▼
UI 实时更新搜索结果
```

---

## 12. 依赖配置参考

### 12.1 libs.versions.toml 新增版本

```toml
[versions]
# ...existing versions...
compose-bom = "2025.02.00"
compose-compiler = "1.5.14"
lifecycle = "2.9.0"
room = "2.7.1"
hilt = "2.54.1"
hiltNavigationCompose = "1.2.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
kotlinxSerialization = "1.7.3"
kotlinxSerializationConverter = "1.0.0"
jsoup = "1.18.3"
navigation = "2.9.0"
datastore = "1.1.4"
coroutines = "1.10.1"
ksp = "2.0.21-1.0.28"
richtext = "1.0.0-alpha01"
securityCrypto = "1.1.0-alpha06"

[libraries]
# Compose BOM
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-activity = { group = "androidx.activity", name = "activity-compose", version = "1.10.1" }

# Lifecycle
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Retrofit + OkHttp
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "kotlinxSerializationConverter" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Kotlinx Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

# Jsoup
jsoup = { group = "org.jsoup", name = "jsoup", version.ref = "jsoup" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Security (API Key 加密存储)
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
# ...existing plugins...
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### 12.2 根 build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
}
```

### 12.3 app/build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    // ...existing config...
    
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.activity)
    debugImplementation(libs.compose.ui.tooling)
    
    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Jsoup
    implementation(libs.jsoup)
    
    // Navigation
    implementation(libs.navigation.compose)
    
    // DataStore
    implementation(libs.datastore.preferences)
    
    // Security (API Key 加密存储)
    implementation(libs.security.crypto)
    
    // Coroutines
    implementation(libs.coroutines.android)
    
    // ...existing test dependencies...
}
```

---

## 13. 后续开发路线

### Phase 1: 基础框架搭建 🏗️
- [ ] 配置所有依赖 (build.gradle, libs.versions.toml)
- [ ] 创建目录结构
- [ ] 实现 Room 数据库 (Entity, DAO, Database)
- [ ] 配置 Room schema 导出目录 (`app/schemas/`) 并纳入版本控制
- [ ] 实现 Hilt DI 模块
- [ ] 创建 Application 类
- [ ] 创建 MainActivity + Compose 基础主题
- [ ] 实现导航图 (NavGraph)
- [ ] 实现 Resource 封装工具类
- [ ] 实现 AiResponseParser (AI 响应容错解析工具)

### Phase 2: 核心数据层 📦
- [ ] 实现 SecureDataStore (EncryptedSharedPreferences, API Key 加密存储)
- [ ] 实现 SettingsDataStore (普通偏好设置)
- [ ] 实现 DeepSeekApiService (Retrofit)
- [ ] 实现 WebParserService (Jsoup，含 SPA 降级检测)
- [ ] 实现 QuestionRepository
- [ ] 实现 AiRepository (含 Semaphore 并发限流 + 429 重试)
- [ ] 实现 SettingsRepository
- [ ] 编写默认提示词 seed 数据

### Phase 3: 核心 UI 页面 📱
- [ ] HomeScreen + HomeViewModel (问题列表)
- [ ] InputScreen + InputViewModel (URL解析 + 手动输入)
- [ ] DetailScreen + DetailViewModel (条目详情 + Markdown 渲染)
- [ ] SettingsScreen + SettingsViewModel (API配置 + 提示词管理)

### Phase 4: 高级功能 🚀
- [ ] SearchScreen + SearchViewModel (全局搜索 + debounce)
- [ ] DrillDownScreen + DrillDownViewModel (深挖功能 + JSON 容错解析)
- [ ] 条目链接关系的 UI 展示与导航
- [ ] 答案重新生成功能
- [ ] 批量操作 (批量删除、批量生成)

### Phase 5: 优化与完善 ✨
- [ ] 流式响应 (SSE) 支持 — 答案逐步显示
- [ ] 答案 Markdown 渲染优化
- [ ] 离线缓存策略
- [ ] 错误处理与重试机制
- [ ] 深色主题适配
- [ ] 导出/分享功能
- [ ] 性能优化 (列表分页 Paging3)
- [ ] SPA 动态页面解析支持 (WebView / Headless Chrome 方案)
- [ ] Room FTS 全文搜索升级
- [ ] 单元测试 & UI 测试

---

## 附录: 通用状态封装

```kotlin
/**
 * 通用 Resource 类，封装加载状态
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
```

---

> **注意**: 本文档为架构设计参考，具体实现时可根据实际需求进行调整。所有代码片段为伪代码 / 接口定义，非最终实现。

