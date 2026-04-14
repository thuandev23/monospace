Phân tích hiện trạng

Kiến trúc

Clean Architecture 3 lớp: Presentation (Compose + MVVM) → Domain (UseCases) → Data (Room + Retrofit)

Đã hoàn thiện (~75%)

┌──────────────────┬───────────────────────────────────────┬────────────┐                                                         
│ Nhóm │ Tính năng │ Trạng thái │
├──────────────────┼───────────────────────────────────────┼────────────┤                                                         
│ Task CRUD │ Tạo, sửa, xóa, hoàn thành │ ✅ │
├──────────────────┼───────────────────────────────────────┼────────────┤
│ Lịch & thời gian │ Start/end time, all-day, timezone │ ✅
│                                                         
├──────────────────┼───────────────────────────────────────┼────────────┤                                                         
│ Nhắc nhở │ Offset configurable, WorkManager │ ✅
│                                                         
├──────────────────┼───────────────────────────────────────┼────────────┤                                                         
│ Lặp lại │ Day/Week/Month/Year, day-of-week │ ✅ │
├──────────────────┼───────────────────────────────────────┼────────────┤                                                         
│ Task Lists │ Nhiều danh sách, filter │ ✅ │
├──────────────────┼───────────────────────────────────────┼────────────┤                                                         
│ Tìm kiếm │ Client-side, title + notes │ ✅ │
├──────────────────┼───────────────────────────────────────┼────────────┤                                                         
│ Multi-select │ Bulk delete │ ✅ │
├──────────────────┼───────────────────────────────────────┼────────────┤                                                         
│ Offline-first │ SyncQueue, retry, conflict resolution │ ✅ │
├──────────────────┼───────────────────────────────────────┼────────────┤                                                         
│ Connectivity │ Banner offline, auto-sync khi có mạng │ ✅ │
├──────────────────┼───────────────────────────────────────┼────────────┤                                                         
│ Settings │ Sidebar reorder/hide │ ✅ │
├──────────────────┼───────────────────────────────────────┼────────────┤                                                         
│ Notifications │ Boot receiver, reschedule │ ✅ │
└──────────────────┴───────────────────────────────────────┴────────────┘

Còn thiếu / chưa làm

- Backend bị tắt (BACKEND_ENABLED = false)
- Authentication - TokenManager có nhưng chưa có UI login
- Focus Mode - Entity có, chưa có UI/logic
- Third-party integrations - Notion, Apple Reminders, Google Tasks...
- App Launcher - ViewModel có, UI chưa đủ
- Onboarding - Stub trong nav, chưa có screen

  ---                                                                                                                               

Kế hoạch phát triển

Phase 1 — Hoàn thiện Local Core (2–3 tuần)

1.1 Bug & UX polish

- TaskDetailScreen — kiểm tra lại form edit đầy đủ (notes, priority, list selector, repeat,
  reminder)
- HomeScreen — Upcoming view (tasks theo ngày, nhóm by date)
- Empty state UI cho từng screen
- Priority sorting & filter
- Swipe-to-complete / swipe-to-delete trên task item

1.2 Focus Mode

- FocusProfileEntity đã có, cần:
    - FocusProfileDao (chỉ có entity, chưa có DAO)
    - Domain model FocusProfile + repository interface/impl
    - Use cases: create/activate/deactivate focus profile
    - FocusScreen UI — cấu hình allowed apps, schedule, linked lists
    - FocusTheme đã khai báo trong Theme.kt, cần apply khi focus active

1.3 App Launcher

- LauncherViewModel + AppRepository đã có
- Màn hình launcher đầy đủ (grid apps, search)
- Tích hợp với Focus Mode (restrict apps)

1.4 Onboarding

- Màn hình welcome/setup
- Permission request flow (notifications, exact alarm)
- Tạo default list + hướng dẫn tính năng đầu tiên

  ---                                                                                                                               

Phase 2 — Authentication & Backend (2–3 tuần)

2.1 Auth Flow   
TokenManager ✅ → AuthInterceptor ✅ → Cần:

- LoginScreen / RegisterScreen
- AuthViewModel
- AuthRepository (API calls)
- Persist login state
- Logout + clear token

2.2 Enable Backend

- Bật BACKEND_ENABLED = true
- Kết nối SyncWorker → TaskApiService thực
- Test full sync cycle: create → push → pull → merge
- Handle 401 → auto logout
- Pagination cho PullTasksUseCase (nếu dữ liệu lớn)

2.3 Multi-device sync

- Device ID tracking
- updatedAfter incremental sync đã có cơ sở → hoàn thiện
- Resolve edge case conflict (offline dài ngày)

  ---                                                                                                                               

Phase 3 — Third-party Integrations (3–4 tuần, phần 10%)

Đây là phần quan trọng nhất theo yêu cầu của bạn.

3.1 Apple Reminders (qua
CalDAV)                                                                                                  
// Integration approach: CalDAV
protocol                                                                                          
// Thư viện: ical4j hoặc tự parse
iCal                                                                                            
interface RemindersIntegration {      
suspend fun fetchReminders():
List<Task>                                                                                      
suspend fun pushTask(task: Task):
Boolean                                                                                     
suspend fun syncBidirectional():
SyncResult                                                                                   
}

