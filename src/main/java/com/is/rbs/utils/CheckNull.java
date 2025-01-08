package com.is.rbs.utils;

import java.sql.Date;

public class CheckNull {
	public static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}
	public static boolean isEmpty(Boolean bool) {
		return bool == null;
	}

	public static boolean isEmpty(Double db) {
		if (db != null) {
			return db.equals(Double.NaN);
		} else {
			return true;
		}
	}

	public static boolean isEmpty(Date dt) {
		return dt == null;
	}

	public static boolean isEmpty(java.util.Date dt) {
		return dt == null;
	}

	public static boolean isEmpty(Long lg) {
		if (lg != null) {
			return lg == 0;
		} else {
			return true;
		}
	}

	public static boolean isEmpty(int it) {
		return it == 0;
	}
}
