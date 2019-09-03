# rolling-update
![CircleCi](https://img.shields.io/circleci/project/BrunoBonacci/rolling-update.svg) ![last-commit](https://img.shields.io/github/last-commit/BrunoBonacci/rolling-update.svg) [![Dependencies Status](https://jarkeeper.com/com.brunobonacci/rolling-update/status.svg)](https://jarkeeper.com/BrunoBonacci/rolling-update)

A command line tool for automated rolling update of auto-scaling groups.

## Installation

  * Install command line tool via [Homebrew](https://brew.sh/)
  ``` bash
  brew tap BrunoBonacci/lazy-tools
  brew install rolling-update

  # to update once installed
  # brew update && brew upgrade rolling-update
  ```

  * Otherwise use the Manual installation
  ``` bash
  mkdir -p ~/bin
  wget https://github.com/BrunoBonacci/rolling-update/releases/download/0.3.1/rolling-update -O ~/bin/rolling-update
  chmod +x ~/bin/rolling-update
  export PATH=~/bin:$PATH
  ```

## Usage

```
NAME

    rolling-update - A command line tool for automated rolling update
         of auto-scaling groups in cloud environments.


SYNOPSIS

    rolling-update [global-options] [TARGET...]

DESCRIPTION

    rolling-update is a tool which allows you to automate rolling
    restart of cloud entities such as auto-scaling groups on AWS.

GLOBAL-OPTIONS

    -h, --help
        It displays this page.


    -v, --version
        It shows the tool's version.


    --dryrun, --dry-run, --show-plan
        Just print the plan without performing any step.

    -s <strategy>, --strategy <strategy>
        The name of the strategy used for the rolling update. See
        STRATEGIES section below.

    --grace-period <time-in-seconds>  (default: 60s)
        The time to wait after the new the new instance comes to live
        before starting terminating the next one.
        This time is useful for stateful services such as databases
        to allow the new joined instance to synch up with the rest
        of the cluster. The amount of time required depends on the
        specific of the application and the amount of data to transfer.
        Values are in seconds, a suffix can be added to specify the
        time unit such as: `s` for seconds, `m` minutes, `h` hours.

TARGET

    The auto-scaling group (or groups) which need to be restarted.

    By name: web-app* or booking-asg
        It supports glob matching (? for any singe char, * for any
        number of any char). If provided it will be matched against
        the auto-scaling group names.

    By tag:  tag:<tag-name>=<tag-value>
        It matches the auto-scaling groups with have a tag which
        matches the given tag-name and tag-value pair.
        Examples:
           tag:Foo=bar
           tag:service=booking-service
           tag:GroupName='Any group value'

STRATEGIES

    Which strategy is used for the rolling update of the instances.
    Different use cases will require different strategies.  Currently,
    we support strategy the following strategies:
    The DEFAULT strategy is: terminate-and-wait.

    -s terminate, --strategy terminate, --strategy terminate-and-wait
        It terminates one instance from the selected auto-scaling
        group and it waits until the auto-scaling group stabilizes
        back with a new instance coming into service. After that there
        is a waiting grace period to allow the new instance to join
        the cluster and synchronize with the rest of the group (if
        necessary).

    -s reboot, --strategy reboot, --strategy reboot-and-wait
        It reboots one instance from the selected auto-scaling
        group and it waits until the auto-scaling group stabilizes
        back with a new instance coming into service. After that there
        is a waiting grace period to allow the new instance to join
        the cluster and synchronize with the rest of the group (if
        necessary).

```


Examples:

``` bash
rolling-update user-service* --show-plan
```
This command will only display which steps will be taken without
performing any action.


``` bash
# ATTENTION: instances will be TERMINATED
rolling-update user-service*
```
This command will select all the autoscaling groups which match the
following pattern `user-service*` and one instance at the time it will
terminate, wait for the ASG to create a new one (with potentially a
new config) and then move to the next instance, until completion.


``` bash
# ATTENTION: instances will be REBOOTED
rolling-update user-service* -s reboot
```
This command will select all the autoscaling groups which match the
following pattern `user-service*` and one instance at the time it will
reboot, wait for the ASG to create a new one (with potentially a
new config) and then move to the next instance, until completion.



## License

Copyright © 2019 Bruno Bonacci - Distributed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0)
