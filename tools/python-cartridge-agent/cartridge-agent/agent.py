#!/usr/bin/env python
import stomp
import time
import logging
import sys
import random
import os
import threading
import socket
import json
import extensionhandler
import util
import subprocess


util.validateRequiredSystemProperties()

payloadPath=sys.argv[1]
extensionsDir=sys.argv[2]
extensionhandler.onArtifactUpdatedEvent(extensionsDir,'artifacts-updated.sh')

fo = open(payloadPath, "r+")
str = fo.read(1000);

print "Read String is : ", str

sd = dict(u.split("=") for u in str.split(","))

print [i for i in sd.keys()]


print "HOST_NAME   ", sd['HOST_NAME']

hostname=sd['HOST_NAME']
servicename=sd['SERVICE_NAME']
multitenant=sd['MULTITENANT']
tenantid=sd['TENANT_ID']
tenantrange=sd['TENANT_RANGE']
cartridealies=sd['CARTRIDGE_ALIAS']
clusterid=sd['CLUSTER_ID']
cartridgekey=sd['CARTRIDGE_KEY']
deployement=sd['DEPLOYMENT']
repourl=sd['REPO_URL']
ports=sd['PORTS']
puppetip=sd['PUPPET_IP']
puppethostname=sd['PUPPET_HOSTNAME']
puppetenv=sd['PUPPET_ENV']
commitenabled=sd['COMMIT_ENABLED']
dbhost=sd['DB_HOST']





def runningSuspendScript():
    print "inside thread"
    os.system('./script.sh')
def MyThread2():
    pass

def listeningTopology():
    class MyListener(stomp.ConnectionListener):
        def on_error(self, headers, message):
            print('received an error %s' % message)
        def on_message(self, headers, message):
           # print('received message\n %s'% message)
            for k,v in headers.iteritems():
                print('header: key %s , value %s' %(k,v))
           
                if k=='event-class-name':
                    print('event class name found')
                    if v=='org.apache.stratos.messaging.event.topology.CompleteTopologyEvent':
                        print('CompleteTopologyEvent triggered')
                        print('received message\n %s'% message)
                    if v=='org.apache.stratos.messaging.event.topology.MemberTerminatedEvent':
                        print('MemberTerminatedEvent triggered')
                    if v=='org.apache.stratos.messaging.event.topology.ServiceCreatedEvent':
                        print('MemberTerminatedEvent triggered')
                    if v=='org.apache.stratos.messaging.event.topology.InstanceSpawnedEvent':
                        print('MemberTerminatedEvent triggered')
                        print('received message\n %s'% message)
                    if v=='org.apache.stratos.messaging.event.topology.ClusterCreatedEvent':
                        print('MemberTerminatedEvent triggered')
                    if v=='org.apache.stratos.messaging.event.topology.InstanceSpawnedEvent':
                        print('MemberTerminatedEvent triggered')
                    else: 
                        print('something else')
           


    dest='/topic/topology'
    conn=stomp.Connection([('localhost',61613)])
    print('set up Connection')
    conn.set_listener('somename',MyListener())
    print('Set up listener')

    conn.start()
    print('started connection')

    conn.connect(wait=True)
    print('connected')
    conn.subscribe(destination=dest, ack='auto')
    print('subscribed')


def listeningInstanceNotifier():
    class MyListener(stomp.ConnectionListener):
        def on_error(self, headers, message):
            print('received an error %s' % message)
        def on_message(self, headers, message):
            for k,v in headers.iteritems():
                print('header: key %s , value %s' %(k,v))
                if k=='event-class-name':
                    print('event class name found')
                    if v=='org.apache.stratos.messaging.listener.instance.notifier.ArtifactUpdateEvent':
                        print('ArtifactUpdateEvent triggered')
                        print('received message\n %s'% message)
                    if v=='org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupMemberEvent':
                        print('MemberTerminatedEvent triggered')
                    if v=='org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupClusterEvent':
                        print('MemberTerminatedEvent triggered')
                    else: 
                        print('something else')
            print('received message\n %s'% message)


    dest='/topic/instance-notifier'
    conn=stomp.Connection([('localhost',61613)])
    print('set up Connection')
    conn.set_listener('somename',MyListener())
    print('Set up listener')

    conn.start()
    print('started connection')

    conn.connect(wait=True)
    print('connected')
    conn.subscribe(destination=dest, ack='auto')
    print('subscribed')



def publishInstanceStartedEvent():
    class MyListener(stomp.ConnectionListener):
        def on_error(self, headers, message):
            print('received an error %s' % message)
        def on_message(self, headers, message):
            for k,v in headers.iteritems():
                print('header: key %s , value %s' %(k,v))
            print('received message\n %s'% message)


    dest='/topic/instance-status'
    conn=stomp.Connection([('localhost',61613)])
    print('set up Connection')


    conn.start()
    print('started connection')

    conn.connect(wait=True)
    print('connected')
    conn.subscribe(destination=dest, ack='auto')
    print('subscribed')

    message=InstanceStartedEvent(servicename,clusterid,'','',tenantid).to_JSON()
    conn.send(message=message, destination=dest,headers={'seltype':'mandi-age-to-man','type':'textMessage','MessageNumber':random.randint(0,65535),'event-class-name':'org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent'},ack='auto')
    print('sent message')
    print(message)
    time.sleep(2)
    print('slept')
    conn.disconnect()
    print('disconnected')


def checkPortsActive():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    result = sock.connect_ex(('127.0.0.1',80))
    if result == 0:
       print "Port is open"
    else:
       print "Port is not open"


class InstanceStartedEvent:
    serviceName=''
    def __init__(self, serviceName,clusterId,networkPartitionId,partitionId,memberId):
        self.serviceName = serviceName
        self.clusterId = clusterId
        self.networkPartitionId = networkPartitionId
        self.partitionId = partitionId
        self.memberId = memberId
    def to_JSON(self):
        return json.dumps(self, default=lambda o: o.__dict__, sort_keys=True, indent=4)

def onInstanceStartedEvent():
    print('on instance start up event')
    event = InstanceStartedEvent(servicename,clusterid,'','',tenantid)
    print(event.to_JSON())


def onArtifactUpdatedEvent():
    print('on srtifcats update event')


t1 = threading.Thread(target=runningSuspendScript, args=[])

t1.start()

t2 = threading.Thread(target=listeningInstanceNotifier, args=[])

t2.start()

t3 = threading.Thread(target=listeningTopology, args=[])

t3.start()



onInstanceStartedEvent()

checkPortsActive()

publishInstanceStartedEvent()

extensionhandler.startServerExtension()


def git(*args):
    return subprocess.check_call(['git'] + list(args))

# examples
git("status")
git("clone", "git://git.xyz.com/platform/manifest.git", "-b", "jb_2.5")





