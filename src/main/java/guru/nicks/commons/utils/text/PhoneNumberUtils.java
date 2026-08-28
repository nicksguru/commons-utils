package guru.nicks.commons.utils.text;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.experimental.UtilityClass;

/**
 * Phone number-related utility methods.
 */
@UtilityClass
public class PhoneNumberUtils {

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    /**
     * Checks whether the given phone number is a valid international phone number. Textual prefixes and punctuation are
     * ignored - see {@link PhoneNumberUtil#parse(CharSequence, String)} for details.
     *
     * @param phoneNumber phone number to check
     * @return true if the number parses and is valid per {@link PhoneNumberUtil} metadata: real country code and valid
     *         suffix for the country detected
     */
    public static boolean isValidInternationalPhoneNumber(String phoneNumber) {
        try {
            Phonenumber.PhoneNumber parsed = PHONE_NUMBER_UTIL.parse(phoneNumber, null);
            return PHONE_NUMBER_UTIL.isValidNumber(parsed);
        } catch (NumberParseException e) {
            return false;
        }
    }

    /**
     * Validates and normalizes the phone number with {@link PhoneNumberUtil}. Textual prefixes and punctuation are
     * removed - see {@link PhoneNumberUtil#parse(CharSequence, String)} for details.
     *
     * @param phoneNumber raw phone number
     * @return normalized phone number
     * @throws IllegalArgumentException no country code / number too short for the country detected / etc.
     */
    public static String normalizeInternationalPhoneNumber(String phoneNumber) {
        Phonenumber.PhoneNumber parsedNumber;

        try {
            parsedNumber = PHONE_NUMBER_UTIL.parse(phoneNumber, null);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Invalid international phone number: " + e.getMessage(), e);
        }

        return PHONE_NUMBER_UTIL.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

}
