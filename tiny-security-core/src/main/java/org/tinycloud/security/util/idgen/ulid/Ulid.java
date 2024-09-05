package org.tinycloud.security.util.idgen.ulid;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A class that represents ULIDs.
 * https://github.com/f4b6a3/ulid-creator
 * <p>
 * ULID is a 128-bit value that has two components:
 * <ul>
 * <li><b>Time component</b>: a number of milliseconds since 1970-01-01 (Unix
 * epoch).
 * <li><b>Random component</b>: a sequence of 80 random bits generated by a
 * secure random generator.
 * </ul>
 * <p>
 * ULID has 128-bit compatibility with {@link UUID}. Like a UUID, a ULID can
 * also be stored as a 16-byte array.
 * <p>
 * Instances of this class are <b>immutable</b>.
 *
 * @see <a href="https://github.com/ulid/spec">ULID Specification</a>
 */
public final class Ulid implements Serializable, Comparable<Ulid> {

    private static final long serialVersionUID = 2625269413446854731L;

    /**
     * The most significant bits
     */
    private final long msb;
    /**
     * The least significant bits
     */
    private final long lsb;

    /**
     * Number of characters of a ULID.
     */
    public static final int ULID_CHARS = 26;
    /**
     * Number of characters of the time component of a ULID.
     */
    public static final int TIME_CHARS = 10;
    /**
     * Number of characters of the random component of a ULID.
     */
    public static final int RANDOM_CHARS = 16;

    /**
     * Number of bytes of a ULID.
     */
    public static final int ULID_BYTES = 16;
    /**
     * Number of bytes of the time component of a ULID.
     */
    public static final int TIME_BYTES = 6;
    /**
     * Number of bytes of the random component of a ULID.
     */
    public static final int RANDOM_BYTES = 10;
    /**
     * A special ULID that has all 128 bits set to ZERO.
     */
    public static final Ulid MIN = new Ulid(0x0000000000000000L, 0x0000000000000000L);
    /**
     * A special ULID that has all 128 bits set to ONE.
     */
    public static final Ulid MAX = new Ulid(0xffffffffffffffffL, 0xffffffffffffffffL);

    static final byte[] ALPHABET_VALUES = new byte[256];
    static final char[] ALPHABET_UPPERCASE = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    static final char[] ALPHABET_LOWERCASE = "0123456789abcdefghjkmnpqrstvwxyz".toCharArray();

    static {

        // Initialize the alphabet map with -1
        Arrays.fill(ALPHABET_VALUES, (byte) -1);

        // Map the alphabets chars to values
        for (int i = 0; i < ALPHABET_UPPERCASE.length; i++) {
            ALPHABET_VALUES[ALPHABET_UPPERCASE[i]] = (byte) i;
        }
        for (int i = 0; i < ALPHABET_LOWERCASE.length; i++) {
            ALPHABET_VALUES[ALPHABET_LOWERCASE[i]] = (byte) i;
        }

        // Upper case OIL
        ALPHABET_VALUES['O'] = 0x00;
        ALPHABET_VALUES['I'] = 0x01;
        ALPHABET_VALUES['L'] = 0x01;

        // Lower case OIL
        ALPHABET_VALUES['o'] = 0x00;
        ALPHABET_VALUES['i'] = 0x01;
        ALPHABET_VALUES['l'] = 0x01;
    }

    // 0xffffffffffffffffL + 1 = 0x0000000000000000L
    private static final long INCREMENT_OVERFLOW = 0x0000000000000000L;

    /**
     * Creates a new ULID.
     * <p>
     * Useful to make copies of ULIDs.
     *
     * @param ulid a ULID
     */
    public Ulid(Ulid ulid) {
        this.msb = ulid.msb;
        this.lsb = ulid.lsb;
    }

    /**
     * Creates a new ULID.
     * <p>
     * If you want to make a copy of a {@link UUID}, use {@link Ulid#from(UUID)}
     * instead.
     *
     * @param mostSignificantBits  the first 8 bytes as a long value
     * @param leastSignificantBits the last 8 bytes as a long value
     */
    public Ulid(long mostSignificantBits, long leastSignificantBits) {
        this.msb = mostSignificantBits;
        this.lsb = leastSignificantBits;
    }

