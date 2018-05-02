'''
    This script will query the zipkin server specified by the endpoint for the
    latest trace, and extract the endpoints. The script will then construct the
    powerset of these endpoints, and perform delay tests for all of these set
    items. 
'''

import requests
import sys

from itertools import *


if len(sys.argv)>2:
    app_ip = sys.argv[1] 
    port=sys.argv[2]
else:
    app_ip = "localhost"
    port = "7000"



zipkin_ip = "localhost" 
zipkin_port="9411"


#target request
request = "/app/test" 
base_url = "http://" + app_ip + ":" + port 
url = base_url + request 

zipkin_url = "http://" + zipkin_ip + ":" + zipkin_port  


def get_latest_spans() : 
    res = requests.get(zipkin_url + '/api/v2/traces')
    
    services = res.json()
    latestTrace = services[0]

    endpoints = set()

    for spans in latestTrace:
        #get the endpoint

        endpoint = spans['tags']['http.path']
        endpoints.add(endpoint)

    #for endpoint in endpoints:
    #    print endpoint

    return endpoints 


def construct_powerset(endpoints):
    s = list(endpoints)
    
    powerset =  chain.from_iterable(combinations(s, r) for r in range(1, len(s)+1))
    for i in powerset:
        print i

    return powerset 

   
def get_power_set(endpoints):

    power_set = [set()]

    for element in endpoints:
        one_element_set = {element}
        power_set += [subset | one_element_set for subset in power_set]

    return power_set




def delay_powerset(powerset):
    delays = {100, 200, 300}


    #remove the empty set from the list
    powerset.pop(0)

    test = powerset[-1]
    print test


    #for endpoint in test:
    #    print endpoint
    for delay in delays:

        for myset in powerset:
            header_val = ""
            i = 0
            #convert to list so that we can add delimiters
            mylist = list(myset)
            while i < len(mylist):
                header_val += mylist[i]
                header_val += "=DELAY:" + str(delay) 
                i = i+1
                if i == len(mylist):
                    break
                header_val += "|"
            print header_val  
            headers = {'InjectFault': header_val}
            res = requests.post(url, headers = headers)
       


endpoints = get_latest_spans()
powerset = get_power_set(endpoints)
delay_powerset(powerset)



