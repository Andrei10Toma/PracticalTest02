package ro.pub.cs.systems.eim.practicaltest02;

import java.sql.Timestamp;

public class CoinInformation {
    public String value;
    public long updateTimestamp;

    public CoinInformation(String value, long updateTimestamp) {
        this.value = value;
        this.updateTimestamp = updateTimestamp;
    }
}