    /**
     * Creates a new ULID.
     * <p>
     * The time parameter is the number of milliseconds since 1970-01-01, also known
     * as Unix epoch. It must be a positive number not larger than 2^48-1.
     * <p>
     * The random parameter must be an arbitrary array of 10 bytes.
     * <p>
     * Note: ULIDs cannot be composed of dates before 1970-01-01, as their embedded
     * timestamp is internally treated as an unsigned integer, i.e., it can only
     * represent the set of natural numbers including zero, up to 2^48-1.
     *
     * @param time   the number of milliseconds since 1970-01-01
     * @param random an array of 10 bytes
     * @throws IllegalArgumentException if time is negative or larger than 2^48-1
     * @throws IllegalArgumentException if random is null or its length is not 10
     */
    public Ulid(long time, byte[] random) {

        // The time component has 48 bits.
        if ((time & 0xffff000000000000L) != 0) {
            // ULID specification:
            // "Any attempt to decode or encode a ULID larger than this (time > 2^48-1)
            // should be rejected by all implementations, to prevent overflow bugs."
            throw new IllegalArgumentException("Invalid time value"); // overflow or negative time!
        }
        // The random component has 80 bits (10 bytes).
        if (random == null || random.length != RANDOM_BYTES) {
            throw new IllegalArgumentException("Invalid random bytes"); // null or wrong length!
        }

        long long0 = 0;
        long long1 = 0;

        long0 |= time << 16;
        long0 |= (long) (random[0x0] & 0xff) << 8;
        long0 |= (long) (random[0x1] & 0xff);

        long1 |= (long) (random[0x2] & 0xff) << 56;
        long1 |= (long) (random[0x3] & 0xff) << 48;
        long1 |= (long) (random[0x4] & 0xff) << 40;
        long1 |= (long) (random[0x5] & 0xff) << 32;
        long1 |= (long) (random[0x6] & 0xff) << 24;
        long1 |= (long) (random[0x7] & 0xff) << 16;
        long1 |= (long) (random[0x8] & 0xff) << 8;
        long1 |= (long) (random[0x9] & 0xff);

        this.msb = long0;
        this.lsb = long1;
    }

    /**
     * Returns a fast new ULID.
     * <p>
     * This static method is a quick alternative to {@link UlidCreator#getUlid()}.
     * <p>
     * It employs {@link ThreadLocalRandom} which works very well, although not
     * cryptographically strong. It can be useful, for example, for logging.
     * <p>
     * Security-sensitive applications that require a cryptographically secure
     * pseudo-random generator should use {@link UlidCreator#getUlid()}.
     *
     * @return a ULID
     * @since 5.1.0
     */
    public static Ulid fast() {
        final long time = System.currentTimeMillis();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new Ulid((time << 16) | (random.nextLong() & 0xffffL), random.nextLong());
    }

    /**
     * Returns the minimum ULID for a given time.
     * <p>
     * The 48 bits of the time component are filled with the given time and the 80
     * bits of the random component are all set to ZERO.
     * <p>
     * For example, the minimum ULID for 2022-02-22 22:22:22.222 is
     * `{@code new Ulid(0x017f2387460e0000L, 0x0000000000000000L)}`, where
     * `{@code 0x017f2387460e}` is the timestamp in hexadecimal.
     * <p>
     * It can be useful to find all records before or after a specific timestamp in
     * a table without a `{@code created_at}` field.
     *
     * @param time the number of milliseconds since 1970-01-01
     * @return a ULID
     * @since 5.2.0
     */
    public static Ulid min(long time) {
        return new Ulid((time << 16) | 0x0000L, 0x0000000000000000L);
    }

    /**
     * Returns the maximum ULID for a given time.
     * <p>
     * The 48 bits of the time component are filled with the given time and the 80
     * bits or the random component are all set to ONE.
     * <p>
     * For example, the maximum ULID for 2022-02-22 22:22:22.222 is
     * `{@code new Ulid(0x017f2387460effffL, 0xffffffffffffffffL)}`, where
     * `{@code 0x017f2387460e}` is the timestamp in hexadecimal.
     * <p>
     * It can be useful to find all records before or after a specific timestamp in
     * a table without a `{@code created_at}` field.
     *
     * @param time the number of milliseconds since 1970-01-01
     * @return a ULID
     * @since 5.2.0
     */
    public static Ulid max(long time) {
        return new Ulid((time << 16) | 0xffffL, 0xffffffffffffffffL);
    }

