# DNS-Resolver
This is a small tool which takes in a Fully Qualified Domain Name and then queries the nameserver it was provided with as an argument iteratively or recursively (can be changed from inside the code, currently set to iterative), and outputs the IP addresses orresponding to that FQDN.

Maintains a cache to make querying effecient, see README.txt for limitations.
Use the trace command to get details of each query made and the response of the query from the respective nameserver.
