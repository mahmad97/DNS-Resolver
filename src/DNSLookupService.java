import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DNSLookupService {

    private static final int DEFAULT_DNS_PORT = 53;
    private static final int MAX_INDIRECTION_LEVEL = 10;

    private static InetAddress rootServer;
    private static boolean verboseTracing = false;
    private static DatagramSocket socket;
    private static int mainPointer = 0;
    private static int curIndLvl = 0;

    private static DNSCache cache = DNSCache.getInstance();

    private static Random random = new Random();

    /**
     * Main function, called when program is first invoked.
     *
     * @param args list of arguments specified in the command line.
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Invalid call. Usage:");
            System.err.println("\tjava -jar DNSLookupService.jar rootServer");
            System.err.println("where rootServer is the IP address (in dotted form) of the root DNS server to start the search at.");
            System.exit(1);
        }

        try {
            rootServer = InetAddress.getByName(args[0]);
            System.out.println("Root DNS server is: " + rootServer.getHostAddress());
        } catch (UnknownHostException e) {
            System.err.println("Invalid root server (" + e.getMessage() + ").");
            System.exit(1);
        }

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);
        } catch (SocketException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        Scanner in = new Scanner(System.in);
        Console console = System.console();
        do {
            // Use console if one is available, or standard input if not.
            String commandLine;
            if (console != null) {
                System.out.print("DNSLOOKUP> ");
                commandLine = console.readLine();
            } else
                try {
                    commandLine = in.nextLine();
                } catch (NoSuchElementException ex) {
                    break;
                }
            // If reached end-of-file, leave
            if (commandLine == null) break;

            // Ignore leading/trailing spaces and anything beyond a comment character
            commandLine = commandLine.trim().split("#", 2)[0];

            // If no command shown, skip to next command
            if (commandLine.trim().isEmpty()) continue;

            String[] commandArgs = commandLine.split(" ");

            if (commandArgs[0].equalsIgnoreCase("quit") ||
                    commandArgs[0].equalsIgnoreCase("exit"))
                break;
            else if (commandArgs[0].equalsIgnoreCase("server")) {
                // SERVER: Change root nameserver
                if (commandArgs.length == 2) {
                    try {
                        rootServer = InetAddress.getByName(commandArgs[1]);
                        System.out.println("Root DNS server is now: " + rootServer.getHostAddress());
                    } catch (UnknownHostException e) {
                        System.out.println("Invalid root server (" + e.getMessage() + ").");
                        continue;
                    }
                } else {
                    System.out.println("Invalid call. Format:\n\tserver IP");
                    continue;
                }
            } else if (commandArgs[0].equalsIgnoreCase("trace")) {
                // TRACE: Turn trace setting on or off
                if (commandArgs.length == 2) {
                    if (commandArgs[1].equalsIgnoreCase("on"))
                        verboseTracing = true;
                    else if (commandArgs[1].equalsIgnoreCase("off"))
                        verboseTracing = false;
                    else {
                        System.err.println("Invalid call. Format:\n\ttrace on|off");
                        continue;
                    }
                    System.out.println("Verbose tracing is now: " + (verboseTracing ? "ON" : "OFF"));
                } else {
                    System.err.println("Invalid call. Format:\n\ttrace on|off");
                    continue;
                }
            } else if (commandArgs[0].equalsIgnoreCase("lookup") ||
                    commandArgs[0].equalsIgnoreCase("l")) {
                // LOOKUP: Find and print all results associated to a name.
                RecordType type;
                if (commandArgs.length == 2)
                    type = RecordType.A;
                else if (commandArgs.length == 3)
                    try {
                        type = RecordType.valueOf(commandArgs[2].toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        System.err.println("Invalid query type. Must be one of:\n\tA, AAAA, NS, MX, CNAME");
                        continue;
                    }
                else {
                    System.err.println("Invalid call. Format:\n\tlookup hostName [type]");
                    continue;
                }
                findAndPrintResults(commandArgs[1], type);
            } else if (commandArgs[0].equalsIgnoreCase("dump")) {
                // DUMP: Print all results still cached
                cache.forEachNode(DNSLookupService::printResults);
            } else {
                System.err.println("Invalid command. Valid commands are:");
                System.err.println("\tlookup fqdn [type]");
                System.err.println("\ttrace on|off");
                System.err.println("\tserver IP");
                System.err.println("\tdump");
                System.err.println("\tquit");
                continue;
            }

        } while (true);

        socket.close();
        System.out.println("Goodbye!");
    }

    /**
     * Finds all results for a host name and type and prints them on the standard output.
     *
     * @param hostName Fully qualified domain name of the host being searched.
     * @param type     Record type for search.
     */
    private static void findAndPrintResults(String hostName, RecordType type) {

        DNSNode node = new DNSNode(hostName, type);
        printResults(node, getResults(node, 0));
    }

    /**
     * Finds all the result for a specific node.
     *
     * @param node             Host and record type to be used for search.
     * @param indirectionLevel Control to limit the number of recursive calls due to CNAME redirection.
     *                         The initial call should be made with 0 (zero), while recursive calls for
     *                         regarding CNAME results should increment this value by 1. Once this value
     *                         reaches MAX_INDIRECTION_LEVEL, the function prints an error message and
     *                         returns an empty set.
     * @return A set of resource records corresponding to the specific query requested.
     */
    private static Set<ResourceRecord> getResults(DNSNode node, int indirectionLevel) {

        if (indirectionLevel > MAX_INDIRECTION_LEVEL) {
            System.err.println("Maximum number of indirection levels reached.");
            return Collections.emptySet();
        }

        // TODO To be completed by the student
        // reset the global variable curIndLvl if its a fresh search, it wont change otherwise
        curIndLvl = indirectionLevel;

        // check cache for record, if not found commence query
        if (cache.getCachedResults(node).isEmpty()) {
            retrieveResultsFromServer(node, rootServer);
        }

        return cache.getCachedResults(node);
    }

    /**
     * Retrieves DNS results from a specified DNS server. Queries are sent in iterative mode,
     * and the query is repeated with a new server if the provided one is non-authoritative.
     * Results are stored in the cache.
     *
     * @param node   Host name and record type to be used for the query.
     * @param server Address of the server to be used for the query.
     */
    private static void retrieveResultsFromServer(DNSNode node, InetAddress server) {
        // TODO To be completed by the student
        if (curIndLvl > MAX_INDIRECTION_LEVEL) {
            return;
        }
        // Make a ByteArrayOutputStream to construct the DNSQuery in
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        int Transaction_ID = random.nextInt(65536);
        try {
            // Transaction ID
            dos.writeShort(Transaction_ID);
            // Flags (always standard query)
            dos.writeShort(0x0000);
            // Questions (always 1 question)
            dos.writeShort(0x0001);
            // Answer RRs (always 0)
            dos.writeShort(0x0000);
            // Authority RRs (always 0)
            dos.writeShort(0x0000);
            // Additional RRs (always 0)
            dos.writeShort(0x0000);

            // Question
            // QNAME
            String[] labels = node.getHostName().split("[.]");
            for (String label : labels) {
                byte[] labelBytes = label.getBytes(StandardCharsets.UTF_8);
                dos.writeByte(labelBytes.length);
                dos.write(labelBytes);
            }
            // Terminate QNAME
            dos.writeByte(0x00);
            // QTYPE
            dos.writeShort(node.getType().getCode());
            // QCLASS (always IN, always 1)
            dos.writeShort(0x0001);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Put the DNSQuery (constructed in the ByteArrayOutputStream) into a byte array to be able to send it
        byte[] DNSQuery = baos.toByteArray();

        // Sending DNS Request using UDP
        try {
            socket.setSoTimeout(5000);
            DatagramPacket DNSQueryPkt = new DatagramPacket(DNSQuery, DNSQuery.length, server, DEFAULT_DNS_PORT);
            socket.send(DNSQueryPkt);
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (verboseTracing) {
            System.out.println("\n");
            System.out.printf("%-12s %d %s  %s %s %s\n", "Query ID", Transaction_ID, node.getHostName(), node.getType(), "-->", server.getHostAddress());
        }

        // Get response from DNS server and put it into a byte array
        byte[] DNSResponse = new byte[1024];
        try {
            DatagramPacket DNSResponsePkt = new DatagramPacket(DNSResponse, DNSResponse.length);
            socket.receive(DNSResponsePkt);
        } catch (SocketTimeoutException e) {
            return;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Reset the global pointer before starting reading the message
        mainPointer = 12;

        // Read the DNSResponsePkt
        // Read the Header
        int RCODE = 0, Authoritative = 0, Questions, AnswerRRs, AuthorityRRs, AdditionalRRs;
        Transaction_ID = (((DNSResponse[0] & 0xFF) << 8) + (DNSResponse[1] & 0xFF));
        Authoritative = (DNSResponse[2] & 0x04) >>> 2;
        RCODE = (DNSResponse[3] & 0x0F);
        Questions = (((DNSResponse[4] & 0xFF) << 8) + (DNSResponse[5] & 0xFF)); // Always 1
        AnswerRRs = (((DNSResponse[6] & 0xFF) << 8) + (DNSResponse[7] & 0xFF));
        AuthorityRRs = (((DNSResponse[8] & 0xFF) << 8) + (DNSResponse[9] & 0xFF));
        AdditionalRRs = (((DNSResponse[10] & 0xFF) << 8) + (DNSResponse[11] & 0xFF));

        if (verboseTracing) {
            System.out.printf("%-12s %d %s %s\n", "Response ID:", Transaction_ID, "Authoritative =", (Authoritative == 1 ? "true" : "false"));
        }

        // Read the Queries (always 1)
        for (int i = 0; i < Questions; i++) {
            StringBuilder QNAME;
            int QTYPE, QCLASS, pointer = mainPointer;

            QNAME = readName(DNSResponse, pointer, true);
            QTYPE = (((DNSResponse[mainPointer] & 0xFF) << 8) + (DNSResponse[mainPointer + 1] & 0xFF));
            mainPointer = mainPointer + 2;
            QCLASS = (((DNSResponse[mainPointer] & 0xFF) << 8) + (DNSResponse[mainPointer + 1] & 0xFF));
            mainPointer = mainPointer + 2;
        }

        // Read the Answers
        if (verboseTracing) {
            System.out.println("  Answers (" + AnswerRRs + ")");
        }
        String[] canonicalName = new String[AnswerRRs];
        for (int i = 0; i < AnswerRRs; i++) {
            ResourceRecord record;
            record = readRecord(DNSResponse);
            verbosePrintResourceRecord(record, record.getType().getCode());

            if (record.getType().getCode() == 5) {
                canonicalName[i] = record.getTextResult();
            } else {
                canonicalName[i] = null;
            }

            cache.addResult(record);
        }

        // Read the Authoritative nameservers
        if (verboseTracing) {
            System.out.println("  Nameservers (" + AuthorityRRs + ")");
        }
        String[] nameServers = new String[AuthorityRRs];
        for (int i = 0; i < AuthorityRRs; i++) {
            ResourceRecord record;
            record = readRecord(DNSResponse);
            verbosePrintResourceRecord(record, record.getType().getCode());

            if (record.getType().getCode() == 2) {
                nameServers[i] = record.getTextResult();
            } else {
                nameServers[i] = null;
            }

            cache.addResult(record);
        }

        // Read the Additional records
        if (verboseTracing) {
            System.out.println("  Additional Information (" + AdditionalRRs + ")");
        }
        for (int i = 0; i < AdditionalRRs; i++) {
            ResourceRecord record;
            record = readRecord(DNSResponse);
            verbosePrintResourceRecord(record, record.getType().getCode());

            cache.addResult(record);
        }

        // If answer not found (and name servers are returned) query the returned name servers
        if ((AnswerRRs == 0) && (AuthorityRRs > 0) && (RCODE == 0)) {
            DNSNode temp; Set<ResourceRecord> buffer; ResourceRecord[] bufferarray;
            for (int i = 0; i < AuthorityRRs; i++) {
                if (nameServers[i] != null){
                    buffer = getResults(new DNSNode(nameServers[i], RecordType.A), curIndLvl);
                    if (buffer.isEmpty()) {
                        buffer = getResults(new DNSNode(nameServers[i], RecordType.AAAA), curIndLvl);
                    }
                    if (buffer.isEmpty()) {
                        continue;
                    }
                    bufferarray = buffer.toArray(new ResourceRecord[0]);
                    retrieveResultsFromServer(node, bufferarray[0].getInetResult());
                    if (!cache.getCachedResults(node).isEmpty()) {
                        break;
                    }
                }
            }
        }

        // If returned answer is a CNAME, repeat the whole query for CNAME
        for (int i = 0; (i < AnswerRRs) && (RCODE == 0); i++) {
            if (canonicalName[i] != null) {
                if (curIndLvl > MAX_INDIRECTION_LEVEL) {
                    break;
                }
                curIndLvl++;
                DNSNode CNnode = new DNSNode(canonicalName[i], node.getType());
                Set<ResourceRecord> CRecords = getResults(CNnode, curIndLvl);
                if (!CRecords.isEmpty()) {
                    for (ResourceRecord record: CRecords) {
                        cache.addResult(new ResourceRecord(node.getHostName(), node.getType(), record.getTTL(), record.getTextResult()));
                    }
                    break;
                }
            }
        }
    }

    /**
     * Reads a name contained in the DNSResponse message.
     *
     * @param DNSResponse   The byte array containing the DNSResponse message.
     * @param pointer   int value telling the location to read from from the DNSResponse array.
     * @param incMain   boolean value telling whether we are to increment the global pointer
     *                  depending on if we have been incurred a pointer while reading the name.
     * @return  The name read in the StringBuilder object.
     */
    private static StringBuilder readName(byte [] DNSResponse, int pointer, boolean incMain) {
        StringBuilder name = new StringBuilder();

        while (true) {
            if ((DNSResponse[pointer] & 0xFF) >= 192) {
                int goTo = ((DNSResponse[pointer] & 0x3F) << 8) + (DNSResponse[pointer + 1] & 0xFF);
                if (incMain) { mainPointer = mainPointer + 2; }
                return name.append(readName(DNSResponse, goTo, false));
            } else if ((DNSResponse[pointer] & 0xFF) == 0){
                if (incMain) { mainPointer++; }
                break;
            } else {
                int labelLength = (DNSResponse[pointer++] & 0xFF);
                if (incMain) {
                    mainPointer++;
                }
                for (int i = 0; i < labelLength; i++) {
                    name.append((char) (DNSResponse[pointer++] & 0xFF));
                    if (incMain) {
                        mainPointer++;
                    }
                }
                if ((DNSResponse[pointer] & 0xFF) != 0) { name.append("."); }
            }
        }

        return name;
    }

    /**
     * Reads a Resourse Record contained in a DNSResponse message.
     *
     * @param DNSResponse   The byte array containing the DNSResponse message.
     * @return  The Record which was read.
     */
    private static ResourceRecord readRecord(byte [] DNSResponse) {
        ResourceRecord record; String hostName; RecordType type; long ttl;
        int RCLASS; int dataLength;

        StringBuilder name; int pointer = mainPointer;
        name = readName(DNSResponse, pointer, true);
        hostName = name.toString();

        type = RecordType.getByCode((((DNSResponse[mainPointer] & 0xFF) << 8) + (DNSResponse[mainPointer + 1] & 0xFF)));
        mainPointer = mainPointer + 2;

        RCLASS = ((DNSResponse[mainPointer] & 0xFF) << 8) + (DNSResponse[mainPointer + 1] & 0xFF);
        mainPointer = mainPointer + 2;

        ttl = ((DNSResponse[mainPointer] & 0xFF) << 24) + ((DNSResponse[mainPointer + 1] & 0xFF) << 16) + ((DNSResponse[mainPointer + 2] & 0xFF) << 8) + (DNSResponse[mainPointer + 3] & 0xFF);
        mainPointer = mainPointer + 4;

        dataLength = ((DNSResponse[mainPointer] & 0xFF) << 8) + (DNSResponse[mainPointer + 1] & 0xFF);
        mainPointer = mainPointer + 2;

        StringBuilder data = new StringBuilder();
        if (type.getCode() == 1) {
            for (int i = 0; i < 4; i++) {
                data.append(DNSResponse[mainPointer] & 0xFF);
                mainPointer++;
                if (i != 3) { data.append("."); }
            }
        } else if (type.getCode() == 28) {
            for (int i = 0; i < 8; i++) {
                data.append(Integer.toHexString(((DNSResponse[mainPointer] & 0xFF) << 8) + (DNSResponse[mainPointer + 1] & 0xFF)));
                mainPointer = mainPointer + 2;
                if (i != 7) { data.append(":"); }
            }
        } else if (type.getCode() == 2 || type.getCode() == 5) {
            pointer = mainPointer;
            data = readName(DNSResponse, pointer, true);
        } else if (type.getCode() == 15) {
            data.append(((DNSResponse[mainPointer] & 0xFF) << 8) + (DNSResponse[mainPointer + 1] & 0xFF));
            mainPointer = mainPointer + 2;
            data.append(" ");
            pointer = mainPointer;
            data.append(readName(DNSResponse, pointer, true));
        } else {
            mainPointer = mainPointer + dataLength;
        }

        if (type.getCode() == 1 || type.getCode() == 28) {
            try {
                record = new ResourceRecord(hostName, type, ttl, InetAddress.getByName(data.toString()));
                return record;
            } catch (UnknownHostException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        record = new ResourceRecord(hostName, type, ttl, data.toString());
        return record;
    }

    /**
     * Prints a Resource Record, for use with verbose tracing.
     *
     * @param record    The resource record being printed.
     * @param rtype The type of the record being preinted
     */
    private static void verbosePrintResourceRecord(ResourceRecord record, int rtype) {
        if (verboseTracing)
            System.out.format("       %-30s %-10d %-4s %s\n", record.getHostName(),
                    record.getTTL(),
                    record.getType() == RecordType.OTHER ? rtype : record.getType(),
                    record.getTextResult());
    }

    /**
     * Prints the result of a DNS query.
     *
     * @param node    Host name and record type used for the query.
     * @param results Set of results to be printed for the node.
     */
    private static void printResults(DNSNode node, Set<ResourceRecord> results) {
        if (results.isEmpty())
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), -1, "0.0.0.0");
        for (ResourceRecord record : results) {
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), record.getTTL(), record.getTextResult());
        }
    }
}
