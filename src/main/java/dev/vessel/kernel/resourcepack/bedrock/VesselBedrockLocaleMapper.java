package dev.vessel.kernel.resourcepack.bedrock;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Normalizer mapping Java Edition locale identifiers to Bedrock Edition locale specifications.
 * <p>
 * Java Edition conventionally employs lowercase locale codes (e.g. {@code en_us.json}, {@code zh_cn.json}),
 * whereas Bedrock Edition strictly requires uppercase country suffixes (e.g. {@code texts/en_US.lang},
 * {@code texts/zh_CN.lang}).
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class VesselBedrockLocaleMapper {

    private static final Map<String, String> OVERRIDES = new ConcurrentHashMap<>();

    static {
        OVERRIDES.put("en_us", "en_US");
        OVERRIDES.put("en_gb", "en_GB");
        OVERRIDES.put("de_de", "de_DE");
        OVERRIDES.put("es_es", "es_ES");
        OVERRIDES.put("es_mx", "es_MX");
        OVERRIDES.put("fr_fr", "fr_FR");
        OVERRIDES.put("fr_ca", "fr_CA");
        OVERRIDES.put("it_it", "it_IT");
        OVERRIDES.put("ja_jp", "ja_JP");
        OVERRIDES.put("ko_kr", "ko_KR");
        OVERRIDES.put("pt_br", "pt_BR");
        OVERRIDES.put("pt_pt", "pt_PT");
        OVERRIDES.put("ru_ru", "ru_RU");
        OVERRIDES.put("zh_cn", "zh_CN");
        OVERRIDES.put("zh_tw", "zh_TW");
        OVERRIDES.put("nl_nl", "nl_NL");
        OVERRIDES.put("bg_bg", "bg_BG");
        OVERRIDES.put("cs_cz", "cs_CZ");
        OVERRIDES.put("da_dk", "da_DK");
        OVERRIDES.put("el_gr", "el_GR");
        OVERRIDES.put("fi_fi", "fi_FI");
        OVERRIDES.put("hu_hu", "hu_HU");
        OVERRIDES.put("id_id", "id_ID");
        OVERRIDES.put("nb_no", "nb_NO");
        OVERRIDES.put("no_no", "nb_NO");
        OVERRIDES.put("pl_pl", "pl_PL");
        OVERRIDES.put("sk_sk", "sk_SK");
        OVERRIDES.put("sv_se", "sv_SE");
        OVERRIDES.put("tr_tr", "tr_TR");
        OVERRIDES.put("uk_ua", "uk_UA");
        OVERRIDES.put("vi_vn", "vi_VN");
        OVERRIDES.put("fil_ph", "fil_PH");

        // Regional Spanish fallbacks to Latin American Bedrock Spanish
        OVERRIDES.put("es_ar", "es_MX");
        OVERRIDES.put("es_cl", "es_MX");
        OVERRIDES.put("es_co", "es_MX");
        OVERRIDES.put("es_ec", "es_MX");
        OVERRIDES.put("es_uy", "es_MX");
        OVERRIDES.put("es_ve", "es_MX");
        OVERRIDES.put("es_pe", "es_MX");

        // Traditional Chinese regionals
        OVERRIDES.put("zh_hk", "zh_TW");
        OVERRIDES.put("zh_mo", "zh_TW");

        // Single language code aliases
        OVERRIDES.put("en", "en_US");
        OVERRIDES.put("es", "es_ES");
        OVERRIDES.put("fr", "fr_FR");
        OVERRIDES.put("de", "de_DE");
        OVERRIDES.put("it", "it_IT");
        OVERRIDES.put("ja", "ja_JP");
        OVERRIDES.put("ko", "ko_KR");
        OVERRIDES.put("pt", "pt_BR");
        OVERRIDES.put("ru", "ru_RU");
        OVERRIDES.put("zh", "zh_CN");
        OVERRIDES.put("vi", "vi_VN");
        OVERRIDES.put("nl", "nl_NL");
        OVERRIDES.put("pl", "pl_PL");
        OVERRIDES.put("sv", "sv_SE");
        OVERRIDES.put("tr", "tr_TR");
        OVERRIDES.put("uk", "uk_UA");
    }

    /**
     * Utility class constructor.
     */
    public VesselBedrockLocaleMapper() {
    }

    /**
     * Maps a Java locale identifier (e.g. "en_us", "zh_cn", "en_US.json") to Bedrock locale code (e.g. "en_US").
     *
     * @param javaLocale input locale identifier or filename
     * @return normalized Bedrock locale string
     */
    public static String toBedrockLocale(String javaLocale) {
        Objects.requireNonNull(javaLocale, "javaLocale cannot be null");
        String clean = javaLocale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if (clean.endsWith(".json")) {
            clean = clean.substring(0, clean.length() - 5);
        } else if (clean.endsWith(".lang")) {
            clean = clean.substring(0, clean.length() - 5);
        }

        // Check if override exists
        String mapped = OVERRIDES.get(clean);
        if (mapped != null) {
            return mapped;
        }

        // Generic transformation: xx_yy -> xx_YY
        int underscoreIdx = clean.indexOf('_');
        if (underscoreIdx > 0 && underscoreIdx < clean.length() - 1) {
            String lang = clean.substring(0, underscoreIdx).toLowerCase(Locale.ROOT);
            String country = clean.substring(underscoreIdx + 1).toUpperCase(Locale.ROOT);
            return lang + "_" + country;
        }

        // Single language code: en -> en_US, etc.
        return clean;
    }

    /**
     * Resolves the target relative Bedrock language file path (e.g. "texts/en_US.lang").
     *
     * @param javaLocale input locale identifier or filename
     * @return relative Bedrock file path
     */
    public static String toBedrockFilePath(String javaLocale) {
        return "texts/" + toBedrockLocale(javaLocale) + ".lang";
    }
}
