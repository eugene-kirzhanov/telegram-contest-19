package by.anegin.telegram_contests.data.model

sealed class Column(val id: String, val values: LongArray) {

    class X(id: String, values: LongArray) : Column(id, values)

    class Line(id: String, values: LongArray, val name: String, val color: Int) : Column(id, values)

}