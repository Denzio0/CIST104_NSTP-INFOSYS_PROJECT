package util;

public class Util {
	public static int toInt(Object value) {
		if (value == null)
			return 0;
		try {
			return Integer.parseInt(value.toString().replaceAll("[^0-9]", ""));
		} catch (Exception e) {
			return 0;
		}
	}
}