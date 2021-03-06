VirtualBox Java API
===
[![Build Status](https://api.travis-ci.org/alexec/maven-vbox-plugin.png)](https://travis-ci.org/alexec/maven-vbox-plugin)

Overview
===
This project provides support for creating, starting and stopping VirtualBox VMs. This is aimed at development and integration testing of projects by allowing you to package a complete software stack onto a single machine, install your code and perform your tests.

Some typical scenarios would be:

* Provide a dev stack and a touch of a button.
* Install all apps onto a VBox and test it.

It provides:

 1. A Java API for programmatic control of boxes.
 2. A set of Maven Mojos.
 3. A set of matching Ant tasks.

Goals
===
To provide support for:

* Multiple host and guest OS, not least including Linux, Windows and OS-X
* Unattended install and provisioning of guest OSs.
* Multiple VMs per project.
* *Not* to be a replacement for VeeWee, Vagrant, Chef or Puppet.

Usage
===
The main mojos/tasks are split into three groups:

Managing Definitions
---
Task that work on a single definition:

* list-definitions - list available template definitions
* create-definition - creates a box from template definition
* delete-definition - delete the definition
* list-predefined-patches - list built-in patches
* patch-definition - patch a definition with one or more patches

Provisioning Tasks
---
Task related to setting up one or more boxes:

* clean - deletes boxes
* create - creates boxes, generally not used as provision will create the box if it doesn't exist
* provision - provisions boxes, creating them if needs be

Runtime Tasks
---
Tasks related to the usage of boxes:

* start - start boxes
* stop - stops boxes
* suspend - suspend the boxes
* resume - resume boxes


Examples
===

<iframe width="640" height="360" src="http://www.youtube.com/embed/Y4ZXD7psIuM" frameborder="0" allowfullscreen="true"></iframe>

* [Five minute demo](http://www.youtube.com/watch?v=Y4ZXD7psIuM)
* [Ant and Maven examples](https://github.com/alexec/maven-vbox-plugin/tree/master/vbox-examples/)
* [Example Maven project](https://github.com/alexec/maven-vbox-plugin-example)

Maven
===
Quick Start
---
Add this to your pom.xml:

    <plugin>
        <groupId>com.alexecollins.vbox</groupId>
        <artifactId>vbox-maven-plugin</artifactId>
        <version>2.0.0</version>
        <executions>
            <execution>
                <goals>
                    <goal>clean</goal>
                    <goal>provision</goal>
                    <goal>start</goal>
                    <goal>stop</goal>
                </goals>
            </execution>
        </executions>
    </plugin>

Execute this:

    mvn vbox:create-definition -Dvbox.name=CentOS_6_5

Execute:

    mvn verify

Maven searches for VM definitions under src/main/vbox.

Example can be [found here](https://github.com/alexec/maven-vbox-plugin/tree/master/vbox-examples/maven).
Ant
===
Quick Start
---
Add this to your build.xml:

    <project name="vbox-ant-tasks" default="build" xmlns:vbox="antlib:com.alexecollins.vbox.ant">
        <target name="build">
            <property name="context" value="ant-project:1.0.0"/>
            <property name="app" location="src/vbox/app1"/>
            <vbox:purge-local-repository/>
            <vbox:list-definitions/>
            <vbox:delete-definition dir="${app}"/>
            <vbox:create-definition name="CentOS_6_5" dir="${app}"/>
            <vbox:patch-definition dir="${app}">
                <archPatch/>
                <predefinedPatch name="CentOS_6_5--tomcat6"/>
            </vbox:patch-definition>
            <vbox:clean dir="${app}" context="${context}"/>
            <vbox:create dir="${app}" context="${context}"/>
            <vbox:provision dir="${app}" context="${context}"/>
            <vbox:start dir="${app}"/>
            <!-- ... -->
            <vbox:stop dir="${app}"/>
        </target>
    </project>

Add the vbox-ant-tasks-*.jar to Ant's class path.

Ant tasks do not currently allow you to do multiple VMs in a single command. You'll need to use multiple ones.

An example can be [found here](https://github.com/alexec/maven-vbox-plugin/tree/master/vbox-examples/ant).
Definitions
===
Definitions can be found in src/test/vbox. Typically you'd create a series of definitions in src/main/vbox, alongside supporting files, for example an Ubuntu server might be named "UbuntuServer":

* src/main/vbox/
    * UbuntuServer/         - The name of the server.
        * MediaRegistry.xml - A list of media to get (e.g. from a URL or fileshare). Similar to a fragment of VirtualBox.xml file.
            * VirtualBox.xml    - The configuration of the server (e.g. disk etc.). Intentionally similar to one of Virtual Box's .vbox XML files.
            * Manifest.xml      - A list of all files used by the server (e.g. preseed.cfg, AutoUnattend.xml etc.). Optional.
            * Provisioning.xml  - The steps required to get the box ready (e.g. install Apache, set-up DNS etc.). Intentionally similar to an Ant script.
            * Profile.xml       - Information about the box, such as if it is headless, and how to determine if it's stared successfully.

The Ubuntu example downloads (by setting the DVDImage location to the URL) and attaches it. It then uses a preseed.cfg to create the VM.

You'll want to include an additional files, either a preseed.cfg for an Ubuntu VM, or an AutoUnattend.xml for a Windows. These files tell the installer how to set-up the OS.  To expose them to the VM you can either:

* Mount a floppy (esp. for Windows).
* Access the files by HTTP. When provisioning starts up, all the files in your definition dir are available on http://${server.ip}:${server.port}/.

Typically you'll want to make sure you VMs has:

* SSH access (or similar).
* ACPI support for graceful shutdown (many minimal installs don't).

Tokens
---
The following tokens are recognised in some XML documents:

* ${vbox.name} - Then name of the guest OS.
* ${server.ip} - The IP of the host.
* ${server.port} - The port the web server is running on.
* ${vbox.additions} - The path the VirtualBox Guest Additions on the host OS.

Authentication
---
By default the username is "vbox" and the default password "123456".

Supported Host OS Types
---
* Mac OS-X
* Windows 7

Unlisted OSs should all work.

Supported Guest OS Types/Supplied Definitions
---
* CentOS_6_5
* UbuntuServer_12_10
* WindowsServer2008

Unlisted OSs may work.
32 Bit vs 64 Bit
===
Currently the definitions are all 32 bit. I _think_ you'll want to use the same bits on the guest as the host. It'll be faster.

If you want use 64 bit you typically need to:

- Ensure hardware virtualizaiton is enabled on the host (see (http://www.parallels.com/uk/products/novt))
- Append "_64" to the OS type, e.g. "RedHat_64".
- Enable IO ACPI (as a side-effect, it'll be much faster, if your host OS is 64 bit).
- Use a 64 ISO (note that Windows will install the appropriate kernel for you, but you cannot change it once it's installed).

To save time, a patch is provided that will detect the host arch and apply a patch to the guest, e.g.:

    <patches>
        <archPatch/>
    </patches>

The patch will make the appropriate changes and choose an ISO for you.

Patches
===
A patch is a way of modifying a definition. Typically a patch will take a base definition and add support for new features. An example would be installing Tomcat onto a server. Patches are applied after a definition in created, but before the machine is created (in fact, applying a patch after a machine is created will change its (signature)[signatures.md] and result in its rebuild.

There are two types of patch _predefined_, _user defined_ and _custom_.

Predefined Patches
---
Predefined patches can be listed with the Maven plugin:

    mvn vbox:list-predefined-patches

Typically a predefined patch has a name which is the concatenation of the template used to create it, two dashes, and a short description. E.g.:

    CentOS_6_5--tomcat6

To apply a patch you need to add it to your XML. For example, you can get it to create patches as follows:

    <execution>
        <id>create-definition</id>
        <goals><goal>create-definition</goal></goals>
        <configuration>
            <templateName>CentOS_6_5</templateName>
            <name>app1</name>
        </configuration>
    </execution>
    <execution>
        <id>patch-definition</id>
        <goals><goal>patch-definition</goal></goals>
        <configuration>
            <patches>
                <predefinedPatch>
                    <name>CentOS_6_5--tomcat6</name>
                </predefinedPatch>
            </patches>
        </configuration>
    </execution>

User Defined Patches
---
As pre-defined patches might not cover all cases you can also use user defined ones. The format is unified diff, so you can use diff to create the patch, e.g:

    $ diff -ruN app1 app2 > patches/user-defined.patch
    diff -ruN app1/Provisioning.xml app2/Provisioning.xml
    --- app1/Provisioning.xml	2013-02-03 14:54:29.000000000 +0000
    +++ app2/Provisioning.xml	2013-02-03 14:33:34.000000000 +0000
    @@ -22,6 +22,5 @@
         </Target>
         <Target name="cleanup">
             <Exec>vboxmanage storageattach ${vbox.name} --storagectl "Floppy Controller" --port 0 --device 0 --medium none</Exec>
    -        <PortForward hostport="10022" guestport="22"/>
         </Target>
     </Provisioning>

And XML to configure it.

    <patches>
        <userDefinedPatch>
            <file>src/vbox/patches/user-defined.patch</file>
            <!-- default level is 1 -->
            <level>1</level>
        </userDefinedPatch>
    </patches>

Note that patches are level 1 by default.

Custom Patches
---
You can create a custom patch if you want. This is an advanced topic. Simple implement com.alexecollins.vbox.core.patch.Patch and add that and the appropriate information to your POM. E.g.

    package com.alexecollins.vbox.maven.patch;

    import com.alexecollins.vbox.core.VBox;
    import com.alexecollins.vbox.core.patch.Patch;

    public class NoopPatch implements Patch {
        public void apply(VBox box) throws Exception {
            // nop
        }

        public String getName() {
            return "NoopPatch";
        }
    }

And add the standard Maven implementation detail:

    <patches>
        <noopPatch implementation="com.alexecollins.vbox.patch.demo.NoopPatch"/>
    </patches>Known Issues
===
* US keyboard layouts only.
* Limited sub-set of vbox set-up supported.

Troubleshooting
===
* Sometimes VirtualBox gets in a state if VMs are not cleanly removed. Kill all VBox processes you can see.
* Host and guest firewalls can prevent OSs getting web resources. Disable the firewall.

References
===
* [VBoxManage](http://www.virtualbox.org/manual/ch08.html)
* [Ubuntu/Debian preseed.cfg](https://help.ubuntu.com/12.10/installation-guide/i386/preseed-using.html)
* [VeeWee](https://github.com/jedi4ever/veewee)
* [Oracle blog on VirtualBox networking](https://blogs.oracle.com/fatbloke/entry/networking_in_virtualbox1)
* [Enforcer Plugin Custom Rule](http://maven.apache.org/enforcer/enforcer-api/writing-a-custom-rule.html)
* [Patch Format](http://www.markusbe.com/2009/12/how-to-read-a-patch-or-diff-and-understand-its-structure-to-apply-it-manually/#how-to-read-a-unified-diff)

