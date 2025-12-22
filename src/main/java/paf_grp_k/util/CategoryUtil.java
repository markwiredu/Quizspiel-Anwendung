package paf_grp_k.util;

public final class CategoryUtil {

    private CategoryUtil() {}

    public static String normalize(String category) {
        if (category == null || category.isBlank()) {
            return "ALL";
        }
        return category.trim().toUpperCase();
    }
}
