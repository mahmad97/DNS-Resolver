import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

/** A resource record corresponds to each individual result returned by a DNS response. It links
 * a DNS node (host name and record type) to either an IP address (e.g., A or AAAA records) or
 * a textual response (e.g., CNAME or NS records). A TTL (time-to-live) field is also specified,
 * and is represented by an expiration time calculated as a delta from the current time.
 */
public class ResourceRecord implements Serializable {

    private DNSNode node;
    private Date expirationTime;
    private String textResult;
    private InetAddress inetResult;

    public ResourceRecord(String hostName, RecordType type, long ttl, String result) {
        this.node = new DNSNode(hostName, type);
        this.expirationTime = new Date(System.currentTimeMillis() + (ttl * 1000));
        this.textResult = result;
        this.inetResult = null;
    }

    public ResourceRecord(String hostName, RecordType type, long ttl, InetAddress result) {
        this(hostName, type, ttl, result.getHostAddress());
        this.inetResult = result;
    }

    public DNSNode getNode() {
        return node;
    }

    public String getHostName() {
        return node.getHostName();
    }

    public RecordType getType() {
        return node.getType();
    }

    /** The TTL for this record. It is returned based on the (ceiling of the) number of seconds
     * remaining until this record expires. The TTL returned by this method will only match the
     * TTL obtained from the DNS server in the first second from the time this record was
     * created.
     *
     * @return The number of seconds, rounded up, until this record expires.
     */
    public long getTTL() {
        return (expirationTime.getTime() - System.currentTimeMillis() + 999) / 1000;
    }

    /** Returns true if this record has not expired yet, and false otherwise. An expired record
     * should not be maintained in cache, and should instead be retrieved again from an
     * authoritative DNS server.
     *
     * @return true if this record has not expired yet, and false otherwise.
     */
    public boolean isStillValid() {
        return expirationTime.after(new Date());
    }

    /** Returns true if this record expires before another record. This method may be used to
     * identify if a newly acquired record should replace the one currently in the cache. It
     * may also potentially be used, for example, to identify if a CNAME record expires before
     * the equivalent A record it links to.
     *
     * @param record Another resource record whose expiration this record should be compared with.
     * @return true if this record expires before the parameter record, or false otherwise.
     */
    public boolean expiresBefore(ResourceRecord record) {
        return this.expirationTime.before(record.expirationTime);
    }

    public String getTextResult() {
        return textResult;
    }

    public InetAddress getInetResult() {
        return inetResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceRecord record = (ResourceRecord) o;

        if (!node.equals(record.node)) return false;
        if (!textResult.equals(record.textResult)) return false;
        return inetResult != null ? inetResult.equals(record.inetResult) : record.inetResult == null;
    }

    @Override
    public int hashCode() {
        int result = node.hashCode();
        result = 31 * result + textResult.hashCode();
        return result;
    }
}
