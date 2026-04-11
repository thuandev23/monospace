//package com.monospace.app.core.data
//
//import kotlinx.coroutines.flow.MutableStateFlow
//
//// core/data/FakeTaskRepository.kt — Dùng để test UI
//class FakeTaskRepository : TaskRepository {
//    private val tasks = MutableStateFlow(
//        listOf(
//            Task(id = "1", title = "Thiết kế UI cho FocusOS", listId = "inbox"),
//            Task(id = "2", title = "Kết nối API Notion", listId = "inbox"),
//            Task(id = "3", title = "Mua cà phê", listId = "inbox", isCompleted = true),
//        )
//    )
//
//    override fun observeTasks(listId: String): Flow<List<Task>> =
//        tasks.map { it.filter { t -> t.listId == listId } }
//
//    override suspend fun createTask(task: Task): Result<Unit> {
//        tasks.update { it + task }
//        return Result.success(Unit)
//    }
//
//    override suspend fun completeTask(id: String): Result<Unit> {
//        tasks.update { list -> list.map { if (it.id == id) it.copy(isCompleted = true) else it } }
//        return Result.success(Unit)
//    }
//}