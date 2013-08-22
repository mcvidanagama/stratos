#!/bin/bash
# ----------------------------------------------------------------------------
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#
# ----------------------------------------------------------------------------
#
#  Server configuration script for Apache Stratos
# ----------------------------------------------------------------------------

# Die on any error:
set -e

SLEEP=60

source "./conf/setup.conf"
export LOG=$log_path/stratos-setup.log

cc="false"
elb="false"
agent="false"
sc="false"
#bam="false"
demo="openstack"
product_list="cc;elb;agent;sc"
enable_internal_git=false

function help {
    echo ""
    echo "demo means you will setup a demo server of S2 in a single physical machine which has Openstack installed."
    echo "This demo server include all S2 related packs."
    echo "usage:"
    echo "setup.sh -u<host user name> -d<demo> -p\"<product list>\""
    echo "eg."
    echo "sudo JAVA_HOME=/opt/jdk1.6.0_24 ./setup.sh -dopenstack -p\"cc elb\""
    echo "sudo JAVA_HOME=/opt/jdk1.6.0_24 ./setup.sh -dopenstack -p\"all\""
    echo ""
    echo "-d: <demo name> whether you need to run demo for ec2 or openstack. The value is one of ec2|openstack. By default no demo is setup"
    echo "-p: <product list> Give one or more of the servers to be setup in this machine. The available servers are"
    echo "-g: <enable_internal_git true|false> Whether enable internal git repo for Stratos2. Default is false"
    echo "    cc, elb, agent, sc or all. 'all' means you need to setup all servers in this machine. Default is all"
    echo ""
}

while getopts p:d:g: opts
do
  case $opts in
    p)
        product_list=${OPTARG}
        echo $product_list
        ;;
    d)
        demo=${OPTARG}
        echo $demo
        ;;
    g)
        enable_internal_git=${OPTARG}
        echo $enable_internal_git
        ;;
    *)
        help
        exit 1
        ;;
  esac
done

arr=$(echo $product_list | tr " " "\n")

for x in $arr
do
    if [[ $x = "cc" ]]; then
        cc="true"
    fi
    if [[ $x = "elb" ]]; then
        elb="true"
    fi
    if [[ $x = "agent" ]]; then
        agent="true"
    fi
    if [[ $x = "sc" ]]; then
        sc="true"
    fi
#   if [[ $x = "bam" ]]; then
#       bam="true"
#   fi
    if [[ $x = "all" ]]; then
        cc="true"
        elb="true"
        agent="true"
        sc="true"
#       bam="true"
    fi
done
product_list=`echo $product_list | sed 's/^ *//g' | sed 's/ *$//g'`
if [[ -z $product_list || $product_list = "" ]]; then
    help
    exit 1
fi

function helpsetup {
    echo ""
    echo "Set up the environment variables correctly in conf/setup.conf"
    echo ""
}

echo "user provided in conf/setup.conf is $host_user. If you want to provide some other user name please specify it at the prompt."
echo "If you want to continue with $host_user just press enter to continue"
read username
if [[ $username != "" ]]; then
    host_user=$username
fi
user=`id $host_user`
if [[ $? = 1 ]]; then
    echo "User $host_user does not exist. The system will create it."
    adduser --home /home/$host_user $host_user
fi

echo "StrictHostKeyChecking no" > /home/$host_user/.ssh/config
chmod 600 /home/$host_user/.ssh/config
chown $host_user:$host_user /home/$host_user/.ssh/config
export $enable_internal_git
export $host_user
export hostname=`hostname -f`

