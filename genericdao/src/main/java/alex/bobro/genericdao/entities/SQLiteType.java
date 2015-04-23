package alex.bobro.genericdao.entities;

public enum SQLiteType {
	INTEGER("INTEGER"),
	REAL("REAL"),
	TEXT("TEXT"),
    BLOB("BLOB");

	private String sqlType;

	SQLiteType(String value) {
		this.sqlType = value;
	}

	public String getSqlType() {
		return sqlType;
	}
}