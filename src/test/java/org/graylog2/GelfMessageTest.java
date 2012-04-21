package org.graylog2;

import me.moocar.logbackgelf.SyslogLevel;
import org.json.simple.JSONValue;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;

public class GelfMessageTest {

    @Test
    public void testAdditionalFieldsIds() throws Exception {
        final GelfMessage message = new GelfMessage();
      message.setShortMessage( "Short" );
      message.setFullMessage( "Long" );
      message.setJavaTimestamp( System.currentTimeMillis() );
      message.setLevel( SyslogLevel.ALERT );
      message.getAdditionalFields().put( "id", "LOLCAT" );

      message.getAdditionalFields().put( "id", "LOLCAT" );

      message.getAdditionalFields().put("id", "LOLCAT");
      message.getAdditionalFields().put("_id", "typos in my closet");

        String data = message.toJson();
        Map resultingMap = (Map) JSONValue.parse(data);
        assertNull(resultingMap.get("_id"));
        assertNotNull(resultingMap.get("__id"));
    }

    @Test
    public void testSendLongMessage() throws Exception {
        String longString = "01234567890123456789 ";
        for (int i = 0; i < 15; i++) {
            longString += longString;
        }
        final GelfMessage message = new GelfMessage();
        message.setShortMessage( "Short" );
        message.setFullMessage( longString );
        message.setJavaTimestamp( System.currentTimeMillis() );
        message.setLevel( SyslogLevel.ALERT );
        List<byte[]> bytes2 = message.toDatagrams();
        assertEquals(2, bytes2.size());
        assertTrue(Arrays.equals(Arrays.copyOfRange(bytes2.get(0), 10, 11), new byte[] {0x00}));
        assertTrue(Arrays.equals(Arrays.copyOfRange(bytes2.get(0), 11, 12), new byte[] {0x02}));
        assertTrue(Arrays.equals(Arrays.copyOfRange(bytes2.get(1), 10, 11), new byte[] {0x01}));
        assertTrue(Arrays.equals(Arrays.copyOfRange(bytes2.get(1), 11, 12), new byte[] {0x02}));
    }

    @Test
    public void testSimpleMessage() throws Exception {
        final GelfMessage message = new GelfMessage();
        message.setShortMessage( "Short" );
        message.setFullMessage( "Long" );
        message.setJavaTimestamp( System.currentTimeMillis() );
        message.setLevel( SyslogLevel.ALERT );
        List<byte[]> bytes = message.toDatagrams();
        assertEquals(1, bytes.size());
    }

    @Test
    public void testAdditionalFields() throws Exception {
        GelfMessage message = new GelfMessage();
        message.setJavaTimestamp(1L);
      message.getAdditionalFields().put( "one", "two" );

      message.getAdditionalFields().put( "one", "two" );

      message.getAdditionalFields().put("one", "two");
      message.getAdditionalFields().put("three", 4);
      message.getAdditionalFields().put("five", 6.0);
      message.getAdditionalFields().put("seven",8);

        String json = message.toJson();

        Map resultingMap = (Map) JSONValue.parse(json);

        assertEquals(resultingMap.get("_one"), "two");
        assertEquals(resultingMap.get("_three"), 4L);
        assertEquals(resultingMap.get("_five"), 6.0);
        assertEquals(resultingMap.get("_seven"), 8L);
    }

    @Test
    public void generateBallastMessage() {
        Date date = new Date();
    }
}
