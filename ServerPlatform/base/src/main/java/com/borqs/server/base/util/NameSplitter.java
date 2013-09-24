package com.borqs.server.base.util;

import org.apache.commons.lang.StringUtils;

import java.lang.Character.UnicodeBlock;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class NameSplitter {

    public static final int MAX_TOKENS = 10;

    private static final String JAPANESE_LANGUAGE = Locale.JAPANESE.getLanguage().toLowerCase();
    private static final String KOREAN_LANGUAGE = Locale.KOREAN.getLanguage().toLowerCase();

    // This includes simplified and traditional Chinese
    private static final String CHINESE_LANGUAGE = Locale.CHINESE.getLanguage().toLowerCase();

    private final HashSet<String> mPrefixesSet;
    private final HashSet<String> mSuffixesSet;
    private final int mMaxSuffixLength;
    private final HashSet<String> mLastNamePrefixesSet;
    private final HashSet<String> mConjuctions;
    private final Locale mLocale;
    private final String mLanguage;

    /**
     * Two-Chracter long Korean family names.
     * http://ko.wikipedia.org/wiki/%ED%95%9C%EA%B5%AD%EC%9D%98_%EB%B3%B5%EC%84%B1
     */
    private static final String[] KOREAN_TWO_CHARCTER_FAMILY_NAMES = {
        "\uAC15\uC804", // Gang Jeon
        "\uB0A8\uAD81", // Nam Goong
        "\uB3C5\uACE0", // Dok Go
        "\uB3D9\uBC29", // Dong Bang
        "\uB9DD\uC808", // Mang Jeol
        "\uC0AC\uACF5", // Sa Gong
        "\uC11C\uBB38", // Seo Moon
        "\uC120\uC6B0", // Seon Woo
        "\uC18C\uBD09", // So Bong
        "\uC5B4\uAE08", // Uh Geum
        "\uC7A5\uACE1", // Jang Gok
        "\uC81C\uAC08", // Je Gal
        "\uD669\uBCF4"  // Hwang Bo
    };
    
    /**
     * Constants for various styles of combining given name, family name etc into
     * a full name.  For example, the western tradition follows the pattern
     * 'given name' 'middle name' 'family name' with the alternative pattern being
     * 'family name', 'given name' 'middle name'.  The CJK tradition is
     * 'family name' 'middle name' 'given name', with Japanese favoring a space between
     * the names and Chinese omitting the space.
     */
    public interface FullNameStyle {
        public static final int UNDEFINED = 0;
        public static final int WESTERN = 1;

        /**
         * Used if the name is written in Hanzi/Kanji/Hanja and we could not determine
         * which specific language it belongs to: Chinese, Japanese or Korean.
         */
        public static final int CJK = 2;

        public static final int CHINESE = 3;
        public static final int JAPANESE = 4;
        public static final int KOREAN = 5;
    }

    /**
     * Constants for various styles of capturing the pronunciation of a person's name.
     */
    public interface PhoneticNameStyle {
        public static final int UNDEFINED = 0;

        /**
         * Pinyin is a phonetic method of entering Chinese characters. Typically not explicitly
         * shown in UIs, but used for searches and sorting.
         */
        public static final int PINYIN = 3;

        /**
         * Hiragana and Katakana are two common styles of writing out the pronunciation
         * of a Japanese names.
         */
        public static final int JAPANESE = 4;

        /**
         * Hangul is the Korean phonetic alphabet.
         */
        public static final int KOREAN = 5;
    }


    public static class Name {
        public String prefix;
        public String givenNames;
        public String middleName;
        public String familyName;
        public String suffix;

        public int fullNameStyle;

        public String phoneticFamilyName;
        public String phoneticMiddleName;
        public String phoneticGivenName;

        public int phoneticNameStyle;

        public Name() {
        }

        public Name(String prefix, String givenNames, String middleName, String familyName,
                String suffix) {
            this.prefix = prefix;
            this.givenNames = givenNames;
            this.middleName = middleName;
            this.familyName = familyName;
            this.suffix = suffix;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getGivenNames() {
            return givenNames;
        }

        public String getMiddleName() {
            return middleName;
        }

        public String getFamilyName() {
            return familyName;
        }

        public String getSuffix() {
            return suffix;
        }

        public int getFullNameStyle() {
            return fullNameStyle;
        }

        public String getPhoneticFamilyName() {
            return phoneticFamilyName;
        }

        public String getPhoneticMiddleName() {
            return phoneticMiddleName;
        }

        public String getPhoneticGivenName() {
            return phoneticGivenName;
        }

        public int getPhoneticNameStyle() {
            return phoneticNameStyle;
        }       

        public void clear() {
            prefix = null;
            givenNames = null;
            middleName = null;
            familyName = null;
            suffix = null;
            fullNameStyle = FullNameStyle.UNDEFINED;
            phoneticFamilyName = null;
            phoneticMiddleName = null;
            phoneticGivenName = null;
            phoneticNameStyle = PhoneticNameStyle.UNDEFINED;
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(givenNames)
                    && TextUtils.isEmpty(middleName)
                    && TextUtils.isEmpty(familyName)
                    && TextUtils.isEmpty(suffix)
                    && TextUtils.isEmpty(phoneticFamilyName)
                    && TextUtils.isEmpty(phoneticMiddleName)
                    && TextUtils.isEmpty(phoneticGivenName);
        }

        @Override
        public String toString() {
            return "[prefix: " + prefix + " given: " + givenNames + " middle: " + middleName
                    + " family: " + familyName + " suffix: " + suffix + " ph/given: "
                    + phoneticGivenName + " ph/middle: " + phoneticMiddleName + " ph/family: "
                    + phoneticFamilyName + "]";
        }
    }

    private static class NameTokenizer extends StringTokenizer {
        private final String[] mTokens;
        private int mDotBitmask;
        private int mCommaBitmask;
        private int mStartPointer;
        private int mEndPointer;

        public NameTokenizer(String fullName) {
            super(fullName, " .,", true);

            mTokens = new String[MAX_TOKENS];

            // Iterate over tokens, skipping over empty ones and marking tokens that
            // are followed by dots.
            while (hasMoreTokens() && mEndPointer < MAX_TOKENS) {
                final String token = nextToken();
                if (token.length() > 0) {
                    final char c = token.charAt(0);
                    if (c == ' ') {
                        continue;
                    }
                }

                if (mEndPointer > 0 && token.charAt(0) == '.') {
                    mDotBitmask |= (1 << (mEndPointer - 1));
                } else if (mEndPointer > 0 && token.charAt(0) == ',') {
                    mCommaBitmask |= (1 << (mEndPointer - 1));
                } else {
                    mTokens[mEndPointer] = token;
                    mEndPointer++;
                }
            }
        }

        /**
         * Returns true if the token is followed by a dot in the original full name.
         */
        public boolean hasDot(int index) {
            return (mDotBitmask & (1 << index)) != 0;
        }

        /**
         * Returns true if the token is followed by a comma in the original full name.
         */
        public boolean hasComma(int index) {
            return (mCommaBitmask & (1 << index)) != 0;
        }
    }

    /**
     * Constructor.
     *
     * @param commonPrefixes comma-separated list of common prefixes,
     *            e.g. "Mr, Ms, Mrs"
     * @param commonLastNamePrefixes comma-separated list of common last name prefixes,
     *            e.g. "d', st, st., von"
     * @param commonSuffixes comma-separated list of common suffixes,
     *            e.g. "Jr, M.D., MD, D.D.S."
     * @param commonConjunctions comma-separated list of common conjuctions,
     *            e.g. "AND, Or"
     */
    public NameSplitter(String commonPrefixes, String commonLastNamePrefixes,
            String commonSuffixes, String commonConjunctions, Locale locale) {
        // TODO: refactor this to use <string-array> resources
        mPrefixesSet = convertToSet(commonPrefixes);
        mLastNamePrefixesSet = convertToSet(commonLastNamePrefixes);
        mSuffixesSet = convertToSet(commonSuffixes);
        mConjuctions = convertToSet(commonConjunctions);
        mLocale = locale != null ? locale : Locale.getDefault();
        mLanguage = mLocale.getLanguage().toLowerCase();

        int maxLength = 0;
        for (String suffix : mSuffixesSet) {
            if (suffix.length() > maxLength) {
                maxLength = suffix.length();
            }
        }

        mMaxSuffixLength = maxLength;
    }

    /**
     * Converts a comma-separated list of Strings to a set of Strings. Trims strings
     * and converts them to upper case.
     */
    private static HashSet<String> convertToSet(String strings) {
        HashSet<String> set = new HashSet<String>();
        if (strings != null) {
            String[] split = strings.split(",");
            for (int i = 0; i < split.length; i++) {
                set.add(split[i].trim().toUpperCase());
            }
        }
        return set;
    }

    /**
     * Parses a full name and returns components as a list of tokens.
     */
    public int tokenize(String[] tokens, String fullName) {
        if (fullName == null) {
            return 0;
        }

        NameTokenizer tokenizer = new NameTokenizer(fullName);

        if (tokenizer.mStartPointer == tokenizer.mEndPointer) {
            return 0;
        }

        String firstToken = tokenizer.mTokens[tokenizer.mStartPointer];
        if (mPrefixesSet.contains(firstToken.toUpperCase())) {
           tokenizer.mStartPointer++;
        }
        int count = 0;
        for (int i = tokenizer.mStartPointer; i < tokenizer.mEndPointer; i++) {
            tokens[count++] = tokenizer.mTokens[i];
        }

        return count;
    }


    /**
     * Parses a full name and returns parsed components in the Name object.
     */
    public void split(Name name, String fullName) {
        if (fullName == null) {
            return;
        }

        int fullNameStyle = guessFullNameStyle(fullName);
        if (fullNameStyle == FullNameStyle.CJK) {
            fullNameStyle = getAdjustedFullNameStyle(fullNameStyle);
        }

        split(name, fullName, fullNameStyle);
    }

    /**
     * Parses a full name and returns parsed components in the Name object
     * with a given fullNameStyle.
     */
    public void split(Name name, String fullName, int fullNameStyle) {
        if (fullName == null) {
            return;
        }

        name.fullNameStyle = fullNameStyle;

        switch (fullNameStyle) {
            case FullNameStyle.CHINESE:
                splitChineseName(name, fullName);
                break;

            case FullNameStyle.JAPANESE:
                splitJapaneseName(name, fullName);
                break;

            case FullNameStyle.KOREAN:
                splitKoreanName(name, fullName);
                break;

            default:
                splitWesternName(name, fullName);
        }
    }

    /**
     * Splits a full name composed according to the Western tradition:
     * <pre>
     *   [prefix] given name(s) [[middle name] family name] [, suffix]
     *   [prefix] family name, given name [middle name] [,suffix]
     * </pre>
     */
    private void splitWesternName(Name name, String fullName) {
        NameTokenizer tokens = new NameTokenizer(fullName);
        parsePrefix(name, tokens);

        // If the name consists of just one or two tokens, treat them as first/last name,
        // not as suffix.  Example: John Ma; Ma is last name, not "M.A.".
        if (tokens.mEndPointer > 2) {
            parseSuffix(name, tokens);
        }

        if (name.prefix == null && tokens.mEndPointer - tokens.mStartPointer == 1) {
            name.givenNames = tokens.mTokens[tokens.mStartPointer];
        } else {
            parseLastName(name, tokens);
            parseMiddleName(name, tokens);
            parseGivenNames(name, tokens);
        }
    }

    /**
     * Splits a full name composed according to the Chinese tradition:
     * <pre>
     *   [family name [middle name]] given name
     * </pre>
     */

    private static String hasDoubleLastName(String inStr) {
        String regx = "宇文、尉迟、延陵、羊舌、羊角、乐正、诸葛、颛孙、仲孙、仲长、长孙、钟离、宗政、左丘、主父、" +
                "宰父、子书、子车、子桑、百里、北堂、北野、哥舒、谷梁、闻人、王孙、王官、王叔、巫马、微生、淳于、" +
                "单于、成公、叱干、叱利、褚师、端木、东方、东郭、东宫、东野、东里、东门、第二、第五、公祖、公玉、" +
                "公西、公孟、公伯、公仲、公孙、公广、公上、公冶、公羊、公良、公户、公仪、公山、公门、公坚、公乘、" +
                "欧阳、濮阳、青阳、漆雕、壤驷、上官、司徒、司马、司空、司寇、士孙、申屠、叔孙、叔仲、侍其、令狐、" +
                "梁丘、闾丘、刘傅、慕容、万俟、谷利、高堂、南宫、南门、南荣、南野、女娲、纳兰、澹台、拓跋、太史、" +
                "太叔、太公、秃发、夏侯、西门、鲜于、轩辕、相里、皇甫、赫连、呼延、胡母、亓官、夹谷、即墨、独孤、" +
                "段干、达奚";
        String doubleLastName = "";
        List<String> l = StringUtils2.splitList(regx, "、", true);
        for (String l0 : l) {
            if (inStr.contains(l0)) {
                if (inStr.startsWith(l0)) {
                    doubleLastName = l0;
                    break;
                }
            }
        }

        return doubleLastName;
    }

    private void splitChineseName(Name name, String fullName) {
        StringTokenizer tokenizer = new StringTokenizer(fullName);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (name.givenNames == null) {
                name.givenNames = token;
            } else if (name.familyName == null) {
                name.familyName = name.givenNames;
                name.givenNames = token;
            } else if (name.middleName == null) {
                //name.middleName = name.givenNames;
                name.givenNames =  name.givenNames + token;
            } else {
                //name.middleName = name.middleName + name.givenNames;
                name.givenNames = name.givenNames + token;
            }
        }

        // If a single word parse that word up.
        if (name.givenNames != null && name.familyName == null && name.middleName == null) {
            int length = fullName.length();
            if (hasDoubleLastName(fullName).equals("")) {
                if (length == 2) {
                    name.familyName = fullName.substring(0, 1);
                    name.givenNames = fullName.substring(1);
                } else if (length == 3) {
                    name.familyName = fullName.substring(0, 1);
                    //name.middleName = fullName.substring(1, 2);
                    name.givenNames = fullName.substring(1, length);
                } else if (length >= 4) {
                    name.familyName = fullName.substring(0, 2);
                    //name.middleName = fullName.substring(2, 3);
                    name.givenNames = fullName.substring(2, length);
                }
            } else {
                if (length == 2) {
                    name.givenNames = fullName;
                } else if (length == 3) {
                    name.familyName = fullName.substring(0, 2);
                    name.givenNames = fullName.substring(2,3);
                } else if (length >= 4) {
                    name.familyName = fullName.substring(0, 2);
                    //name.middleName = fullName.substring(2, 3);
                    name.givenNames = fullName.substring(2,length);
                }
            }
        }
    }

    /**
     * Splits a full name composed according to the Japanese tradition:
     * <pre>
     *   [family name] given name(s)
     * </pre>
     */
    private void splitJapaneseName(Name name, String fullName) {
        StringTokenizer tokenizer = new StringTokenizer(fullName);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (name.givenNames == null) {
                name.givenNames = token;
            } else if (name.familyName == null) {
                name.familyName = name.givenNames;
                name.givenNames = token;
            } else {
                name.givenNames += " " + token;
            }
        }
    }

    /**
     * Splits a full name composed according to the Korean tradition:
     * <pre>
     *   [family name] given name(s)
     * </pre>
     */
    private void splitKoreanName(Name name, String fullName) {
        StringTokenizer tokenizer = new StringTokenizer(fullName);
        if (tokenizer.countTokens() > 1) {
            // Each name can be identified by separators.
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (name.givenNames == null) {
                    name.givenNames = token;
                } else if (name.familyName == null) {
                    name.familyName = name.givenNames;
                    name.givenNames = token;
                } else {
                    name.givenNames += " " + token;
                }
            }
        } else {
            // There is no separator. Try to guess family name.
            // The length of most family names is 1.
            int familyNameLength = 1;

            // Compare with 2-length family names.
            for (String twoLengthFamilyName : KOREAN_TWO_CHARCTER_FAMILY_NAMES) {
                if (fullName.startsWith(twoLengthFamilyName)) {
                    familyNameLength = 2;
                    break;
                }
            }

            name.familyName = fullName.substring(0, familyNameLength);
            if (fullName.length() > familyNameLength) {
                name.givenNames = fullName.substring(familyNameLength);
            }
        }
    }

    /**
     * Concatenates components of a name according to the rules dictated by the name style.
     *
     * @param givenNameFirst is ignored for CJK display name styles
     */
    public String join(Name name, boolean givenNameFirst, boolean includePrefix) {
        String prefix = includePrefix ? name.prefix : null;
        switch (name.fullNameStyle) {
            case FullNameStyle.CJK:
            case FullNameStyle.CHINESE:
            case FullNameStyle.KOREAN:
                return join(prefix, name.familyName, name.middleName, name.givenNames,
                        name.suffix, false, false, false);

            case FullNameStyle.JAPANESE:
                return join(prefix, name.familyName, name.middleName, name.givenNames,
                        name.suffix, true, false, false);

            default:
                if (givenNameFirst) {
                    return join(prefix, name.givenNames, name.middleName, name.familyName,
                            name.suffix, true, false, true);
                } else {
                    return join(prefix, name.familyName, name.givenNames, name.middleName,
                            name.suffix, true, true, true);
                }
        }
    }

    /**
     * Concatenates components of the phonetic name following the CJK tradition:
     * family name + middle name + given name(s).
     */
    public String joinPhoneticName(Name name) {
        return join(null, name.phoneticFamilyName,
                name.phoneticMiddleName, name.phoneticGivenName, null, true, false, false);
    }

    /**
     * Concatenates parts of a full name inserting spaces and commas as specified.
     */
    private String join(String prefix, String part1, String part2, String part3, String suffix,
            boolean useSpace, boolean useCommaAfterPart1, boolean useCommaAfterPart3) {
        prefix = prefix == null ? null: prefix.trim();
        part1 = part1 == null ? null: part1.trim();
        part2 = part2 == null ? null: part2.trim();
        part3 = part3 == null ? null: part3.trim();
        suffix = suffix == null ? null: suffix.trim();

        boolean hasPrefix = !TextUtils.isEmpty(prefix);
        boolean hasPart1 = !TextUtils.isEmpty(part1);
        boolean hasPart2 = !TextUtils.isEmpty(part2);
        boolean hasPart3 = !TextUtils.isEmpty(part3);
        boolean hasSuffix = !TextUtils.isEmpty(suffix);

        boolean isSingleWord = true;
        String singleWord = null;

        if (hasPrefix) {
            singleWord = prefix;
        }

        if (hasPart1) {
            if (singleWord != null) {
                isSingleWord = false;
            } else {
                singleWord = part1;
            }
        }

        if (hasPart2) {
            if (singleWord != null) {
                isSingleWord = false;
            } else {
                singleWord = part2;
            }
        }

        if (hasPart3) {
            if (singleWord != null) {
                isSingleWord = false;
            } else {
                singleWord = part3;
            }
        }

        if (hasSuffix) {
            if (singleWord != null) {
                isSingleWord = false;
            } else {
                singleWord = normalizedSuffix(suffix);
            }
        }

        if (isSingleWord) {
            return singleWord;
        }

        StringBuilder sb = new StringBuilder();

        if (hasPrefix) {
            sb.append(prefix);
        }

        if (hasPart1) {
            if (hasPrefix) {
                sb.append(' ');
            }
            sb.append(part1);
        }

        if (hasPart2) {
            if (hasPrefix || hasPart1) {
                if (useCommaAfterPart1) {
                    sb.append(',');
                }
                if (useSpace) {
                    sb.append(' ');
                }
            }
            sb.append(part2);
        }

        if (hasPart3) {
            if (hasPrefix || hasPart1 || hasPart2) {
                if (useSpace) {
                    sb.append(' ');
                }
            }
            sb.append(part3);
        }

        if (hasSuffix) {
            if (hasPrefix || hasPart1 || hasPart2 || hasPart3) {
                if (useCommaAfterPart3) {
                    sb.append(',');
                }
                if (useSpace) {
                    sb.append(' ');
                }
            }
            sb.append(normalizedSuffix(suffix));
        }

        return sb.toString();
    }

    /**
     * Puts a dot after the supplied suffix if that is the accepted form of the suffix,
     * e.g. "Jr." and "Sr.", but not "I", "II" and "III".
     */
    private String normalizedSuffix(String suffix) {
        int length = suffix.length();
        if (length == 0 || suffix.charAt(length - 1) == '.') {
            return suffix;
        }

        String withDot = suffix + '.';
        if (mSuffixesSet.contains(withDot.toUpperCase())) {
            return withDot;
        } else {
            return suffix;
        }
    }

    /**
     * If the supplied name style is undefined, returns a default based on the language,
     * otherwise returns the supplied name style itself.
     *
     * @param nameStyle See {@link com.borqs.server.base.util.NameSplitter.FullNameStyle}.
     */
    public int getAdjustedFullNameStyle(int nameStyle) {
        if (nameStyle == FullNameStyle.UNDEFINED) {
            if (JAPANESE_LANGUAGE.equals(mLanguage)) {
                return FullNameStyle.JAPANESE;
            } else if (KOREAN_LANGUAGE.equals(mLanguage)) {
                return FullNameStyle.KOREAN;
            } else if (CHINESE_LANGUAGE.equals(mLanguage)) {
                return FullNameStyle.CHINESE;
            } else {
                return FullNameStyle.WESTERN;
            }
        } else if (nameStyle == FullNameStyle.CJK) {
            if (JAPANESE_LANGUAGE.equals(mLanguage)) {
                return FullNameStyle.JAPANESE;
            } else if (KOREAN_LANGUAGE.equals(mLanguage)) {
                return FullNameStyle.KOREAN;
            } else {
                return FullNameStyle.CHINESE;
            }
        }
        return nameStyle;
    }

    /**
     * Parses the first word from the name if it is a prefix.
     */
    private void parsePrefix(Name name, NameTokenizer tokens) {
        if (tokens.mStartPointer == tokens.mEndPointer) {
            return;
        }

        String firstToken = tokens.mTokens[tokens.mStartPointer];
        if (mPrefixesSet.contains(firstToken.toUpperCase())) {
            if (tokens.hasDot(tokens.mStartPointer)) {
                firstToken += '.';
            }
            name.prefix = firstToken;
            tokens.mStartPointer++;
        }
    }

    /**
     * Parses the last word(s) from the name if it is a suffix.
     */
    private void parseSuffix(Name name, NameTokenizer tokens) {
        if (tokens.mStartPointer == tokens.mEndPointer) {
            return;
        }

        String lastToken = tokens.mTokens[tokens.mEndPointer - 1];

        // Take care of an explicit comma-separated suffix
        if (tokens.mEndPointer - tokens.mStartPointer > 2
                && tokens.hasComma(tokens.mEndPointer - 2)) {
            if (tokens.hasDot(tokens.mEndPointer - 1)) {
                lastToken += '.';
            }
            name.suffix = lastToken;
            tokens.mEndPointer--;
            return;
        }

        if (lastToken.length() > mMaxSuffixLength) {
            return;
        }

        String normalized = lastToken.toUpperCase();
        if (mSuffixesSet.contains(normalized)) {
            name.suffix = lastToken;
            tokens.mEndPointer--;
            return;
        }

        if (tokens.hasDot(tokens.mEndPointer - 1)) {
            lastToken += '.';
        }
        normalized += ".";

        // Take care of suffixes like M.D. and D.D.S.
        int pos = tokens.mEndPointer - 1;
        while (normalized.length() <= mMaxSuffixLength) {

            if (mSuffixesSet.contains(normalized)) {
                name.suffix = lastToken;
                tokens.mEndPointer = pos;
                return;
            }

            if (pos == tokens.mStartPointer) {
                break;
            }

            pos--;
            if (tokens.hasDot(pos)) {
                lastToken = tokens.mTokens[pos] + "." + lastToken;
            } else {
                lastToken = tokens.mTokens[pos] + " " + lastToken;
            }

            normalized = tokens.mTokens[pos].toUpperCase() + "." + normalized;
        }
    }

    private void parseLastName(Name name, NameTokenizer tokens) {
        if (tokens.mStartPointer == tokens.mEndPointer) {
            return;
        }

        // If the first word is followed by a comma, assume that it's the family name
        if (tokens.hasComma(tokens.mStartPointer)) {
           name.familyName = tokens.mTokens[tokens.mStartPointer];
           tokens.mStartPointer++;
           return;
        }

        // If the second word is followed by a comma and the first word
        // is a last name prefix as in "de Sade" and "von Cliburn", treat
        // the first two words as the family name.
        if (tokens.mStartPointer + 1 < tokens.mEndPointer
                && tokens.hasComma(tokens.mStartPointer + 1)
                && isFamilyNamePrefix(tokens.mTokens[tokens.mStartPointer])) {
            String familyNamePrefix = tokens.mTokens[tokens.mStartPointer];
            if (tokens.hasDot(tokens.mStartPointer)) {
                familyNamePrefix += '.';
            }
            name.familyName = familyNamePrefix + " " + tokens.mTokens[tokens.mStartPointer + 1];
            tokens.mStartPointer += 2;
            return;
        }

        // Finally, assume that the last word is the last name
        name.familyName = tokens.mTokens[tokens.mEndPointer - 1];
        tokens.mEndPointer--;

        // Take care of last names like "de Sade" and "von Cliburn"
        if ((tokens.mEndPointer - tokens.mStartPointer) > 0) {
            String lastNamePrefix = tokens.mTokens[tokens.mEndPointer - 1];
            if (isFamilyNamePrefix(lastNamePrefix)) {
                if (tokens.hasDot(tokens.mEndPointer - 1)) {
                    lastNamePrefix += '.';
                }
                name.familyName = lastNamePrefix + " " + name.familyName;
                tokens.mEndPointer--;
            }
        }
    }

    /**
     * Returns true if the supplied word is an accepted last name prefix, e.g. "von", "de"
     */
    private boolean isFamilyNamePrefix(String word) {
        final String normalized = word.toUpperCase();

        return mLastNamePrefixesSet.contains(normalized)
                || mLastNamePrefixesSet.contains(normalized + ".");
    }


    private void parseMiddleName(Name name, NameTokenizer tokens) {
        if (tokens.mStartPointer == tokens.mEndPointer) {
            return;
        }

        if ((tokens.mEndPointer - tokens.mStartPointer) > 1) {
            if ((tokens.mEndPointer - tokens.mStartPointer) == 2
                    || !mConjuctions.contains(tokens.mTokens[tokens.mEndPointer - 2].
                            toUpperCase())) {
                name.middleName = tokens.mTokens[tokens.mEndPointer - 1];
                if (tokens.hasDot(tokens.mEndPointer - 1)) {
                    name.middleName += '.';
                }
                tokens.mEndPointer--;
            }
        }
    }

    private void parseGivenNames(Name name, NameTokenizer tokens) {
        if (tokens.mStartPointer == tokens.mEndPointer) {
            return;
        }

        if ((tokens.mEndPointer - tokens.mStartPointer) == 1) {
            name.givenNames = tokens.mTokens[tokens.mStartPointer];
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = tokens.mStartPointer; i < tokens.mEndPointer; i++) {
                if (i != tokens.mStartPointer) {
                    sb.append(' ');
                }
                sb.append(tokens.mTokens[i]);
                if (tokens.hasDot(i)) {
                    sb.append('.');
                }
            }
            name.givenNames = sb.toString();
        }
    }

    /**
     * Makes the best guess at the expected full name style based on the character set
     * used in the supplied name.  If the phonetic name is also supplied, tries to
     * differentiate between Chinese, Japanese and Korean based on the alphabet used
     * for the phonetic name.
     */
    public void guessNameStyle(Name name) {
        guessFullNameStyle(name);
        guessPhoneticNameStyle(name);
        name.fullNameStyle = getAdjustedNameStyleBasedOnPhoneticNameStyle(name.fullNameStyle,
                name.phoneticNameStyle);
    }

    /**
     * Updates the display name style according to the phonetic name style if we
     * were unsure about display name style based on the name components, but
     * phonetic name makes it more definitive.
     */
    public int getAdjustedNameStyleBasedOnPhoneticNameStyle(int nameStyle, int phoneticNameStyle) {
        if (phoneticNameStyle != PhoneticNameStyle.UNDEFINED) {
            if (nameStyle == FullNameStyle.UNDEFINED || nameStyle == FullNameStyle.CJK) {
                if (phoneticNameStyle == PhoneticNameStyle.JAPANESE) {
                    return FullNameStyle.JAPANESE;
                } else if (phoneticNameStyle == PhoneticNameStyle.KOREAN) {
                    return FullNameStyle.KOREAN;
                }
                if (nameStyle == FullNameStyle.CJK && phoneticNameStyle == PhoneticNameStyle.PINYIN) {
                    return FullNameStyle.CHINESE;
                }
            }
        }
        return nameStyle;
    }

    /**
     * Makes the best guess at the expected full name style based on the character set
     * used in the supplied name.
     */
    private void guessFullNameStyle(Name name) {
        if (name.fullNameStyle != FullNameStyle.UNDEFINED) {
            return;
        }

        int bestGuess = guessFullNameStyle(name.givenNames);
        // A mix of Hanzi and latin chars are common in China, so we have to go through all names
        // if the name is not JANPANESE or KOREAN.
        if (bestGuess != FullNameStyle.UNDEFINED && bestGuess != FullNameStyle.CJK
                && bestGuess != FullNameStyle.WESTERN) {
            name.fullNameStyle = bestGuess;
            return;
        }

        int guess = guessFullNameStyle(name.familyName);
        if (guess != FullNameStyle.UNDEFINED) {
            if (guess != FullNameStyle.CJK && guess != FullNameStyle.WESTERN) {
                name.fullNameStyle = guess;
                return;
            }
            bestGuess = guess;
        }

        guess = guessFullNameStyle(name.middleName);
        if (guess != FullNameStyle.UNDEFINED) {
            if (guess != FullNameStyle.CJK && guess != FullNameStyle.WESTERN) {
                name.fullNameStyle = guess;
                return;
            }
            bestGuess = guess;
        }

        guess = guessFullNameStyle(name.prefix);
        if (guess != FullNameStyle.UNDEFINED) {
            if (guess != FullNameStyle.CJK && guess != FullNameStyle.WESTERN) {
                name.fullNameStyle = guess;
                return;
            }
            bestGuess = guess;
        }

        guess = guessFullNameStyle(name.suffix);
        if (guess != FullNameStyle.UNDEFINED) {
            if (guess != FullNameStyle.CJK && guess != FullNameStyle.WESTERN) {
                name.fullNameStyle = guess;
                return;
            }
            bestGuess = guess;
        }

        name.fullNameStyle = bestGuess;
    }

    public int guessFullNameStyle(String name) {
        if (name == null) {
            return FullNameStyle.UNDEFINED;
        }

        int nameStyle = FullNameStyle.UNDEFINED;
        int length = name.length();
        int offset = 0;
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                UnicodeBlock unicodeBlock = UnicodeBlock.of(codePoint);

                if (!isLatinUnicodeBlock(unicodeBlock)) {

                    if (isCJKUnicodeBlock(unicodeBlock)) {
                        // We don't know if this is Chinese, Japanese or Korean -
                        // trying to figure out by looking at other characters in the name
                        return guessCJKNameStyle(name, offset + Character.charCount(codePoint));
                    }

                    if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                        return FullNameStyle.JAPANESE;
                    }

                    if (isKoreanUnicodeBlock(unicodeBlock)) {
                        return FullNameStyle.KOREAN;
                    }
                }
                nameStyle = FullNameStyle.WESTERN;
            }
            offset += Character.charCount(codePoint);
        }
        return nameStyle;
    }

    private int guessCJKNameStyle(String name, int offset) {
        int length = name.length();
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                UnicodeBlock unicodeBlock = UnicodeBlock.of(codePoint);
                if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                    return FullNameStyle.JAPANESE;
                }
                if (isKoreanUnicodeBlock(unicodeBlock)) {
                    return FullNameStyle.KOREAN;
                }
            }
            offset += Character.charCount(codePoint);
        }

        return FullNameStyle.CJK;
    }

    private void guessPhoneticNameStyle(Name name) {
        if (name.phoneticNameStyle != PhoneticNameStyle.UNDEFINED) {
            return;
        }

        int bestGuess = guessPhoneticNameStyle(name.phoneticFamilyName);
        if (bestGuess != FullNameStyle.UNDEFINED && bestGuess != FullNameStyle.CJK) {
            name.phoneticNameStyle = bestGuess;
            return;
        }

        int guess = guessPhoneticNameStyle(name.phoneticGivenName);
        if (guess != FullNameStyle.UNDEFINED) {
            if (guess != FullNameStyle.CJK) {
                name.phoneticNameStyle = guess;
                return;
            }
            bestGuess = guess;
        }

        guess = guessPhoneticNameStyle(name.phoneticMiddleName);
        if (guess != FullNameStyle.UNDEFINED) {
            if (guess != FullNameStyle.CJK) {
                name.phoneticNameStyle = guess;
                return;
            }
            bestGuess = guess;
        }
    }

    public int guessPhoneticNameStyle(String name) {
        if (name == null) {
            return PhoneticNameStyle.UNDEFINED;
        }

        int nameStyle = PhoneticNameStyle.UNDEFINED;
        int length = name.length();
        int offset = 0;
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                UnicodeBlock unicodeBlock = UnicodeBlock.of(codePoint);
                if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                    return PhoneticNameStyle.JAPANESE;
                }
                if (isKoreanUnicodeBlock(unicodeBlock)) {
                    return PhoneticNameStyle.KOREAN;
                }
                if (isLatinUnicodeBlock(unicodeBlock)) {
                    return PhoneticNameStyle.PINYIN;
                }
            }
            offset += Character.charCount(codePoint);
        }

        return nameStyle;
    }

    private static boolean isLatinUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.BASIC_LATIN ||
                unicodeBlock == UnicodeBlock.LATIN_1_SUPPLEMENT ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_A ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_B ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_ADDITIONAL;
    }

    private static boolean isCJKUnicodeBlock(UnicodeBlock block) {
        return block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == UnicodeBlock.CJK_RADICALS_SUPPLEMENT
                || block == UnicodeBlock.CJK_COMPATIBILITY
                || block == UnicodeBlock.CJK_COMPATIBILITY_FORMS
                || block == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
    }

    private static boolean isKoreanUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.HANGUL_SYLLABLES ||
                unicodeBlock == UnicodeBlock.HANGUL_JAMO ||
                unicodeBlock == UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }

    private static boolean isJapanesePhoneticUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.KATAKANA ||
                unicodeBlock == UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS ||
                unicodeBlock == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ||
                unicodeBlock == UnicodeBlock.HIRAGANA;
    }
    public static Name split(String fullName) {
        Name name = new Name();
        NameSplitter splitter = new NameSplitter(
                "Mr, Ms, Mrs", "d', st, st., von", "Jr., M.D., MD, D.D.S.", "&, AND", Locale.CHINA);
        splitter.split(name, fullName);
        return name;
    }

    public static String join(Name name) {
        NameSplitter splitter = new NameSplitter(
                "Mr, Ms, Mrs", "d', st, st., von", "Jr., M.D., MD, D.D.S.", "&, AND", Locale.CHINA);
        splitter.guessFullNameStyle(name);
        boolean givenNameFirst = true;
        if (name.fullNameStyle == FullNameStyle.CJK
                || name.fullNameStyle == FullNameStyle.CHINESE
                || name.fullNameStyle == FullNameStyle.KOREAN
                || name.fullNameStyle == FullNameStyle.JAPANESE)
            givenNameFirst = false;

        return splitter.join(name, givenNameFirst, true);
    }
}

class TextUtils {

	public static boolean isEmpty(String middleName) {
		return StringUtils.isEmpty(middleName);
	}

}


