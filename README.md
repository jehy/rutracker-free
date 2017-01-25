#Rutracker-Free

[![Build Status](https://travis-ci.org/jehy/rutracker-free.svg?branch=master)](https://travis-ci.org/jehy/rutracker-free)
[![Github All Releases](https://img.shields.io/github/downloads/jehy/rutracker-free/total.svg)](https://github.com/jehy/rutracker-free/releases)
[![GitHub license](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://raw.githubusercontent.com/jehy/rutracker-free/master/License.md)

Android thin client for rutracker.org.
Version 9.0 implemented TOR
(using my [own library distribution](https://github.com/jehy/Tor-Onion-Proxy-Library))
instead of Google Compresssion Proxy at least.
It is a bit slow but should be usable from any location.

Rutracker-free on different resources:

* [4pda](http://4pda.ru/forum/index.php?showtopic=733085)
* [habrahabr - google compression proxy](https://habrahabr.ru/post/279267/)
* [habrahabr - tor](https://habrahabr.ru/post/313030/)
* [rutracker](http://rutracker.org/forum/viewtopic.php?t=5191131)

Old branch using Google compression proxy can be found
 [here.](https://github.com/jehy/rutracker-free/tree/old/GCP)

#Requirements
To build project, you will need:
* Android Studio 2.1+

#Build instructions
* If you are making a **release** build, add your own [fabric](https://fabric.io)
key to app/fabric.properties file like this:
```
#Contains API Secret used to validate your application. Commit to internal source control; avoid making secret public.
#Tue Jan 10 11:18:34 MSK 2017
apiSecret=cc4ffaxxxx0b91ax9fab11338d438xxxxe6f2f824xd4b60bbdxxxxa788bc629
apiKey=f11xxx5261b4f4a1e4ecxx2493b41xxxx58a59
```