function setup_validate {
    

if [[ -z $hostname ]]; then
    echo "Set up the hostname of the node"
    exit 1
fi

if [[ -z $userstore_db_hostname ]]; then
    userstore_db_hostname=""
fi
if [[ -z $sc_hostname ]]; then
    sc_hostname=$hostname
fi
if [[ -z $s_foundation_db_hostname ]]; then
    s_foundation_db_hostname=$hostname
fi
if [[ -z $agent_hostname ]]; then
    agent_hostname=$hostname
fi
if [[ -z $cc_hostname ]]; then
    cc_hostname=$hostname
fi
if [[ -z $git_hostname ]]; then
    git_hostname=$hostname
fi
if [[ -z $nova_controller_hostname ]]; then
    nova_controller_hostname=$hostname
fi
if [[ -z $bam_hostname ]]; then
    bam_hostname=$hostname
fi
if [[ -z $elb_hostname ]]; then
   elb_hostname=$hostname
fi


if [[ ( -z $hostip ) ]]; then
    hostip=$(ifconfig eth0| sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p')
    if [[ ( -z $hostip ) ]]; then
        helpsetup
        exit 1
    fi
fi

if [[ -z $git_ip ]]; then
    git_ip=$hostip
fi

if [[ $sc = "true" ]]; then

    if [[ $enable_internal_git = "true" ]]; then
        if [[ -z $git_user ]]; then
            echo "Please specify the git user, because it will be needed to create an internal git repo"
        fi
        if [[ -z $axis2c_path ]]; then
            echo "Please specify the path to Axis2/C binary, because it will be needed to create an internal git repo"
        fi

        echo "$hostip    git.$stratos2_domain" >> /etc/hosts

    fi

    if [[ ( -z $email|| -z $s_foundation_db_user || -z $s_foundation_db_pass || -z $hostname
        || -z $sc_path ) ]]; then
        helpsetup
        exit 1
    fi
fi

if [[ $cc = "true" ]]; then
    if [[ ( -z $hostname || -z $cc_path ) ]]; then
        helpsetup
        exit 1
    fi
fi

if [[ $elb = "true" ]]; then
    if [[ ( -z $hostname || -z $elb_path ) ]]; then
        helpsetup
        exit 1
    fi
fi

if [[ $agent = "true" ]]; then
    if [[ ( -z $hostname || -z $agent_path ) ]]; then
        helpsetup
        exit 1
    fi
fi

if [[ ! -f $mysql_connector_jar ]]; then
    echo "Please copy the mysql connector jar into the same folder as this command(stratos2 release pack folder) and update conf/setup.conf file"
    exit 1
fi

if [[ ! -d $JAVA_HOME ]]; then
    echo "Please set the JAVA_HOME environment variable for the running user"
    exit 1
fi
export JAVA_HOME=$JAVA_HOME


if [[ $openstack_provider_enable = "true" ]]; then
    if [[ ( -z $openstack_identity || -z $openstack_credential || -z $openstack_jclouds_endpoint ) ]]; then
        helpsetup
        exit 1
    fi

fi

if [[ $ec2_provider_enable = "true" ]]; then
    if [[ ( -z $ec2_identity || -z $ec2_credential || -z $ec2_keypair ) ]]; then
        helpsetup
        exit 1
    fi
fi

}

setup_validate

# Make sure the user is running as root.
if [ "$UID" -ne "0" ]; then
	echo ; echo "  You must be root to run $0.  (Try running 'sudo bash' first.)" ; echo 
	exit 69
fi

if [[ ! -d $log_path ]]; then
    mkdir -p $log_path
fi

echo ""
echo "For all the questions asked while during executing the script please just press the enter button"
echo ""


if [[ $sc = "true" ]]; then
    if [[ ! -d $resource_path ]]; then
        cp -rf ./resources $stratos_path
    fi

    if [[ ! -d $script_path ]]; then
        cp -rf ./scripts $stratos_path
    fi

    if [[ ! -d $sc_path ]]; then
        unzip $sc_pack -d $stratos_path
    fi
    #if [[ ! -d $axis2c_path ]]; then
      #  unzip $axis2c_pack -d $stratos_path
    #fi
fi
if [[ $elb = "true" ]]; then
    if [[ ! -d $elb_path ]]; then
        unzip $elb_pack -d $stratos_path
    fi
fi
if [[ $cc = "true" ]]; then
    if [[ ! -d $cc_path ]]; then
        unzip $cc_pack -d $stratos_path
    fi
fi
if [[ $agent = "true" ]]; then
    if [[ ! -d $agent_path ]]; then
        unzip $agent_pack -d $stratos_path
    fi
fi

if [[ $bam = "true" ]]; then
    if [[ ! -d $bam_path ]]; then
        unzip $bam_pack -d $stratos_path
    fi
fi


if [[ $sc = "true" ]]; then
    ##
#    mysql -u${userstore_db_user} -p${userstore_db_pass} -e "GRANT ALL PRIVILEGES ON *.* TO '${userstore_db_user}'@'%'   IDENTIFIED BY '${userstore_db_pass}' WITH GRANT OPTION;flush privileges;"


    # Setup SC
    #--------------------------------------------------
    echo "Setup SC" >> $LOG
    echo "Configuring SC"

    cp -f ./config/sc/repository/conf/cartridge-config.properties $sc_path/repository/conf/
    cp -f ./config/sc/repository/conf/carbon.xml $sc_path/repository/conf/
    cp -f ./config/sc/repository/conf/axis2/axis2.xml $sc_path/repository/conf/axis2
    cp -f ./config/sc/bin/stratos.sh $sc_path/bin/
    cp -fr ./config/sc/repository/resources/user-data/* $sc_path/repository/resources/user-data/
    cp -f ./config/sc/repository/conf/datasources/master-datasources.xml $sc_path/repository/conf/datasources/
    cp -f ./config/sc/repository/conf/datasources/stratos-datasources.xml $sc_path/repository/conf/datasources/
    cp -f $mysql_connector_jar $sc_path/repository/components/lib/
    cp -f ./config/sc/repository/conf/log4j.properties  $sc_path/repository/conf/
    cp -f ./config/sc/repository/conf/etc/logging-config.xml  $sc_path/repository/conf/etc/
    pushd $sc_path

    echo "Set mb hostname and mb port in bin/stratos.sh." >> $LOG
    cp -f ./bin/stratos.sh bin/stratos.sh.orig
    cat bin/stratos.sh.orig | sed -e "s@CC_HOSTNAME:MB_LISTEN_PORT@$cc_hostname:$mb_listen_port@g" > bin/stratos.sh

    echo "Change CC hostname in repository/conf/cartridge-config.properties" >> $LOG

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@CC_HOSTNAME:CC_HTTPS_PORT@$cc_hostname:$cc_https_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@AGENT_HOSTNAME:AGENT_PORT@$agent_ip:$agent_https_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@STRATOS_DOMAIN@$stratos2_domain@g" > repository/conf/cartridge-config.properties

    if [[ $enable_internal_git = "true" ]]; then
        cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
        cat repository/conf/cartridge-config.properties.orig | sed -e "s@GIT_IP@$git_ip@g" > repository/conf/cartridge-config.properties
    fi

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@SC_HOSTNAME:SC_HTTPS_PORT@$sc_ip:$sc_https_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@s_foundation_db_HOSTNAME:s_foundation_db_PORT@$s_foundation_db_hostname:$s_foundation_db_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@s_foundation_db_USER@$s_foundation_db_user@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@s_foundation_db_PASS@$s_foundation_db_pass@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@s_foundation_db_SCHEMA@$s_foundation_db_schema@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@CC_HOSTNAME:MB_LISTEN_PORT@$cc_hostname:$mb_listen_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@ELB_IP@$elb_ip@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@BAM_IP@$bam_ip@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@BAM_PORT@$bam_port@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@SCRIPT_PATH@$script_path@g" > repository/conf/cartridge-config.properties

    cp -f repository/conf/cartridge-config.properties repository/conf/cartridge-config.properties.orig
    cat repository/conf/cartridge-config.properties.orig | sed -e "s@HOST_USER@$host_user@g" > repository/conf/cartridge-config.properties

    echo "Change mysql password in repository/conf/datasources/master-datasources.xml" >> $LOG
    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_HOSTNAME@$userstore_db_hostname@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_PORT@$userstore_db_port@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_SCHEMA@$userstore_db_schema@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_USER@$userstore_db_user@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_PASS@$userstore_db_pass@g" > repository/conf/datasources/master-datasources.xml

    echo "Change mysql password in repository/conf/datasources/master-datasources.xml" >> $LOG
    cp -f repository/conf/datasources/stratos-datasources.xml repository/conf/datasources/stratos-datasources.xml.orig
    cat repository/conf/datasources/stratos-datasources.xml.orig | sed -e "s@BILLING_DB_HOSTNAME@$billing_db_hostanme@g" > repository/conf/datasources/stratos-datasources.xml

    cp -f repository/conf/datasources/stratos-datasources.xml repository/conf/datasources/stratos-datasources.xml.orig
    cat repository/conf/datasources/stratos-datasources.xml.orig | sed -e "s@BILLING_DB_PORT@$billing_db_port@g" > repository/conf/datasources/stratos-datasources.xml

    cp -f repository/conf/datasources/stratos-datasources.xml repository/conf/datasources/stratos-datasources.xml.orig
    cat repository/conf/datasources/stratos-datasources.xml.orig | sed -e "s@BILLING_DB_SCHEMA@$billing_db_schema@g" > repository/conf/datasources/stratos-datasources.xml

    cp -f repository/conf/datasources/stratos-datasources.xml repository/conf/datasources/stratos-datasources.xml.orig
    cat repository/conf/datasources/stratos-datasources.xml.orig | sed -e "s@BILLING_USERNAME@$billing_db_username@g" > repository/conf/datasources/stratos-datasources.xml

    cp -f repository/conf/datasources/stratos-datasources.xml repository/conf/datasources/stratos-datasources.xml.orig
    cat repository/conf/datasources/stratos-datasources.xml.orig | sed -e "s@BILLING_PASSWORD@$billing_db_password@g" > repository/conf/datasources/stratos-datasources.xml
    
    cp -f repository/conf/axis2/axis2.xml repository/conf/axis2/axis2.xml.orig
    cat repository/conf/axis2/axis2.xml.orig | sed -e "s@SC_HOSTNAME@${sc_hostname}@g" > repository/conf/axis2/axis2.xml
    
    cp -f repository/conf/axis2/axis2.xml repository/conf/axis2/axis2.xml.orig
    cat repository/conf/axis2/axis2.xml.orig | sed -e "s@SC_CLUSTER_PORT@${sc_cluster_port}@g" > repository/conf/axis2/axis2.xml
    
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@SC_PORT_OFFSET@${sc_port_offset}@g" > repository/conf/carbon.xml
    
    cp -f repository/conf/log4j.properties repository/conf/log4j.properties.orig
    cat repository/conf/log4j.properties.orig | sed -e "s@BAM_HOSTNAME:BAM_RECEIVER_PORT@$bam_hostname:$bam_reciever_port@g" > repository/conf/log4j.properties

    cp -f repository/conf/etc/logging-config.xml repository/conf/etc/logging-config.xml.orig
    cat repository/conf/etc/logging-config.xml.orig | sed -e "s@BAM_HOSTNAME:CASSANDRA_PORT@$bam_hostname:$cassandra_port@g" > repository/conf/etc/logging-config.xml

    popd # sc_path


    # Database Configuration
    # -----------------------------------------------
    echo "Create and configure MySql Databases" >> $LOG

    echo "Create userstore database"
    mysql -u$userstore_db_user -p$userstore_db_pass < $resource_path/userstore.sql
    #mysql -u$userstore_db_user -p$userstore_db_pass < $resource_path/registry.sql   #registry schema is only for AF
    
    echo "Create stratos_foundation database"
    mysql -u$s_foundation_db_user -p$s_foundation_db_pass < $resource_path/stratos_foundation.sql

    #mysql -u$billing_db_username -p$billing_db_password < $resource_path/billing-mysql.sql

    #mysql -u$billing_db_username -p$billing_db_password < $resource_path/metering_mysql.sql

    #Namespace Binding
    # -----------------------------------------------
    echo "bind Namespaces" >> $LOG
    #apt-get install bind9 zip
    #Copy the /db.stratos.com file into /etc/bind. Edit it as necessary
    #cp -f ./resources/db.stratos.com $resource_path/db.$stratos2_domain
    #echo "Set ELb Hostname in /etc/bind/db.stratos.com" >> $LOG
    #cat $resource_path/db.$stratos2_domain | sed -e "s@SC_HOSTNAME@$sc_hostname@g" | sed -e "s@ELB_IP@$elb_ip@g" | sed -e "s@STRATOS_DOMAIN@$stratos2_domain@g" > /etc/bind/db.$stratos2_domain

    #echo "Add the following content to /etc/bind/named.conf.local" >> $LOG
    #echo "zone \"$stratos2_domain\" {" >> /etc/bind/named.conf.local
    #echo "      type master;" >> /etc/bind/named.conf.local
    #echo "      file \"/etc/bind/db.$stratos2_domain\";" >> /etc/bind/named.conf.local
    #echo "};" >> /etc/bind/named.conf.local

    #Copy https://svn.wso2.org/repos/wso2/scratch/hosting/build/tropos/resources/append_zone_file.sh into /opt/scripts folder
    if [[ ! -d $stratos_path/scripts ]]; then
        mkdir -p $stratos_path/scripts
    fi
    cp -f ./scripts/add_entry_zone_file.sh $stratos_path/scripts/add_entry_zone_file.sh
    cp -f ./scripts/remove_entry_zone_file.sh $stratos_path/scripts/remove_entry_zone_file.sh


    echo "End configuring the SC"
fi #End SC server installation


# Setup CC
# ------------------------------------------------
if [[ $cc = "true" ]]; then
    echo "Setup CC" >> $LOG
    echo "Configuring the Cloud Controller"
    echo "Creating payload directory ... " >> $LOG
    if [[ ! -d $cc_path/repository/resources/payload ]]; then
        mkdir -p $cc_path/repository/resources/payload
    fi

    cp -f ./config/cc/bin/stratos.sh $cc_path/bin/
    cp -f ./config/cc/repository/conf/cloud-controller.xml $cc_path/repository/conf/
    cp -f ./config/cc/repository/conf/carbon.xml $cc_path/repository/conf/

    #MB specific file copying
    cp -f ./config/cc/repository/conf/advanced/qpid-virtualhosts.xml $cc_path/repository/conf/advanced/
    #cp -f ./config/cc/repository/conf/carbon.xml $cc_path/repository/conf/
    #End MB specific file copying

    pushd $cc_path

# Setup IaaS providers
# ------------------------------------------------
#        <iaasProviders>
#        <!--iaasProvider type="ec2" name="ec2 specific details">
#            <className>org.wso2.carbon.stratos.cloud.controller.iaases.AWSEC2Iaas</className>
#                        <provider>aws-ec2</provider>
#                        <identity svns:secretAlias="elastic.scaler.openstack.identity">dhsaghfdal</identity>
#                        <credential svns:secretAlias="elastic.scaler.openstack.credential">jdkjaskd</credential>
#                        <scaleUpOrder>1</scaleUpOrder>
#                        <scaleDownOrder>2</scaleDownOrder>
#                        <property name="jclouds.ec2.ami-query" value="owner-id=XX-XX-XX;state=available;image-type=machine"/>
#            <property name="availabilityZone" value="us-east-1c"/>
#                        <property name="securityGroups" value="manager,cep,mb,default"/>
#            <property name="instanceType" value="m1.large"/>
#            <property name="keyPair" value="nirmal-key"/>
#                        <imageId>us-east-1/ami-52409a3b</imageId>
#                </iaasProvider-->
#                <iaasProvider type="openstack" name="openstack specific details">
#            <className>org.wso2.carbon.stratos.cloud.controller.iaases.OpenstackNovaIaas</className>
#                        <provider>openstack-nova</provider>
#                        <identity svns:secretAlias="cloud.controller.openstack.identity">demo:demo</identity>
#                        <credential svns:secretAlias="cloud.controller.openstack.credential">openstack</credential>
#                        <property name="jclouds.endpoint" value="http://192.168.16.20:5000/" />
#            <property name="jclouds.openstack-nova.auto-create-floating-ips" value="false"/>
#                        <property name="jclouds.api-version" value="2.0/" />
#                        <scaleUpOrder>2</scaleUpOrder>
#                        <scaleDownOrder>3</scaleDownOrder>
#                        <property name="X" value="x" />
#                        <property name="Y" value="y" />
#                        <imageId>nova/dab37f0e-cf6f-4812-86fc-733acf22d5e6</imageId>
#                </iaasProvider>
#        </iaasProviders>

   echo "Set openstack provider specific info on repository/conf/cloud-controller.xml" >> $LOG

   if [[ $ec2_provider_enable = "true" ]]; then

       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_PROVIDER_START@@g" > repository/conf/cloud-controller.xml

       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_IDENTITY@$ec2_identity@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_CREDENTIAL@$ec2_credential@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_SCALEUP_ORDER@$ec2_scaleup_order@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_SCALEDOWN_ORDER@$ec2_scaledown_order@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_AVAILABILITY@$ec2_availability_zone@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_SECURITY_GROUPS@$ec2_security_groups@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_INSTANCE_TYPE@$ec2_instance_type@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_KEYPAIR@$ec2_keypair@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_IMAGE_ID@$ec2_image_id@g" > repository/conf/cloud-controller.xml

       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_PROVIDER_END@@g" > repository/conf/cloud-controller.xml
   
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_PROVIDER_START@!--@g" > repository/conf/cloud-controller.xml

       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_PROVIDER_END@--@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@MB_HOSTNAME@$cc_hostname@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@MB_LISTEN_PORT@$mb_listen_port@g" > repository/conf/cloud-controller.xml

   fi

    
   echo "Set EC2 provider specific info on repository/conf/cloud-controller.xml" >> $LOG

   if [[ $openstack_provider_enable = "true" ]]; then

       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_PROVIDER_START@@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_IDENTITY@$openstack_identity@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_CREDENTIAL@$openstack_credential@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_ENDPOINT@$openstack_jclouds_endpoint@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_SCALEUP_ORDER@$openstack_scaleup_order@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_SCALEDOWN_ORDER@$openstack_scaledown_order@g" > repository/conf/cloud-controller.xml
       
       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_IMAGE_ID@$openstack_image_id@g" > repository/conf/cloud-controller.xml

       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@OPENSTACK_PROVIDER_END@@g" > repository/conf/cloud-controller.xml

       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_PROVIDER_START@!--@g" > repository/conf/cloud-controller.xml

       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@EC2_PROVIDER_END@--@g" > repository/conf/cloud-controller.xml


       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@MB_HOSTNAME@$cc_hostname@g" > repository/conf/cloud-controller.xml

       cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
       cat repository/conf/cloud-controller.xml.orig | sed -e "s@MB_LISTEN_PORT@$mb_listen_port@g" > repository/conf/cloud-controller.xml

   fi

    cp -f repository/conf/cloud-controller.xml repository/conf/cloud-controller.xml.orig
    cat repository/conf/cloud-controller.xml.orig | sed -e "s@CC_HOSTNAME:MB_LISTEN_PORT@$cc_hostname:$mb_listen_port@g" > repository/conf/cloud-controller.xml

    echo "In repository/conf/carbon.xml"
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@BAM_HOSTNAME:BAM_PORT@$bam_hostname:$bam_port@g" > repository/conf/carbon.xml

    #Before starting CC we need to delete
    #rm ./repository/conf/service-topology.conf
    #rm ./repository/conf/service-topology.conf.back


    # Setup MB
    # -------------------------------------------------------------
    echo "Setup MB" >> $LOG
    echo "Set settings in cc/repository/conf/advanced/qpid-virtualhosts.xml" >> $LOG
    cp -f repository/conf/advanced/qpid-virtualhosts.xml repository/conf/advanced/qpid-virtualhosts.xml.orig
    cat repository/conf/advanced/qpid-virtualhosts.xml.orig | sed -e "s@MB_CASSANDRA_HOST@$mb_cassandra_host@g" > repository/conf/advanced/qpid-virtualhosts.xml
    cp -f repository/conf/advanced/qpid-virtualhosts.xml repository/conf/advanced/qpid-virtualhosts.xml.orig
    cat repository/conf/advanced/qpid-virtualhosts.xml.orig | sed -e "s@MB_CASSANDRA_PORT@$mb_cassandra_port@g" > repository/conf/advanced/qpid-virtualhosts.xml

    echo "Set settings in cc/repository/conf/carbon.xml" >> $LOG
    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@CC_PORT_OFFSET@$cc_port_offset@g" > repository/conf/carbon.xml
    #Before starting sc delete rm -rf tmp/ at mb root folder
    rm -rf ./tmp


    popd #cc_path
    echo "End configuring the Cloud Controller"
fi


# Setup ELB
# -------------------------------------------------------------    
if [[ $elb = "true" ]]; then
    echo "Setup ELB" >> $LOG
    echo "Configuring the ELB"
    cp -f ./config/elb/bin/stratos.sh $elb_path/bin/
    cp -f ./config/elb/repository/conf/loadbalancer.conf $elb_path/repository/conf/
    cp -f ./config/elb/repository/conf/axis2/axis2.xml $elb_path/repository/conf/axis2/
    cp -f ./config/elb/repository/conf/datasources/master-datasources.xml $elb_path/repository/conf/datasources/
    cp -f $mysql_connector_jar $elb_path/repository/components/lib/

    pushd $elb_path
    #If autoscaling enabled
    echo "Set CC host and port in repository/conf/loadbalancer.conf" >> $LOG
    # autoscaler_service_epr  https://CC_HOSTNAME:CC_PORT/services/CloudControllerService;
    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@CC_HOSTNAME:CC_PORT@$cc_hostname:$cc_https_port@g" > repository/conf/loadbalancer.conf

    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@ENABLE_AUTOSCALER@$enable_autoscaler@g" > repository/conf/loadbalancer.conf
    
    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@ENABLE_EMBEDDED_AUTOSCALER@$enable_embedded_autoscaler@g" > repository/conf/loadbalancer.conf

    cp -f repository/conf/loadbalancer.conf repository/conf/loadbalancer.conf.orig
    cat repository/conf/loadbalancer.conf.orig | sed -e "s@MB_HOSTNAME:MB_LISTEN_PORT@$cc_hostname:$mb_listen_port@g" > repository/conf/loadbalancer.conf

    echo "Set hostname of the machine where elb run, in repository/conf/axis2/axis2.xml" >> $LOG
    #<!--parameter name="localMemberHost">ELB_HOSTNAME</parameter-->
    cp -f repository/conf/axis2/axis2.xml repository/conf/axis2/axis2.xml.orig
    cat repository/conf/axis2/axis2.xml.orig | sed -e "s@ELB_HOSTNAME@$elb_hostname@g" > repository/conf/axis2/axis2.xml


    echo "Set hostname of the machine where elb run, in repository/conf/etc/jmx.xml" >> $LOG
    cp -f repository/conf/etc/jmx.xml repository/conf/etc/jmx.xml.orig
    cat repository/conf/etc/jmx.xml.orig | sed -e "s@ELB_HOSTNAME@$elb_hostname@g" > repository/conf/etc/jmx.xml


    echo "Change mysql password in repository/conf/datasources/master-datasources.xml" >> $LOG
    cp -f ./repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@USERSTORE_DB_HOSTNAME@$userstore_db_hostname@g" | sed -e "s@USERSTORE_DB_PORT@$userstore_db_port@g"| sed -e "s@USERSTORE_DB_SCHEMA@$userstore_db_schema@g"|sed -e "s@USERSTORE_DB_USER@$userstore_db_user@g" |sed -e "s@USERSTORE_DB_PASS@$userstore_db_pass@g" > repository/conf/datasources/master-datasources.xml

    popd #elb_path
    echo "End configuring the ELB"
fi


# Setup Agent
# -------------------------------------------------------------    
if [[ $agent = "true" ]]; then
    echo "Setup Agent" >> $LOG
    echo "Configuring the Agent"
    cp -f ./config/agent/repository/conf/agent.properties $agent_path/repository/conf/
    cp -f ./config/agent/repository/conf/carbon.xml $agent_path/repository/conf/
    pushd $agent_path

    rm -rf registrants/

    #Set agent host and ELb host in repository/conf/agent.properties.
    #Note that loadBalancerDomain=wso2.carbon.lb.domain should be same as elb/repository/conf/axis2/axis2.xml
    #<parameter name="domain">wso2.carbon.lb.domain</parameter>
    echo "Set agent hostname in repository/conf/agent.properties." >> $LOG
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@AGENT_HOSTNAME@$agent_hostname@g" > repository/conf/agent.properties

    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@AGENT_CLUSTERING_PORT@$agent_clustering_port@g" > repository/conf/agent.properties

    echo "Set ELB hostname in repository/conf/agent.properties." >> $LOG
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@ELB_HOSTNAME@$elb_hostname@g" > repository/conf/agent.properties
    
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@ELB_CLUSTER_PORT@$elb_cluster_port@g" > repository/conf/agent.properties

    echo "Set SC_PATH in repository/conf/agent.properties." >> $LOG
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@SC_PATH@$sc_path@g" > repository/conf/agent.properties

    echo "Set SC_HOST in repository/conf/agent.properties." >> $LOG
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@SC_HOSTNAME@$sc_hostname@g" > repository/conf/agent.properties

    echo "Set SC_HTTPS_PORT in repository/conf/agent.properties." >> $LOG
    cp -f ./repository/conf/agent.properties repository/conf/agent.properties.orig
    cat repository/conf/agent.properties.orig | sed -e "s@SC_HTTPS_PORT@$sc_https_port@g" > repository/conf/agent.properties

    cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
    cat repository/conf/carbon.xml.orig | sed -e "s@AGENT_PORT_OFFSET@${agent_port_offset}@g" > repository/conf/carbon.xml

    popd #agent_path
    echo "End configuring the Agent"
fi


# Setup BAM
# --------------------------------------------------
if [[ $bam = "true" ]]; then
 echo "Setup BAM" >> $LOG
 echo "Configuring the BAM"
 cp -f ./config/bam/bin/stratos.sh $bam_path/bin/
 cp -f ./config/bam/repository/conf/carbon.xml $bam_path/repository/conf/
 cp -f ./config/bam/repository/conf/etc/cassandra-component.xml $bam_path/repository/conf/etc/
 cp -f ./config/bam/repository/conf/etc/cassandra.yaml $bam_path/repository/conf/etc/
 cp -f ./config/bam/repository/conf/datasources/master-datasources.xml $bam_path/repository/conf/datasources/
 cp -f $mysql_connector_jar $bam_path/repository/components/lib/

  pushd $bam_path

  cp -f repository/conf/carbon.xml repository/conf/carbon.xml.orig
  cat repository/conf/carbon.xml.orig | sed -e "s@BAM_PORT_OFFSET@${bam_port_offset}@g" > repository/conf/carbon.xml

  cp -f repository/conf/etc/cassandra.yaml repository/conf/etc/cassandra.yaml.orig
  cat repository/conf/etc/cassandra.yaml.orig | sed -e "s@BAM_HOSTNAME@${bam_hostname}@g" > repository/conf/etc/cassandra.yaml

  cp -f repository/conf/etc/cassandra-component.xml repository/conf/etc/cassandra-component.xml.orig
  cat repository/conf/etc/cassandra-component.xml.orig | sed -e "s@BAM_HOSTNAME:CASSANDRA_PORT@$bam_hostname:$cassandra_port@g" > repository/conf/etc/cassandra-component.xml

 echo "Change mysql password in repository/conf/datasources/master-datasources.xml" >> $LOG
    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@BILLING_DB_HOSTNAME@$billing_db_hostanme@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@BILLING_DB_PORT@$billing_db_port@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@BILLING_DB_SCHEMA@$billing_db_schema@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@BILLING_USERNAME@$billing_db_username@g" > repository/conf/datasources/master-datasources.xml

    cp -f repository/conf/datasources/master-datasources.xml repository/conf/datasources/master-datasources.xml.orig
    cat repository/conf/datasources/master-datasources.xml.orig | sed -e "s@BILLING_PASSWORD@$billing_db_password@g" > repository/conf/datasources/master-datasources.xml

 popd #bam_path
fi

# Configure cartridges
# ---------------------------------------------------------
if [[ $demo = "openstack" ]]; then
   ./openstack-cartridge-setup.sh
fi

if [[ $demo = "ec2" ]]; then
    ./ec2-cartridge-setup.sh
fi

echo "Apache Stratos setup has successfully completed"

read -p "Do you want to start the servers [y/n]? " answer
if [[ $answer != y ]] ; then
    exit 1
fi

# Starting the servers
# ---------------------------------------------------------
echo "Starting the servers" >> $LOG
#Starting the servers in the following order is recommended
#mb, cc, elb, is, agent, sc

chown $host_user:$host_user -R $stratos_path/*

echo "Starting up servers. This may take time. Look at $LOG file for server startup details"

chown -R $host_user.$host_user $log_path
chmod -R 777 $log_path

export setup_dir=$PWD
su - $host_user -c "export JAVA_HOME=$JAVA_HOME;source $setup_dir/conf/setup.conf;$setup_dir/start-servers.sh -p$product_list >> $LOG"

echo "Servers started. Please look at $LOG file for server startup details"
if [[ $sc == "true" ]]; then
    echo "**************************************************************"
    echo "Management Console : https://$HOSTNAME:$sc_https_port/"
    echo "**************************************************************"
fi

