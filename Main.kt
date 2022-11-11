package tasklist

import kotlinx.datetime.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.lang.reflect.ParameterizedType

private val red = "\u001B[101m \u001B[0m"
private val yellow = "\u001B[103m \u001B[0m"
private val green = "\u001B[102m \u001B[0m"
private val blue = "\u001B[104m \u001B[0m"

private val header = """
            +----+------------+-------+---+---+--------------------------------------------+
            | N  |    Date    | Time  | P | D |                   Task                     |
            +----+------------+-------+---+---+--------------------------------------------+
        """.trimIndent()

private val divider = "+----+------------+-------+---+---+--------------------------------------------+"

val file = File("taskList.json")
val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
val type: ParameterizedType = Types.newParameterizedType(MutableList::class.java, Task::class.java)
val taskListAdapter = moshi.adapter<MutableList<Task>>(type)

val finalTaskList = readJson(file)

fun readJson(file: File): MutableList<Task> {
    return if (file.exists()) {
        val taskJsonString = file.readText()
        taskListAdapter.fromJson(taskJsonString) ?: mutableListOf()
    } else {
        mutableListOf()
    }
}

fun saveJson(finalTaskList: MutableList<Task>) {
    file.writeText(taskListAdapter.toJson(finalTaskList))
}

class Task(
    _priority: String? = null, _date: String? = null, _dateTime: String? = null,
    _text: MutableList<String> = mutableListOf()
) {
    var priority: String? = _priority
    var date: String? = _date
    var dateTime: String? = _dateTime
    var text: MutableList<String> = _text
    var time: String? = null
    var terms: String? = null

    operator fun component1(): MutableList<String?> = mutableListOf(date, time, priority, terms)
    operator fun component2(): MutableList<String> = text
}


fun isPriorityValid(priorityInput: String?): Boolean {
    return priorityInput in listOf(red, yellow, green, blue)
}

fun setPriority(task: Task): Task {
    println("Input the task priority (C, H, N, L):")
    val priorityInput = readln().uppercase()
    val priorityInColor = when(priorityInput) {
        "C" -> red
        "H" -> yellow
        "N" -> green
        "L" -> blue
        else -> null
    }
    task.priority = priorityInColor
    return task
}

fun getLocalDate(dateInput: String): String {
    val dateInputSplit = dateInput.trim().split("-")
    return LocalDate(dateInputSplit[0].toInt(), dateInputSplit[1].toInt(), dateInputSplit[2].toInt()).toString()
}

fun setDate(task: Task): Task {
    println("Input the date (yyyy-mm-dd):")
    val dateInput: String = readln()
    try {
        val localDate = getLocalDate(dateInput)
        task.date = localDate
    } catch (e: Exception) {
        task.date = null
    }
    return task
}

fun getLocalDateTime(timeInput: String, date: LocalDate?): LocalDateTime? {
    val timeInputSplit = timeInput.trim().split(":")
    val localDateTime = if (date != null) {
        LocalDateTime(
            date.year, date.month, date.dayOfMonth,
            timeInputSplit[0].toInt(), timeInputSplit[1].toInt()
        )
    } else null
    return localDateTime
}

fun getTime(dateTime: String?): String {
    return dateTime.toString().split("T").last()
}

fun getDaysUntil(datetime: String?): Long? {
    val today = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0"))
    return (datetime?.toLocalDateTime()?.toInstant(TimeZone.UTC)?.minus(today.toInstant(TimeZone.UTC)))?.inWholeDays
}

fun getTerms(dateTime: String?): String? {
    val numberOfDaysRemaining: Long? = getDaysUntil(dateTime)
    return when {
        numberOfDaysRemaining == null -> null
        numberOfDaysRemaining == 0L -> yellow
        numberOfDaysRemaining > 0 -> green
        else -> red
    }
}

fun setDateTime(task: Task): Task {
    println("Input the time (hh:mm):")
    val timeInput = readln()
    try {
        val currentLocalDateTime = getLocalDateTime(timeInput, task.date?.toLocalDate())
        task.dateTime = currentLocalDateTime.toString()
        task.time = getTime(task.dateTime)
        task.terms = getTerms(task.dateTime)
    } catch (e: Exception) {
        task.dateTime = null
    }
    return task
}

