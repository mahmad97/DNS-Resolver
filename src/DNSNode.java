import java.io.Serializable;

/** DNS nodes can be used to specify an individual DNS query or the key to a specific result.
 * Each node represents a fully-qualified domain name (represented by hostName) and a record
 * type. Two nodes with the same host name and type are considered equal.
 */
public class DNSNode implements Comparable<DNSNode>, Serializable {

    private String hostName;
    private RecordType type;

    public DNSNode(String hostName, RecordType type) {
        this.hostName = hostName;
        this.type = type;
    }

    public String getHostName() {
        return hostName;
    }

    public RecordType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DNSNode dnsNode = (DNSNode) o;

        if (!hostName.equals(dnsNode.hostName)) return false;
        return type == dnsNode.type;
    }

    @Override
    public int hashCode() {
        int result = hostName.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return hostName + " (" + type + ")";
    }

    @Override
    public int compareTo(DNSNode o) {
        if (!hostName.equalsIgnoreCase(o.hostName))
            return hostName.compareToIgnoreCase(o.hostName);
        else
            return type.compareTo(o.type);
    }
}
