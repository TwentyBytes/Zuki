package me.twentybytes.zuki.api.request;

public class Query {

    private final StringBuilder builder = new StringBuilder();

    public Query select() {
        builder.append(builder.isEmpty() ? "SELECT" : " SELECT");
        return this;
    }

    public Query update() {
        builder.append(builder.isEmpty() ? "UPDATE" : " UPDATE");
        return this;
    }

    public Query delete() {
        builder.append(builder.isEmpty() ? "DELETE" : " DELETE");
        return this;
    }

    public Query from(String from) {
        builder.append(" FROM ").append(from);
        return this;
    }

    public Query where(String where) {
        builder.append(" WHERE ").append(where);
        return this;
    }

    public Query orderBy(String orderBy) {
        builder.append(" ORDER BY ").append(orderBy);
        return this;
    }

    public Query desc() {
        builder.append(" DESC");
        return this;
    }

    public Query asc() {
        builder.append(" ASC");
        return this;
    }

    public Query limit(int limit) {
        builder.append(" LIMIT ").append(limit);
        return this;
    }

    public Query onDuplicateKey() {
        builder.append(" ON DUPLICATE KEY");
        return this;
    }

    public Query set() {
        builder.append(" SET");
        return this;
    }

    public Query and() {
        builder.append(" AND");
        return this;
    }

    public Query append(Object data) {
        builder.append(data);
        return this;
    }

    public String build() {
        return builder.toString();
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    public static Query newBuilder() {
        return new Query();
    }

}
