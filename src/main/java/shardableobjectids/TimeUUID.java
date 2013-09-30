package shardableobjectids;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

import com.eaio.util.lang.Hex;
import com.eaio.uuid.UUIDGen;

/**
 * 
 * Copyright Georg Koester 2012. Licensed under Apache License 2.0
 *
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

/**
 * 
 */
public class TimeUUID {

    /**
     * Only a helper.
     */
    private TimeUUID() {

    }

    /**
     * Gets a new object id.
     * 
     * @return the new id
     */
    public static java.util.UUID get() {
        return new java.util.UUID(UUIDGen.newTime(),
                UUIDGen.getClockSeqAndNode());
    }

    public static UUID get(Date time, Random rand) {
        return TimeUUIDUtils.createForGivenTimeAndRand(time.getTime() * 10000,
                rand);
    }

    public static String getAsSortableUrlSafeBase64() {
        return TimeUUIDUtils.toSortableUrlSafeBase64(get());
    }

    public static UUID parse(String s) {
        return parse(s, 0, s.length());
    }

    /**
     * Parses standard hex and sortable url safe base64
     * 
     * @param s
     * @param offset
     * @param len
     * @return
     * @see TimeUUIDUtils#toSortableUrlSafeBase64(UUID)
     */
    public static UUID parse(CharSequence s, int offset, int len) {

        // if (!isValid(s, offset, len))
        // throw new IllegalArgumentException("invalid TimeUUID ["
        // + s.subSequence(offset, offset + len) + "]");

        UUID retval = null;
        if (len == 32) {
            long msb = Hex.parseLong(s.subSequence(offset, offset + len));
            long lsb = Hex.parseLong(s.subSequence(offset + 16, offset + 32));
            retval = new UUID(msb, lsb);
        } else if (len == 36) {
            long msb = Hex.parseLong(s.subSequence(offset, offset + len));
            long lsb = Hex.parseLong(s.subSequence(offset + 19, offset + 36));
            retval = new UUID(msb, lsb);
        } else if (len == 22) {
            retval = TimeUUIDUtils.fromSortableUrlSafeBase64(s, offset);
        }

        if (retval == null) {
            throw new IllegalArgumentException(
                    "String not in expected format ["
                            + s.subSequence(offset, offset + len) + "]");
        }
        return retval;
    }

}
