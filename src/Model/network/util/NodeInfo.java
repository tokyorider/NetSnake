package Model.network.util;

import java.net.InetAddress;
import java.util.Date;

public class NodeInfo {
    public InetAddress address;

    //Last time we sent something to node
    public Date lastOurActivity;

    //Last time node sent something to us
    public Date lastTheirActivity;

    public NodeInfo(InetAddress address, Date lastOurActivity, Date lastTheirActivity) {
        this.address = address;
        this.lastOurActivity = lastOurActivity;
        this.lastTheirActivity = lastTheirActivity;
    }
}