    /**
     * Converts a UUID into a ULID.
     *
     * @param uuid a UUID
     * @return a ULID
     */
    public static Ulid from(UUID uuid) {
        return new Ulid(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    /**
     * Converts a byte array into a ULID.
     *
     * @param bytes an array of 16 bytes
     * @return a ULID
     * @throws IllegalArgumentException if bytes are null or its length is not 16
     */
    public static Ulid from(byte[] bytes) {

        if (bytes == null || bytes.length != ULID_BYTES) {
            throw new IllegalArgumentException("Invalid ULID bytes"); // null or wrong length!
        }

        long msb = 0;
        long lsb = 0;

        msb |= (bytes[0x0] & 0xffL) << 56;
        msb |= (bytes[0x1] & 0xffL) << 48;
        msb |= (bytes[0x2] & 0xffL) << 40;
        msb |= (bytes[0x3] & 0xffL) << 32;
        msb |= (bytes[0x4] & 0xffL) << 24;
        msb |= (bytes[0x5] & 0xffL) << 16;
        msb |= (bytes[0x6] & 0xffL) << 8;
        msb |= (bytes[0x7] & 0xffL);

        lsb |= (bytes[0x8] & 0xffL) << 56;
        lsb |= (bytes[0x9] & 0xffL) << 48;
        lsb |= (bytes[0xa] & 0xffL) << 40;
        lsb |= (bytes[0xb] & 0xffL) << 32;
        lsb |= (bytes[0xc] & 0xffL) << 24;
        lsb |= (bytes[0xd] & 0xffL) << 16;
        lsb |= (bytes[0xe] & 0xffL) << 8;
        lsb |= (bytes[0xf] & 0xffL);

        return new Ulid(msb, lsb);
    }

    /**
     * Converts a canonical string into a ULID.
     * <p>
     * The input string must be 26 characters long and must contain only characters
     * from Crockford's base 32 alphabet.
     * <p>
     * The first character of the input string must be between 0 and 7.
     *
     * @param string a canonical string
     * @return a ULID
     * @throws IllegalArgumentException if the input string is invalid
     * @see <a href="https://www.crockford.com/base32.html">Crockford's Base 32</a>
     */
    public static Ulid from(String string) {

        final char[] chars = toCharArray(string);

        long time = 0;
        long random0 = 0;
        long random1 = 0;

        time |= (long) ALPHABET_VALUES[chars[0x00]] << 45;
        time |= (long) ALPHABET_VALUES[chars[0x01]] << 40;
        time |= (long) ALPHABET_VALUES[chars[0x02]] << 35;
        time |= (long) ALPHABET_VALUES[chars[0x03]] << 30;
        time |= (long) ALPHABET_VALUES[chars[0x04]] << 25;
        time |= (long) ALPHABET_VALUES[chars[0x05]] << 20;
        time |= (long) ALPHABET_VALUES[chars[0x06]] << 15;
        time |= (long) ALPHABET_VALUES[chars[0x07]] << 10;
        time |= (long) ALPHABET_VALUES[chars[0x08]] << 5;
        time |= (long) ALPHABET_VALUES[chars[0x09]];

        random0 |= (long) ALPHABET_VALUES[chars[0x0a]] << 35;
        random0 |= (long) ALPHABET_VALUES[chars[0x0b]] << 30;
        random0 |= (long) ALPHABET_VALUES[chars[0x0c]] << 25;
        random0 |= (long) ALPHABET_VALUES[chars[0x0d]] << 20;
        random0 |= (long) ALPHABET_VALUES[chars[0x0e]] << 15;
        random0 |= (long) ALPHABET_VALUES[chars[0x0f]] << 10;
        random0 |= (long) ALPHABET_VALUES[chars[0x10]] << 5;
        random0 |= (long) ALPHABET_VALUES[chars[0x11]];

        random1 |= (long) ALPHABET_VALUES[chars[0x12]] << 35;
        random1 |= (long) ALPHABET_VALUES[chars[0x13]] << 30;
        random1 |= (long) ALPHABET_VALUES[chars[0x14]] << 25;
        random1 |= (long) ALPHABET_VALUES[chars[0x15]] << 20;
        random1 |= (long) ALPHABET_VALUES[chars[0x16]] << 15;
        random1 |= (long) ALPHABET_VALUES[chars[0x17]] << 10;
        random1 |= (long) ALPHABET_VALUES[chars[0x18]] << 5;
        random1 |= (long) ALPHABET_VALUES[chars[0x19]];

        final long msb = (time << 16) | (random0 >>> 24);
        final long lsb = (random0 << 40) | (random1 & 0xffffffffffL);

        return new Ulid(msb, lsb);
    }

    /**
     * Convert the ULID into a UUID.
     * <p>
     * A ULID has 128-bit compatibility with a {@link UUID}.
     * <p>
     * If you need a RFC-4122 UUIDv4 do this: {@code Ulid.toRfc4122().toUuid()}.
     *
     * @return a UUID.
     */
    public UUID toUuid() {
        return new UUID(this.msb, this.lsb);
    }

    /**
     * Convert the ULID into a byte array.
     *
     * @return a byte array.
     */
    public byte[] toBytes() {

        final byte[] bytes = new byte[ULID_BYTES];

        bytes[0x0] = (byte) (msb >>> 56);
        bytes[0x1] = (byte) (msb >>> 48);
        bytes[0x2] = (byte) (msb >>> 40);
        bytes[0x3] = (byte) (msb >>> 32);
        bytes[0x4] = (byte) (msb >>> 24);
        bytes[0x5] = (byte) (msb >>> 16);
        bytes[0x6] = (byte) (msb >>> 8);
        bytes[0x7] = (byte) (msb);

        bytes[0x8] = (byte) (lsb >>> 56);
        bytes[0x9] = (byte) (lsb >>> 48);
        bytes[0xa] = (byte) (lsb >>> 40);
        bytes[0xb] = (byte) (lsb >>> 32);
        bytes[0xc] = (byte) (lsb >>> 24);
        bytes[0xd] = (byte) (lsb >>> 16);
        bytes[0xe] = (byte) (lsb >>> 8);
        bytes[0xf] = (byte) (lsb);

        return bytes;
    }

    /**
     * Converts the ULID into a canonical string in upper case.
     * <p>
     * The output string is 26 characters long and contains only characters from
     * Crockford's Base 32 alphabet.
     * <p>
     * For lower case string, use the shorthand {@code Ulid#toLowerCase()}, instead
     * of {@code Ulid#toString()#toLowerCase()}.
     *
     * @return a ULID string
     * @see <a href="https://www.crockford.com/base32.html">Crockford's Base 32</a>
     */
    @Override
    public String toString() {
        return toString(ALPHABET_UPPERCASE);
    }

    /**
     * Converts the ULID into a canonical string in lower case.
     * <p>
     * The output string is 26 characters long and contains only characters from
     * Crockford's Base 32 alphabet.
     * <p>
     * It is a shorthand at least twice as fast as
     * {@code Ulid.toString().toLowerCase()}.
     *
     * @return a string
     * @see <a href="https://www.crockford.com/base32.html">Crockford's Base 32</a>
     */
    public String toLowerCase() {
        return toString(ALPHABET_LOWERCASE);
    }

    /**
     * Converts the ULID into another ULID that is compatible with UUIDv4.
     * <p>
     * The bytes of the returned ULID are compliant with the RFC-4122 version 4.
     * <p>
     * If you need a RFC-4122 UUIDv4 do this: {@code Ulid.toRfc4122().toUuid()}.
     * <p>
     * <b>Note:</b> If you use this method, you can not get the original ULID, since
     * it changes 6 bits of it to generate a UUIDv4.
     *
     * @return a ULID
     * @see <a href="https://www.rfc-editor.org/rfc/rfc4122">RFC-4122</a>
     */
    public Ulid toRfc4122() {

        // set the 4 most significant bits of the 7th byte to 0, 1, 0 and 0
        final long msb4 = (this.msb & 0xffffffffffff0fffL) | 0x0000000000004000L; // RFC-4122 version 4
        // set the 2 most significant bits of the 9th byte to 1 and 0
        final long lsb4 = (this.lsb & 0x3fffffffffffffffL) | 0x8000000000000000L; // RFC-4122 variant 2

        return new Ulid(msb4, lsb4);
    }

    /**
     * Returns the instant of creation.
     * <p>
     * The instant of creation is extracted from the time component.
     *
     * @return the {@link Instant} of creation
     */
    public Instant getInstant() {
        return Instant.ofEpochMilli(this.getTime());
    }

    /**
     * Returns the instant of creation.
     * <p>
     * The instant of creation is extracted from the time component.
     *
     * @param string a canonical string
     * @return the {@link Instant} of creation
     * @throws IllegalArgumentException if the input string is invalid
     */
    public static Instant getInstant(String string) {
        return Instant.ofEpochMilli(getTime(string));
    }

    /**
     * Returns the time component as a number.
     * <p>
     * The time component is a number between 0 and 2^48-1. It is equivalent to the
     * count of milliseconds since 1970-01-01 (Unix epoch).
     *
     * @return a number of milliseconds
     */
    public long getTime() {
        return this.msb >>> 16;
    }

    /**
     * Returns the time component as a number.
     * <p>
     * The time component is a number between 0 and 2^48-1. It is equivalent to the
     * count of milliseconds since 1970-01-01 (Unix epoch).
     *
     * @param string a canonical string
     * @return a number of milliseconds
     * @throws IllegalArgumentException if the input string is invalid
     */
    public static long getTime(String string) {

        final char[] chars = toCharArray(string);

        long time = 0;

        time |= (long) ALPHABET_VALUES[chars[0x00]] << 45;
        time |= (long) ALPHABET_VALUES[chars[0x01]] << 40;
        time |= (long) ALPHABET_VALUES[chars[0x02]] << 35;
        time |= (long) ALPHABET_VALUES[chars[0x03]] << 30;
        time |= (long) ALPHABET_VALUES[chars[0x04]] << 25;
        time |= (long) ALPHABET_VALUES[chars[0x05]] << 20;
        time |= (long) ALPHABET_VALUES[chars[0x06]] << 15;
        time |= (long) ALPHABET_VALUES[chars[0x07]] << 10;
        time |= (long) ALPHABET_VALUES[chars[0x08]] << 5;
        time |= (long) ALPHABET_VALUES[chars[0x09]];

        return time;
    }

    /**
     * Returns the random component as a byte array.
     * <p>
     * The random component is an array of 10 bytes (80 bits).
     *
     * @return a byte array
     */
    public byte[] getRandom() {

        final byte[] bytes = new byte[RANDOM_BYTES];

        bytes[0x0] = (byte) (msb >>> 8);
        bytes[0x1] = (byte) (msb);

        bytes[0x2] = (byte) (lsb >>> 56);
        bytes[0x3] = (byte) (lsb >>> 48);
        bytes[0x4] = (byte) (lsb >>> 40);
        bytes[0x5] = (byte) (lsb >>> 32);
        bytes[0x6] = (byte) (lsb >>> 24);
        bytes[0x7] = (byte) (lsb >>> 16);
        bytes[0x8] = (byte) (lsb >>> 8);
        bytes[0x9] = (byte) (lsb);

        return bytes;
    }

    /**
     * Returns the random component as a byte array.
     * <p>
     * The random component is an array of 10 bytes (80 bits).
     *
     * @param string a canonical string
     * @return a byte array
     * @throws IllegalArgumentException if the input string is invalid
     */
    public static byte[] getRandom(String string) {

        final char[] chars = toCharArray(string);

        long random0 = 0;
        long random1 = 0;

        random0 |= (long) ALPHABET_VALUES[chars[0x0a]] << 35;
        random0 |= (long) ALPHABET_VALUES[chars[0x0b]] << 30;
        random0 |= (long) ALPHABET_VALUES[chars[0x0c]] << 25;
        random0 |= (long) ALPHABET_VALUES[chars[0x0d]] << 20;
        random0 |= (long) ALPHABET_VALUES[chars[0x0e]] << 15;
        random0 |= (long) ALPHABET_VALUES[chars[0x0f]] << 10;
        random0 |= (long) ALPHABET_VALUES[chars[0x10]] << 5;
        random0 |= (long) ALPHABET_VALUES[chars[0x11]];

        random1 |= (long) ALPHABET_VALUES[chars[0x12]] << 35;
        random1 |= (long) ALPHABET_VALUES[chars[0x13]] << 30;
        random1 |= (long) ALPHABET_VALUES[chars[0x14]] << 25;
        random1 |= (long) ALPHABET_VALUES[chars[0x15]] << 20;
        random1 |= (long) ALPHABET_VALUES[chars[0x16]] << 15;
        random1 |= (long) ALPHABET_VALUES[chars[0x17]] << 10;
        random1 |= (long) ALPHABET_VALUES[chars[0x18]] << 5;
        random1 |= (long) ALPHABET_VALUES[chars[0x19]];

        final byte[] bytes = new byte[RANDOM_BYTES];

        bytes[0x0] = (byte) (random0 >>> 32);
        bytes[0x1] = (byte) (random0 >>> 24);
        bytes[0x2] = (byte) (random0 >>> 16);
        bytes[0x3] = (byte) (random0 >>> 8);
        bytes[0x4] = (byte) (random0);

        bytes[0x5] = (byte) (random1 >>> 32);
        bytes[0x6] = (byte) (random1 >>> 24);
        bytes[0x7] = (byte) (random1 >>> 16);
        bytes[0x8] = (byte) (random1 >>> 8);
        bytes[0x9] = (byte) (random1);

        return bytes;
    }

    /**
     * Returns the most significant bits as a number.
     *
     * @return a number.
     */
    public long getMostSignificantBits() {
        return this.msb;
    }

    /**
     * Returns the least significant bits as a number.
     *
     * @return a number.
     */
    public long getLeastSignificantBits() {
        return this.lsb;
    }

    /**
     * Returns a new ULID by incrementing the random component of the current ULID.
     * <p>
     * Since the random component contains 80 bits:
     * <ul>
     * <li>(1) This method can generate up to 1208925819614629174706176 (2^80) ULIDs
     * per millisecond;
     * <li>(2) This method can generate monotonic increasing ULIDs
     * 99.999999999999992% ((2^80 - 10^9) / (2^80)) of the time within a single
     * millisecond interval, considering an unrealistic rate of 1,000,000,000 ULIDs
     * per millisecond.
     * </ul>
     * <p>
     * Due to (1) and (2), it does not throw the error message recommended by the
     * specification. When an overflow occurs in the random 80 bits, the time
     * component is simply incremented to <b>maintain monotonicity</b>.
     *
     * @return a ULID
     */
    public Ulid increment() {

        long newMsb = this.msb;
        long newLsb = this.lsb + 1; // increment the LEAST significant bits

        if (newLsb == INCREMENT_OVERFLOW) {
            newMsb += 1; // increment the MOST significant bits
        }

        return new Ulid(newMsb, newLsb);
    }

    /**
     * Checks if the input string is valid.
     * <p>
     * The input string must be 26 characters long and must contain only characters
     * from Crockford's base 32 alphabet.
     * <p>
     * The first character of the input string must be between 0 and 7.
     *
     * @param string a canonical string
     * @return true if the input string is valid
     * @see <a href="https://www.crockford.com/base32.html">Crockford's Base 32</a>
     */
    public static boolean isValid(String string) {
        return string != null && isValidCharArray(string.toCharArray());
    }

    /**
     * Returns a hash code value for the ULID.
     */
    @Override
    public int hashCode() {
        final long bits = msb ^ lsb;
        return (int) (bits ^ (bits >>> 32));
    }

    /**
     * Checks if some other ULID is equal to this one.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (other.getClass() != Ulid.class)
            return false;
        Ulid that = (Ulid) other;
        if (lsb != that.lsb)
            return false;
        else if (msb != that.msb)
            return false;
        return true;
    }

    /**
     * Compares two ULIDs as unsigned 128-bit integers.
     * <p>
     * The first of two ULIDs is greater than the second if the most significant
     * byte in which they differ is greater for the first ULID.
     *
     * @param that a ULID to be compared with
     * @return -1, 0 or 1 as {@code this} is less than, equal to, or greater than
     * {@code that}
     */
    @Override
    public int compareTo(Ulid that) {

        // used to compare as UNSIGNED longs
        final long min = 0x8000000000000000L;

        final long a = this.msb + min;
        final long b = that.msb + min;

        if (a > b)
            return 1;
        else if (a < b)
            return -1;

        final long c = this.lsb + min;
        final long d = that.lsb + min;

        if (c > d)
            return 1;
        else if (c < d)
            return -1;

        return 0;
    }

    String toString(char[] alphabet) {

        final char[] chars = new char[ULID_CHARS];

        long time = this.msb >>> 16;
        long random0 = ((this.msb & 0xffffL) << 24) | (this.lsb >>> 40);
        long random1 = (this.lsb & 0xffffffffffL);

        chars[0x00] = alphabet[(int) (time >>> 45 & 0b11111)];
        chars[0x01] = alphabet[(int) (time >>> 40 & 0b11111)];
        chars[0x02] = alphabet[(int) (time >>> 35 & 0b11111)];
        chars[0x03] = alphabet[(int) (time >>> 30 & 0b11111)];
        chars[0x04] = alphabet[(int) (time >>> 25 & 0b11111)];
        chars[0x05] = alphabet[(int) (time >>> 20 & 0b11111)];
        chars[0x06] = alphabet[(int) (time >>> 15 & 0b11111)];
        chars[0x07] = alphabet[(int) (time >>> 10 & 0b11111)];
        chars[0x08] = alphabet[(int) (time >>> 5 & 0b11111)];
        chars[0x09] = alphabet[(int) (time & 0b11111)];

        chars[0x0a] = alphabet[(int) (random0 >>> 35 & 0b11111)];
        chars[0x0b] = alphabet[(int) (random0 >>> 30 & 0b11111)];
        chars[0x0c] = alphabet[(int) (random0 >>> 25 & 0b11111)];
        chars[0x0d] = alphabet[(int) (random0 >>> 20 & 0b11111)];
        chars[0x0e] = alphabet[(int) (random0 >>> 15 & 0b11111)];
        chars[0x0f] = alphabet[(int) (random0 >>> 10 & 0b11111)];
        chars[0x10] = alphabet[(int) (random0 >>> 5 & 0b11111)];
        chars[0x11] = alphabet[(int) (random0 & 0b11111)];

        chars[0x12] = alphabet[(int) (random1 >>> 35 & 0b11111)];
        chars[0x13] = alphabet[(int) (random1 >>> 30 & 0b11111)];
        chars[0x14] = alphabet[(int) (random1 >>> 25 & 0b11111)];
        chars[0x15] = alphabet[(int) (random1 >>> 20 & 0b11111)];
        chars[0x16] = alphabet[(int) (random1 >>> 15 & 0b11111)];
        chars[0x17] = alphabet[(int) (random1 >>> 10 & 0b11111)];
        chars[0x18] = alphabet[(int) (random1 >>> 5 & 0b11111)];
        chars[0x19] = alphabet[(int) (random1 & 0b11111)];

        return new String(chars);
    }

    static char[] toCharArray(String string) {
        char[] chars = string == null ? null : string.toCharArray();
        if (!isValidCharArray(chars)) {
            throw new IllegalArgumentException(String.format("Invalid ULID: \"%s\"", string));
        }
        return chars;
    }

    /*
     * Checks if the string is a valid ULID.
     *
     * A valid ULID string is a sequence of 26 characters from Crockford's Base 32
     * alphabet.
     *
     * The first character of the input string must be between 0 and 7.
     */
    static boolean isValidCharArray(final char[] chars) {

        if (chars == null || chars.length != ULID_CHARS) {
            return false; // null or wrong size!
        }

        for (int i = 0; i < chars.length; i++) {
            try {
                if (ALPHABET_VALUES[chars[i]] == -1) {
                    return false; // invalid character!
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return false; // Multibyte character!
            }
        }

        // The time component has 48 bits.
        // The base32 encoded time component has 50 bits.
        // The time component cannot be greater than than 2^48-1.
        // So the 2 first bits of the base32 decoded time component must be ZERO.
        // As a consequence, the 1st char of the input string must be between 0 and 7.
        if ((ALPHABET_VALUES[chars[0]] & 0b11000) != 0) {
            // ULID specification:
            // "Any attempt to decode or encode a ULID larger than this (time > 2^48-1)
            // should be rejected by all implementations, to prevent overflow bugs."
            return false; // time overflow!
        }

        return true; // It seems to be OK.
    }
}