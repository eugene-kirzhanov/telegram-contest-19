package by.anegin.telegram_contests.data.model

sealed class Column(val id: String, val values: LongArray) {

    class X(id: String, values: LongArray) : Column(id, values)

    class Line(id: String, val name: String, val color: Int, values: LongArray) : Column(id, values)

}