fun setTaskText(task: Task): Task {
    println("Input a new task (enter a blank line to end):")
    val taskBody = mutableListOf<String>()
    while (true) {
        val input = readln().trim()
        if (input.isBlank()) { //isBlank() == trim().isEmpty()
            if (taskBody.isEmpty()) {
                println("The task is blank")
            } else {
                task.text = taskBody
                return task
            }
            break
        } else {
            taskBody.add(input)
        }
    }
    return task
}

fun isFieldToEditValid(field: String): Boolean {
    return field in listOf("priority", "date", "time", "task")
}

fun addTasks(finalTaskList: MutableList<Task>) {
    val currentTask = Task()
    MainLoop@ while (true) {
        if (isPriorityValid(setPriority(currentTask).priority)) {
            while (true) {
                if (setDate(currentTask).date != null) {
                    while (true) {
                        if (setDateTime(currentTask).time != null) {
                            setTaskText(currentTask)
                            break@MainLoop
                        } else {
                            println("The input time is invalid")
                        }
                    }
                } else {
                    println("The input date is invalid")
                }
            }

        }
    }
    if (currentTask.text.isNotEmpty()) {
        finalTaskList.add(currentTask)
    }
}

private fun printSingleTask(num: String, stamp: MutableList<String?>, items: MutableList<String>) {
    val (date, time, priority, terms) = stamp
    val bodyWidth = 44
    val tasks = mutableListOf<MutableList<String>>()
    for (item in items) {
        val stringList = mutableListOf<String>()
        for (i in item.indices step bodyWidth) {
            if (i + bodyWidth <= item.length) {
                stringList.add(item.substring(i, i + bodyWidth))
            } else {
                stringList.add(
                    "${
                        item.substring(
                            i,
                            item.length
                        )
                    }${" ".repeat(bodyWidth - (item.length % bodyWidth))}"
                )
            }
        }
        tasks.add(stringList)
    }

    for (i in tasks.indices) {
        for (j in 0 until tasks[i].size) {
            if (i == 0 && j == 0) {
                println("| $num| $date | $time | $priority | $terms |${tasks[0][0]}|")
                continue
            }
            val bodyTemplate = "|    |            |       |   |   |${tasks[i][j]}|"
            println(bodyTemplate)
        }
    }
}

fun printTasks(finalTaskList: MutableList<Task>) {
    if (finalTaskList.isNotEmpty()) {
        val list = finalTaskList
        println(header)

        for (i in list.indices) {
            val (stamp, items) = list[i]
            var num = ""

            if (i in 0 until 9) num = "${i + 1}  "
            if (i in 10 until 99) num = "${i + 1} "
            if (i > 99) num = "${i + 1}"

            printSingleTask(num, stamp, items)
            println(divider)
        }
    } else {
        println("No tasks have been input")
    }
}

fun doTasks(command: String, finalTaskList: MutableList<Task>) {
    if (finalTaskList.isNotEmpty()) {
        printTasks(finalTaskList)
        MainLoop@ while (true) {
            println("Input the task number (1-${finalTaskList.size}):")
            try {
                val taskToDo = readln().toInt()
                if (taskToDo in 1..finalTaskList.size) {
                    if (command == "delete") {
                        finalTaskList.removeAt(taskToDo - 1)
                        println("The task is deleted")
                        break@MainLoop
                    } else if (command == "edit") {
                        val task = finalTaskList[taskToDo - 1]
                        while (true) {
                            println("Input a field to edit (priority, date, time, task):")
                            val fieldToEdit = readln().trim()
                            if (isFieldToEditValid(fieldToEdit)) {
                                while (true) {
                                    when (fieldToEdit) {
                                        "priority" -> setPriority(task)
                                        "date" -> setDate(task)
                                        "time" -> setDateTime(task)
                                        "task" -> setTaskText(task)
                                    }
                                    println("The task is changed")
                                    break@MainLoop
                                }
                            } else {
                                println("Invalid field")
                            }
                        }
                    }
                } else {
                    println("Invalid task number")
                }
            } catch (e: Exception) {
                println("Invalid task number")
            }
        }
    } else {
        println("No tasks have been input")
    }
}

fun main() {
    while (true) {
        println("Input an action (add, print, edit, delete, end):")
        when (val input = readln().trim()) {
            "add" -> addTasks(finalTaskList)
            "print" -> printTasks(finalTaskList)
            "delete" -> doTasks(input, finalTaskList)
            "edit" -> doTasks(input, finalTaskList)
            "end" -> {
                saveJson(finalTaskList)
                println("Tasklist exiting!")
                break
            }
            else -> println("The input action is invalid")
        }
    }
}
