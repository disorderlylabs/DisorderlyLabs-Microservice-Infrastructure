#!/usr/bin/python

import boto3
from apscheduler.scheduler import Scheduler

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

experiment_run = 0
services = []

for name in service_names:
    service = Service(name)
    services.append(service)


def get_running_instances(ec2_resource): 
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
        for service in crashed_services:
            logfile.write(service + "\t")

        logfile.write("\n")


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




#assigns new instances to crashed services and tag accordingly
def assign_new_instances(ec2, instance_id_dict):
    #get the key set
    keys = set(new_instance_dict.keys())

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

    new_instance_dict = filter_new_instances(instance_list)
    assign_new_instances(ec2, new_instance_dict)


scheduler = Scheduler()
scheduler.add_cron_job(aws_check, minute=60)
scheduler.start()





