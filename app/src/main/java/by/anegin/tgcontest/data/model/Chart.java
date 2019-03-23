package by.anegin.tgcontest.data.model;

import java.util.List;

public class Chart {

    public final Column.X x;
    public final List<Column.Line> lines;

    public Chart(Column.X x, List<Column.Line> lines) {
        this.x = x;
        this.lines = lines;
    }

}