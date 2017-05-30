# -*- mode: ruby -*-
# vi: set ft=ruby :

# Copyright 2017. assertj-generator-gradle-plugin contributors.
# Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at

# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
# an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
# specific language governing permissions and limitations under the License.

# This vagrantfile is used for creating a ubuntu wily environment for testing changes on Linux

require 'dotenv'

Dotenv.load

Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/xenial64"

  cpu_count   = ENV.has_key?("CPUS") ? ENV["CPUS"] : 1
  memory_size = ENV.has_key?("MEMORY") ? ENV["MEMORY"] : 1024
  vram_size   = ENV.has_key?("VRAM") ? ENV["VRAM"] : 64

  with_gui    = ENV.has_key?("WITH_GUI") ? (ENV["WITH_GUI"]) : false
  with_gui    = with_gui == "true" || with_gui == "1"

  puts ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
  puts "Hardware Configuration:"
  puts "\tCPUS\t\t= #{cpu_count}"
  puts "\tRAM\t\t= #{memory_size}"
  puts "\tVRAM\t\t= #{vram_size}"
  puts "\tGUI\t\t= #{with_gui}"
  puts ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"

  config.vm.provider "virtualbox" do |vb|

    vb.cpus = cpu_count
    vb.memory = memory_size

    vb.customize ["modifyvm", :id, "--uartmode1", "file", "procsim-xenial.log" ]

    vb.gui = with_gui
    if with_gui
      vb.customize ["modifyvm", :id, "--vram", vram_size]
    end
  end

  # Install/update prerequisite software:

  config.vm.provision "shell", privileged: false,
    inline: "sed -i '1i force_color_prompt=yes' ~/.bashrc"

  config.vm.provision "shell", privileged: true, inline: <<-EOF
    apt-get update -qq
    apt-get dist-upgrade -y -q
  EOF

  config.vm.provision "shell", privileged: true, inline: <<-EOF
    echo "Installing oraclejdk8"
    add-apt-repository ppa:webupd8team/java  -y
    apt-get update -qq
    echo debconf shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
    echo debconf shared/accepted-oracle-license-v1-1 seen true   | /usr/bin/debconf-set-selections
    apt-get install --yes oracle-java8-installer
    yes "" | apt-get -f install

    echo "Installing git"
    apt-get install -q -y git
  EOF

  if ENV.has_key?("WORKSPACE")
    WORKSPACE = ENV["WORKSPACE"]
    config.vm.provision "shell", run: "always", inline: "echo 'Linking workspace: ~/workspace -> #{WORKSPACE}'"

    config.vm.synced_folder WORKSPACE, "/home/vagrant/workspace"
  end

  if with_gui
    config.vm.provision "shell", privileged: true, inline: <<-EOF
      apt-get install -y -q kdevelop konsole
      apt-get install -y -q ubuntu-desktop --no-install-recommends
      apt-get install -y -q unity evince unity-lens-files unity-lens-applications
    EOF
  end

  # Do this last since it's best to make sure everyone else is updated first
  config.vm.provision "shell", privileged: true, inline: "apt-get dist-upgrade -y ; apt-get autoremove --purge -y"

  if (Vagrant.has_plugin?('vagrant-reload'))
    config.vm.provision :reload
  end
end # end vagrant file