- CalDAVClient — HTTP client cho CalDAV endpoint
- iCal parser (VTODO format)
- Mapping: VTODO ↔ Task domain model
- Màn hình config: nhập Apple ID credentials (App Password)
- Sync strategy: one-way import hoặc two-way

3.2 Google
Tasks                                                                                                                  
// Dùng Google Tasks API
v1                                                                                                       
// Dependency: Google API Client for
Android                                                                                      
interface
GoogleTasksIntegration {                                                                                                
suspend fun authenticate(): Boolean //
OAuth2                                                                                
suspend fun fetchTaskLists():
List<TaskList>                                                                                  
suspend fun fetchTasks(listId: String):
List<Task>                                                                            
suspend fun pushTask(task: Task):
Boolean                                                                                     
}

- OAuth2 flow (Google Sign-In)
- GoogleTasksApiClient (Retrofit service)
- Mapping: Google Task ↔ domain model
- Bidirectional sync với conflict resolution

3.3 Notion      
// Notion API v1 (database
integration)                                                                                           
interface NotionIntegration {          
suspend fun authenticate(token: String): Boolean // Integration
token                                                        
suspend fun selectDatabase(): List<NotionDatabase>                    
suspend fun fetchPages(databaseId: String):
List<Task>                                                                        
suspend fun createPage(task: Task): String // returns page
ID                                                                
suspend fun updatePage(notionId: String, task: Task):
Boolean                                                                 
}

- NotionApiClient — Retrofit service cho Notion API
- Property mapping: Title/Checkbox/Date ↔ Task fields
- Màn hình setup: nhập Integration Token, chọn Database
- Import one-time hoặc continuous sync

3.4 Integration Architecture
chung                                                                                                
core/                                                                                                                             
integrations/
domain/                                                                                                                       
IntegrationRepository.kt     (interface)
SyncResult.kt                           
IntegrationConfig.kt
data/                                                                                                                         
notion/
NotionApiService.kt                                                                                                       
NotionMapper.kt    
NotionRepository.kt
google_tasks/        
GoogleTasksService.kt
GoogleTasksMapper.kt
caldav/               
CalDAVClient.kt
ICalParser.kt                                                                                                             
di/              
IntegrationModule.kt                                                                                                        
feature/                  
integrations/
IntegrationsScreen.kt      (list tất cả integrations)
IntegrationDetailScreen.kt (config từng integration)
IntegrationViewModel.kt

3.5 Sync Settings UI

- IntegrationsScreen — list các platform có thể connect
- Trạng thái: Connected / Disconnected / Syncing / Error
- Last sync time
- Manual sync trigger
- Import-only vs bidirectional option

  ---             

Phase 4 — Advanced Features (4–6 tuần)

4.1 Subtasks

- Thêm parentId: String? vào TaskEntity + migration Room v3
- Nested UI trong TaskDetail
- Collapse/expand subtask groups

4.2 Tags / Labels

- TagEntity + nhiều-nhiều với Task
- Filter by tag trong HomeScreen

4.3 Attachments

- Link-only (URL) attachment trước
- File attachment sau (cần backend storage)

4.4 Widgets

- Android home screen widget (Glance API)
- Hiển thị today's tasks

4.5 Shortcuts & Quick Add

- System share sheet integration
- Quick add từ notification
- Siri Shortcuts / Assistant Actions

  ---

Phase 5 — Polish & Infrastructure (ongoing)

5.1 Tests

- Unit tests cho tất cả UseCases
- Repository tests với in-memory Room
- Integration tests cho sync logic
- UI tests cho critical flows

5.2 Observability

- Firebase Crashlytics (đã thêm dependency, chưa init)
- Firebase Analytics events (key user actions)
- Logging strategy

5.3 CI/CD

- GitHub Actions workflow
- Build, lint, test on PR
- Firebase App Distribution cho beta

5.4 Database migrations

- Hiện đang dùng fallbackToDestructiveMigration — cần thay bằng proper migration trước khi release
- Viết migration scripts cho mỗi schema change

  ---                                                                                                                               

Thứ tự ưu tiên đề xuất

Tuần 1–2:  Phase 1 (TaskDetail polish, Upcoming view, Focus Mode)
Tuần 3–4:  Phase 2 (Auth + Backend
enable)                                                                                        
Tuần 5–8:  Phase 3 (Google Tasks → Notion → Apple
Reminders)                                                                      
Tuần 9+:   Phase 4 & 5 (Advanced features + Tests)
                                                                                                                                    
---                                                                                                                               
Vấn đề kỹ thuật cần lưu ý ngay

1. fallbackToDestructiveMigration đang xóa data khi schema đổi — cần viết migration scripts thực sự
   trước khi có users
2. FocusProfileEntity có trong database nhưng không có DAO — sẽ bị ignore hoàn toàn
3. LauncherViewModel không được dùng trong NavGraph hiện tại — dead code
4. JSON fields trong Room (reminders, repeats) — nên cân nhắc dùng @TypeConverter thay vì string
   serialization thủ công để        
   type-safe hơn
5. QUERY_ALL_PACKAGES permission cần justify với Google Play nếu publish