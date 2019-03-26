CNAME, NS, A and AAAA records follow the format in the specs
and additional documents.
Was confused about SOA, and MX.

So, it skips the data portion in SOA and saves nothing.
For MX it stores the data in the format:
	(Preference) (space) (Mail Exchange)
Only ran on one test query:
l www.cs.ubc.ca MX

so it may fail for others, or not.
Who knows.