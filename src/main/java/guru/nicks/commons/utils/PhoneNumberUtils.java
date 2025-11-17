package guru.nicks.commons.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.experimental.UtilityClass;

/**
 * Text-related utility methods.
 */
@UtilityClass
public class PhoneNumberUtils {

    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    /**
     * Validates the phone number with Google PhoneLib.
     *
     * @param phoneNumber phone number
     * @return {@code true} if the phone number is valid, {@code false} otherwise
     */
    public static boolean isValidInternationalPhoneNumber(String phoneNumber) {
        // the 2nd argument (region to dial from) is null, therefore validation checks for the leading '+' (tested)
        return PHONE_NUMBER_UTIL.isPossibleNumber(phoneNumber, null);
    }

    /**
     * Validates and normalizes the phone number with Google PhoneLib.
     *
     * @param phoneNumber phone number
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
