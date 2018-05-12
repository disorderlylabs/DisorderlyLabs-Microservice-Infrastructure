#!/usr/bin/python

import boto3
import time
import requests
from random import randint

aws_region = "us-west-1"

instance_filters = [
    {
        'Name' : 'instance-state-name',
        'Values': ['running']
    }
]

service_names = ["app", "inventory", "cart", "invoice", 
            "payment", "inventory_backup", "cart_backup", 
            "invoice_backup", "payment_backup"]

class Service(object):
    name = ""
    instance_id = ""
    status = ""

    def __init__(self, name):
        self.name = name
        self.status = "DOWN"

    def set_instance_id(self, instance_id):
        self.instance_id = instance_id

run = 0
MINUTE = 60
services = []

for name in service_names:
    service = Service(name)
    services.append(service)


def get_running_instances(ec2):
    #filter the running instances
    instances  = ec2.instances.filter(Filters=instance_filters)

    
    return instances 


#this function will filter out the Chaos Monkey instance from the list
def filter_chaosmonkey_instance(instance_list):
    for instance in instance_list:
        tags = instance.tags
        for tag in tags:
            if tag['Value'] == "chaosmonkey":
                instance_list.remove(instance)
    
    return instance_list


def write_to_file(crashed_services):
    #for loggin the crashed services
    with open('logfile.txt', 'a') as logfile:
        logfile.write("run: %3s | " % run)
        
        for service in crashed_services:
            logfile.write(service + "\t")

        logfile.write("\n")
        logfile.flush()
    run = run + 1    


#this function will filter out the new instances spawned in the ASG
#it will then mark the crashed instances accordingly
def filter_new_instances(instance_list):
    
    #first put all instance ids in a set
    #instance_id_set = set()
    instance_id_dict = dict()
    for instance in instance_list:
        #instance_id_set.add(instance.id)
        instance_id_dict[instance.id] = instance.public_dns_name

    crashed_services = []

    #loop through the current services and determine which ones crashed
    for service in services:
        #the service is still up, remove id from set
        #if service.instance_id in instance_id_set:
        #    instance_id_set.remove(service.instance_id)
        if service.instance_id in instance_id_dict:
            del instance_id_dict[service.instance_id]
        else:
            service.status = "DOWN"
            crashed_services.append(service.name)

    write_to_file(crashed_services)

    return instance_id_dict   


#assign ids to old instances
def filter_old_instances(instance_list):
    for instance in instance_list:
        tags = instance.tags 
        for tag in tags:
            if tag['Value'] in service_names:
                service_name = tag['Value']
                for service in services:
                    if service.name == service_name and service.instance_id != instance.id:
                        service.instance_id = instance.id
                        service.status = "UP"
                        print "service: " + service_name + " is up" + "assigning: " + instance.id +"\n"




#assigns new instances to crashed services and tag accordingly
def assign_new_instances(ec2, instance_id_dict):
    #get the key set
    keys = set(instance_id_dict.keys())

    for service in services:
        if service.status == "DOWN":
            new_id = keys.pop()
            del instance_id_dict[new_id] #remove from dict
            service.instance_id = new_id 
            service.status = "UP"

            print "Assigning instance: " + new_id + " to: " + service.name 

            #tag service
            ec2.create_tags(Resources=[new_id], 
                            Tags=[{'Key':'Name', 'Value':service.name}])                


def aws_check():
    session = boto3.Session(region_name=aws_region)
    ec2 = session.resource('ec2', aws_region)

    instances = get_running_instances(ec2)
    instance_list = list(instances)
    instance_list = filter_chaosmonkey_instance(instance_list)

    filter_old_instances(instance_list)
    new_instance_dict = filter_new_instances(instance_list)
    assign_new_instances(ec2, new_instance_dict)



def terminate_ondemand():
    monkey_url = 'http://localhost:8080/simianarmy/api/v1/chaos'
    payload = {'eventType': 'CHAOS_TERMINATION','groupType': 'ASG','groupName': 'microservice'}

    #result = requests.post(monkey_url, data=json.dumps(payload))
    #print result.content


while True:
    #check for new nodes and assign to crashed services
    aws_check()

    num_nodes = randint(1, 2)
    for i in range(num_nodes):
        terminate_ondemand()

    #sleep for 10 minutes
    time.sleep(10 * MINUTE)










