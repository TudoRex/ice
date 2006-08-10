# **********************************************************************
#
# Copyright (c) 2003-2006 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

#
# The test cases for TAO 
#

taoTPConf = " -ORBSvcConf svc.threadPool.conf"
taoTCConf = " -ORBSvcConf svc.threadPerConnection.conf"
taoReactiveConf = " -ORBSvcConf svc.reactive.conf"
taoBlockingConf = " -ORBSvcConf svc.blocking.conf"

productName = "TAO"

# Form
# (productName, (group name, directory, (cases...)))
#
# case form (simple descriptor, unknown, client configuration, server configuration)

testDefinitions = [
	(productName, "latency twoway", "", [
		("1tp", taoReactiveConf + " latency twoway", taoTPConf + " threadPool 1"),
		("4tp", taoReactiveConf + " latency twoway", taoTPConf + " threadPool 4"),
		("tpc", taoReactiveConf + " latency twoway", taoTCConf),
		("tpc blocking", taoBlockingConf + " latency twoway", taoTCConf),
		]
	    ),
	(productName, "latency twoway with 2k payload", "", [
		("1tp", taoReactiveConf + " latency twoway", taoTPConf + " threadPool 1", [("payload", "2000")]),
		("4tp", taoReactiveConf + " latency twoway", taoTPConf + " threadPool 4",  [("payload", "2000")]),
		("tpc", taoReactiveConf + " latency twoway", taoTCConf,  [("payload", "2000")]),
		("tpc blocking", taoBlockingConf + " latency twoway", taoTCConf,  [("payload", "2000")]),
		]
	    ),
	(productName, "latency twoway with 10k payload", "", [
		("1tp", taoReactiveConf + " latency twoway", taoTPConf + " threadPool 1", [("payload", "10000")]),
		("4tp", taoReactiveConf + " latency twoway", taoTPConf + " threadPool 4",  [("payload", "10000")]),
		("tpc", taoReactiveConf + " latency twoway", taoTCConf,  [("payload", "10000")]),
		("tpc blocking", taoBlockingConf + " latency twoway", taoTCConf,  [("payload", "10000")]),
		]
	    ),
	(productName, "latency oneway", "", [
		("1tp", taoReactiveConf + " latency oneway", taoTPConf + " threadPool 1"),
		("4tp", taoReactiveConf + " latency oneway", taoTPConf + " threadPool 4"),
		("tpc", taoReactiveConf + " latency oneway", taoTCConf),
		("tpc blocking", taoBlockingConf + " latency oneway", taoTCConf),
		]
	    ),
	(productName, "latency oneway with 2k payload", "", [
		("1tp", taoReactiveConf + " latency oneway", taoTPConf + " threadPool 1", [("payload", "2000")]),
		("4tp", taoReactiveConf + " latency oneway", taoTPConf + " threadPool 4",  [("payload", "2000")]),
		("tpc", taoReactiveConf + " latency oneway", taoTCConf,  [("payload", "2000")]),
		("tpc blocking", taoBlockingConf + " latency oneway", taoTCConf,  [("payload", "2000")]),
		]
	    ),
	(productName, "latency oneway with 10k payload", "", [
		("1tp", taoReactiveConf + " latency oneway", taoTPConf + " threadPool 1", [("payload", "10000")]),
		("4tp", taoReactiveConf + " latency oneway", taoTPConf + " threadPool 4",  [("payload", "10000")]),
		("tpc", taoReactiveConf + " latency oneway", taoTCConf,  [("payload", "10000")]),
		("tpc blocking", taoBlockingConf + " latency oneway", taoTCConf,  [("payload", "10000")]),
		]
	    ),
	(productName, "latency twoway AMI", "", [
		("1tp", taoReactiveConf + " latency twoway ami", taoTPConf + " threadPool 1"),
		("4tp", taoReactiveConf + " latency twoway ami", taoTPConf + " threadPool 4"),
		("tpc", taoReactiveConf + " latency twoway ami", taoTCConf),
		#("tpc blocking", taoBlockingConf + " latency twoway ami", taoTCConf),
		]
	    ),
	(productName, "latency twoway AMI with 2k payload", "", [
		("1tp", taoReactiveConf + " latency twoway ami", taoTPConf + " threadPool 1", [("payload", "2000")]),
		("4tp", taoReactiveConf + " latency twoway ami", taoTPConf + " threadPool 4", [("payload", "2000")]),
		("tpc", taoReactiveConf + " latency twoway ami", taoTCConf, [("payload", "2000")]),
		]
	    ),
	(productName, "latency twoway AMI with 10k payload", "", [
		("1tp", taoReactiveConf + " latency twoway ami", taoTPConf + " threadPool 1", [("payload", "10000")]),
		("4tp", taoReactiveConf + " latency twoway ami", taoTPConf + " threadPool 4", [("payload", "10000")]),
		("tpc", taoReactiveConf + " latency twoway ami", taoTCConf, [("payload", "10000")]),
		]
	    ),
	(productName, "throughput byte", "", [
		("1tp", taoReactiveConf + " throughput byte", taoTPConf + " threadPool 1"),
		("4tp", taoReactiveConf + " throughput byte", taoTPConf + " threadPool 4"),
		("tpc", taoReactiveConf + " throughput byte", taoTCConf),
		("tpc blocking", taoBlockingConf + " latency throughput byte", taoTCConf),
		]
	    ),
	(productName, "throughput string sequence", "", [
		("1tp", taoReactiveConf + " throughput string", taoTPConf + " threadPool 1"),
		("4tp", taoReactiveConf + " throughput string", taoTPConf + " threadPool 4"),
		("tpc", taoReactiveConf + " throughput string", taoTCConf),
		("tpc blocking", taoBlockingConf + " throughput string", taoTCConf),
		]
	    ),
	(productName, "throughput long string sequence", "", [
		("1tp", taoReactiveConf + " throughput longString", taoTPConf + " threadPool 1"),
		("4tp", taoReactiveConf + " throughput longString", taoTPConf + " threadPool 4"),
		("tpc", taoReactiveConf + " throughput longString", taoTCConf),
		("tpc blocking", taoBlockingConf + " throughput longString", taoTCConf),
		]
	    ),
	(productName, "throughput struct sequence", "", [
		("1tp", taoReactiveConf + " throughput struct", taoTPConf + " threadPool 1"),
		("4tp", taoReactiveConf + " throughput struct", taoTPConf + " threadPool 4"),
		("tpc", taoReactiveConf + " throughput struct", taoTCConf),
		("tpc blocking", taoBlockingConf + " throughput struct", taoTCConf),
		]
	    ),
	]

def getDefinitions():
    return testDefinitions
