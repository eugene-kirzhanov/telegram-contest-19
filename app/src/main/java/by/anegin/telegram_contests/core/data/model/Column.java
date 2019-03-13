package by.anegin.telegram_contests.core.data.model;

public class Column {

    public final String id;
    public final long[] values;

    private Column(String id, long[] values) {
        this.id = id;
        this.values = values;
    }

    public static final class X extends Column {

        public X(String id, long[] values) {
            super(id, values);
        }

    }

    public static final class Line extends Column {

        public final String name;
        public final int color;

        public Line(String id, String name, int color, long[] values) {
            super(id, values);
            this.name = name;
            this.color = color;
        }
    }

}
